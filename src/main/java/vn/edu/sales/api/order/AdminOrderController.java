package vn.edu.sales.api.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;
import vn.edu.sales.application.service.OrderService;
import vn.edu.sales.domain.model.OrderStatus;

import java.util.List;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {
    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public List<OrderController.OrderResponse> getAll(@RequestParam(required = false) OrderStatus status) {
        return orderService.getAll(status).stream().map(OrderController.OrderResponse::from).toList();
    }

    @GetMapping("/search")
    public OrderController.PagedOrders search(@RequestParam(required = false) OrderStatus status,
                                               @RequestParam(defaultValue = "") String code,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return OrderController.PagedOrders.from(orderService.searchAll(status, code, page, size));
    }

    @PatchMapping("/{id}/status")
    public OrderController.OrderResponse updateStatus(@PathVariable Long id,
                                                       @Valid @RequestBody UpdateOrderStatusRequest request) {
        return OrderController.OrderResponse.from(orderService.changeStatus(id, request.status()));
    }

    public record UpdateOrderStatusRequest(@NotNull OrderStatus status) {
    }
}
