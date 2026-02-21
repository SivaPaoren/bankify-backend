package seniorproject.bankifycore.web.v1;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import seniorproject.bankifycore.consants.ApiPaths;
import seniorproject.bankifycore.dto.customer.CreateCustomerRequest;
import seniorproject.bankifycore.dto.customer.UpdateCustomerRequest;
import seniorproject.bankifycore.dto.customer.CustomerResponse;
import seniorproject.bankifycore.service.CustomerService;

@RestController
@RequestMapping(ApiPaths.CUSTOMERS)
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public List<CustomerResponse> getCustomers() {
        return customerService.getCustomers();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ResponseEntity<CustomerResponse> create(
            @Valid @RequestBody CreateCustomerRequest request) {
        CustomerResponse customer = customerService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(customer);
    }

    @GetMapping("/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ResponseEntity<CustomerResponse> get(@PathVariable UUID customerId) {
        CustomerResponse customer = customerService.getCustomerById(customerId);
        return ResponseEntity.status(HttpStatus.OK).body(customer);
    }

    @PatchMapping("/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ResponseEntity<CustomerResponse> update(@PathVariable UUID customerId,
            @Valid @RequestBody UpdateCustomerRequest request) {
        CustomerResponse updateCustomer = customerService.updateCustomer(customerId, request);
        return ResponseEntity.status(HttpStatus.OK).body(updateCustomer);
    }

    @PatchMapping("/{id}/disable")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public CustomerResponse disableCustomer(@PathVariable UUID id) {
        return customerService.disable(id);
    }



}
