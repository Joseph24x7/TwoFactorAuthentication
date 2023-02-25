package com.auth.twofactor.exception;

import org.springframework.http.HttpStatus;

public enum ErrorEnums {

	EMAIL_ALREADY_REGISTERED(HttpStatus.BAD_REQUEST, "EMAIL_ALREADY_REGISTERED",
			"The email address is already registered. Please use a different email address."),
	INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS",
			"Invalid email or password. Please check your credentials and try again."),
	INVALID_2FA_CODE(HttpStatus.UNAUTHORIZED, "INVALID_2FA_CODE",
			"Invalid Two Factor Authentication code. Please enter a valid code and try again."),
	AUTHORIZATION_REQUIRED(HttpStatus.UNAUTHORIZED, "AUTHORIZATION_REQUIRED",
			"Authorization token is required. Please provide a valid token to proceed."),
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR",
			"Internal Server Error. Something went wrong on our end. Please try again later.");

	HttpStatus httpStatus;
	String errorCode;
	String errorDescription;

	private ErrorEnums(HttpStatus httpStatus, String errorCode, String errorDescription) {
		this.httpStatus = httpStatus;
		this.errorCode = errorCode;
		this.errorDescription = errorDescription;
	}

	public HttpStatus getHttpStatus() {
		return httpStatus;
	}

	public String getErrorCode() {
		return errorCode;
	}

	public String getErrorDescription() {
		return errorDescription;
	}

}
