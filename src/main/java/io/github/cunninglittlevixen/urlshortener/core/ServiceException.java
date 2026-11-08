package io.github.cunninglittlevixen.urlshortener.core;

public class ServiceException extends RuntimeException {

    public ServiceException(String message) {
        super(message);
    }
}
