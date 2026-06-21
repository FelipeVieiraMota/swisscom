package com.swisscom.services.links.exception;

public class ShortCodeGenerationException extends RuntimeException {

    public ShortCodeGenerationException() {
        super("Could not generate a unique short code");
    }
}
