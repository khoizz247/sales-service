package vn.edu.sales.api.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import vn.edu.sales.application.port.out.OrganizationStore;
import vn.edu.sales.application.service.OrganizationService;

import java.time.LocalDate;
import java.util.List;

@RestController
public class OrganizationController {
    private final OrganizationService service;
    public OrganizationController(OrganizationService service) { this.service = service; }

    @GetMapping("/api/admin/offices")
    public List<OrganizationStore.Office> offices() { return service.offices(); }
    @GetMapping("/api/admin/offices/{id}")
    public OrganizationStore.Office office(@PathVariable Long id) { return service.office(id); }
    @PostMapping("/api/admin/offices")
    @ResponseStatus(HttpStatus.CREATED)
    public OrganizationStore.Office createOffice(@Valid @RequestBody OfficeCreate request) {
        return service.createOffice(request.toOffice());
    }
    @PutMapping("/api/admin/offices/{id}")
    public OrganizationStore.Office updateOffice(@PathVariable Long id, @Valid @RequestBody OfficeUpdate request) {
        return service.updateOffice(id, request.toOffice());
    }

    @GetMapping("/api/admin/employees")
    public List<OrganizationStore.Employee> employees() { return service.employees(); }
    @GetMapping("/api/admin/employees/{userId}")
    public OrganizationStore.Employee employee(@PathVariable Long userId) { return service.employee(userId); }
    @PostMapping("/api/admin/employees")
    @ResponseStatus(HttpStatus.CREATED)
    public OrganizationStore.Employee createEmployee(@Valid @RequestBody EmployeeCreate request) {
        return service.createEmployee(request.toEmployee());
    }
    @PutMapping("/api/admin/employees/{userId}")
    public OrganizationStore.Employee updateEmployee(@PathVariable Long userId,
                                                       @Valid @RequestBody EmployeeUpdate request) {
        return service.updateEmployee(userId, request.toEmployee());
    }

    public record OfficeCreate(@NotBlank @Size(max=20) String officeCode,
                               @NotBlank @Size(max=120) String name,
                               @NotBlank @Size(max=30) String phone,
                               @Email @Size(max=190) String email,
                               @NotBlank @Size(max=150) String addressLine1,
                               @Size(max=150) String addressLine2,
                               @NotBlank @Size(max=80) String city,
                               @Size(max=80) String stateProvince,
                               @Size(max=20) String postalCode,
                               @NotBlank @Pattern(regexp="[A-Za-z]{2}") String countryCode) {
        OrganizationStore.Office toOffice() {
            return new OrganizationStore.Office(null, officeCode, name, phone, email, addressLine1,
                    addressLine2, city, stateProvince, postalCode, countryCode, "ACTIVE");
        }
    }
    public record OfficeUpdate(@NotBlank @Size(max=120) String name,
                               @NotBlank @Size(max=30) String phone,
                               @Email @Size(max=190) String email,
                               @NotBlank @Size(max=150) String addressLine1,
                               @Size(max=150) String addressLine2,
                               @NotBlank @Size(max=80) String city,
                               @Size(max=80) String stateProvince,
                               @Size(max=20) String postalCode,
                               @NotBlank @Pattern(regexp="[A-Za-z]{2}") String countryCode,
                               @NotNull @Pattern(regexp="ACTIVE|INACTIVE") String status) {
        OrganizationStore.Office toOffice() {
            return new OrganizationStore.Office(null, null, name, phone, email, addressLine1,
                    addressLine2, city, stateProvince, postalCode, countryCode, status);
        }
    }
    public record EmployeeCreate(@NotNull Long userId, @NotBlank @Size(max=30) String employeeCode,
                                 @NotNull Long officeId, Long managerUserId,
                                 @NotBlank @Size(max=100) String jobTitle,
                                 @Size(max=20) String extension,
                                 @NotNull @PastOrPresent LocalDate hireDate) {
        OrganizationStore.Employee toEmployee() {
            return new OrganizationStore.Employee(userId, employeeCode, officeId, managerUserId,
                    jobTitle, extension, hireDate, "ACTIVE");
        }
    }
    public record EmployeeUpdate(@NotNull Long officeId, Long managerUserId,
                                 @NotBlank @Size(max=100) String jobTitle,
                                 @Size(max=20) String extension,
                                 @NotNull @PastOrPresent LocalDate hireDate,
                                 @NotNull @Pattern(regexp="ACTIVE|INACTIVE") String status) {
        OrganizationStore.Employee toEmployee() {
            return new OrganizationStore.Employee(null, null, officeId, managerUserId,
                    jobTitle, extension, hireDate, status);
        }
    }
}
