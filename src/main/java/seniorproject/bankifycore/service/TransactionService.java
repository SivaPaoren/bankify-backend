package seniorproject.bankifycore.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import seniorproject.bankifycore.domain.Account;
import seniorproject.bankifycore.domain.LedgerEntry;
import seniorproject.bankifycore.domain.Transaction;
import seniorproject.bankifycore.domain.enums.AccountStatus;
import seniorproject.bankifycore.domain.enums.EntryDirection;
import seniorproject.bankifycore.domain.enums.TransactionStatus;
import seniorproject.bankifycore.domain.enums.TransactionType;
import seniorproject.bankifycore.dto.transaction.DepositRequest;
import seniorproject.bankifycore.dto.transaction.TransactionResponse;
import seniorproject.bankifycore.dto.transaction.TransferRequest;
import seniorproject.bankifycore.dto.transaction.WithdrawRequest;
import seniorproject.bankifycore.repository.AccountRepository;
import seniorproject.bankifycore.repository.LedgerEntryRepository;
import seniorproject.bankifycore.repository.TransactionRepository;
import seniorproject.bankifycore.utils.ActorContext;

import java.math.BigDecimal;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepo;
    private final AccountRepository accountRepo;
    private final LedgerEntryRepository ledgerEntryRepo;
    private final AuditService auditService;

    @Transactional
    public TransactionResponse deposit(String idemKey, DepositRequest request) {

        // check for repeated transaction idempotency
        TransactionResponse existing = returnExistingIfDuplicate(idemKey);
        if (existing != null)
            return existing;

        // find the account | Check the account existence
        Account account = accountRepo.findById(request.accountId())
                .orElseThrow(() -> new IllegalArgumentException("Account Not found"));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Account is not active");
        }

        // Validating the amount of the
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Depositing amount must  be greater than zero");
        }

        // add the amount into the existing balance
        // Update the balance
        account.setBalance(account.getBalance().add(request.amount()));

        // Create a transaction and save transaction
        Transaction transaction = Transaction.builder()
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.SUCCESS)
                .amount(request.amount())
                .toAccount(account)
                .reference(idemKey) // idempotency key stored here(unique)
                .note(request.note())
                .build();

        accountRepo.save(account);
        try {
            Transaction saved = transactionRepo.save(transaction);

            // ✅ audit only when NEW transaction is created
            auditService.log(
                    ActorContext.actorType(), ActorContext.actorId(),
                    "TRANSACTION_DEPOSIT",
                    "Transaction", saved.getId().toString(),
                    "type=" + saved.getType()
                            + ",amount=" + saved.getAmount()
                            + ",ref=" + saved.getReference()
                            + ",toAccountId=" + account.getId());

            writeLedger(account, saved, EntryDirection.CREDIT, request.amount());
            return toResponse(transaction);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // race safe:if two requests with the same key hit at the same time
            return transactionRepo.findByReference(idemKey)
                    .map(this::toResponse)
                    .orElseThrow(() -> e);
        }

    }

    @Transactional
    public TransactionResponse withdraw(String idemKey, WithdrawRequest request) {

        // check for repeated transaction
        TransactionResponse existing = returnExistingIfDuplicate(idemKey);
        if (existing != null)
            return existing;

        Account account = accountRepo.findById(request.accountId())
                .orElseThrow(() -> new IllegalArgumentException("Account Not found"));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Account is not active");
        }

        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Withdrawing amount must  be greater than zero");
        }

        if (account.getBalance().compareTo(request.amount()) < 0) {
            throw new IllegalStateException("Insufficient balance");
        }

        account.setBalance(account.getBalance().subtract(request.amount()));

        Transaction transaction = Transaction.builder()
                .type(TransactionType.WITHDRAWAL)
                .status(TransactionStatus.SUCCESS)
                .amount(request.amount())
                .fromAccount(account)
                .reference(idemKey)
                .note(request.note())
                .build();

        accountRepo.save(account);
        try {
            Transaction saved = transactionRepo.save(transaction);

            // audit log is done here
            auditService.log(
                    ActorContext.actorType(), ActorContext.actorId(),
                    "TRANSACTION_WITHDRAW",
                    "Transaction", saved.getId().toString(),
                    "type=" + saved.getType()
                            + ",amount=" + saved.getAmount()
                            + ",ref=" + saved.getReference()
                            + ",fromAccountId=" + account.getId());

            // ledger record
            writeLedger(account, saved, EntryDirection.DEBIT, request.amount());
            return toResponse(transaction);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            return transactionRepo.findByReference(idemKey)
                    .map(this::toResponse)
                    .orElseThrow(() -> e);
        }
    }

    @Transactional
    public TransactionResponse transfer(String idemKey, TransferRequest request) {


        System.out.println("To id in Original transfer "+request.toAccountId());
        // check for repeated transaction idempotency
        TransactionResponse existing = returnExistingIfDuplicate(idemKey);
        if (existing != null)
            return existing;

        if (request.fromAccountId().equals(request.toAccountId())) {
            throw new IllegalStateException("Cannot transfer to the same account");
        }

        Account fromAccount = accountRepo.findById(request.fromAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Sending Account Not found"));

        Account toAccount = accountRepo.findById(request.toAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Receiving Account Not found"));

        if (fromAccount.getStatus() != AccountStatus.ACTIVE || toAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Both Account must be active to transfer");
        }

        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Transfer amount must  be greater than zero");
        }

        if (fromAccount.getBalance().compareTo(request.amount()) < 0) {
            throw new IllegalStateException("Insufficient balance");
        }

        fromAccount.setBalance(fromAccount.getBalance().subtract(request.amount()));
        toAccount.setBalance(toAccount.getBalance().add(request.amount()));

        Transaction transaction = Transaction.builder()
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.SUCCESS)
                .amount(request.amount())
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .reference(idemKey)
                .note(request.note())
                .build();

        accountRepo.save(fromAccount);
        accountRepo.save(toAccount);
        try {
            Transaction saved = transactionRepo.save(transaction);

            // audit logs is saved here
            auditService.log(
                    ActorContext.actorType(),
                    ActorContext.actorId(),
                    "TRANSACTION_TRANSFER",
                    "Transaction",
                    saved.getId().toString(),
                    "type=" + saved.getType()
                            + ",amount=" + saved.getAmount()
                            + ",ref=" + saved.getReference()
                            + ",fromAccountId=" + fromAccount.getId()
                            + ",toAccountId=" + toAccount.getId());

            // ledger record is done here
            writeLedger(fromAccount, saved, EntryDirection.DEBIT, request.amount());
            writeLedger(toAccount, saved, EntryDirection.CREDIT, request.amount());

            return toResponse(saved);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            return transactionRepo.findByReference(idemKey)
                    .map(this::toResponse)
                    .orElseThrow(() -> e);
        }

    }

    // idempotencyCheck method helper
    private TransactionResponse returnExistingIfDuplicate(String idemKey) {
        if (idemKey == null || idemKey.isBlank()) {
            throw new IllegalStateException("Idempotency-Key header is required");
        }
        return transactionRepo.findByReference(idemKey)
                .map(this::toResponse)
                .orElse(null);
    }

    // list by aacount id
    @Transactional
    public List<TransactionResponse> list(UUID accountId) {
        List<Transaction> transaction;
        if (accountId == null) {
            transaction = transactionRepo.findAllByOrderByCreatedAtDesc();
        } else {
            transaction = transactionRepo
                    .findByFromAccount_IdOrToAccount_IdOrderByCreatedAtDesc(accountId, accountId);
        }
        return transaction.stream().map(this::toResponse).toList();
    }

    @Transactional
    public TransactionResponse get(UUID transactionId) {
        Transaction transaction = transactionRepo.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction Not found"));
        return toResponse(transaction);
    }

    // helper that help Entity -> DTO
    public TransactionResponse toResponse(Transaction transaction) {

        UUID fromId = transaction.getFromAccount() == null ? null : transaction.getFromAccount().getId();
        UUID toId = transaction.getToAccount() == null ? null : transaction.getToAccount().getId();

        return new TransactionResponse(
                transaction.getId(),
                transaction.getType(),
                transaction.getStatus(),
                transaction.getAmount(),
                fromId,
                toId,
                transaction.getReference(),
                transaction.getNote(),
                transaction.getCreatedAt());
    }

    // helper to write writeledger
    private void writeLedger(Account account, Transaction transaction, EntryDirection dir, BigDecimal amount) {
        ledgerEntryRepo.save(
                LedgerEntry.builder()
                        .account(account)
                        .transaction(transaction)
                        .direction(dir)
                        .amount(amount)
                        .currency(account.getCurrency())
                        .build());
    }

}
