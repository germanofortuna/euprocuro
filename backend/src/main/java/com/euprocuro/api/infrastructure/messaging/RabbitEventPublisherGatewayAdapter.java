package com.euprocuro.api.infrastructure.messaging;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.euprocuro.api.domain.gateway.EventPublisherGateway;

@Component
public class RabbitEventPublisherGatewayAdapter implements EventPublisherGateway {

    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitEventPublisherGatewayAdapter.class);

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;

    public RabbitEventPublisherGatewayAdapter(
            RabbitTemplate rabbitTemplate,
            @Value("${application.messaging.rabbit.exchange:euprocuro.exchange}") String exchange
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
    }

    @Override
    public void publish(String eventType, Map<String, Object> payload) {
        try {
            rabbitTemplate.convertAndSend(exchange, eventType, payload);
        } catch (Exception exception) {
            LOGGER.warn(
                    "Nao foi possivel publicar evento '{}' no RabbitMQ. Aplicacao seguira normalmente.",
                    eventType
            );
        }
    }
}
