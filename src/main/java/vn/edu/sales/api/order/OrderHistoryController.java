package vn.edu.sales.api.order;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.sales.application.port.out.OrderHistoryStore;
import vn.edu.sales.application.port.out.OrderRepository;
import vn.edu.sales.application.service.OrderService;
import vn.edu.sales.domain.exception.ResourceNotFoundException;

import java.util.List;

@RestController
public class OrderHistoryController {
    private final OrderHistoryStore history;
    private final OrderService orders;
    private final OrderRepository orderRepository;

    public OrderHistoryController(OrderHistoryStore history, OrderService orders, OrderRepository orderRepository) {
        this.history = history;
        this.orders = orders;
        this.orderRepository = orderRepository;
    }

    @GetMapping("/api/orders/{orderId}/history")
    public List<OrderHistoryStore.Change> mine(Authentication authentication, @PathVariable Long orderId) {
        orders.getMineById(authentication.getName(), orderId);
        return history.byOrder(orderId);
    }

    @GetMapping("/api/admin/orders/{orderId}/history")
    public List<OrderHistoryStore.Change> byOrder(@PathVariable Long orderId) {
        orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng: " + orderId));
        return history.byOrder(orderId);
    }
}
