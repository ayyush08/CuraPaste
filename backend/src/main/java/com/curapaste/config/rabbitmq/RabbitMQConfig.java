package com.curapaste.config.rabbitmq;


import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    private  final RabbitMQProperties rabbitMQProperties;

    public RabbitMQConfig(RabbitMQProperties rabbitMQProperties) {
        this.rabbitMQProperties = rabbitMQProperties;
    }

    @Bean
    public JacksonJsonMessageConverter jacksonJsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public TopicExchange pasteEventsExchange() {
        return new TopicExchange(rabbitMQProperties.getExchange());
    }

    @Bean
    public TopicExchange pasteEventsDLX(){
        return new TopicExchange(rabbitMQProperties.getDeadLetterExchange());
    }

    @Bean
    public Queue abuseScanQueue(){
        return QueueBuilder
                .durable(rabbitMQProperties.getAbuseScanQueue())
                .withArgument(
                        "x-dead-letter-exchange",
                        rabbitMQProperties.getDeadLetterExchange()
                )
                .withArgument(
                        "x-dead-letter-routing-key",
                        "paste.created"
                )
                .build();
    }



    @Bean
    public Queue deadLetterQueue(){
        return QueueBuilder
                .durable(rabbitMQProperties.getDeadLetterQueue())
                .build();
    }

    @Bean
    public Binding abuseScanBinding(
            Queue abuseScanQueue,
            TopicExchange pasteEventsExchange
    ){
        return BindingBuilder
                .bind(abuseScanQueue)
                .to(pasteEventsExchange)
                .with("paste.created");
    }



    @Bean
    public Binding deadLetterBinding(
            Queue deadLetterQueue,
            TopicExchange pasteEventsDLX
    ){
        return BindingBuilder
                .bind(deadLetterQueue)
                .to(pasteEventsDLX)
                .with("#");
    }
}
