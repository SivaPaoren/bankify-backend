package seniorproject.bankifycore.service.atm;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import seniorproject.bankifycore.domain.Account;
import seniorproject.bankifycore.dto.atm.*;
import seniorproject.bankifycore.dto.transaction.DepositRequest;
import seniorproject.bankifycore.dto.transaction.TransactionResponse;
import seniorproject.bankifycore.dto.transaction.TransferRequest;
import seniorproject.bankifycore.dto.transaction.WithdrawRequest;
import seniorproject.bankifycore.repository.AccountRepository;
import seniorproject.bankifycore.service.AuditService;
import seniorproject.bankifycore.service.LedgerService;
import seniorproject.bankifycore.service.TransactionService;
import seniorproject.bankifycore.utils.ActorContext;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AtmMeService {

    private final AtmAccountService atmAccountService;
    private final TransactionService transactionService;
    private final LedgerService ledgerService;
    private final PasswordEncoder passwordEncoder;
    private final AccountRepository accountRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public AtmBalanceResponse balance() {
        Account acc = atmAccountService.myActiveAccountOrThrow();
        return new AtmBalanceResponse(acc.getId(), acc.getBalance(), acc.getCurrency().name());
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> transactions(Integer limit) {
        Account acc = atmAccountService.myActiveAccountOrThrow();
        int n = (limit == null || limit <= 0) ? 3 : Math.min(limit, 50);

        // If already have list(accountId) only:
        // return transactionService.listLatest(acc.getId(), n);
        return ledgerService.listLatest(acc.getId(), n);
    }

    @Transactional
    public TransactionResponse deposit(String idemKey, AtmDepositRequest req) {

        // validate ammount > 0
        validateAmmount(req.amount());

        Account acc = atmAccountService.myActiveAccountOrThrow();
        return transactionService.deposit(idemKey, new DepositRequest(acc.getId(), req.amount(), req.note()));
    }

    @Transactional
    public TransactionResponse withdraw(String idemKey, AtmWithdrawRequest req) {

        validateAmmount(req.amount());
        Account acc = atmAccountService.myActiveAccountOrThrow();
        return transactionService.withdraw(idemKey, new WithdrawRequest(acc.getId(), req.amount(), req.note()));
    }

    @Transactional
    public TransactionResponse transfer(String idemKey, AtmTransferRequest req) {

        validateAmmount(req.amount());
        //find the from account which is currently log in one
        Account from  = atmAccountService.myActiveAccountOrThrow();

        //to account to which account to transfer
        Account to = accountRepository.findByAccountNumber(req.toAccountNumber())
                .orElseThrow(() -> new IllegalArgumentException("Receiver account not found"));

        // Optional: block transferring to self by account number
        if (from.getId().equals(to.getId())) {
            throw new IllegalStateException("Cannot transfer to the same account");
        }

        return transactionService.transfer(idemKey,
                new TransferRequest(from.getId(), to.getId(), req.amount(), req.note()));
    }



    @Transactional
    public void changePin(AtmChangePinRequest req) {
        Account acc = atmAccountService.myAccountAllowPinChangeOrThrow();

        if (req.oldPin() == null || req.newPin() == null) {
            throw new IllegalArgumentException("PIN is required");
        }
        if (!req.newPin().matches("\\d{6}")) {
            throw new IllegalArgumentException("PIN must be 6 digits");
        }
        if (acc.getPinHash() == null || !passwordEncoder.matches(req.oldPin(), acc.getPinHash())) {
            throw new IllegalArgumentException("Invalid old PIN");
        }

        acc.setPinHash(passwordEncoder.encode(req.newPin()));
        acc.setPinFailedAttempts(0);
        acc.setPinLockedUntil(null);
        acc.setPinChangeRequired(false);

        accountRepository.save(acc);

        // audit log is generated here
        auditService.log(
                ActorContext.actorType(), ActorContext.actorId(),
                "PARTNER_APP_APPROVED",
                "PartnerApp", acc.getId().toString(),
                "status=" + acc.getStatus().name());
    }




    public void validateAmmount(BigDecimal amount) {

        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be > 0");
        }

    }

}
