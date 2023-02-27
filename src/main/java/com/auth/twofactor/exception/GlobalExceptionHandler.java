package com.auth.twofactor.exception;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.jsonwebtoken.MalformedJwtException;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

	private final ObservationRegistry observationRegistry;
	
	@ExceptionHandler(Exception.class)
	public ProblemDetail handleException(Exception exception, HttpServletRequest request) {
		return populateException(ErrorEnums.INTERNAL_SERVER_ERROR , request, exception.getMessage());
	}
	
	@ExceptionHandler(ConstraintViolationException.class)
	public ProblemDetail handleConstraintViolationException(ConstraintViolationException ex,
			HttpServletRequest request) {

		List<String> errors = new ArrayList<>();
		
		ex.getConstraintViolations().forEach(violation -> errors.add(violation.getMessage()));

		return populateException(ErrorEnums.BAD_REQUEST, request , List.copyOf(errors).toString());

	}
	
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ProblemDetail handleMethodArgumentNotValidException(MethodArgumentNotValidException validationEx,
			HttpServletRequest request) {

		List<String> errors = new ArrayList<>();
		
		validationEx.getBindingResult().getFieldErrors().forEach(error -> errors.add(error.getDefaultMessage()));

		return populateException(ErrorEnums.BAD_REQUEST, request , List.copyOf(errors).toString());
	}

	@ExceptionHandler(ServiceException.class)
	public ProblemDetail handleServiceException(ServiceException serviceException, HttpServletRequest request) {
		return populateException(serviceException.getErrorEnums(), request);
	}
	
	@ExceptionHandler({ BadCredentialsException.class, MalformedJwtException.class })
	public ProblemDetail handleCredentialsException(Exception exception, HttpServletRequest request) {
		return populateException(ErrorEnums.INVALID_CREDENTIALS, request);
	}
	
	private ProblemDetail populateException(ErrorEnums error, HttpServletRequest request) {
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(error.getHttpStatus(), error.getErrorDescription());
		problemDetail.setTitle(error.getErrorCode());
		return Observation.createNotStarted(request.getRequestURI().substring(1), observationRegistry).observe(() -> problemDetail);
	}
	
	private ProblemDetail populateException(ErrorEnums error, HttpServletRequest request , String errorDescription) {
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(error.getHttpStatus(), errorDescription);
		problemDetail.setTitle(error.getErrorCode());
		return Observation.createNotStarted(request.getRequestURI().substring(1), observationRegistry).observe(() -> problemDetail);
	}

}
