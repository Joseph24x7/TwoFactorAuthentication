package com.auth.twofactor.reqresp;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;

@Getter
public class VerifyRequest extends AuthRequest{
	
	@NotBlank(message = "twoFaCode is required")
	@Pattern(regexp = "^B-\\d{6}$", message = "twoFaCode should be in the format 'B-XXXXXX', where X represents a 6-digit random number")
    private String twoFaCode;

}
