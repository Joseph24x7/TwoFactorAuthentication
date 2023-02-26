package com.auth.twofactor.service;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.AuthenticationManager;
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
import com.auth.twofactor.utils.CommonUtils;
import com.auth.twofactor.utils.TokenGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

	private final UserRepository repository;

	private final PasswordEncoder passwordEncoder;
	private final TokenGenerator jwtService;
	private final AuthenticationManager authenticationManager;

	private final CommonUtils commonUtils;

	private final RabbitTemplate rabbitTemplate;
	private final KafkaTemplate<String, User> kafkaTemplate;

	private final ObjectMapper objectMapper;
	private final Random random;

	@Transactional
	@SneakyThrows({ JsonProcessingException.class })
	public AuthResponse register(AuthRequest authRequest) {

		final String username = authRequest.getEmail().substring(0, authRequest.getEmail().indexOf('@'));

		repository.findByUsername(username).ifPresent(u -> {
			throw new ServiceException(ErrorEnums.EMAIL_ALREADY_REGISTERED);
		});

		var user = User.builder().username(username).password(passwordEncoder.encode(authRequest.getPassword()))
				.email(authRequest.getEmail()).role(Role.CUSTOMER).fullName(authRequest.getFullname())
				.twoFaCode("B-" + (random.nextInt(900000) + 100000))
				.twoFaExpiry(ZonedDateTime.now().plusMinutes(10).format(DateTimeFormatter.ISO_ZONED_DATE_TIME)).build();

		commonUtils.validate(user);

		rabbitTemplate.convertAndSend("directExchange", "sendOtpRoutingKey", objectMapper.writeValueAsString(user));

		var jwtToken = jwtService.generateToken(user);

		repository.save(user);

		return AuthResponse.builder().token(jwtToken).build();

	}

	@Transactional
	@SneakyThrows({ JsonProcessingException.class })
	public AuthResponse authenticate(AuthRequest authRequest, boolean isVerification) {

		final String username = authRequest.getEmail().substring(0, authRequest.getEmail().indexOf('@'));

		String jwtToken = null;

		authenticationManager
				.authenticate(new UsernamePasswordAuthenticationToken(username, authRequest.getPassword()));

		var user = repository.findByUsername(username)
				.orElseThrow(() -> new ServiceException(ErrorEnums.INVALID_CREDENTIALS));

		if (!isVerification) {

			user.toBuilder().twoFaCode("B-" + (random.nextInt(900000) + 100000))
					.twoFaExpiry(ZonedDateTime.now().plusMinutes(10).format(DateTimeFormatter.ISO_ZONED_DATE_TIME))
					.build();

			rabbitTemplate.convertAndSend("directExchange", "sendOtpRoutingKey", objectMapper.writeValueAsString(user));

			repository.save(user);

			jwtToken = jwtService.generateToken(user);

		}

		return AuthResponse.builder().token(jwtToken).user(user).authenticationStatus(AuthenticationStatus.SUCCESS)
				.build();

	}

	public AuthResponse verify(VerifyRequest verifyRequest) {

		CompletableFuture<AuthResponse> completableFuture = new CompletableFuture<>();

		// runAsync is used to produce a result asynchronously
		CompletableFuture<Void> authCheckFuture = CompletableFuture.runAsync(() -> {
			AuthResponse authResponse = authenticate(verifyRequest, true);
			if (authResponse.getAuthenticationStatus().equals(AuthenticationStatus.SUCCESS)
					&& authResponse.getUser().getTwoFaCode().equalsIgnoreCase(verifyRequest.getTwoFaCode())) {
				completableFuture.complete(authResponse);
			} else {
				completableFuture.completeExceptionally(new ServiceException(ErrorEnums.INVALID_2FA_CODE));
			}
		});

		try {
			
			CompletableFuture.allOf(authCheckFuture).get();
			AuthResponse authResponse = completableFuture.get();

			authCheckFuture.thenApplyAsync(result -> {
				try {
					return kafkaTemplate.send("mytopic", authResponse.getUser()).get();
				} catch (InterruptedException | ExecutionException e) {
					Thread.currentThread().interrupt();
					throw new ServiceException(ErrorEnums.INTERNAL_SERVER_ERROR);
				}
			}).exceptionally(ex -> {
				completableFuture.completeExceptionally(ex);
				throw new ServiceException(ErrorEnums.INTERNAL_SERVER_ERROR);
			});

			return authResponse;
		} catch (InterruptedException | ExecutionException e) {
			Thread.currentThread().interrupt();
			if (e.getCause() instanceof ServiceException ex) {
				ex = (ServiceException) e.getCause();
				throw new ServiceException(ex.getErrorEnums());
			}
			throw new ServiceException(ErrorEnums.INTERNAL_SERVER_ERROR);
		}
	}

}
