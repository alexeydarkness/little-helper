package ru.littlehelper.backend.shop;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import ru.littlehelper.backend.common.ApiException;
import ru.littlehelper.backend.user.User;
import ru.littlehelper.backend.user.UserRepository;

import java.util.List;
import java.util.Set;

@Service
public class ShopService {

    private final UserRepository users;
    private final ShopItemRepository items;
    private final ShopPurchaseRepository purchases;

    public ShopService(UserRepository users, ShopItemRepository items, ShopPurchaseRepository purchases) {
        this.users = users;
        this.items = items;
        this.purchases = purchases;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Catalog catalog(Long userId) {
        User user = users.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized("Пользователь не найден"));
        Set<String> ownedIds = purchases.findOwnedItemIds(userId);
        List<CatalogItem> catalog = items.findAllByOrderByDisplayOrderAsc().stream()
                .map(item -> new CatalogItem(item.getId(), item.getName(), item.getDescription(), item.getType(),
                        item.getPrice(), item.isIncluded() || ownedIds.contains(item.getId())))
                .toList();
        return new Catalog(user.getBalance(), catalog);
    }

    @Transactional
    public PurchaseResult purchase(Long userId, String itemId) {
        User user = users.findByIdForUpdate(userId)
                .orElseThrow(() -> ApiException.unauthorized("Пользователь не найден"));
        ShopItem item = items.findById(itemId)
                .orElseThrow(() -> ApiException.notFound("Товар не найден"));
        if (item.isIncluded() || purchases.existsByUserIdAndItemId(userId, itemId)) {
            return new PurchaseResult(itemId, user.getBalance(), true);
        }
        if (!user.trySpendCoins(item.getPrice())) {
            throw ApiException.conflict("Недостаточно монет");
        }
        purchases.save(new ShopPurchase(userId, item));
        return new PurchaseResult(itemId, user.getBalance(), false);
    }

    public record Catalog(int balance, List<CatalogItem> items) {
    }

    public record CatalogItem(String id, String name, String description, String type, int price, boolean owned) {
    }

    public record PurchaseResult(String itemId, int balance, boolean alreadyOwned) {
    }
}
