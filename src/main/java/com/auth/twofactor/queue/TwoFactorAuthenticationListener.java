package com.auth.twofactor.queue;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import com.auth.twofactor.entity.User;
import com.auth.twofactor.service.EmailService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TwoFactorAuthenticationListener {

	private final EmailService emailService;

	@RabbitListener(queues = "sendOtpQueue", containerFactory = "rabbitListenerContainerFactory")
	public void handleSendOtpEmail(User user) {
		emailService.sendOtpEmail(user);
	}

}
