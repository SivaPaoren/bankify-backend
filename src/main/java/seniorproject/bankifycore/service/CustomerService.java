package seniorproject.bankifycore.service;

import jakarta.transaction.Transactional;

import org.springframework.stereotype.Service;
import seniorproject.bankifycore.domain.Account;
import seniorproject.bankifycore.domain.Customer;
import seniorproject.bankifycore.domain.enums.AccountStatus;
import seniorproject.bankifycore.domain.enums.CustomerStatus;
import seniorproject.bankifycore.domain.enums.CustomerType;
import seniorproject.bankifycore.dto.customer.CreateCustomerRequest;
import seniorproject.bankifycore.dto.customer.UpdateCustomerRequest;
import seniorproject.bankifycore.dto.customer.CustomerResponse;
import seniorproject.bankifycore.repository.AccountRepository;
import seniorproject.bankifycore.repository.CustomerRepository;
import seniorproject.bankifycore.utils.EnumMapper;
import seniorproject.bankifycore.utils.ActorContext;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepo;
    private final AccountRepository accountRepo;
    private final AuditService auditService;


    // ── Read ─────────────────────────────────────────────────────────────────

    public List<CustomerResponse> getCustomers() {
        return customerRepo.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public CustomerResponse getCustomerById(UUID id) {
        return toResponse(byId(id));
    }

    // ── Create ───────────────────────────────────────────────────────────────

    @Transactional
    public CustomerResponse create(CreateCustomerRequest req) {
        if (customerRepo.existsByEmail(req.email()))
            throw new IllegalArgumentException("Email already exists");

        Customer customer = Customer.builder()
                .firstName(req.firstName())
                .lastName(req.lastName())
                .email(req.email())
                .phoneNumber(req.phoneNumber())
                .type(EnumMapper.toEnum(CustomerType.class, req.type()))
                .status(CustomerStatus.ACTIVE)
                .build();

        customerRepo.save(customer);

        auditService.log(
                ActorContext.actorType(), ActorContext.actorId(),
                "CUSTOMER_CREATED",
                "Customer", customer.getId().toString(),
                "reason=admin_CREATED");

        return toResponse(customer);
    }

    // ── Update ───────────────────────────────────────────────────────────────

    @Transactional
    public CustomerResponse updateCustomer(UUID id, UpdateCustomerRequest req) {
        Customer customer = byId(id);

        if (req.firstName() != null) customer.setFirstName(req.firstName());
        if (req.lastName() != null)  customer.setLastName(req.lastName());
        if (req.phone() != null)     customer.setPhoneNumber(req.phone());

        if (req.status() != null) {
            CustomerStatus newStatus = EnumMapper.toEnum(CustomerStatus.class, req.status());
            customer.setStatus(newStatus);
            cascadeAccountStatus(id, newStatus);
        }

        Customer saved = customerRepo.save(customer);

        auditService.log(
                ActorContext.actorType(), ActorContext.actorId(),
                "CUSTOMER_UPDATED",
                "Customer", saved.getId().toString(),
                "reason=admin_UPDATED");

        return toResponse(saved);
    }

    // ── Status Actions ───────────────────────────────────────────────────────

    /** FREEZE — temporarily suspends customer and their ACTIVE accounts. */
    @Transactional
    public CustomerResponse freeze(UUID id) {
        Customer customer = byId(id);
        customer.setStatus(CustomerStatus.FROZEN);
        customerRepo.save(customer);
        cascadeAccountStatus(id, CustomerStatus.FROZEN);

        auditService.log(
                ActorContext.actorType(), ActorContext.actorId(),
                "CUSTOMER_FROZEN",
                "Customer", customer.getId().toString(),
                "reason=admin_FROZEN");

        return toResponse(customer);
    }

    /** RE-ACTIVATE — restores a FROZEN customer and their FROZEN accounts. */
    @Transactional
    public CustomerResponse reactivate(UUID id) {
        Customer customer = byId(id);
        if (customer.getStatus() == CustomerStatus.CLOSED)
            throw new IllegalStateException("Cannot re-activate a CLOSED customer");
        customer.setStatus(CustomerStatus.ACTIVE);
        customerRepo.save(customer);
        cascadeAccountStatus(id, CustomerStatus.ACTIVE);

        auditService.log(
                ActorContext.actorType(), ActorContext.actorId(),
                "CUSTOMER_REACTIVATED",
                "Customer", customer.getId().toString(),
                "reason=admin_REACTIVATED");

        return toResponse(customer);
    }

    /** CLOSE — permanently closes the customer and ALL their accounts. */
    @Transactional
    public CustomerResponse close(UUID id) {
        Customer customer = byId(id);
        customer.setStatus(CustomerStatus.CLOSED);
        customerRepo.save(customer);

        // Close ALL accounts regardless of current status
        List<Account> accounts = accountRepo.findByCustomer_Id(id);
        accounts.forEach(a -> a.setStatus(AccountStatus.CLOSED));
        accountRepo.saveAll(accounts);

        auditService.log(
                ActorContext.actorType(), ActorContext.actorId(),
                "CUSTOMER_CLOSED",
                "Customer", customer.getId().toString(),
                "reason=admin_CLOSED");

        return toResponse(customer);
    }

    // ── Cascade helper ───────────────────────────────────────────────────────

    /**
     * Cascades account status changes when customer status changes.
     * FROZEN  → freeze all ACTIVE accounts
     * ACTIVE  → restore all FROZEN accounts
     * CLOSED  → close ALL accounts (handled separately in close())
     */
    private void cascadeAccountStatus(UUID customerId, CustomerStatus newStatus) {
        List<Account> accounts = accountRepo.findByCustomer_Id(customerId);
        if (newStatus == CustomerStatus.FROZEN) {
            accounts.stream()
                    .filter(a -> a.getStatus() == AccountStatus.ACTIVE)
                    .forEach(a -> a.setStatus(AccountStatus.FROZEN));
            accountRepo.saveAll(accounts);
        } else if (newStatus == CustomerStatus.ACTIVE) {
            accounts.stream()
                    .filter(a -> a.getStatus() == AccountStatus.FROZEN)
                    .forEach(a -> a.setStatus(AccountStatus.ACTIVE));
            accountRepo.saveAll(accounts);
        }
    }

    // ── Legacy alias ─────────────────────────────────────────────────────────

    /** @deprecated Use freeze() instead */
    @Transactional
    public CustomerResponse disable(UUID id) {
        return freeze(id);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Customer byId(UUID id) {
        return customerRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + id));
    }

    private CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getEmail(),
                customer.getPhoneNumber(),
                customer.getType(),
                customer.getStatus());
    }
}
