package com.euprocuro.api.infrastructure.messaging;

import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.euprocuro.api.domain.gateway.EventPublisherGateway;

@Component
@Slf4j
public class RabbitEventPublisherGatewayAdapter implements EventPublisherGateway {

    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitEventPublisherGatewayAdapter.class);

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;

    public RabbitEventPublisherGatewayAdapter(
            RabbitTemplate rabbitTemplate,
            MessageConverter jacksonMessageConverter,
            @Value("${application.messaging.rabbit.exchange:euprocuro.exchange}") String exchange
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitTemplate.setMessageConverter(jacksonMessageConverter);
        this.exchange = exchange;
    }

    @Override
    public void publish(String eventType, Map<String, Object> payload) {
        try {
            rabbitTemplate.convertAndSend(exchange, eventType, payload);
        } catch (Exception exception) {
            log.error(
                    "Nao foi possivel publicar evento '{}' no RabbitMQ. Aplicacao seguira normalmente. {}",
                    eventType, exception.getMessage()
            );
        }
    }
}
