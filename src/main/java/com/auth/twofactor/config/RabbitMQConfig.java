package com.auth.twofactor.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.RequiredArgsConstructor;

@EnableRabbit
@Configuration
@RequiredArgsConstructor
public class RabbitMQConfig {
	
	private final ObjectMapper objectMapper;

	@Bean
	public Queue sendOtp() {
		return new Queue("sendOtpQueue", false);
	}

	@Bean
	public DirectExchange directExchange() {
		return new DirectExchange("directExchange");
	}

	@Bean
	public Binding sendOtpBinding(Queue sendOtp, DirectExchange directExchange) {
		return BindingBuilder.bind(sendOtp).to(directExchange).with("sendOtpRoutingKey");
	}

	@Bean
	public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
		final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
		rabbitTemplate.setMessageConverter(jsonMessageConverter);
		return rabbitTemplate;
	}

	@Bean
	public MessageConverter jsonMessageConverter() {
		JsonMapper jsonMapper = new JsonMapper();
		jsonMapper.registerModule(new JavaTimeModule());
		objectMapper.registerModule(new JavaTimeModule());
	    objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
	    objectMapper.disable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS);
		return new Jackson2JsonMessageConverter(jsonMapper);
	}
}
