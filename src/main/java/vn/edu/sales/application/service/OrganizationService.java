package vn.edu.sales.application.service;

import vn.edu.sales.application.port.out.OrganizationStore;
import vn.edu.sales.application.port.out.TransactionRunner;
import vn.edu.sales.application.port.out.UserRepository;
import vn.edu.sales.domain.exception.BusinessConflictException;
import vn.edu.sales.domain.exception.ResourceNotFoundException;
import vn.edu.sales.domain.model.Role;
import vn.edu.sales.domain.model.UserStatus;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class OrganizationService {
    private final OrganizationStore store;
    private final UserRepository users;
    private final TransactionRunner transactions;

    public OrganizationService(OrganizationStore store, UserRepository users, TransactionRunner transactions) {
        this.store = store; this.users = users; this.transactions = transactions;
    }

    public List<OrganizationStore.Office> offices() { return store.offices(); }
    public OrganizationStore.Office office(Long id) {
        return store.office(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy văn phòng: " + id));
    }
    public OrganizationStore.Office createOffice(OrganizationStore.Office request) {
        return transactions.execute(() -> {
            String code = request.officeCode().trim().toUpperCase(Locale.ROOT);
            if (store.officeCodeExists(code)) throw new BusinessConflictException("Mã văn phòng đã tồn tại: " + code);
            return store.createOffice(copyOffice(request, null, code));
        });
    }
    public OrganizationStore.Office updateOffice(Long id, OrganizationStore.Office request) {
        return transactions.execute(() -> {
            OrganizationStore.Office old = office(id);
            if ("INACTIVE".equals(request.status()) && store.hasActiveEmployees(id))
                throw new BusinessConflictException("Văn phòng còn nhân viên đang hoạt động");
            return store.updateOffice(copyOffice(request, id, old.officeCode()));
        });
    }

    public List<OrganizationStore.Employee> employees() { return store.employees(); }
    public OrganizationStore.Employee employee(Long userId) {
        return store.employee(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ nhân viên: " + userId));
    }
    public OrganizationStore.Employee createEmployee(OrganizationStore.Employee request) {
        return transactions.execute(() -> {
            requireAdminAccount(request.userId());
            if (store.employee(request.userId()).isPresent())
                throw new BusinessConflictException("Tài khoản đã có hồ sơ nhân viên");
            String code = request.employeeCode().trim().toUpperCase(Locale.ROOT);
            if (store.employeeCodeExists(code)) throw new BusinessConflictException("Mã nhân viên đã tồn tại: " + code);
            validateAssignment(request.userId(), request.officeId(), request.managerUserId(), request.status());
            return store.createEmployee(copyEmployee(request, code));
        });
    }
    public OrganizationStore.Employee updateEmployee(Long userId, OrganizationStore.Employee request) {
        return transactions.execute(() -> {
            OrganizationStore.Employee old = employee(userId);
            if ("INACTIVE".equals(request.status()) && store.hasActiveReports(userId))
                throw new BusinessConflictException("Nhân viên còn cấp dưới đang hoạt động");
            validateAssignment(userId, request.officeId(), request.managerUserId(), request.status());
            return store.updateEmployee(new OrganizationStore.Employee(userId, old.employeeCode(),
                    request.officeId(), request.managerUserId(), request.jobTitle().trim(), request.extension(),
                    request.hireDate(), request.status()));
        });
    }

    private OrganizationStore.Office copyOffice(OrganizationStore.Office o, Long id, String code) {
        return new OrganizationStore.Office(id, code, o.name().trim(), o.phone().trim(), o.email(),
                o.addressLine1().trim(), o.addressLine2(), o.city().trim(), o.stateProvince(),
                o.postalCode(), o.countryCode().toUpperCase(Locale.ROOT), o.status());
    }
    private OrganizationStore.Employee copyEmployee(OrganizationStore.Employee e, String code) {
        return new OrganizationStore.Employee(e.userId(), code, e.officeId(), e.managerUserId(),
                e.jobTitle().trim(), e.extension(), e.hireDate(), e.status());
    }
    private void requireAdminAccount(Long userId) {
        users.findById(userId).filter(user -> user.role() == Role.ADMIN && user.status() == UserStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản ADMIN đang hoạt động: " + userId));
    }
    private void validateAssignment(Long userId, Long officeId, Long managerUserId, String status) {
        OrganizationStore.Office office = office(officeId);
        if ("ACTIVE".equals(status) && !"ACTIVE".equals(office.status()))
            throw new BusinessConflictException("Không thể gán nhân viên đang hoạt động vào văn phòng ngừng hoạt động");
        if (managerUserId == null) return;
        Set<Long> visited = new HashSet<>();
        Long cursor = managerUserId;
        while (cursor != null) {
            if (cursor.equals(userId) || !visited.add(cursor))
                throw new BusinessConflictException("Quan hệ quản lý tạo vòng lặp");
            OrganizationStore.Employee manager = employee(cursor);
            requireAdminAccount(cursor);
            if (!"ACTIVE".equals(manager.status()))
                throw new BusinessConflictException("Người quản lý không còn hoạt động");
            cursor = manager.managerUserId();
        }
    }
}
