package vn.edu.sales.application.port.out;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderHistoryStore {
    List<Change> byOrder(Long orderId);

    record Change(Long id, Long orderId, String fromStatus, String toStatus,
                  Long changedByUserId, String note, LocalDateTime changedAt) {}
}
