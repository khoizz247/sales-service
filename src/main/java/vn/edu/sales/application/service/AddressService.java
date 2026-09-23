package vn.edu.sales.application.service;

import vn.edu.sales.application.port.out.AddressStore;
import vn.edu.sales.application.port.out.TransactionRunner;
import vn.edu.sales.application.port.out.UserRepository;
import vn.edu.sales.domain.exception.ResourceNotFoundException;

import java.util.List;
import java.util.Locale;

public class AddressService {
    private final AddressStore store;
    private final UserRepository users;
    private final TransactionRunner transactions;
    public AddressService(AddressStore store, UserRepository users, TransactionRunner transactions) {
        this.store = store; this.users = users; this.transactions = transactions;
    }
    public List<AddressStore.Address> mine(String email) { return store.findByUserId(userId(email)); }
    public AddressStore.Address create(String email, AddressStore.Address address) {
        return transactions.execute(() -> {
            Long userId = userId(email);
            store.ensureProfile(userId);
            if (address.isDefault()) store.clearDefault(userId);
            return store.create(copy(address, null, userId));
        });
    }
    public AddressStore.Address update(String email, Long id, AddressStore.Address address) {
        return transactions.execute(() -> {
            Long userId = userId(email);
            owned(id, userId);
            if (address.isDefault()) store.clearDefault(userId);
            return store.update(copy(address, id, userId));
        });
    }
    public void delete(String email, Long id) { Long userId = userId(email); owned(id, userId); store.delete(id); }
    private AddressStore.Address owned(Long id, Long userId) {
        return store.findById(id).filter(address -> address.customerUserId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy địa chỉ: " + id));
    }
    private Long userId(String email) {
        return users.findByEmail(email.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng")).id();
    }
    private AddressStore.Address copy(AddressStore.Address address, Long id, Long userId) {
        return new AddressStore.Address(id, userId, address.label(), address.recipientName(), address.recipientPhone(),
                address.addressLine1(), address.addressLine2(), address.ward(), address.district(), address.city(),
                address.stateProvince(), address.postalCode(), address.countryCode(), address.isDefault());
    }
}
