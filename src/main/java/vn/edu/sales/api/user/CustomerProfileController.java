package vn.edu.sales.api.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.edu.sales.application.port.out.CustomerProfileStore;
import vn.edu.sales.application.service.CustomerProfileService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
public class CustomerProfileController {
    private final CustomerProfileService service;
    public CustomerProfileController(CustomerProfileService service) { this.service = service; }

    @GetMapping("/api/users/me/profile")
    public CustomerProfileStore.Profile mine(Authentication authentication) {
        return service.mine(authentication.getName());
    }

    @PutMapping("/api/users/me/profile")
    public CustomerProfileStore.Profile updateMine(Authentication authentication,
                                                    @Valid @RequestBody PersonalRequest request) {
        return service.updateMine(authentication.getName(), request.phone(), request.dateOfBirth());
    }

    @GetMapping("/api/admin/customers")
    public List<CustomerProfileStore.Profile> all() { return service.all(); }

    @GetMapping("/api/admin/customers/{userId}/profile")
    public CustomerProfileStore.Profile byUserId(@PathVariable Long userId) { return service.byUserId(userId); }

    @PatchMapping("/api/admin/customers/{userId}/credit-limit")
    public CustomerProfileStore.Profile creditLimit(@PathVariable Long userId,
                                                     @Valid @RequestBody CreditLimitRequest request) {
        return service.setCreditLimit(userId, request.creditLimit());
    }

    public record PersonalRequest(@Pattern(regexp="[0-9+(). -]{8,30}") String phone, LocalDate dateOfBirth) {}
    public record CreditLimitRequest(@NotNull @DecimalMin("0.00") @Digits(integer=13, fraction=2)
                                     BigDecimal creditLimit) {}
}
