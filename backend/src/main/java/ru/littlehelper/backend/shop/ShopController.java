package ru.littlehelper.backend.shop;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/shop")
public class ShopController {

    private final ShopService shop;

    public ShopController(ShopService shop) {
        this.shop = shop;
    }

    @GetMapping
    public ShopService.Catalog catalog(@AuthenticationPrincipal Jwt jwt) {
        return shop.catalog(Long.valueOf(jwt.getSubject()));
    }

    @PostMapping("/purchases")
    public ShopService.PurchaseResult purchase(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PurchaseRequest request) {
        return shop.purchase(Long.valueOf(jwt.getSubject()), request.itemId().trim());
    }

    public record PurchaseRequest(
            @NotBlank(message = "Выберите товар") @Size(max = 40, message = "Некорректный ID товара") String itemId
    ) {
    }
}
