package vn.edu.sales.application.service;

import vn.edu.sales.application.port.out.PasswordHasher;
import vn.edu.sales.application.port.out.TokenProvider;
import vn.edu.sales.application.port.out.UserRepository;
import vn.edu.sales.application.port.out.CustomerProfileStore;
import vn.edu.sales.application.port.out.TransactionRunner;
import vn.edu.sales.domain.exception.BusinessConflictException;
import vn.edu.sales.domain.exception.InvalidCredentialsException;
import vn.edu.sales.domain.model.Role;
import vn.edu.sales.domain.model.User;
import vn.edu.sales.domain.model.UserStatus;

import java.util.Locale;

public class AuthService {
    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final TokenProvider tokenProvider;
    private final CustomerProfileStore profiles;
    private final TransactionRunner transactions;

    public AuthService(UserRepository userRepository, PasswordHasher passwordHasher, TokenProvider tokenProvider,
                       CustomerProfileStore profiles, TransactionRunner transactions) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.tokenProvider = tokenProvider;
        this.profiles = profiles;
        this.transactions = transactions;
    }

    public AuthResult register(String fullName, String email, String rawPassword) {
        String normalizedEmail = normalizeEmail(email);
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new BusinessConflictException("Email đã được sử dụng");
        }

        User user = transactions.execute(() -> {
            User saved = userRepository.save(new User(
                null,
                normalizedEmail,
                passwordHasher.hash(rawPassword),
                fullName.trim(),
                Role.CUSTOMER,
                UserStatus.ACTIVE,
                null
            ));
            profiles.ensureProfile(saved.id());
            return saved;
        });
        return new AuthResult(tokenProvider.generate(user.email(), user.role()), user.email(), user.role());
    }

    public AuthResult login(String email, String rawPassword) {
        String normalizedEmail = normalizeEmail(email);
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new InvalidCredentialsException("Email hoặc mật khẩu không chính xác"));

        if (user.status() != UserStatus.ACTIVE || !passwordHasher.matches(rawPassword, user.passwordHash())) {
            throw new InvalidCredentialsException("Email hoặc mật khẩu không chính xác");
        }

        return new AuthResult(tokenProvider.generate(user.email(), user.role()), user.email(), user.role());
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public record AuthResult(String accessToken, String email, Role role) {
    }
}
