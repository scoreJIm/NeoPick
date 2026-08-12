package com.neopick.adapter.web.controller;

import com.neopick.application.service.FileValidationService;
import com.neopick.infrastructure.config.NeopickProperties;
import com.neopick.infrastructure.metrics.BusinessMetrics;
import com.neopick.port.security.SecurityContext;
import com.neopick.port.storage.FileStorage;
import com.neopick.port.storage.PresignedUrlResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MediaController.class)
@DisplayName("Media API Integration Tests")
class MediaControllerIT {

    @Autowired private MockMvc mockMvc;

    @MockBean private FileStorage fileStorage;
    @MockBean private FileValidationService fileValidationService;
    @MockBean private SecurityContext securityContext;
    @MockBean private BusinessMetrics businessMetrics;
    @MockBean private NeopickProperties properties;

    private static final String USER_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String TEST_FILE_KEY = "avatar/" + USER_ID + "/test-uuid.jpg";
    private static final String TEST_UPLOAD_URL = "https://s3.amazonaws.com/test-bucket/" + TEST_FILE_KEY
            + "?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=test";
    private static final String TEST_CDN_URL = "https://cdn.neopick.test/" + TEST_FILE_KEY;
    private static final String TEST_DOWNLOAD_URL = "https://s3.amazonaws.com/test-bucket/" + TEST_FILE_KEY
            + "?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=download";

    @BeforeEach
    void setUp() {
        when(securityContext.requireCurrentUserId()).thenReturn(USER_ID);
        when(securityContext.getCurrentUserId()).thenReturn(Optional.of(USER_ID));

        when(properties.s3()).thenReturn(new NeopickProperties.S3Properties(
                "test-bucket", "us-east-1", 300, 3600));
        when(properties.cdn()).thenReturn(new NeopickProperties.CdnProperties("https://cdn.neopick.test"));
        when(properties.media()).thenReturn(new NeopickProperties.MediaProperties(
                5242880, java.util.List.of("image/jpeg", "image/png", "image/webp")));
    }

    @Nested
    @DisplayName("POST /api/v1/media/presign — Generate presigned upload URL")
    class Presign {

        @Test
        @DisplayName("should return presigned URL for valid image/jpeg request")
        void shouldGeneratePresignedUrlForJpeg() throws Exception {
            doNothing().when(fileValidationService).validateContentType("image/jpeg");
            doNothing().when(fileValidationService).validateFileSize(102400L);
            when(fileValidationService.generateFileKey(any(), anyString(), eq("image/jpeg")))
                    .thenReturn(TEST_FILE_KEY);
            when(fileValidationService.buildCdnUrl(TEST_FILE_KEY)).thenReturn(TEST_CDN_URL);

            Instant expiresAt = Instant.now().plus(Duration.ofSeconds(300));
            PresignedUrlResult presignedResult = new PresignedUrlResult(TEST_UPLOAD_URL, TEST_FILE_KEY, expiresAt);
            when(fileStorage.generatePresignedUploadUrl(eq(TEST_FILE_KEY), eq("image/jpeg"), any()))
                    .thenReturn(presignedResult);

            mockMvc.perform(post("/api/v1/media/presign")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"type": "AVATAR", "contentType": "image/jpeg", "fileSize": 102400}"""))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.uploadUrl").value(TEST_UPLOAD_URL))
                    .andExpect(jsonPath("$.data.fileKey").value(TEST_FILE_KEY))
                    .andExpect(jsonPath("$.data.cdnUrl").value(TEST_CDN_URL))
                    .andExpect(jsonPath("$.data.expiresAt").isNotEmpty());
        }

        @Test
        @DisplayName("should return presigned URL for image/png")
        void shouldGeneratePresignedUrlForPng() throws Exception {
            String pngKey = "avatar/" + USER_ID + "/test-png.png";
            doNothing().when(fileValidationService).validateContentType("image/png");
            doNothing().when(fileValidationService).validateFileSize(204800L);
            when(fileValidationService.generateFileKey(any(), anyString(), eq("image/png")))
                    .thenReturn(pngKey);
            when(fileValidationService.buildCdnUrl(pngKey)).thenReturn("https://cdn.neopick.test/" + pngKey);

            PresignedUrlResult result = new PresignedUrlResult(
                    "https://s3.amazonaws.com/test-bucket/" + pngKey + "?sig=test",
                    pngKey, Instant.now().plus(Duration.ofSeconds(300)));
            when(fileStorage.generatePresignedUploadUrl(eq(pngKey), eq("image/png"), any()))
                    .thenReturn(result);

            mockMvc.perform(post("/api/v1/media/presign")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"type": "CHAT_IMAGE", "contentType": "image/png", "fileSize": 204800}"""))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.fileKey").value(pngKey));
        }

        @Test
        @DisplayName("should return 400 for missing required fields")
        void shouldReturn400ForMissingFields() throws Exception {
            mockMvc.perform(post("/api/v1/media/presign")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"type": "AVATAR"}"""))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/media/{fileKey} — Redirect to presigned download URL")
    class Download {

        @Test
        @DisplayName("should redirect to presigned download URL")
        void shouldRedirectToDownloadUrl() throws Exception {
            when(fileStorage.generatePresignedDownloadUrl(eq(TEST_FILE_KEY), any()))
                    .thenReturn(TEST_DOWNLOAD_URL);

            mockMvc.perform(get("/api/v1/media/" + TEST_FILE_KEY))
                    .andExpect(status().isFound())
                    .andExpect(header().string("Location", TEST_DOWNLOAD_URL));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/media/{fileKey} — Delete media file")
    class Delete {

        @Test
        @DisplayName("should delete file from S3")
        void shouldDeleteFile() throws Exception {
            doNothing().when(fileStorage).deleteFile(TEST_FILE_KEY);

            mockMvc.perform(delete("/api/v1/media/" + TEST_FILE_KEY))
                    .andExpect(status().isOk());

            verify(fileStorage).deleteFile(TEST_FILE_KEY);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/media/upload-complete — Confirm upload completed")
    class UploadComplete {

        @Test
        @DisplayName("should acknowledge upload completion")
        void shouldAcknowledgeUploadComplete() throws Exception {
            mockMvc.perform(post("/api/v1/media/upload-complete")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"fileKey": "avatar/test/file.jpg"}"""))
                    .andExpect(status().isOk());
        }
    }
}
