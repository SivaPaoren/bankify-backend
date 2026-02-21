package seniorproject.bankifycore.utils.rabbitmq;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import seniorproject.bankifycore.domain.Transaction;
import seniorproject.bankifycore.utils.ActorContext;

@Component
@RequiredArgsConstructor
public class TransactionMqPublisher {


    private final RabbitTemplate rabbitTemplate;

    @Value("${bankify.mq.exchange}") private String exchange;
    @Value("${bankify.mq.routingKey}") private String routingKey;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTransactionCommitted(Transaction savedTx) {
        TxCreatedEvent event = new TxCreatedEvent(
                "TX_CREATED",
                ActorContext.actorType(),
                ActorContext.actorId(),
                savedTx.getId(),
                savedTx.getType().name(),
                savedTx.getAmount(),
                savedTx.getReference(),
                savedTx.getFromAccount() == null ? null : savedTx.getFromAccount().getId(),
                savedTx.getToAccount() == null ? null : savedTx.getToAccount().getId(),
                savedTx.getCreatedAt()
        );

        rabbitTemplate.convertAndSend(exchange, routingKey, event);
    }
}
