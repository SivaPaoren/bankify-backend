package seniorproject.bankifycore.config;

import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class RabbitMqMessagingConfig {

    @Bean
    public JacksonJsonMessageConverter jacksonMessageConverter(JsonMapper mapper) {
        return new JacksonJsonMessageConverter(mapper, "seniorproject.bankifycore");
    }
}