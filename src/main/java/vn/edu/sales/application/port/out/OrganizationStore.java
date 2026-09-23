package vn.edu.sales.application.port.out;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface OrganizationStore {
    record Office(Long id, String officeCode, String name, String phone, String email,
                  String addressLine1, String addressLine2, String city, String stateProvince,
                  String postalCode, String countryCode, String status) {}
    record Employee(Long userId, String employeeCode, Long officeId, Long managerUserId,
                    String jobTitle, String extension, LocalDate hireDate, String status) {}

    List<Office> offices();
    Optional<Office> office(Long id);
    boolean officeCodeExists(String code);
    Office createOffice(Office office);
    Office updateOffice(Office office);
    boolean hasActiveEmployees(Long officeId);

    List<Employee> employees();
    Optional<Employee> employee(Long userId);
    boolean employeeCodeExists(String code);
    Employee createEmployee(Employee employee);
    Employee updateEmployee(Employee employee);
    boolean hasActiveReports(Long managerUserId);
}
