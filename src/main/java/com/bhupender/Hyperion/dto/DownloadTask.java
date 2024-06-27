package com.bhupender.Hyperion.dto;

import com.bhupender.Hyperion.service.DownloadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

public class DownloadTask implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(DownloadTask.class);

    private final String url;
    private final ConcurrentHashMap<String, Integer> progressMap;
    private final int maxBytesPerSecond;
    private final long startByte;
    private final long endByte;
    private Future<?> future;
    private final AtomicBoolean paused = new AtomicBoolean(false);
    private final Object pauseLock = new Object();
    private final int chunkIndex;
    private final DownloadService downloadService;
    private final int chunkSize;
    private final int totalChunks;

    public DownloadTask(String url, ConcurrentHashMap<String, Integer> progressMap, int maxBytesPerSecond, DownloadService downloadService, long startByte, long endByte, int chunkIndex, int chunkSize, int totalChunks) {
        this.url = url;
        this.progressMap = progressMap;
        this.maxBytesPerSecond = maxBytesPerSecond;
        this.startByte = startByte;
        this.endByte = endByte;
        this.chunkIndex = chunkIndex;
        this.downloadService = downloadService;
        this.chunkSize = chunkSize;
        this.totalChunks = totalChunks;
    }

    public void setFuture(Future<?> future) {
        this.future = future;
    }

    public void pause() {
        paused.set(true);
    }

    public void resume() {
        paused.set(false);
        synchronized (pauseLock) {
            pauseLock.notifyAll();
        }
    }

    @Override
    public void run() {
        try {
            URL downloadUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) downloadUrl.openConnection();
            connection.setRequestProperty("Range", "bytes=" + startByte + "-" + endByte);
            int contentLength = connection.getContentLength();
            InputStream inputStream = connection.getInputStream();
            File file = new File("downloads/" + getFileNameFromUrl(url) + ".part" + chunkIndex);

            // Ensure the directory exists
            file.getParentFile().mkdirs();

            try (FileOutputStream outputStream = new FileOutputStream(file)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                int totalBytesRead = 0;
                long startTime = System.currentTimeMillis(), endTime, sleepTime;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    checkPaused();
                    outputStream.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                    progressMap.put(url + "-" + chunkIndex, (totalBytesRead * 100) / contentLength);

                    // Throttling logic
                    if (maxBytesPerSecond > 0) {
                        endTime = System.currentTimeMillis();
                        long elapsedTime = endTime - startTime;
                        if (elapsedTime < 1000) {
                            sleepTime = (1000 - elapsedTime);
                            Thread.sleep(sleepTime);
                        }
                        startTime = System.currentTimeMillis();
                    }

                    logger.info("Progress for chunk {}: {}", chunkIndex, progressMap.get(url + "-" + chunkIndex));
                }
            }

            inputStream.close();
            logger.info("Chunk {} downloaded for URL: {}", chunkIndex, url);

            // If all chunks are downloaded, merge them
            if (progressMap.values().stream().filter(progress -> progress == 100).count() == totalChunks) {
                downloadService.mergeChunks(url, totalChunks);
            }
        } catch (IOException | InterruptedException e) {
            logger.error("Error during download: ", e);
        }
    }

    private void checkPaused() {
        synchronized (pauseLock) {
            while (paused.get()) {
                try {
                    pauseLock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private String getFileNameFromUrl(String url) {
        return url.substring(url.lastIndexOf('/') + 1);
    }
}
