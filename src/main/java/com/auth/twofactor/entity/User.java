package com.auth.twofactor.entity;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

@Entity
@Table(name = "USERS", schema = "myapp")
@Builder
@Getter
public class User implements UserDetails {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "USER_ID", nullable = false)
	private Long userId;

	@Column(name = "USERNAME", nullable = false)
	@NotBlank(message = "Username is required")
	private String username;

	@Column(name = "PASSWORD", nullable = false)
	@NotBlank(message = "Password is required")
	private String password;
	
	@Column(name = "EMAIL", nullable = false)
	@NotNull(message = "Email is required")
	private String email;
	
	@Column(name = "FULL_NAME", nullable = false)
	@NotBlank(message = "Full name is required")
	private String fullName;

	@Column(name = "ROLE", nullable = false)
	@NotNull(message = "Role is required")
	@Enumerated(EnumType.STRING)
	private Role role;

	@Column(name = "TWO_FA_CODE")
	private String twoFaCode;

	@Column(name = "TWO_FA_EXPIRY")
	private ZonedDateTime twoFaExpiry;

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority(role.name()));
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

}
