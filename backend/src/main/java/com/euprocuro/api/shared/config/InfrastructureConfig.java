package com.euprocuro.api.shared.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class InfrastructureConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public TopicExchange euProcuroExchange(
            @Value("${application.messaging.rabbit.exchange:euprocuro.exchange}") String exchangeName
    ) {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public Queue interestCreatedQueue(
            @Value("${application.messaging.rabbit.interest-created-queue:euprocuro.interest.created}") String queueName
    ) {
        return new Queue(queueName, true);
    }

    @Bean
    public Queue offerCreatedQueue(
            @Value("${application.messaging.rabbit.offer-created-queue:euprocuro.offer.created}") String queueName
    ) {
        return new Queue(queueName, true);
    }

    @Bean
    public Queue authQueue(
            @Value("${application.messaging.rabbit.auth-queue:euprocuro.auth.events}") String queueName
    ) {
        return new Queue(queueName, true);
    }

    @Bean
    public Binding interestCreatedBinding(
            @Qualifier("interestCreatedQueue") Queue interestCreatedQueue,
            TopicExchange euProcuroExchange
    ) {
        return BindingBuilder.bind(interestCreatedQueue).to(euProcuroExchange).with("interest.created");
    }

    @Bean
    public Binding offerCreatedBinding(
            @Qualifier("offerCreatedQueue") Queue offerCreatedQueue,
            TopicExchange euProcuroExchange
    ) {
        return BindingBuilder.bind(offerCreatedQueue).to(euProcuroExchange).with("offer.created");
    }

    @Bean
    public Binding authBinding(
            @Qualifier("authQueue") Queue authQueue,
            TopicExchange euProcuroExchange
    ) {
        return BindingBuilder.bind(authQueue).to(euProcuroExchange).with("auth.*");
    }

    @Bean
    public MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
