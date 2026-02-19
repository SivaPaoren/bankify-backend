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
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepo;
    private final AccountRepository accountRepo;

    // Get all the list of customers
    public List<CustomerResponse> getCustomers() {
        List<Customer> customers = customerRepo.findAll();

        return customers.stream()
                .map(this::toResponse)
                .toList();
    }

    // Create a customer
    @Transactional
    public CustomerResponse create(CreateCustomerRequest req) {

        // check if the email is unique
        if (customerRepo.existsByEmail(req.email())) {
            throw new IllegalArgumentException("Email already exists");
        }

        Customer customer = Customer.builder()
                .firstName(req.firstName())
                .lastName(req.lastName())
                .email(req.email())
                .phoneNumber(req.phoneNumber())
                .type(EnumMapper.toEnum(CustomerType.class, req.type()))
                .status(CustomerStatus.ACTIVE)
                .build();

        customerRepo.save(customer);
        return toResponse(customer);
    }

    public CustomerResponse getCustomerById(UUID id) {
        return toResponse(byId(id));
    }

    // helper method to find by id
    private Customer byId(UUID id) {
        return customerRepo.findById(id).orElseThrow(
                () -> new IllegalArgumentException("Customer not found" + id));
    }

    @Transactional
    public CustomerResponse updateCustomer(UUID id, UpdateCustomerRequest req) {
        Customer customer = byId(id);

        if (req.firstName() != null) customer.setFirstName(req.firstName());
        if (req.lastName() != null)  customer.setLastName(req.lastName());
        if (req.phone() != null)     customer.setPhoneNumber(req.phone());

        if (req.status() != null) {
            CustomerStatus newStatus = EnumMapper.toEnum(CustomerStatus.class, req.status());
            customer.setStatus(newStatus);

            // ── Cascade to accounts ──────────────────────────────────────────
            List<Account> accounts = accountRepo.findByCustomer_Id(id);
            if (newStatus == CustomerStatus.FROZEN) {
                // Freeze all ACTIVE accounts — frozen customer cannot transact
                accounts.stream()
                        .filter(a -> a.getStatus() == AccountStatus.ACTIVE)
                        .forEach(a -> a.setStatus(AccountStatus.FROZEN));
                accountRepo.saveAll(accounts);
            } else if (newStatus == CustomerStatus.ACTIVE) {
                // Re-activate all FROZEN accounts when customer is re-enabled
                accounts.stream()
                        .filter(a -> a.getStatus() == AccountStatus.FROZEN)
                        .forEach(a -> a.setStatus(AccountStatus.ACTIVE));
                accountRepo.saveAll(accounts);
            }
        }

        Customer saved = customerRepo.save(customer);
        return toResponse(saved);
    }

    @Transactional
    public CustomerResponse disable(UUID id) {
        Customer customer = customerRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Customer cannot be disabled, because customer is not found"));

        customer.setStatus(CustomerStatus.FROZEN);
        customerRepo.save(customer);

        // ── Cascade: freeze all ACTIVE accounts of this customer ─────────
        List<Account> accounts = accountRepo.findByCustomer_Id(id);
        accounts.stream()
                .filter(a -> a.getStatus() == AccountStatus.ACTIVE)
                .forEach(a -> a.setStatus(AccountStatus.FROZEN));
        accountRepo.saveAll(accounts);

        return toResponse(customer);
    }

    // helper that help Entity -> DTO
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
