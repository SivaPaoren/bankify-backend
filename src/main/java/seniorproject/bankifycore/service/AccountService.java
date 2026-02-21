package seniorproject.bankifycore.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import seniorproject.bankifycore.domain.Account;
import seniorproject.bankifycore.domain.Customer;
import seniorproject.bankifycore.domain.enums.AccountStatus;

import seniorproject.bankifycore.domain.enums.CustomerStatus;
import seniorproject.bankifycore.dto.account.AccountResponse;
import seniorproject.bankifycore.dto.account.CreateAccountRequest;
import seniorproject.bankifycore.dto.account.UpdateAccountRequest;
import seniorproject.bankifycore.dto.admin.ResetPinRequest;
import seniorproject.bankifycore.repository.AccountRepository;
import seniorproject.bankifycore.repository.CustomerRepository;
import seniorproject.bankifycore.utils.ActorContext;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepo;
    private final CustomerRepository customerRepo;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    private static final SecureRandom secureRandom = new SecureRandom();

    // ✅ Rule: customer exists
    // ✅ Rule: customer status must be ACTIVE
    // ✅ Rule: generate unique accountNumber
    // ✅ Rule: balance = 0, status = ACTIVE

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public AccountResponse create(CreateAccountRequest req) {

        Customer customer = customerRepo.findById(req.customerId())
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        if (customer.getStatus() != CustomerStatus.ACTIVE) {
            throw new RuntimeException("Customer status must be ACTIVE to create account");
        }

        if (req.pin() == null || !req.pin().matches("\\d{6}")) {
            throw new IllegalArgumentException("PIN must be 6 digits and only digits");
        }

        Account account = Account.builder()
                .customer(customer)
                .type(req.type())
                .currency(req.currency())
                .status(AccountStatus.ACTIVE)
                .accountNumber(generateUniqueAccountNumber())
                .balance(BigDecimal.ZERO) // ✅ REQUIRED
                .pinHash(passwordEncoder.encode(req.pin()))
                .pinChangeRequired(true)
                .pinFailedAttempts(0)
                .pinLockedUntil(null)
                .build();

        Account savedAccount = accountRepo.save(account);

        // Audit Log
        auditService.log(
                ActorContext.actorType(), ActorContext.actorId(),
                "ACCOUNT_CREATE",
                "Account", savedAccount.getId().toString(),
                "reason=admin_Create");

        return toResponse(savedAccount);
    }

    @Transactional
    public List<AccountResponse> list(UUID customerId) {
        List<Account> accounts = (customerId == null)
                ? accountRepo.findAll()
                : accountRepo.findByCustomer_Id(customerId);

        return accounts.stream().map(this::toResponse).toList();
    }

    @Transactional
    public AccountResponse get(UUID accountId) {
        Account account = accountRepo.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        return toResponse(account);
    }

    @Transactional
    public AccountResponse updateStatus(UUID accountId, UpdateAccountRequest req) {
        Account account = accountRepo.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        account.setStatus(req.status());

        Account saved = accountRepo.save(account);

        // audit log is done here
        String action = "ACCOUNT_UPDATE";
        String reason = "admin_Update";

        if (req.status() == AccountStatus.ACTIVE) {
            action = "ACCOUNT_REACTIVATE";
            reason = "admin_Reactivate";
        } else if (req.status() == AccountStatus.FROZEN) {
            action = "ACCOUNT_FREEZE";
            reason = "admin_Freeze";
        } else if (req.status() == AccountStatus.CLOSED) {
            action = "ACCOUNT_CLOSE";
            reason = "admin_Close";
        }

        // audit log is done here
        auditService.log(
                ActorContext.actorType(), ActorContext.actorId(),
                action,
                "Account", account.getId().toString(),
                "reason=" + reason);

        return toResponse(saved);
    }

    @Transactional
    public AccountResponse disable(UUID id) {
        Account account = accountRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Account cannot be disabled , because account is not found"));

        account.setStatus(AccountStatus.FROZEN);
        accountRepo.save(account);

        // audit log is done here
        auditService.log(
                ActorContext.actorType(), ActorContext.actorId(),
                "ACCOUNT_FREEZE",
                "Account", account.getId().toString(),
                "reason=admin_Freeze");
        return toResponse(account);
    }

    @Transactional
    public void resetPin(UUID accountId, ResetPinRequest req) {
        Account acc = accountRepo.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        // ❌ Partner accounts never get ATM PINs
        if (acc.getPartnerApp() != null) {
            throw new IllegalArgumentException("Not a customer account");
        }

        // ❌ Frozen accounts cannot change PIN
        if (acc.getStatus() == AccountStatus.FROZEN) {
            throw new IllegalStateException("Account is frozen");
        }

        if (acc.getCustomer() != null && acc.getCustomer().getStatus() == CustomerStatus.FROZEN) {
            throw new IllegalStateException("Customer is frozen");
        }

        // Validate PIN format
        if (req.pin() == null || !req.pin().matches("\\d{6}")) {
            throw new IllegalArgumentException("PIN must be 6 digits");
        }

        acc.setPinHash(passwordEncoder.encode(req.pin()));
        acc.setPinChangeRequired(true);
        acc.setPinFailedAttempts(0);
        acc.setPinLockedUntil(null);
        accountRepo.save(acc);

        // audit log is done here
        auditService.log(
                ActorContext.actorType(), ActorContext.actorId(),
                "ACCOUNT_PIN_RESET",
                "Account", accountId.toString(),
                "reason=admin_reset");

    }

    // ----------------------- Helpers -------------------------

    private String generateUniqueAccountNumber() {
        // Example: 12 digits. can change the format later.
        // We loop until we find a number not in DB.
        String number;
        do {
            number = randomDigits(12);
        } while (accountRepo.existsByAccountNumber(number));
        return number;
    }

    public String getUniqueAccountNumber() {
        return generateUniqueAccountNumber();
    }

    private String randomDigits(int len) {
        StringBuilder sb = new StringBuilder(len);

        for (int i = 0; i < len; i++) {
            sb.append(secureRandom.nextInt(10));
        }
        return sb.toString();

    }

    private AccountResponse toResponse(Account account) {

        UUID customerId = (account.getCustomer() == null) ? null : account.getCustomer().getId();
        UUID partnerAppId = (account.getPartnerApp() == null) ? null : account.getPartnerApp().getId();
        String customerName = (account.getCustomer() == null) ? null
                : account.getCustomer().getFirstName() + " " + account.getCustomer().getLastName();

        return new AccountResponse(
                account.getId(),
                customerId,
                customerName,
                partnerAppId,
                account.getAccountNumber(),
                account.getType(),
                account.getCurrency().name(),
                account.getBalance(),
                account.getStatus(),
                account.getCreatedAt(),
                account.getUpdatedAt());
    }

}
