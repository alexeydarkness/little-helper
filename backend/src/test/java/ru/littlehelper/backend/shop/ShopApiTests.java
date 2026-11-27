package ru.littlehelper.backend.shop;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import ru.littlehelper.backend.auth.JwtService;
import ru.littlehelper.backend.user.Role;
import ru.littlehelper.backend.user.User;
import ru.littlehelper.backend.user.UserRepository;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ShopApiTests {

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired ShopPurchaseRepository purchases;
    @Autowired JwtService tokens;
    @Autowired JdbcTemplate jdbc;

    @Test
    void purchaseUsesCatalogPriceAndUpdatesOnlyTheCurrentAccount() throws Exception {
        User buyer = account(1200);
        User other = account(1200);

        mvc.perform(get("/api/v1/shop").header("Authorization", bearer(buyer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(1200))
                .andExpect(jsonPath("$.items[0].id").value("frame_default"))
                .andExpect(jsonPath("$.items[0].owned").value(true))
                .andExpect(jsonPath("$.items[1].id").value("frame_gold"))
                .andExpect(jsonPath("$.items[1].price").value(500))
                .andExpect(jsonPath("$.items[1].owned").value(false));

        mvc.perform(purchase(buyer, "frame_gold"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemId").value("frame_gold"))
                .andExpect(jsonPath("$.balance").value(700))
                .andExpect(jsonPath("$.alreadyOwned").value(false));
        mvc.perform(purchase(buyer, "frame_gold"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(700))
                .andExpect(jsonPath("$.alreadyOwned").value(true));
        mvc.perform(purchase(buyer, "bg_default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(700))
                .andExpect(jsonPath("$.alreadyOwned").value(true));

        mvc.perform(get("/api/v1/shop").header("Authorization", bearer(buyer)))
                .andExpect(jsonPath("$.balance").value(700))
                .andExpect(jsonPath("$.items[1].owned").value(true));
        mvc.perform(get("/api/v1/users/me").header("Authorization", bearer(buyer)))
                .andExpect(jsonPath("$.balance").value(700));
        mvc.perform(get("/api/v1/shop").header("Authorization", bearer(other)))
                .andExpect(jsonPath("$.balance").value(1200))
                .andExpect(jsonPath("$.items[1].owned").value(false));
        assertThat(purchases.findOwnedItemIds(buyer.getId())).containsExactly("frame_gold");
    }

    @Test
    void invalidMissingAndUnaffordableItemsDoNotChangeBalance() throws Exception {
        User buyer = account(200);

        mvc.perform(get("/api/v1/shop")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/shop/purchases").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":\"frame_gold\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(purchase(buyer, " ")).andExpect(status().isBadRequest());
        mvc.perform(purchase(buyer, "missing")).andExpect(status().isNotFound());
        mvc.perform(purchase(buyer, "frame_gold"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Недостаточно монет"));

        assertThat(users.findById(buyer.getId()).orElseThrow().getBalance()).isEqualTo(200);
        assertThat(purchases.findOwnedItemIds(buyer.getId())).isEmpty();
    }

    @Test
    void simultaneousIdenticalPurchasesChargeOnce() throws Exception {
        User buyer = account(1200);
        assertThat(concurrentStatuses(buyer, "frame_gold", "frame_gold")).containsExactly(200, 200);
        assertThat(users.findById(buyer.getId()).orElseThrow().getBalance()).isEqualTo(700);
        assertThat(purchases.findOwnedItemIds(buyer.getId())).containsExactly("frame_gold");
    }

    @Test
    void simultaneousDifferentPurchasesCannotSpendTheSameCoins() throws Exception {
        User buyer = account(600);
        List<Integer> statuses = concurrentStatuses(buyer, "frame_gold", "bg_dark");
        assertThat(statuses).containsExactlyInAnyOrder(200, 409);
        boolean boughtFrame = statuses.getFirst() == 200;
        assertThat(users.findById(buyer.getId()).orElseThrow().getBalance()).isEqualTo(boughtFrame ? 100 : 200);
        assertThat(purchases.findOwnedItemIds(buyer.getId())).containsExactly(boughtFrame ? "frame_gold" : "bg_dark");
    }

    private List<Integer> concurrentStatuses(User buyer, String firstItem, String secondItem) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> {
                start.await();
                return mvc.perform(purchase(buyer, firstItem)).andReturn().getResponse().getStatus();
            });
            var second = executor.submit(() -> {
                start.await();
                return mvc.perform(purchase(buyer, secondItem)).andReturn().getResponse().getStatus();
            });
            start.countDown();
            return List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
        }
    }

    private User account(int balance) {
        String name = "shop" + UUID.randomUUID().toString().substring(0, 8);
        User user = users.saveAndFlush(new User(name, name + "@test.com", "unused", "Shop User", null, Role.STUDENT));
        jdbc.update("UPDATE users SET balance = ? WHERE id = ?", balance, user.getId());
        return user;
    }

    private MockHttpServletRequestBuilder purchase(User user, String itemId) {
        return post("/api/v1/shop/purchases").header("Authorization", bearer(user))
                .contentType(MediaType.APPLICATION_JSON).content("{\"itemId\":\"%s\"}".formatted(itemId));
    }

    private String bearer(User user) {
        return "Bearer " + tokens.createToken(user);
    }
}
