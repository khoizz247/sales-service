package vn.edu.sales.application.service;

import vn.edu.sales.application.port.out.CustomerProfileStore;
import vn.edu.sales.application.port.out.TransactionRunner;
import vn.edu.sales.application.port.out.UserRepository;
import vn.edu.sales.domain.exception.ResourceNotFoundException;
import vn.edu.sales.domain.model.Role;
import vn.edu.sales.domain.model.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

public class CustomerProfileService {
    private final CustomerProfileStore profiles;
    private final UserRepository users;
    private final TransactionRunner transactions;

    public CustomerProfileService(CustomerProfileStore profiles, UserRepository users, TransactionRunner transactions) {
        this.profiles = profiles; this.users = users; this.transactions = transactions;
    }

    public CustomerProfileStore.Profile mine(String email) { return byUserId(customer(email).id()); }

    public CustomerProfileStore.Profile updateMine(String email, String phone, LocalDate dateOfBirth) {
        return transactions.execute(() -> {
            Long id = customer(email).id();
            byUserId(id);
            if (dateOfBirth != null && dateOfBirth.isAfter(LocalDate.now()))
                throw new IllegalArgumentException("Ngày sinh không thể ở tương lai");
            profiles.updatePersonal(id, phone == null ? null : phone.trim(), dateOfBirth);
            return byUserId(id);
        });
    }

    public List<CustomerProfileStore.Profile> all() { return profiles.all(); }

    public CustomerProfileStore.Profile byUserId(Long id) {
        requireCustomer(id);
        return profiles.findByUserId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ khách hàng: " + id));
    }

    public CustomerProfileStore.Profile setCreditLimit(Long id, BigDecimal amount) {
        return transactions.execute(() -> {
            byUserId(id);
            if (amount == null || amount.signum() < 0)
                throw new IllegalArgumentException("Hạn mức tín dụng không hợp lệ");
            profiles.updateCreditLimit(id, amount);
            return byUserId(id);
        });
    }

    private User customer(String email) {
        return users.findByEmail(email.toLowerCase(Locale.ROOT))
                .filter(user -> user.role() == Role.CUSTOMER)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng"));
    }

    private void requireCustomer(Long id) {
        users.findById(id).filter(user -> user.role() == Role.CUSTOMER)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng: " + id));
    }
}
