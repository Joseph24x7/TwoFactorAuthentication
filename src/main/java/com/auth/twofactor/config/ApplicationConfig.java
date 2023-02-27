package com.auth.twofactor.config;

import java.util.Random;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import jakarta.validation.Validator;

@Configuration
public class ApplicationConfig{
	
	@Bean
	public Validator validator() {
	    return new LocalValidatorFactoryBean();
	}
	
	@Bean
	public Random random() {
	    return new Random();
	}
	
}