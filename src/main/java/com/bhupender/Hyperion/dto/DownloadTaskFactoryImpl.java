package com.bhupender.Hyperion.dto;

import com.bhupender.Hyperion.service.DownloadService;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class DownloadTaskFactoryImpl implements DownloadTaskFactory {
    @Override
    public DownloadTask create(String url, ConcurrentHashMap<String, Integer> progressMap, int maxBytesPerSecond, DownloadService downloadService, long startByte, long endByte, int chunkIndex, int chunkSize, int totalChunks) {
        return new DownloadTask(url, progressMap, maxBytesPerSecond, downloadService, startByte, endByte, chunkIndex, chunkSize, totalChunks);
    }
}
