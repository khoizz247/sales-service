package vn.edu.sales.application.port.out;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CustomerProfileStore {
    record Profile(Long userId, String email, String fullName, String phone,
                   LocalDate dateOfBirth, BigDecimal creditLimit) {}

    List<Profile> all();
    Optional<Profile> findByUserId(Long userId);
    void ensureProfile(Long userId);
    void updatePersonal(Long userId, String phone, LocalDate dateOfBirth);
    void updateCreditLimit(Long userId, BigDecimal creditLimit);
}
