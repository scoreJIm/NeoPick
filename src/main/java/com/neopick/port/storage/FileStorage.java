package com.neopick.port.storage;

import java.io.InputStream;

public interface FileStorage {

    String upload(String key, InputStream inputStream, String contentType, long contentLength);

    void delete(String key);

    String generatePresignedUrl(String key, long expirationSeconds);
}
