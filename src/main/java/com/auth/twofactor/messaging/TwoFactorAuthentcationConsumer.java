package com.auth.twofactor.messaging;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.auth.twofactor.entity.User;
import com.auth.twofactor.service.EmailService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TwoFactorAuthentcationConsumer {

	private final EmailService emailService;

	@KafkaListener(topics = "mytopic", groupId = "${spring.kafka.consumer.group-id}")
	public void consume(User verificationRecord) {
		emailService.sendVerificationSuccessEmail(verificationRecord);
	}

}
