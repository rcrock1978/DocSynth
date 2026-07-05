package com.docsynth.infrastructure.security;

public class RbacDeniedException extends RuntimeException {
    public RbacDeniedException(String message) {
        super(message);
    }
}
