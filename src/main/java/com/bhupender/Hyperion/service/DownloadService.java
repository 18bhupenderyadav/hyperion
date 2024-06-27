package com.bhupender.Hyperion.service;

import com.bhupender.Hyperion.dto.DownloadTask;
import com.bhupender.Hyperion.dto.DownloadTaskFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.*;

@Service
public class DownloadService {

    private static final Logger logger = LoggerFactory.getLogger(DownloadService.class);

    private final ConcurrentHashMap<String, Integer> progressMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, DownloadTask> downloadTasks = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final DownloadTaskFactory downloadTaskFactory;

    @Value("${download.maxBytesPerSecond:0}")
    private int defaultMaxBytesPerSecond;

    @Value("${download.chunkSize:1048576}") // Default chunk size is 1MB
    private int chunkSize;

    public DownloadService(DownloadTaskFactory downloadTaskFactory) {
        this.downloadTaskFactory = downloadTaskFactory;
    }

    public void downloadFile(String url, int maxBytesPerSecond) {
        try {
            int speedLimit = (maxBytesPerSecond > 0) ? maxBytesPerSecond : defaultMaxBytesPerSecond;
            long contentLength = getContentLength(url); // Method to get the content length of the file
            logger.info("Content length for {} is {}", url, contentLength);
            logger.info("Chunk size is {}", chunkSize);

            if (contentLength <= 0 || chunkSize <= 0) {
                logger.error("Invalid content length {} or chunk size {}", contentLength, chunkSize);
                throw new IllegalArgumentException("Invalid content length or chunk size");
            }

            int numberOfChunks = (int) Math.ceil((double) contentLength / chunkSize);
            logger.info("Number of chunks for {} is {}", url, numberOfChunks);

            // Add a reasonable limit to prevent excessive chunk creation
            if (numberOfChunks > 1000) {
                throw new IllegalArgumentException("Too many chunks: " + numberOfChunks);
            }

            for (int i = 0; i < numberOfChunks; i++) {
                long startByte = i * chunkSize;
                long endByte = Math.min(startByte + chunkSize - 1, contentLength - 1);

                DownloadTask task = downloadTaskFactory.create(url, progressMap, speedLimit, this, startByte, endByte, i, chunkSize, numberOfChunks);
                Future<?> future = executor.submit(task);
                task.setFuture(future);
                downloadTasks.put(url + "-" + i, task);
                logger.info("Submitted task for chunk {}", i);
            }
            logger.info("Download started for URL: {}", url);
        } catch (Exception e) {
            logger.error("Failed to start download for URL: {}", url, e);
            throw new DownloadException("Failed to start download", e);
        }
    }

    public Integer getProgress(String url) {
        return progressMap.getOrDefault(url, 0);
    }

    public void pauseDownload(String url) {
        downloadTasks.forEach((key, task) -> {
            if (key.startsWith(url)) {
                task.pause();
                logger.info("Download paused for URL: {}", key);
            }
        });
    }

    public void resumeDownload(String url) {
        downloadTasks.forEach((key, task) -> {
            if (key.startsWith(url)) {
                task.resume();
                logger.info("Download resumed for URL: {}", key);
            }
        });
    }

    public void mergeChunks(String url, int numberOfChunks) {
        File mergedFile = new File("downloads/" + getFileNameFromUrl(url));
        try (FileOutputStream fos = new FileOutputStream(mergedFile)) {
            for (int i = 0; i < numberOfChunks; i++) {
                File chunkFile = new File("downloads/" + getFileNameFromUrl(url) + ".part" + i);
                if (chunkFile.exists()) {
                    Files.copy(chunkFile.toPath(), fos);
                    logger.info("Deleting chunk file {}", chunkFile.getName());
                    Files.delete(chunkFile.toPath());
                } else {
                    logger.warn("Chunk file {} does not exist and cannot be merged", chunkFile.getName());
                }
            }
            logger.info("Chunks merged into final file for URL: {}", url);
        } catch (IOException e) {
            logger.error("Failed to merge chunks for URL: {}", url, e);
        }
    }

    private long getContentLength(String url) throws IOException {
        // Implementation to get content length from URL
        // This method can use HttpURLConnection or any other method to determine the content length
        // Here we are just simulating with a fixed value for testing purposes
        return 10000000L; // Example length, 2MB
    }

    private String getFileNameFromUrl(String url) {
        return url.substring(url.lastIndexOf('/') + 1);
    }
}
