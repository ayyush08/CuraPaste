package com.curapaste.events;


import com.curapaste.config.rabbitmq.RabbitMQProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class PasteEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMQProperties rabbitMQProperties;

    public PasteEventPublisher(RabbitTemplate rabbitTemplate, RabbitMQProperties rabbitMQProperties) {
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitMQProperties = rabbitMQProperties;
    }

    public void publish(String shortId){
        PasteEvent pasteEvent = new PasteEvent(
                shortId,
                "CREATED",
                Instant.now()
        );


        try{
            rabbitTemplate.convertAndSend(
                    rabbitMQProperties.getExchange(),
                    "paste.created",
                    pasteEvent
            );
        }
        catch (Exception e){
            System.out.println(
                    "Failed to publish paste.created event: "
                            + e.getMessage()
            );
        }
    }
}
