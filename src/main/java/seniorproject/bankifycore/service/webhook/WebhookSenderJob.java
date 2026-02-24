package seniorproject.bankifycore.service.webhook;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import seniorproject.bankifycore.domain.PartnerApp;
import seniorproject.bankifycore.domain.WebhookDelivery;
import seniorproject.bankifycore.domain.enums.WebhookDeliveryStatus;
import seniorproject.bankifycore.repository.PartnerAppRepository;
import seniorproject.bankifycore.repository.WebhookDeliveryRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class WebhookSenderJob {

    private final WebhookDeliveryRepository webhookDeliveryRepo;
    private final PartnerAppRepository partnerAppRepo;

    private final WebClient webClient = WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(
                    HttpClient.create()
                            .responseTimeout(Duration.ofSeconds(4))
            ))
            .build();

    @Scheduled(fixedDelay = 2000)
    @Transactional
    public void run() {
        Instant now = Instant.now();
        var due = webhookDeliveryRepo
                .findTop50ByStatusInAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
                        List.of(WebhookDeliveryStatus.PENDING, WebhookDeliveryStatus.RETRYING),
                        now
                );

        for (WebhookDelivery d : due) {
            PartnerApp p = d.getPartnerApp();

            try {
                int status = webClient.post()
                        .uri(p.getCallbackUrl())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Webhook-Event", d.getEventType())
                        .header("X-Webhook-Id", d.getId().toString())
                        .bodyValue(d.getPayloadJson())
                        .retrieve()
                        .toBodilessEntity()
                        .block()
                        .getStatusCode()
                        .value();

                p.setWebhookLastAttemptAt(now);
                p.setWebhookLastStatusCode(status);

                if (status >= 200 && status < 300) {
                    d.setStatus(WebhookDeliveryStatus.SUCCESS);
                    d.setLastStatusCode(status);
                    d.setLastError(null);

                    p.setWebhookLastSuccessAt(now);
                    p.setWebhookLastError(null);
                    p.setWebhookConsecutiveFailures(0);
                } else {
                    failAndScheduleRetry(d, p, status, "Non-2xx response");
                }

            } catch (Exception ex) {
                failAndScheduleRetry(d, p, null, ex.getMessage());
            }

            partnerAppRepo.save(p);
            webhookDeliveryRepo.save(d);
        }
    }

    private void failAndScheduleRetry(WebhookDelivery d, PartnerApp p, Integer status, String err) {
        int nextAttempt = d.getAttemptCount() + 1;
        d.setAttemptCount(nextAttempt);
        d.setLastStatusCode(status);
        d.setLastError(shortErr(err));
        p.setWebhookLastAttemptAt(Instant.now());
        p.setWebhookLastStatusCode(status);
        p.setWebhookLastError(shortErr(err));
        p.setWebhookConsecutiveFailures(p.getWebhookConsecutiveFailures() + 1);

        if (nextAttempt >= 5) {
            d.setStatus(WebhookDeliveryStatus.FAILED);
            d.setNextAttemptAt(Instant.now().plusSeconds(60 * 60)); // irrelevant now
            return;
        }

        d.setStatus(WebhookDeliveryStatus.RETRYING);
        d.setNextAttemptAt(Instant.now().plusSeconds(backoffSeconds(nextAttempt)));
    }

    private long backoffSeconds(int attempt) {
        return switch (attempt) {
            case 1 -> 10;
            case 2 -> 30;
            case 3 -> 120;
            case 4 -> 600;
            default -> 600;
        };
    }

    private String shortErr(String s) {
        if (s == null) return "unknown";
        return s.length() > 250 ? s.substring(0, 250) : s;
    }
}