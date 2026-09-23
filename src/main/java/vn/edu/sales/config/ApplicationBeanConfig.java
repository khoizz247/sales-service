package vn.edu.sales.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.edu.sales.application.port.out.PasswordHasher;
import vn.edu.sales.application.port.out.CatalogStore;
import vn.edu.sales.application.port.out.AddressStore;
import vn.edu.sales.application.port.out.AdminAccountAuditStore;
import vn.edu.sales.application.port.out.CartStore;
import vn.edu.sales.application.port.out.PaymentStore;
import vn.edu.sales.application.port.out.InventoryStore;
import vn.edu.sales.application.port.out.CustomerProfileStore;
import vn.edu.sales.application.port.out.OrganizationStore;
import vn.edu.sales.application.port.out.OrderRepository;
import vn.edu.sales.application.port.out.OrderSearchStore;
import vn.edu.sales.application.port.out.ProductRepository;
import vn.edu.sales.application.port.out.TokenProvider;
import vn.edu.sales.application.port.out.TransactionRunner;
import vn.edu.sales.application.port.out.UserRepository;
import vn.edu.sales.application.service.AuthService;
import vn.edu.sales.application.service.CatalogService;
import vn.edu.sales.application.service.AddressService;
import vn.edu.sales.application.service.CartService;
import vn.edu.sales.application.service.PasswordChangeService;
import vn.edu.sales.application.service.OrderService;
import vn.edu.sales.application.service.ProductService;
import vn.edu.sales.application.service.PaymentService;
import vn.edu.sales.application.service.InventoryService;
import vn.edu.sales.application.service.CustomerProfileService;
import vn.edu.sales.application.service.OrganizationService;
import vn.edu.sales.application.service.AdminAccountService;

@Configuration
public class ApplicationBeanConfig {

    @Bean
    ProductService productService(ProductRepository productRepository, InventoryStore inventoryStore,
                                  TransactionRunner transactionRunner, UserRepository userRepository) {
        return new ProductService(productRepository, inventoryStore, transactionRunner, userRepository);
    }

    @Bean
    AuthService authService(UserRepository userRepository, PasswordHasher passwordHasher, TokenProvider tokenProvider,
                            CustomerProfileStore customerProfileStore, TransactionRunner transactionRunner) {
        return new AuthService(userRepository, passwordHasher, tokenProvider, customerProfileStore, transactionRunner);
    }

    @Bean
    OrderService orderService(OrderRepository orderRepository, ProductRepository productRepository,
                              UserRepository userRepository, TransactionRunner transactionRunner,
                              InventoryStore inventoryStore, PaymentStore paymentStore,
                              OrderSearchStore searchStore) {
        return new OrderService(orderRepository, productRepository, userRepository, transactionRunner,
                inventoryStore, paymentStore, searchStore);
    }

    @Bean
    CatalogService catalogService(CatalogStore catalogStore, ProductRepository productRepository) {
        return new CatalogService(catalogStore, productRepository);
    }

    @Bean
    AddressService addressService(AddressStore addressStore, UserRepository userRepository,
                                  TransactionRunner transactionRunner) {
        return new AddressService(addressStore, userRepository, transactionRunner);
    }

    @Bean
    PaymentService paymentService(PaymentStore paymentStore, OrderRepository orderRepository,
                                  UserRepository userRepository, TransactionRunner transactionRunner) {
        return new PaymentService(paymentStore, orderRepository, userRepository, transactionRunner);
    }

    @Bean
    InventoryService inventoryService(InventoryStore inventoryStore, ProductRepository productRepository,
                                      UserRepository userRepository, TransactionRunner transactionRunner) {
        return new InventoryService(inventoryStore, productRepository, userRepository, transactionRunner);
    }

    @Bean
    CustomerProfileService customerProfileService(CustomerProfileStore customerProfileStore,
                                                  UserRepository userRepository, TransactionRunner transactionRunner) {
        return new CustomerProfileService(customerProfileStore, userRepository, transactionRunner);
    }

    @Bean
    OrganizationService organizationService(OrganizationStore organizationStore, UserRepository userRepository,
                                            TransactionRunner transactionRunner) {
        return new OrganizationService(organizationStore, userRepository, transactionRunner);
    }

    @Bean
    AdminAccountService adminAccountService(UserRepository users, PasswordHasher passwords,
                                            OrganizationService organization, TransactionRunner transactions,
                                            AdminAccountAuditStore audit) {
        return new AdminAccountService(users, passwords, organization, transactions, audit);
    }

    @Bean
    CartService cartService(CartStore carts, UserRepository users, ProductRepository products,
                            OrderService orders, TransactionRunner transactions) {
        return new CartService(carts, users, products, orders, transactions);
    }

    @Bean
    PasswordChangeService passwordChangeService(UserRepository users, PasswordHasher passwords,
                                                TransactionRunner transactions) {
        return new PasswordChangeService(users, passwords, transactions);
    }
}
