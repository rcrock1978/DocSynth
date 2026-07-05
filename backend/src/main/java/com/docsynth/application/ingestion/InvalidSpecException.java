package com.docsynth.application.ingestion;

public class InvalidSpecException extends RuntimeException {
    public InvalidSpecException(String message) {
        super(message);
    }
}
