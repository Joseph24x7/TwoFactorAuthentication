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

@EnableRabbit
@Configuration
public class RabbitMQConfig {

	@Bean
	public Queue sendOtp() {
		return new Queue("sendOtpQueue", false);
	}

	@Bean
	public DirectExchange directExchange() {
		return new DirectExchange("directExchange");
	}

	@Bean
	public Binding sendOtpBinding(Queue sendOtp) {
		return BindingBuilder.bind(sendOtp).to(directExchange()).with("sendOtpRoutingKey");
	}

	@Bean
	public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
	    final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
	    rabbitTemplate.setMessageConverter(jsonMessageConverter());
	    return rabbitTemplate;
	}

	@Bean
	public MessageConverter jsonMessageConverter() {
		return new Jackson2JsonMessageConverter(new ObjectMapper());
	}
}
