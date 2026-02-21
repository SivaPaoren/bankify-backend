package seniorproject.bankifycore.web.v1;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import seniorproject.bankifycore.consants.ApiPaths;
import seniorproject.bankifycore.dto.account.AccountResponse;
import seniorproject.bankifycore.dto.account.CreateAccountRequest;
import seniorproject.bankifycore.dto.account.UpdateAccountRequest;
import seniorproject.bankifycore.dto.ledger.LedgerEntryResponse;
import seniorproject.bankifycore.service.AccountService;
import seniorproject.bankifycore.service.LedgerService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(ApiPaths.ACCOUNTS)
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final LedgerService ledgerService;

    // POST api/account service.create()
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public AccountResponse create(@RequestBody CreateAccountRequest req) {
        return accountService.create(req);
    }

    // GET /api/account/(id) -> service.list()
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public List<AccountResponse> list(@RequestParam(required = false) UUID customerId) {
        return accountService.list(customerId);
    }

    // GET /api/account/{accountId} -> service.get()
    @GetMapping("/{accountId}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public AccountResponse get(@PathVariable UUID accountId) {
        return accountService.get(accountId);
    }

    // Pathc api/v1/admin/account/{id} -> service.updateStatus()
    @PatchMapping("/{accountId}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public AccountResponse update(@PathVariable UUID accountId, @RequestBody UpdateAccountRequest req) {
        return accountService.updateStatus(accountId, req);
    }

    @GetMapping("/{accountId}/ledger")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public List<LedgerEntryResponse> ledger(@PathVariable UUID accountId) {
        return ledgerService.listByAccount(accountId);
    }

    @PatchMapping("/{id}/disable")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public AccountResponse disable(@PathVariable UUID id) {
        return accountService.disable(id);
    }

}
