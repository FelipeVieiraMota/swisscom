package com.swisscom.services.links.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class ShortCodeGenerator {

    private static final char[] ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
    private static final int CODE_LENGTH = 8;

    private final SecureRandom random = new SecureRandom();

    public String generate() {
        final char[] code = new char[CODE_LENGTH];
        for (int index = 0; index < code.length; index++) {
            code[index] = ALPHABET[random.nextInt(ALPHABET.length)];
        }
        return new String(code);
    }
}
