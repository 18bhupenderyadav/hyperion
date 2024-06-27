package com.bhupender.Hyperion.controller;

import com.bhupender.Hyperion.service.DownloadService;
import com.bhupender.Hyperion.service.DownloadException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/downloads")
public class DownloadController {

    @Autowired
    private DownloadService downloadService;

    @PostMapping("/download")
    public String download(@RequestParam String url, @RequestParam(required = false) Integer maxBytesPerSecond) {
        if (maxBytesPerSecond == null) {
            maxBytesPerSecond = 0;
        }
        System.out.println("We here to download");
        downloadService.downloadFile(url, maxBytesPerSecond);
        return "Download started for URL: " + url;
    }

    @GetMapping("/progress")
    public Integer getProgress(@RequestParam String url) {
        return downloadService.getProgress(url);
    }

    @PostMapping("/pause")
    public String pauseDownload(@RequestParam String url) {
        downloadService.pauseDownload(url);
        return "Download paused for URL: " + url;
    }

    @PostMapping("/resume")
    public String resumeDownload(@RequestParam String url) {
        downloadService.resumeDownload(url);
        return "Download resumed for URL: " + url;
    }

    @ExceptionHandler(DownloadException.class)
    public ResponseEntity<String> handleDownloadException(DownloadException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
    }
}
