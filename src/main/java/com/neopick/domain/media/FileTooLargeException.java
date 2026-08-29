package com.neopick.domain.media;

public class FileTooLargeException extends RuntimeException {

    private final long fileSize;
    private final long maxSize;

    public FileTooLargeException(long fileSize, long maxSize) {
        super(String.format("File size %d exceeds maximum allowed size %d", fileSize, maxSize));
        this.fileSize = fileSize;
        this.maxSize = maxSize;
    }

    public long getFileSize() {
        return fileSize;
    }

    public long getMaxSize() {
        return maxSize;
    }
}
