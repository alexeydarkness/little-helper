package ru.littlehelper.backend.shop;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "shop_purchases", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "item_id"}))
public class ShopPurchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(name = "item_id", nullable = false, updatable = false, length = 40)
    private String itemId;

    @Column(name = "price_paid", nullable = false, updatable = false)
    private int pricePaid;

    @Column(name = "purchased_at", nullable = false, updatable = false)
    private Instant purchasedAt;

    protected ShopPurchase() {
    }

    public ShopPurchase(Long userId, ShopItem item) {
        this.userId = userId;
        this.itemId = item.getId();
        this.pricePaid = item.getPrice();
        this.purchasedAt = Instant.now();
    }
}
