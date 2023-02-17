
package com.auth.twofactor.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.auth.twofactor.reqresp.AuthRequest;
import com.auth.twofactor.reqresp.AuthResponse;
import com.auth.twofactor.service.AuthenticationService;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@OpenAPIDefinition(info = @Info(title = "Authentication Management", version = "0.0.1"))
@RequiredArgsConstructor
@RefreshScope
public class AuthenticationController {

	@Value("${mail.host}")
	private String host;

	@Value("${mail.port}")
	private String port;

	private final AuthenticationService authenticationService;
	private final ObservationRegistry observationRegistry;

	@PostMapping("/register")
	public AuthResponse register(@RequestBody @Valid AuthRequest authRequest, HttpServletRequest request) {

		System.out.println("smtp" + host);
		System.out.println("smtp" + port);

		return Observation.createNotStarted(request.getRequestURI().substring(1), observationRegistry)
				.observe(() -> authenticationService.register(authRequest));

	}

	@PostMapping("/authenticate")
	public AuthResponse authenticate(@RequestBody AuthRequest authRequest, HttpServletRequest request) {

		return Observation.createNotStarted(request.getRequestURI().substring(1), observationRegistry)
				.observe(() -> authenticationService.authenticate(authRequest));

	}

}
