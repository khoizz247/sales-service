package vn.edu.sales.application.port.out;

public interface AdminAccountAuditStore {
    void record(Long actorUserId, Long createdUserId);
}
