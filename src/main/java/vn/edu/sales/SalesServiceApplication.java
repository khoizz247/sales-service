package vn.edu.sales;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class SalesServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SalesServiceApplication.class, args);
    }
}
