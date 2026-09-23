package vn.edu.sales.application.port.out;

import java.util.List;
import java.util.Optional;

public interface AddressStore {
    record Address(Long id, Long customerUserId, String label, String recipientName, String recipientPhone,
                   String addressLine1, String addressLine2, String ward, String district, String city,
                   String stateProvince, String postalCode, String countryCode, boolean isDefault) {}
    List<Address> findByUserId(Long userId);
    Optional<Address> findById(Long id);
    Address create(Address address);
    Address update(Address address);
    void delete(Long id);
    void ensureProfile(Long userId);
    void clearDefault(Long userId);
}
