package vn.edu.sales.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.edu.sales.application.port.out.PasswordHasher;
import vn.edu.sales.application.port.out.OrderRepository;
import vn.edu.sales.application.port.out.ProductRepository;
import vn.edu.sales.application.port.out.TokenProvider;
import vn.edu.sales.application.port.out.TransactionRunner;
import vn.edu.sales.application.port.out.UserRepository;
import vn.edu.sales.application.service.AuthService;
import vn.edu.sales.application.service.OrderService;
import vn.edu.sales.application.service.ProductService;

@Configuration
public class ApplicationBeanConfig {

    @Bean
    ProductService productService(ProductRepository productRepository) {
        return new ProductService(productRepository);
    }

    @Bean
    AuthService authService(UserRepository userRepository, PasswordHasher passwordHasher, TokenProvider tokenProvider) {
        return new AuthService(userRepository, passwordHasher, tokenProvider);
    }

    @Bean
    OrderService orderService(OrderRepository orderRepository, ProductRepository productRepository,
                              UserRepository userRepository, TransactionRunner transactionRunner) {
        return new OrderService(orderRepository, productRepository, userRepository, transactionRunner);
    }
}
