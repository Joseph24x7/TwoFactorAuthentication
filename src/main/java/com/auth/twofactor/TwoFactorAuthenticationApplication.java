package com.auth.twofactor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class TwoFactorAuthenticationApplication {

	public static void main(String[] args) {
		SpringApplication.run(TwoFactorAuthenticationApplication.class, args);
	}

}
