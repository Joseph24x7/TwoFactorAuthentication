package com.auth.twofactor.reqresp;

import java.io.Serializable;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthRequest implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	@NotBlank(message = "Email is required")
    @Email(message = "Email must be in the format of example@domain.com")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min=8, max=20, message = "Password must be between 8 and 20 characters")
	@Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$", message = "Password must contain at least 1 uppercase letter, 1 lowercase letter, 1 number, 1 special character and must not contain any spaces")
    private String password;

    @NotBlank(message = "Full name is required")
    @Size(min=3, max=50, message = "Full name must be between 3 and 50 characters")
    private String fullname;
    
}
