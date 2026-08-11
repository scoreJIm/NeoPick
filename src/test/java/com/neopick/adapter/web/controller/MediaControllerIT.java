package com.neopick.adapter.web.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MediaController.class)
@DisplayName("Media API Integration Tests")
class MediaControllerIT {

    @Autowired private MockMvc mockMvc;

    @Test
    @DisplayName("POST /api/v1/media/upload should return file URL")
    void shouldUploadFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.jpg", MediaType.IMAGE_JPEG_VALUE,
                "fake-image-content".getBytes());

        mockMvc.perform(multipart("/api/v1/media/upload")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.url", containsString("https://s3.amazonaws.com")));
    }
}
