package vn.edu.sales.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.edu.sales.application.port.out.*;
import vn.edu.sales.domain.exception.BusinessConflictException;
import vn.edu.sales.domain.model.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OrderServiceTest {
    private final OrderRepository orders = mock(OrderRepository.class);
    private final ProductRepository products = mock(ProductRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final InventoryStore inventory = mock(InventoryStore.class);
    private final PaymentStore payments = mock(PaymentStore.class);
    private final OrderSearchStore searches = mock(OrderSearchStore.class);
    private OrderService service;

    private final Product keyboard = new Product(11L, "KB-11", "Bàn phím", "",
            new BigDecimal("125.50"), 5, ProductStatus.ACTIVE, 0L);

    @BeforeEach
    void setUp() {
        TransactionRunner transactions = new TransactionRunner() {
            @Override public <T> T execute(Supplier<T> work) { return work.get(); }
        };
        service = new OrderService(orders, products, users, transactions, inventory, payments, searches);
        when(users.findByEmail("customer@example.com")).thenReturn(Optional.of(new User(3L,
                "customer@example.com", "hash", "Customer", Role.CUSTOMER, UserStatus.ACTIVE, null)));
    }

    @Test
    void createsOrderWithServerCalculatedTotalAndDecreasesStock() {
        when(products.findAllByIdsForUpdate(List.of(11L))).thenReturn(List.of(keyboard));
        when(orders.save(any(Order.class))).thenAnswer(call -> {
            Order input = call.getArgument(0);
            return new Order(21L, input.orderCode(), input.userId(), input.recipientName(),
                    input.recipientPhone(), input.shippingAddress(), input.status(), input.totalAmount(),
                    input.createdAt(), input.updatedAt(), input.items());
        });

        Order result = service.create("customer@example.com", "Người nhận", "0901234567", "Hà Nội",
                List.of(new OrderService.CreateLine(11L, 2)));

        assertThat(result.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(result.totalAmount()).isEqualByComparingTo("251.00");
        assertThat(result.items().getFirst().productName()).isEqualTo("Bàn phím");
        assertThat(result.items().getFirst().unitPrice()).isEqualByComparingTo("125.50");
        verify(products).saveAll(argThat(list -> list.size() == 1 && list.getFirst().stockQuantity() == 3));
        verify(inventory).record(eq(11L), eq(21L), eq("SALE"), eq(-2), eq(5), eq(3),
                anyString(), eq("Đặt hàng"), eq(3L));
    }

    @Test
    void insufficientStockDoesNotSaveOrderOrChangeInventory() {
        when(products.findAllByIdsForUpdate(List.of(11L))).thenReturn(List.of(keyboard));

        assertThatThrownBy(() -> service.create("customer@example.com", "Người nhận", "0901234567",
                "Hà Nội", List.of(new OrderService.CreateLine(11L, 6))))
                .isInstanceOf(BusinessConflictException.class);

        verify(products, never()).saveAll(anyList());
        verifyNoInteractions(orders, inventory);
    }

    @Test
    void cancellingRestoresStockOnlyOnce() {
        Order pending = order(OrderStatus.PENDING);
        Order cancelled = pending.withStatus(OrderStatus.CANCELLED);
        when(orders.findByIdForUpdate(21L)).thenReturn(Optional.of(pending), Optional.of(cancelled));
        when(orders.save(any(Order.class))).thenAnswer(call -> call.getArgument(0));
        when(products.findAllByIdsForUpdate(List.of(11L))).thenReturn(List.of(keyboard));
        when(payments.paidTotal(21L)).thenReturn(BigDecimal.ZERO);

        assertThat(service.changeStatus(21L, OrderStatus.CANCELLED).status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(service.changeStatus(21L, OrderStatus.CANCELLED).status()).isEqualTo(OrderStatus.CANCELLED);

        verify(products, times(1)).saveAll(argThat(list -> list.getFirst().stockQuantity() == 7));
        verify(inventory, times(1)).record(eq(11L), eq(21L), eq("SALE_REVERSAL"), eq(2),
                eq(5), eq(7), anyString(), anyString(), isNull());
        verify(orders, times(1)).save(any(Order.class));
    }

    @Test
    void customerCancelsOnlyOwnPendingOrderAndInventoryRecordsActor() {
        Order pending = order(OrderStatus.PENDING);
        when(orders.findByIdForUpdate(21L)).thenReturn(Optional.of(pending));
        when(orders.save(any(Order.class))).thenAnswer(call -> call.getArgument(0));
        when(products.findAllByIdsForUpdate(List.of(11L))).thenReturn(List.of(keyboard));
        when(payments.paidTotal(21L)).thenReturn(BigDecimal.ZERO);

        assertThat(service.cancelMine("customer@example.com", 21L).status())
                .isEqualTo(OrderStatus.CANCELLED);
        verify(inventory).record(eq(11L), eq(21L), eq("SALE_REVERSAL"), eq(2), eq(5), eq(7),
                anyString(), anyString(), eq(3L));
    }

    @Test
    void invalidTransitionAndPaidCancellationReturnConflict() {
        when(orders.findByIdForUpdate(21L)).thenReturn(Optional.of(order(OrderStatus.PENDING)));
        assertThatThrownBy(() -> service.changeStatus(21L, OrderStatus.COMPLETED))
                .isInstanceOf(BusinessConflictException.class);

        when(payments.paidTotal(21L)).thenReturn(new BigDecimal("10.00"));
        assertThatThrownBy(() -> service.changeStatus(21L, OrderStatus.CANCELLED))
                .isInstanceOf(BusinessConflictException.class);

        verify(products, never()).saveAll(anyList());
        verify(orders, never()).save(any(Order.class));
    }

    private Order order(OrderStatus status) {
        return new Order(21L, "ORD-21", 3L, "Người nhận", "0901234567", "Hà Nội", status,
                null, null, null, List.of(new OrderItem(1L, 11L, "Bàn phím", new BigDecimal("125.50"), 2, null)));
    }
}
