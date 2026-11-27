package ru.littlehelper.backend.shop;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Set;

public interface ShopPurchaseRepository extends JpaRepository<ShopPurchase, Long> {

    boolean existsByUserIdAndItemId(Long userId, String itemId);

    @Query("select p.itemId from ShopPurchase p where p.userId = :userId")
    Set<String> findOwnedItemIds(Long userId);
}
