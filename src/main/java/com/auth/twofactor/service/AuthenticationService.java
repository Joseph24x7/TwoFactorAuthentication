package com.auth.twofactor.service;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.auth.twofactor.entity.Role;
import com.auth.twofactor.entity.User;
import com.auth.twofactor.exception.ErrorEnums;
import com.auth.twofactor.exception.ServiceException;
import com.auth.twofactor.repository.UserRepository;
import com.auth.twofactor.reqresp.AuthRequest;
import com.auth.twofactor.reqresp.AuthResponse;
import com.auth.twofactor.reqresp.AuthenticationStatus;
import com.auth.twofactor.reqresp.VerifyRequest;
import com.auth.twofactor.security.TokenUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthenticationService {

	private final UserRepository repository;
	private final PasswordEncoder passwordEncoder;
	private final TokenUtil tokenUtil;
	private final AuthenticationManager authenticationManager;
	private final RabbitTemplate rabbitTemplate;
	private final KafkaTemplate<String, User> kafkaTemplate;
	private final ObjectMapper objectMapper;
	private final Random random;
	private final Validator validator;

	public AuthResponse registerUser(AuthRequest authRequest) {

		String username = getUsernameFromEmail(authRequest.getEmail());

		checkIfUsernameExists(username);

		User user = createUser(authRequest, username);

		sendOtp(user);

		String jwtToken = tokenUtil.generateToken(user);

		return createAuthResponse(user, jwtToken, AuthenticationStatus.SUCCESS);
	}

	public AuthResponse authenticate(AuthRequest authRequest, boolean isVerification) {

		User user = validateUser(authRequest);

		if (!isVerification) {

			user.toBuilder().twoFaCode(generateTwoFaCode()).twoFaExpiry(getExpiryTimeForTwoFa()).build();

			sendOtp(user);

			repository.save(user);
		}

		String jwtToken = tokenUtil.generateToken(user);

		return createAuthResponse(user, jwtToken, AuthenticationStatus.SUCCESS);
	}

	public AuthResponse verify(VerifyRequest verifyRequest) {

		CompletableFuture<AuthResponse> completableFuture = new CompletableFuture<>();

		try {

			// runAsync is used to produce a result asynchronously
			CompletableFuture<Void> authCheckFuture = authenticateTwoFaAsync(completableFuture, verifyRequest);

			CompletableFuture.allOf(authCheckFuture).get();

			AuthResponse authResponse = completableFuture.get();

			sendSuccessNotification(authResponse, authCheckFuture);

			return authResponse;

		} catch (InterruptedException | ExecutionException e) {

			Thread.currentThread().interrupt();
			if (e.getCause() instanceof ServiceException ex) {
				throw new ServiceException(ex.getErrorEnums());
			}else if (e.getCause() instanceof BadCredentialsException ex) {
				throw ex;
			}

			throw new ServiceException(ErrorEnums.INTERNAL_SERVER_ERROR);
		}
	}

	private void sendSuccessNotification(AuthResponse authResponse, CompletableFuture<Void> authCheckFuture) {

		authCheckFuture.thenApplyAsync(result -> {
			try {
				return kafkaTemplate.send("mytopic", authResponse.getUser()).get();
			} catch (InterruptedException | ExecutionException e) {
				Thread.currentThread().interrupt();
				throw new ServiceException(ErrorEnums.INTERNAL_SERVER_ERROR);
			}
		});

	}

	private CompletableFuture<Void> authenticateTwoFaAsync(CompletableFuture<AuthResponse> completableFuture,
			VerifyRequest verifyRequest) {

		return CompletableFuture.runAsync(() -> {
			AuthResponse authResponse = authenticate(verifyRequest, true);
			if (authResponse.getAuthenticationStatus().equals(AuthenticationStatus.SUCCESS)
					&& authResponse.getUser().getTwoFaCode().equalsIgnoreCase(verifyRequest.getTwoFaCode())) {
				completableFuture.complete(authResponse);
			} else {
				completableFuture.completeExceptionally(new ServiceException(ErrorEnums.INVALID_2FA_CODE));
			}
		});

	}

	private User validateUser(AuthRequest authRequest) {

		String username = getUsernameFromEmail(authRequest.getEmail());
		
		authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, authRequest.getPassword()));

		return repository.findByUsername(username).orElseThrow(() -> new ServiceException(ErrorEnums.INVALID_CREDENTIALS));

	}

	private String getUsernameFromEmail(String email) {
		return email.substring(0, email.indexOf('@'));
	}

	private void checkIfUsernameExists(String username) {
		repository.findByUsername(username).ifPresent(u -> {
			throw new ServiceException(ErrorEnums.EMAIL_ALREADY_REGISTERED);
		});
	}

	private void validate(Object object) {
		Set<ConstraintViolation<Object>> violations = validator.validate(object);
		if (!violations.isEmpty()) {
			throw new ConstraintViolationException(violations);
		}
	}

	@SneakyThrows({ JsonProcessingException.class })
	private void sendOtp(User user) {
		rabbitTemplate.convertAndSend("directExchange", "sendOtpRoutingKey", objectMapper.writeValueAsString(user));
	}

	private String generateTwoFaCode() {
		return "B-" + (random.nextInt(900000) + 100000);
	}

	private String getExpiryTimeForTwoFa() {
		return ZonedDateTime.now().plusMinutes(10).format(DateTimeFormatter.ISO_ZONED_DATE_TIME);
	}

	private AuthResponse createAuthResponse(User user, String jwtToken, AuthenticationStatus status) {
		return AuthResponse.builder().token(jwtToken).user(user).authenticationStatus(status).build();
	}

	private User createUser(AuthRequest authRequest, String username) {

		User user = User.builder().username(username).password(passwordEncoder.encode(authRequest.getPassword()))
				.email(authRequest.getEmail()).role(Role.CUSTOMER).fullName(authRequest.getFullname())
				.twoFaCode(generateTwoFaCode()).twoFaExpiry(getExpiryTimeForTwoFa()).build();

		validate(user);
		repository.save(user);

		return user;
	}

}
