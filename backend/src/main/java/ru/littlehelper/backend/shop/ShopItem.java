package ru.littlehelper.backend.shop;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "shop_items")
public class ShopItem {

    @Id
    @Column(length = 40)
    private String id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 200)
    private String description;

    @Column(nullable = false, length = 20)
    private String type;

    @Column(nullable = false)
    private int price;

    @Column(nullable = false)
    private boolean included;

    @Column(name = "display_order", nullable = false, unique = true)
    private int displayOrder;

    protected ShopItem() {
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getType() {
        return type;
    }

    public int getPrice() {
        return price;
    }

    public boolean isIncluded() {
        return included;
    }
}
