package vn.edu.sales.infrastructure.persistence.user;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import vn.edu.sales.application.port.out.OrganizationStore;

import java.util.List;
import java.util.Optional;

@Repository
public class JdbcOrganizationStore implements OrganizationStore {
    private static final String OFFICE_COLUMNS = "id,office_code,name,phone,email,address_line1,address_line2," +
            "city,state_province,postal_code,country_code,status";
    private static final String EMPLOYEE_COLUMNS = "user_id,employee_code,office_id,manager_user_id," +
            "job_title,extension,hire_date,status";
    private static final RowMapper<Office> OFFICE = (rs, row) -> new Office(
            rs.getLong("id"), rs.getString("office_code"), rs.getString("name"), rs.getString("phone"),
            rs.getString("email"), rs.getString("address_line1"), rs.getString("address_line2"),
            rs.getString("city"), rs.getString("state_province"), rs.getString("postal_code"),
            rs.getString("country_code"), rs.getString("status"));
    private static final RowMapper<Employee> EMPLOYEE = (rs, row) -> new Employee(
            rs.getLong("user_id"), rs.getString("employee_code"), rs.getLong("office_id"),
            rs.getObject("manager_user_id", Long.class), rs.getString("job_title"),
            rs.getString("extension"), rs.getDate("hire_date").toLocalDate(), rs.getString("status"));
    private final JdbcTemplate jdbc;
    public JdbcOrganizationStore(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override public List<Office> offices() {
        return jdbc.query("SELECT " + OFFICE_COLUMNS + " FROM offices ORDER BY id", OFFICE);
    }
    @Override public Optional<Office> office(Long id) {
        return jdbc.query("SELECT " + OFFICE_COLUMNS + " FROM offices WHERE id=?", OFFICE, id).stream().findFirst();
    }
    @Override public boolean officeCodeExists(String code) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM offices WHERE office_code=?", Integer.class, code) > 0;
    }
    @Override public Office createOffice(Office office) {
        jdbc.update("INSERT INTO offices(office_code,name,phone,email,address_line1,address_line2,city," +
                        "state_province,postal_code,country_code,status) VALUES(?,?,?,?,?,?,?,?,?,?,?)",
                office.officeCode(), office.name(), office.phone(), office.email(), office.addressLine1(),
                office.addressLine2(), office.city(), office.stateProvince(), office.postalCode(),
                office.countryCode(), office.status());
        return jdbc.query("SELECT " + OFFICE_COLUMNS + " FROM offices WHERE office_code=?", OFFICE,
                office.officeCode()).getFirst();
    }
    @Override public Office updateOffice(Office office) {
        jdbc.update("UPDATE offices SET name=?,phone=?,email=?,address_line1=?,address_line2=?,city=?," +
                        "state_province=?,postal_code=?,country_code=?,status=? WHERE id=?",
                office.name(), office.phone(), office.email(), office.addressLine1(), office.addressLine2(),
                office.city(), office.stateProvince(), office.postalCode(), office.countryCode(),
                office.status(), office.id());
        return office(office.id()).orElseThrow();
    }
    @Override public boolean hasActiveEmployees(Long officeId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM employee_profiles WHERE office_id=? AND status='ACTIVE'",
                Integer.class, officeId) > 0;
    }
    @Override public List<Employee> employees() {
        return jdbc.query("SELECT " + EMPLOYEE_COLUMNS + " FROM employee_profiles ORDER BY user_id", EMPLOYEE);
    }
    @Override public Optional<Employee> employee(Long userId) {
        return jdbc.query("SELECT " + EMPLOYEE_COLUMNS + " FROM employee_profiles WHERE user_id=?",
                EMPLOYEE, userId).stream().findFirst();
    }
    @Override public boolean employeeCodeExists(String code) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM employee_profiles WHERE employee_code=?",
                Integer.class, code) > 0;
    }
    @Override public Employee createEmployee(Employee employee) {
        jdbc.update("INSERT INTO employee_profiles(user_id,employee_code,office_id,manager_user_id," +
                        "job_title,extension,hire_date,status) VALUES(?,?,?,?,?,?,?,?)",
                employee.userId(), employee.employeeCode(), employee.officeId(), employee.managerUserId(),
                employee.jobTitle(), employee.extension(), employee.hireDate(), employee.status());
        return employee(employee.userId()).orElseThrow();
    }
    @Override public Employee updateEmployee(Employee employee) {
        jdbc.update("UPDATE employee_profiles SET office_id=?,manager_user_id=?,job_title=?,extension=?," +
                        "hire_date=?,status=? WHERE user_id=?", employee.officeId(), employee.managerUserId(),
                employee.jobTitle(), employee.extension(), employee.hireDate(), employee.status(), employee.userId());
        return employee(employee.userId()).orElseThrow();
    }
    @Override public boolean hasActiveReports(Long managerUserId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM employee_profiles WHERE manager_user_id=? AND status='ACTIVE'",
                Integer.class, managerUserId) > 0;
    }
}
