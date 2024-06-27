package com.bhupender.Hyperion.controller;

import com.bhupender.Hyperion.service.DownloadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DownloadController.class)
public class DownloadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DownloadService downloadService;

    @BeforeEach
    public void setUp() {
        // Reset the mock between tests
        reset(downloadService);
    }

    @Test
    @WithMockUser
    public void testPauseDownload() throws Exception {
        doNothing().when(downloadService).pauseDownload(anyString());

        mockMvc.perform(post("/downloads/pause")
                        .param("url", "http://212.183.159.230/100MB.zip")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())) // Add CSRF token
                .andExpect(status().isOk())
                .andExpect(content().string("Download paused for URL: http://212.183.159.230/100MB.zip"));

        verify(downloadService, times(1)).pauseDownload(anyString());
    }

    @Test
    @WithMockUser
    public void testResumeDownload() throws Exception {
        doNothing().when(downloadService).resumeDownload(anyString());

        mockMvc.perform(post("/downloads/resume")
                        .param("url", "http://212.183.159.230/100MB.zip")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())) // Add CSRF token
                .andExpect(status().isOk())
                .andExpect(content().string("Download resumed for URL: http://212.183.159.230/100MB.zip"));

        verify(downloadService, times(1)).resumeDownload(anyString());
    }
}
