package seniorproject.bankifycore.web.v1;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import seniorproject.bankifycore.consants.ApiPaths;
import seniorproject.bankifycore.dto.transaction.DepositRequest;
import seniorproject.bankifycore.dto.transaction.TransactionResponse;
import seniorproject.bankifycore.dto.transaction.TransferRequest;
import seniorproject.bankifycore.dto.transaction.WithdrawRequest;
import seniorproject.bankifycore.service.TransactionService;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPaths.TRANSACTIONS)
@PreAuthorize("hasRole('ADMIN')")
public class TransactionController {

    private final TransactionService transactionService;

    // Deposit money into an account
    // API end point for top-ups
    @PostMapping("/deposit")
    public TransactionResponse deposit(
            @RequestHeader("Idempotency-Key") String idemKey,
            @RequestBody DepositRequest depositRequest) {
        return transactionService.deposit(idemKey, depositRequest);
    }

    // What: withdraw money from an account
    // Why: API endpoint for cash-out
    @PostMapping("/withdraw")
    public TransactionResponse withdraw(
            @RequestHeader("Idempotency-Key") String idemKey,
            @RequestBody WithdrawRequest withdrawRequest) {
        return transactionService.withdraw(idemKey, withdrawRequest);
    }

    // What: transfer money between accounts
    // Why: API endpoint for moving money
    @PostMapping("/transfer")
    public TransactionResponse transfer(
            @RequestHeader("Idempotency-Key") String idemKey,
            @RequestBody TransferRequest transferRequest) {
        return transactionService.transfer(idemKey, transferRequest);
    }

    // What: list transactions for history/testing
    // Why: view ledger records (filters optional)
    @GetMapping
    public List<TransactionResponse> list(@RequestParam(required = false) UUID accountId) {
        return transactionService.list(accountId);
    }

    // What: get one transaction detail
    // Why: receipt/debugging
    @GetMapping("/{transactionId}")
    public TransactionResponse get(@PathVariable UUID transactionId) {
        return transactionService.get(transactionId);
    }

}
