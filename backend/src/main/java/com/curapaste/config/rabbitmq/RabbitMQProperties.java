package com.curapaste.config.rabbitmq;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "rabbitmq")
public class RabbitMQProperties {

    private String exchange;
    private String deadLetterExchange;
    private String abuseScanQueue;
    private String deadLetterQueue;
}