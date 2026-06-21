package com.swisscom.services.auth_service.configuration;

import com.swisscom.services.auth_service.domain.properties.JwtProperties;
import com.swisscom.services.auth_service.domain.properties.SecurityProperties;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.annotation.web.configurers.ExceptionHandlingConfigurer;
import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity
@EnableConfigurationProperties({SecurityProperties.class, JwtProperties.class})
public class SecurityConfig {

    private final SecurityProperties securityProperties;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(final HttpSecurity http) {
        return http
                .cors(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(sessionManagement())
                .authorizeHttpRequests(authorizeHttpRequests())
                .exceptionHandling(exceptions())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    private Customizer<ExceptionHandlingConfigurer<HttpSecurity>> exceptions() {
        return exceptions ->
                exceptions
                        .authenticationEntryPoint(
                                (request, response, exception) ->
                                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized")
                        )
                        .accessDeniedHandler(
                                (request, response, exception) ->
                                        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden")
                        );
    }

    private Customizer<AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry> authorizeHttpRequests() {
        return authorize ->
                authorize
                        .requestMatchers(publicPaths()).permitAll()
                        .anyRequest().authenticated();
    }

    private Customizer<SessionManagementConfigurer<HttpSecurity>> sessionManagement() {
        return session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
    }

    private String[] publicPaths() {
        return securityProperties.publicPaths().toArray(String[]::new);
    }
}
