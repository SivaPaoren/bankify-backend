package seniorproject.bankifycore.utils.rabbitmq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class EventConsumer {

    @RabbitListener(queues = "${bankify.mq.queue}")
    public void onEvent(TxCreatedEvent event) {
        log.info("✅ MQ EVENT: {} txId={} amount={} ref={}",
                event.eventType(), event.txId(), event.amount(), event.reference());
    }
}
