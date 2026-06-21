package com.swisscom.services.auth_service;

import com.swisscom.services.auth_service.domain.security.JwtPrincipal;
import com.swisscom.services.auth_service.domain.entity.User;
import com.swisscom.services.auth_service.domain.enums.RoleName;
import com.swisscom.services.auth_service.repository.UserRepository;
import com.swisscom.services.auth_service.service.interfaces.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import com.jayway.jsonpath.JsonPath;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthenticatorApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private UserDetailsService userDetailsService;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void contextLoads() {
	}

	@Test
	@Transactional
	void shouldCreateDevelopmentAdministrator() {
		final var admin = userRepository.findByEmailIgnoreCase("admin@test.local").orElseThrow();

		assertThat(admin.isActive()).isTrue();
		assertThat(admin.isEmailVerified()).isTrue();
		assertThat(admin.getRoles()).containsExactlyInAnyOrder(RoleName.USER, RoleName.ADMIN);
		assertThat(passwordEncoder.matches("Test-Admin-Password-2026!", admin.getPasswordHash())).isTrue();
	}

	@Test
	void shouldExposeOpenApiWithoutAuthentication() throws Exception {
		mockMvc.perform(get("/api/v1/auth/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.servers[0].url").value("/"));
	}

	@Test
	void shouldRejectProtectedEndpointWithoutAuthentication() throws Exception {
		mockMvc.perform(get("/api/v1/auth/me"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void shouldForbidRegistrationWithoutAdminRole() throws Exception {
		final String userToken = jwtService.generateAccessToken(new JwtPrincipal(
				UUID.randomUUID(),
				"user@example.com",
				Set.of("USER")
		));

		mockMvc.perform(post("/api/v1/auth/register")
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"email":"another@example.com","password":"correct horse battery staple"}
							"""))
				.andExpect(status().isForbidden());
	}

	@Test
	void shouldGenerateAndParseAccessToken() {
		final var expectedPrincipal = new JwtPrincipal(
				UUID.randomUUID(),
				"felipe@example.com",
				Set.of("USER")
		);

		final var token = jwtService.generateAccessToken(expectedPrincipal);
		final var authentication = jwtService.parseAuthentication(token);

		assertThat(authentication.isAuthenticated()).isTrue();
		assertThat(authentication.getPrincipal()).isEqualTo(expectedPrincipal);
		assertThat(authentication.getAuthorities())
				.extracting("authority")
				.containsExactly("ROLE_USER");
	}

	@Test
	@Transactional
	void shouldLoadActiveUserFromDatabase() {
		final String encodedPassword = passwordEncoder.encode("correct horse battery staple");
		userRepository.saveAndFlush(User.builder()
				.email("Felipe@Example.com")
				.passwordHash(encodedPassword)
				.build());

		final var userDetails = userDetailsService.loadUserByUsername("felipe@example.com");

		assertThat(userDetails.getUsername()).isEqualTo("felipe@example.com");
		assertThat(passwordEncoder.matches("correct horse battery staple", userDetails.getPassword())).isTrue();
		assertThat(userDetails.isEnabled()).isTrue();
		assertThat(userDetails.getAuthorities())
				.extracting("authority")
				.containsExactly("ROLE_USER");
	}

	@Test
	void shouldRegisterLoginAndAccessCurrentUser() throws Exception {
		final String email = "user-" + UUID.randomUUID() + "@example.com";
		final String password = "correct horse battery staple";
		final String adminToken = jwtService.generateAccessToken(new JwtPrincipal(
				UUID.randomUUID(),
				"admin@example.com",
				Set.of("ADMIN")
		));

		mockMvc.perform(post("/api/v1/auth/register")
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"email":"%s","password":"%s"}
							""".formatted(email, password)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.email").value(email))
				.andExpect(jsonPath("$.roles[0]").value("USER"));

		final var loginResult = mockMvc.perform(post("/api/v1/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"email":"%s","password":"%s"}
							""".formatted(email, password)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.expiresInSeconds").value(900))
				.andReturn();

		final String accessToken = JsonPath.read(
				loginResult.getResponse().getContentAsString(),
				"$.accessToken"
		);

		mockMvc.perform(get("/api/v1/auth/me")
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(email))
				.andExpect(jsonPath("$.roles[0]").value("USER"));
	}

}
