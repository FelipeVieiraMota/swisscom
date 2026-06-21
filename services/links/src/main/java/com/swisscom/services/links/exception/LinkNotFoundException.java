package com.swisscom.services.links.exception;

public class LinkNotFoundException extends RuntimeException {

    public LinkNotFoundException() {
        super("Link not found");
    }
}
