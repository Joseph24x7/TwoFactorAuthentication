package com.auth.twofactor.service;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
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
	private final KafkaTemplate<String, VerifyRequest> kafkaTemplate;

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
	public AuthResponse authenticate(AuthRequest authRequest) {

		final String username = authRequest.getEmail().substring(0, authRequest.getEmail().indexOf('@'));

		authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, authRequest.getPassword()));

		var user = repository.findByUsername(username).orElseThrow(() -> new ServiceException(ErrorEnums.INVALID_CREDENTIALS));
		
		user.toBuilder().twoFaCode("B-" + (random.nextInt(900000) + 100000)).twoFaExpiry(ZonedDateTime.now().plusMinutes(10).format(DateTimeFormatter.ISO_ZONED_DATE_TIME)).build();

		rabbitTemplate.convertAndSend("directExchange", "sendOtpRoutingKey", objectMapper.writeValueAsString(user));
		
		var jwtToken = jwtService.generateToken(user);
		
		repository.save(user);

		return AuthResponse.builder().token(jwtToken).authenticationStatus(AuthenticationStatus.SUCCESS).build();

	}

	/*public AuthResponse verify(VerifyRequest verifyRequest) {
		
		final String username = verifyRequest.getEmail().substring(0, verifyRequest.getEmail().indexOf('@'));

		authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, verifyRequest.getPassword()));

		var user = repository.findByUsername(username).orElseThrow(() -> new ServiceException(ErrorEnums.INVALID_CREDENTIALS));
		
		if(user.getTwoFaCode().equalsIgnoreCase(verifyRequest.getTwoFaCode())) {
			
			var jwtToken = jwtService.generateToken(user);
			
			return AuthResponse.builder().token(jwtToken).authenticationStatus(AuthenticationStatus.SUCCESS).build();
			
		}else {
			
			throw new ServiceException(ErrorEnums.AUTHORIZATION_REQUIRED);
			
		}
	}*/
	
	public AuthResponse verify(VerifyRequest verifyRequest) {
	    CompletableFuture<AuthResponse> completableFuture = new CompletableFuture<>();
	    
	    CompletableFuture<SendResult<String, VerifyRequest>> completableFutureSendResult = CompletableFuture.supplyAsync(() -> {
	        try {
	            return kafkaTemplate.send("mytopic", verifyRequest).get();
	        } catch (InterruptedException | ExecutionException e) {
	            completableFuture.completeExceptionally(e);
	            Thread.currentThread().interrupt();
	            throw new ServiceException(ErrorEnums.INTERNAL_SERVER_ERROR);
	        }
	    });

	    completableFutureSendResult.thenAcceptAsync(result -> {
	        // Process the verification request asynchronously
	        final String username = verifyRequest.getEmail().substring(0, verifyRequest.getEmail().indexOf('@'));
	        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, verifyRequest.getPassword()));

	        var user = repository.findByUsername(username).orElseThrow(() -> new ServiceException(ErrorEnums.INVALID_CREDENTIALS));
	        
	        if(user.getTwoFaCode().equalsIgnoreCase(verifyRequest.getTwoFaCode())) {
	            var jwtToken = jwtService.generateToken(user);
	            AuthResponse authResponse = AuthResponse.builder().token(jwtToken).authenticationStatus(AuthenticationStatus.SUCCESS).build();
	            completableFuture.complete(authResponse);
	        } else {
	            completableFuture.completeExceptionally(new ServiceException(ErrorEnums.INVALID_2FA_CODE));
	        }
	    });

	    completableFutureSendResult.exceptionally(ex -> {
	        // handle the exception
	        completableFuture.completeExceptionally(ex);
	        return null;
	    });

	    try {
	        return completableFuture.get();
	    } catch (InterruptedException | ExecutionException e) {
	    	 Thread.currentThread().interrupt();
	        throw new ServiceException(ErrorEnums.INTERNAL_SERVER_ERROR);
	    }
	}







}
