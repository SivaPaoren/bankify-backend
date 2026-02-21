package seniorproject.bankifycore.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Bean
    public TopicExchange bankifyExchange(@Value("${bankify.mq.exchange}") String exchange) {
        return new TopicExchange(exchange, true, false);
    }

    @Bean
    public Queue bankifyQueue(@Value("${bankify.mq.queue}") String queue) {
        return QueueBuilder.durable(queue).build();
    }

    @Bean
    public Binding bankifyBinding(
            Queue bankifyQueue,
            TopicExchange bankifyExchange,
            @Value("${bankify.mq.routingKey}") String routingKey
    ) {
        return BindingBuilder.bind(bankifyQueue).to(bankifyExchange).with(routingKey);
    }


}
