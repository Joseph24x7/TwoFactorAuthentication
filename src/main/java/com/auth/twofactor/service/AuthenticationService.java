package com.auth.twofactor.service;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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
	
	private final ObjectMapper objectMapper;

	@Transactional
	@SneakyThrows({JsonProcessingException.class, AmqpException.class})
	public AuthResponse register(AuthRequest authInfo) {

		repository.findByUsernameAndEmail(authInfo.getUsername(), authInfo.getEmail()).ifPresent(u -> {
			throw new ServiceException(ErrorEnums.USER_ALREADY_REGISTERED);
		});

		var user = User.builder().username(authInfo.getUsername())
				.password(passwordEncoder.encode(authInfo.getPassword())).email(authInfo.getEmail()).role(Role.CUSTOMER)
				.fullName(authInfo.getFullname()).build();

		commonUtils.validate(user);

		repository.save(user);

		var jwtToken = jwtService.generateToken(user);
		
		rabbitTemplate.convertAndSend("directExchange", "sendOtpRoutingKey", objectMapper.writeValueAsString(user));

		return AuthResponse.builder().token(jwtToken).build();

	}

	public AuthResponse authenticate(AuthRequest authInfo) {

		authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(authInfo.getUsername(), authInfo.getPassword()));

		var user = repository.findByUsername(authInfo.getUsername()).orElseThrow(() -> new ServiceException(ErrorEnums.UNAUTHORIZED));

		var jwtToken = jwtService.generateToken(user);

		return AuthResponse.builder().token(jwtToken).build();

	}

}
