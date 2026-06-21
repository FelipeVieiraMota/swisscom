package com.swisscom.services.links;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class LinksApplication {

    public static void main(final String[] args) {
        SpringApplication.run(LinksApplication.class, args);
    }
}
