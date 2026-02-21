package seniorproject.bankifycore.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import seniorproject.bankifycore.domain.AuditLog;
import seniorproject.bankifycore.dto.AuditLogResponse;
import seniorproject.bankifycore.repository.AuditLogRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditRepo;

    public void log(String actorType, String actorId, String action,
                    String entityType, String entityId, String details) {
        try{
            auditRepo.save(AuditLog.builder()
                        .actorType(actorType)
                        .actorId(actorId)
                        .action(action)
                        .entityType(entityType)
                        .entityId(entityId)
                        .details(details)
                        .build());
            }
        catch(Exception ignored){
            System.out.println("Audit Log Error"+ ignored);
        }
    }


    public List<AuditLogResponse> list(String actorType, String action) {
        List<AuditLog> logs;
        if (actorType != null && action != null) logs = auditRepo.findByActorTypeAndAction(actorType, action);
        else if (actorType != null) logs = auditRepo.findByActorType(actorType);
        else if (action != null) logs = auditRepo.findByAction(action);
        else logs = auditRepo.findAll();

        return logs.stream().map(this::toResponse).toList();
    }

    public AuditLogResponse toResponse(AuditLog log) {
        return new AuditLogResponse(
                log.getCreatedAt(),
                log.getActorType(),
                log.getActorId(),
                log.getAction(),
                log.getEntityType(),
                log.getEntityId(),
                log.getDetails()
        );
    }


}
