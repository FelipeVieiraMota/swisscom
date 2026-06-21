package com.swisscom.services.links.exception;

public class InvalidLinkUrlException extends RuntimeException {

    public InvalidLinkUrlException() {
        super("Only absolute HTTP or HTTPS URLs are supported");
    }
}
