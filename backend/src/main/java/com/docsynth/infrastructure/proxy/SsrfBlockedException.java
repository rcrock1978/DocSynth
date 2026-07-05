package com.docsynth.infrastructure.proxy;

public class SsrfBlockedException extends RuntimeException {
    public SsrfBlockedException(String message) {
        super(message);
    }
}
