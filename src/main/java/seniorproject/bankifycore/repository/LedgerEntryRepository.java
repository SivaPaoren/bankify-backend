package seniorproject.bankifycore.repository;


import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import seniorproject.bankifycore.domain.LedgerEntry;

import java.util.List;
import java.util.UUID;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {
    List<LedgerEntry> findByAccount_IdOrderByCreatedAtDesc(UUID accountId);
    List<LedgerEntry> findByTransaction_Id(UUID transactionId);

    List<LedgerEntry> findByAccountIdOrderByCreatedAtDesc(
            UUID accountId,
            Pageable pageable
    );


    List<LedgerEntry> findAllByOrderByCreatedAtDesc();

    List<LedgerEntry> findByTransaction_ReferenceOrderByCreatedAtDesc(String reference);



}
