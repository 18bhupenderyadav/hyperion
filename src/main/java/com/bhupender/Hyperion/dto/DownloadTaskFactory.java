package com.bhupender.Hyperion.dto;

import com.bhupender.Hyperion.service.DownloadService;

import java.util.concurrent.ConcurrentHashMap;

public interface DownloadTaskFactory {
    DownloadTask create(String url, ConcurrentHashMap<String, Integer> progressMap, int maxBytesPerSecond, DownloadService downloadService, long startByte, long endByte, int chunkIndex, int chunkSize, int totalChunks);
}
