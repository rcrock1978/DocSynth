package com.docsynth.infrastructure.proxy;

public class TokenMismatchException extends RuntimeException {
    public TokenMismatchException(String message) {
        super(message);
    }
}
