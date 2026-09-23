package vn.edu.sales.application.service;

import vn.edu.sales.application.port.out.OrganizationStore;
import vn.edu.sales.application.port.out.AdminAccountAuditStore;
import vn.edu.sales.application.port.out.PasswordHasher;
import vn.edu.sales.application.port.out.TransactionRunner;
import vn.edu.sales.application.port.out.UserRepository;
import vn.edu.sales.domain.exception.BusinessConflictException;
import vn.edu.sales.domain.exception.InvalidCredentialsException;
import vn.edu.sales.domain.model.Role;
import vn.edu.sales.domain.model.User;
import vn.edu.sales.domain.model.UserStatus;

import java.time.LocalDate;
import java.util.Locale;
import java.nio.charset.StandardCharsets;

/** Provisions an ADMIN account and its employee profile in one transaction. */
public class AdminAccountService {
    private final UserRepository users;
    private final PasswordHasher passwords;
    private final OrganizationService organization;
    private final TransactionRunner transactions;
    private final AdminAccountAuditStore audit;

    public AdminAccountService(UserRepository users, PasswordHasher passwords,
                               OrganizationService organization, TransactionRunner transactions,
                               AdminAccountAuditStore audit) {
        this.users = users;
        this.passwords = passwords;
        this.organization = organization;
        this.transactions = transactions;
        this.audit = audit;
    }

    public ProvisionedAccount create(String actorEmail, String actorPassword, String fullName,
                                     String email, String password, String employeeCode, Long officeId,
                                     Long managerUserId, String jobTitle, String extension, LocalDate hireDate) {
        return transactions.execute(() -> {
            User actor = users.findByEmail(actorEmail.toLowerCase(Locale.ROOT))
                    .filter(user -> user.role() == Role.ADMIN && user.status() == UserStatus.ACTIVE)
                    .orElseThrow(() -> new InvalidCredentialsException("Tài khoản quản trị không còn hoạt động"));
            if (!passwords.matches(actorPassword, actor.passwordHash()))
                throw new InvalidCredentialsException("Mật khẩu quản trị không chính xác");
            if (password == null || password.length() < 12
                    || password.getBytes(StandardCharsets.UTF_8).length > 72
                    || !password.matches("(?s)(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).*"))
                throw new IllegalArgumentException("Mật khẩu nhân viên cần 12-72 ký tự, gồm chữ hoa, chữ thường, số và ký tự đặc biệt");
            if (passwords.matches(password, actor.passwordHash()))
                throw new IllegalArgumentException("Không dùng lại mật khẩu quản trị hiện tại");
            String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
            if (users.existsByEmail(normalizedEmail)) throw new BusinessConflictException("Email đã được sử dụng");
            User created = users.save(new User(null, normalizedEmail, passwords.hash(password),
                    fullName.trim(), Role.ADMIN, UserStatus.ACTIVE, null));
            OrganizationStore.Employee employee = organization.createEmployee(new OrganizationStore.Employee(
                    created.id(), employeeCode, officeId, managerUserId, jobTitle, extension, hireDate, "ACTIVE"));
            audit.record(actor.id(), created.id());
            return new ProvisionedAccount(created.id(), created.email(), created.fullName(), created.role(), employee);
        });
    }

    public record ProvisionedAccount(Long userId, String email, String fullName, Role role,
                                     OrganizationStore.Employee employee) {}
}
