package vn.edu.sales.api.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.sales.application.service.AdminAccountService;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/employee-accounts")
public class AdminAccountController {
    private final AdminAccountService service;

    public AdminAccountController(AdminAccountService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminAccountService.ProvisionedAccount create(Authentication authentication,
                                                         @Valid @RequestBody CreateEmployeeAccount request) {
        return service.create(authentication.getName(), request.currentPassword(), request.fullName(),
                request.email(), request.password(), request.employeeCode(), request.officeId(),
                request.managerUserId(), request.jobTitle(), request.extension(), request.hireDate());
    }

    public record CreateEmployeeAccount(
            @NotBlank String currentPassword,
            @NotBlank @Size(max = 120) String fullName,
            @NotBlank @Email @Size(max = 190) String email,
            @NotBlank @Size(min = 12, max = 72) String password,
            @NotBlank @Size(max = 30) String employeeCode,
            @NotNull Long officeId,
            Long managerUserId,
            @NotBlank @Size(max = 100) String jobTitle,
            @Size(max = 20) String extension,
            @NotNull @PastOrPresent LocalDate hireDate) {}
}
