package com.yowyob.crawling.domain.exception;

public class SourceUnavailableException extends CrawlException {
    public SourceUnavailableException(String message) {
        super(message);
    }

    public SourceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
