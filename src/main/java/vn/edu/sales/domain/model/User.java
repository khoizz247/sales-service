package vn.edu.sales.domain.model;

public record User(
        Long id,
        String email,
        String passwordHash,
        Role role
) {
}
