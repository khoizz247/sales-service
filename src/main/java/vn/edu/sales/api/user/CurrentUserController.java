package vn.edu.sales.api.user;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class CurrentUserController {

    @GetMapping("/me")
    public CurrentUserResponse me(Authentication authentication) {
        List<String> roles = authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority().replace("ROLE_", ""))
                .toList();
        return new CurrentUserResponse(authentication.getName(), roles);
    }

    public record CurrentUserResponse(String email, List<String> roles) {
    }
}
