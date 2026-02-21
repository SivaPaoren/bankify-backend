package seniorproject.bankifycore.config;


import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class RabbitMqMessagingConfig {

    @Bean
    public MessageConverter rabbitMessageConverter() {
        JsonMapper mapper = JsonMapper.builder().build();
        return new JacksonJsonMessageConverter(mapper, "seniorproject.bankifycore");
    }

}
