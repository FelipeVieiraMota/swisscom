package com.swisscom.services.auth_service.configuration;

import com.swisscom.services.auth_service.domain.entity.User;
import com.swisscom.services.auth_service.domain.enums.RoleName;
import com.swisscom.services.auth_service.domain.properties.BootstrapAdminProperties;
import com.swisscom.services.auth_service.repository.UserRepository;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile({"dev", "local", "test"})
@EnableConfigurationProperties(BootstrapAdminProperties.class)
public class AdminAccountInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final BootstrapAdminProperties properties;

    @Override
    @Transactional
    public void run(@Nonnull final ApplicationArguments arguments) {
        final var existingUser = userRepository.findByEmailIgnoreCase(properties.email());

        if (existingUser.isPresent()) {
            final User user = existingUser.get();
            if (user.getRoles().add(RoleName.ADMIN)) {
                userRepository.save(user);
                log.info("ADMIN role granted to the development bootstrap account: {}", user.getEmail());
            }
            return;
        }

        final User admin = userRepository.save(User.builder()
                .email(properties.email())
                .passwordHash(passwordEncoder.encode(properties.password()))
                .emailVerified(true)
                .active(true)
                .roles(new HashSet<>(Set.of(RoleName.USER, RoleName.ADMIN)))
                .build());

        log.info("Development bootstrap administrator created: {}", admin.getEmail());
    }
}
