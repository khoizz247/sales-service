package vn.edu.sales.infrastructure.bootstrap;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.edu.sales.domain.model.Role;
import vn.edu.sales.infrastructure.persistence.product.ProductJpaEntity;
import vn.edu.sales.infrastructure.persistence.product.SpringDataProductRepository;
import vn.edu.sales.infrastructure.persistence.user.SpringDataUserRepository;
import vn.edu.sales.infrastructure.persistence.user.UserJpaEntity;

import java.math.BigDecimal;

@Configuration
public class SeedDataConfig {

    @Bean
    @ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
    CommandLineRunner seedData(
            SpringDataUserRepository userRepository,
            SpringDataProductRepository productRepository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {
            if (!userRepository.existsByEmail("admin@example.com")) {
                userRepository.save(new UserJpaEntity(
                        null, "admin@example.com", passwordEncoder.encode("Admin@123"), Role.ADMIN
                ));
            }
            if (productRepository.count() == 0) {
                productRepository.save(new ProductJpaEntity(
                        null,
                        "Bàn phím cơ mẫu",
                        "Dữ liệu mẫu để kiểm tra API",
                        new BigDecimal("890000.00"),
                        20,
                        true
                ));
                productRepository.save(new ProductJpaEntity(
                        null,
                        "Chuột không dây mẫu",
                        "Dữ liệu mẫu để kiểm tra API",
                        new BigDecimal("450000.00"),
                        35,
                        true
                ));
            }
        };
    }
}
