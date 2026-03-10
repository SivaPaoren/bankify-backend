package seniorproject.bankifycore.service;


import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import seniorproject.bankifycore.domain.LedgerEntry;
import seniorproject.bankifycore.dto.ledger.LedgerEntryResponse;
import seniorproject.bankifycore.dto.transaction.TransactionResponse;
import seniorproject.bankifycore.repository.LedgerEntryRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LedgerService {

    private final LedgerEntryRepository ledgerEntryRepo;

    @Transactional
    public List<LedgerEntryResponse> listByAccount(UUID accountId) {
        if (accountId == null) {
            throw new IllegalArgumentException("accountId cannot be null");
        }
        List<LedgerEntry> accounts = ledgerEntryRepo.findByAccount_IdOrderByCreatedAtDesc(accountId);
        return accounts.stream().map(this::toResponse).toList();
    }


    @Transactional
    public List<TransactionResponse> listLatest(UUID accountId, int limit) {
        if (limit <= 0 || limit > 50) {
            limit = 3; // safe default (ATM / partner requirement)
        }

        Pageable pageable = PageRequest.of(0, limit);

        return ledgerEntryRepo
                .findByAccountIdOrderByCreatedAtDesc(accountId, pageable)
                .stream()
                .map(this::toTransactionResponse)
                .toList();
    }


    @Transactional(readOnly = true)
    public List<LedgerEntryResponse> listAll(String reference) {

        var entries = (reference == null || reference.isBlank())
                ? ledgerEntryRepo.findAllByOrderByCreatedAtDesc()
                : ledgerEntryRepo.findByTransaction_ReferenceOrderByCreatedAtDesc(reference);

        return entries.stream()
                .map(this::toResponse)
                .toList();
    }


    public LedgerEntryResponse toResponse(LedgerEntry ledgerEntry) {
        return new LedgerEntryResponse(
                ledgerEntry.getId(),
                ledgerEntry.getAccount().getId(),
                ledgerEntry.getTransaction().getId(),
                ledgerEntry.getDirection().name(),
                ledgerEntry.getAmount(),
                ledgerEntry.getCurrency().name(),
                ledgerEntry.getCreatedAt());
    }


    private TransactionResponse toTransactionResponse(LedgerEntry e) {
        var tx = e.getTransaction();

        UUID fromId = tx.getFromAccount() != null ? tx.getFromAccount().getId() : null;
        UUID toId   = tx.getToAccount()   != null ? tx.getToAccount().getId()   : null;

        return new TransactionResponse(
                tx.getId(),          // ✅ transaction id
                tx.getType(),        // ✅ TransactionType
                tx.getStatus(),      // ✅ TransactionStatus
                tx.getAmount(),      // ✅ amount
                fromId,              // ✅ fromAccountId (null for deposit if your model uses null)
                toId,                // ✅ toAccountId   (null for withdraw if your model uses null)
                tx.getReference(),   // ✅ reference/idempotency ref
                tx.getNote(),        // ✅ note
                tx.getCreatedAt()    // ✅ createdAt
        );
    }

}
