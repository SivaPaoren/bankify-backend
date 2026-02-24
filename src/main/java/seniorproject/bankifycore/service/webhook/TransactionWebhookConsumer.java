package seniorproject.bankifycore.service.webhook;

import com.fasterxml.jackson.databind.json.JsonMapper;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import seniorproject.bankifycore.domain.PartnerApp;
import seniorproject.bankifycore.domain.Transaction;
import seniorproject.bankifycore.domain.WebhookDelivery;
import seniorproject.bankifycore.domain.enums.WebhookDeliveryStatus;
import seniorproject.bankifycore.repository.TransactionRepository;
import seniorproject.bankifycore.repository.WebhookDeliveryRepository;
import seniorproject.bankifycore.utils.rabbitmq.TxCreatedEvent;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class TransactionWebhookConsumer {

    private final TransactionRepository transactionRepo;
    private final WebhookDeliveryRepository webhookDeliveryRepo;
    private final JsonMapper objectMapper; // ✅ use JsonMapper

    // 🔴 THIS METHOD RECEIVES EVENTS FROM RABBIT
    @RabbitListener(queues = "${bankify.mq.queue}")
    @Transactional
    public void onTransactionCreated(TxCreatedEvent event) throws Exception {

        // 1) Load transaction
        Transaction tx = transactionRepo.findById(event.txId()).orElse(null);
        if (tx == null) return;

        // 2) Only partner-originated transactions should trigger webhooks
        PartnerApp partner = tx.getFromAccount() != null
                ? tx.getFromAccount().getPartnerApp()   // your partner settlement account
                : null;

        if (partner == null) return;
        if (partner.getCallbackUrl() == null || partner.getCallbackUrl().isBlank()) return;

        // 3) Convert event to JSON
        String payloadJson = objectMapper.writeValueAsString(event);

        // 4) Create webhook delivery job (QUEUE TABLE)
        WebhookDelivery delivery = WebhookDelivery.builder()
                .partnerApp(partner)
                .eventType(event.eventType())
                .payloadJson(payloadJson)
                .status(WebhookDeliveryStatus.PENDING)
                .attemptCount(0)
                .nextAttemptAt(Instant.now())
                .build();

        webhookDeliveryRepo.save(delivery);
    }
}