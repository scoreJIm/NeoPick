package com.neopick.domain.media;

public class UnsupportedFileTypeException extends RuntimeException {

    private final String contentType;

    public UnsupportedFileTypeException(String contentType) {
        super("Unsupported file type: " + contentType);
        this.contentType = contentType;
    }

    public String getContentType() {
        return contentType;
    }
}
