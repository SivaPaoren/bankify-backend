package seniorproject.bankifycore.web.v1;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import seniorproject.bankifycore.consants.ApiPaths;
import seniorproject.bankifycore.dto.AuditLogResponse;
import seniorproject.bankifycore.dto.admin.ApprovePartnerResponse;
import seniorproject.bankifycore.dto.admin.ResetPinRequest;
import seniorproject.bankifycore.dto.ledger.LedgerEntryResponse;
import seniorproject.bankifycore.dto.partnerapp.PartnerAppResponse;
import seniorproject.bankifycore.dto.rotation.ApproveRotationResponse;
import seniorproject.bankifycore.dto.rotation.RejectRotationResponse;
import seniorproject.bankifycore.dto.rotation.AdminRotationRequestItem;
import seniorproject.bankifycore.service.AccountService;
import seniorproject.bankifycore.service.AuditService;
import seniorproject.bankifycore.service.LedgerService;
import seniorproject.bankifycore.service.partner.PartnerAppAdminService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(ApiPaths.ADMIN)
@RequiredArgsConstructor
public class AdminController {

    private final AccountService accountService;
    private final PartnerAppAdminService partnerAppAdminService;
    private final AuditService auditService;
    private final LedgerService ledgerService;
    // reset pin
    @PatchMapping("/accounts/{accountId}/pin")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public void resetPin(@PathVariable UUID accountId, @RequestBody ResetPinRequest req) {
        accountService.resetPin(accountId, req);
    }

    // Partners
    @GetMapping("/partner-apps")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public List<PartnerAppResponse> listPartners() {
        return partnerAppAdminService.list();
    }

    // disabling the account
    @PatchMapping("/partner-apps/{id}/disable")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public PartnerAppResponse disable(@PathVariable UUID id) {
        return partnerAppAdminService.disable(id);
    }

    // activating the account
    @PatchMapping("/partner-apps/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public PartnerAppResponse activate(@PathVariable UUID id) {
        return partnerAppAdminService.activate(id);
    }

    // approve API for partner first time creating account
    @PatchMapping("/partner-apps/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ApprovePartnerResponse approve(@PathVariable UUID id) {
        return partnerAppAdminService.approve(id);
    }

    @GetMapping("/partner-apps/rotation-requests")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public List<AdminRotationRequestItem> listRotationRequests() {
        return partnerAppAdminService.listRotationRequests();
    }

    // approving api rotation for partner
    @PatchMapping("/partner-apps/rotation-requests/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ApproveRotationResponse approveRotation(@PathVariable UUID id) {
        return partnerAppAdminService.approveRotation(id);
    }

    @PatchMapping("/partner-apps/rotation-requests/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public RejectRotationResponse rejectRotation(@PathVariable UUID id) {
        return partnerAppAdminService.reject(id);
    }

    @GetMapping("/audit-logs")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public List<AuditLogResponse> listAuditLog(
            @RequestParam(required = false) String actorType,
            @RequestParam(required = false) String action) {
        return auditService.list(actorType, action);
    }

    @GetMapping("/transactions/ledger")
    @PreAuthorize("hasRole('ADMIN')")
    public List<LedgerEntryResponse> ledger(@RequestParam(required = false) String reference) {
        return ledgerService.listAll(reference);
    }

}
