package com.auth.twofactor.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.auth.twofactor.entity.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailService {

	private final JavaMailSender javaMailSender;

	public void sendOtpEmail(User user) {
		
		SimpleMailMessage mailMessage = new SimpleMailMessage();
		mailMessage.setTo(user.getEmail());
		mailMessage.setSubject("OTP for Login");
		mailMessage.setFrom("Book My Gift Team <"+ user.getEmail() +">");
		mailMessage.setText("Dear " + user.getFullName() + ",\n\n Your OTP for login is: " + user.getTwoFaCode() + ".\n\n"
				+ "Please enter this OTP to proceed with your login.\n\n" + "Thank you for choosing us.\n\n"
				+ "Warm Regards,\n" + "Book My Gift Team");
		
		javaMailSender.send(mailMessage);
	}
	

}
