package com.neopick.adapter.web.controller;

import com.neopick.adapter.web.dto.common.ApiResponse;
import io.micrometer.core.annotation.Timed;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/media")
public class MediaController {

    @PostMapping("/upload")
    @Timed(value = "neopick.media.upload", description = "Upload media file")
    public ApiResponse<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        String key = "uploads/" + UUID.randomUUID() + "_" + file.getOriginalFilename();
        String url = "https://s3.amazonaws.com/neopick-media/" + key;
        return ApiResponse.success(Map.of("url", url));
    }
}
