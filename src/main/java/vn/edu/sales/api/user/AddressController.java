package vn.edu.sales.api.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.edu.sales.application.port.out.AddressStore;
import vn.edu.sales.application.service.AddressService;

import java.util.List;

@RestController
@RequestMapping("/api/users/me/addresses")
public class AddressController {
    private final AddressService service;
    public AddressController(AddressService service) { this.service = service; }
    @GetMapping public List<AddressStore.Address> list(Authentication authentication) {
        return service.mine(authentication.getName());
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public AddressStore.Address create(Authentication authentication, @Valid @RequestBody AddressRequest request) {
        return service.create(authentication.getName(), request.toAddress());
    }
    @PutMapping("/{id}")
    public AddressStore.Address update(Authentication authentication, @PathVariable Long id,
                                       @Valid @RequestBody AddressRequest request) {
        return service.update(authentication.getName(), id, request.toAddress());
    }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication authentication, @PathVariable Long id) {
        service.delete(authentication.getName(), id);
    }
    public record AddressRequest(@NotBlank @Size(max=50) String label,
                                 @NotBlank @Size(max=120) String recipientName,
                                 @NotBlank @Size(min=8,max=30) String recipientPhone,
                                 @NotBlank @Size(max=150) String addressLine1,
                                 @Size(max=150) String addressLine2, @Size(max=80) String ward,
                                 @Size(max=80) String district, @NotBlank @Size(max=80) String city,
                                 @Size(max=80) String stateProvince, @Size(max=20) String postalCode,
                                 @NotBlank @Pattern(regexp="[A-Za-z]{2}") String countryCode, boolean isDefault) {
        AddressStore.Address toAddress() {
            return new AddressStore.Address(null, null, label, recipientName, recipientPhone, addressLine1,
                    addressLine2, ward, district, city, stateProvince, postalCode,
                    countryCode.toUpperCase(java.util.Locale.ROOT), isDefault);
        }
    }
}
