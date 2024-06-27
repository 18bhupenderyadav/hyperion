package com.bhupender.Hyperion.service;

import com.bhupender.Hyperion.dto.DownloadTask;
import com.bhupender.Hyperion.dto.DownloadTaskFactory;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.*;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class DownloadServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(DownloadServiceTest.class);

    private DownloadService downloadService;
    private DownloadTaskFactory downloadTaskFactory;
    private DownloadTask mockTask;
    private final String testUrl = "http://ipv4.download.thinkbroadband.com/10MB.zip"; // Example URL

    @BeforeEach
    public void setUp() throws Exception {
        downloadTaskFactory = mock(DownloadTaskFactory.class);
        mockTask = mock(DownloadTask.class);
        when(downloadTaskFactory.create(anyString(), any(), anyInt(), any(), anyLong(), anyLong(), anyInt())).thenReturn(mockTask);

        downloadService = spy(new DownloadService(downloadTaskFactory));

        // Use reflection to mock getContentLength method
        Method getContentLengthMethod = DownloadService.class.getDeclaredMethod("getContentLength", String.class);
        getContentLengthMethod.setAccessible(true);
        doReturn(2000000L).when(downloadService).getContentLength(anyString()); // 2MB for example

        // Use reflection to set chunkSize
        Field chunkSizeField = DownloadService.class.getDeclaredField("chunkSize");
        chunkSizeField.setAccessible(true);
        chunkSizeField.set(downloadService, 1048576); // 1MB

        // Use reflection to set executor
        Field executorField = DownloadService.class.getDeclaredField("executor");
        executorField.setAccessible(true);
        executorField.set(downloadService, mock(ExecutorService.class));
    }

    @Test
    public void testChunkedDownload() throws Exception {
        Future<?> mockFuture = mock(Future.class);
        ExecutorService executorService = (ExecutorService) getPrivateField(downloadService, "executor");
        when(executorService.submit(any(DownloadTask.class))).thenReturn((Future) mockFuture);

        downloadService.downloadFile(testUrl, 102400); // 100 KB/s

        verify(executorService, times(2)).submit(any(DownloadTask.class)); // For 2 chunks

        // Simulate progress advancement
        doAnswer(invocation -> {
            logger.info("Simulating progress update...");
            ConcurrentHashMap<String, Integer> progressMap = (ConcurrentHashMap<String, Integer>) getPrivateField(downloadService, "progressMap");
            progressMap.put(testUrl + "-0", 100); // Simulate complete progress for chunk 0
            return null;
        }).when(mockTask).run();

        // Directly invoke run to simulate task execution
        mockTask.run();

        // Use Awaitility to wait for progress to advance
        await().atMost(5, TimeUnit.SECONDS).until(() -> {
            Integer progress = downloadService.getProgress(testUrl + "-0");
            logger.info("Current progress for {}: {}", testUrl + "-0", progress);
            return progress != null && progress == 100;
        });

        assertTrue(downloadService.getProgress(testUrl + "-0") == 100, "Chunk should be completely downloaded");
    }

    private Object getPrivateField(Object object, String fieldName) throws Exception {
        Field field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(object);
    }
}
