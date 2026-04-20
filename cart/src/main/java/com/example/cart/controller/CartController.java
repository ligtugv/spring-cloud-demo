package com.example.cart.controller;

import com.example.cart.entity.CartItem;
import com.example.cart.entity.PurchaseRecord;
import com.example.cart.service.CartService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'EDITOR', 'PRODUCT_ADMIN')")
    public List<CartItem> list(@AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getClaimAsString("username");
        return cartService.getCart(username);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'EDITOR', 'PRODUCT_ADMIN')")
    public CartItem add(@RequestBody Map<String, Object> body, @AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getClaimAsString("username");
        Long productId = Long.valueOf(body.get("productId").toString());
        String productName = body.get("productName").toString();
        BigDecimal price = new BigDecimal(body.getOrDefault("price", "99.00").toString());
        int quantity = Integer.parseInt(body.getOrDefault("quantity", "1").toString());
        return cartService.addToCart(username, productId, productName, price, quantity);
    }

    @PutMapping("/{productId}")
    @PreAuthorize("hasAnyRole('USER', 'EDITOR', 'PRODUCT_ADMIN')")
    public ResponseEntity<CartItem> update(@PathVariable Long productId,
                                           @RequestBody Map<String, Object> body,
                                           @AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getClaimAsString("username");
        int quantity = Integer.parseInt(body.get("quantity").toString());
        cartService.updateQuantity(username, productId, quantity);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{productId}")
    @PreAuthorize("hasAnyRole('USER', 'EDITOR', 'PRODUCT_ADMIN')")
    public ResponseEntity<Void> remove(@PathVariable Long productId, @AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getClaimAsString("username");
        cartService.removeFromCart(username, productId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/checkout")
    @PreAuthorize("hasAnyRole('USER', 'EDITOR', 'PRODUCT_ADMIN')")
    public ResponseEntity<?> checkout(@AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getClaimAsString("username");
        String token = jwt.getTokenValue();
        CartService.CheckoutResult result = cartService.checkout(username, token);
        if (result.records.isEmpty() && !result.warnings.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "message", result.warnings.get(0),
                "warnings", result.warnings
            ));
        }
        if (!result.warnings.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                "records", result.records,
                "warnings", result.warnings
            ));
        }
        if (result.records.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "购物车是空的"));
        }
        return ResponseEntity.ok(result.records);
    }

    @GetMapping("/purchases")
    @PreAuthorize("hasAnyRole('USER', 'EDITOR', 'PRODUCT_ADMIN')")
    public List<PurchaseRecord> purchases(@AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getClaimAsString("username");
        return cartService.getPurchaseHistory(username);
    }

    @GetMapping("/stats/users")
    @PreAuthorize("hasAnyRole('PRODUCT_ADMIN')")
    public List<Object[]> topUsers() {
        return cartService.getTopUsersByPurchase();
    }

    @GetMapping("/stats/products")
    @PreAuthorize("hasAnyRole('PRODUCT_ADMIN')")
    public List<Object[]> topProducts() {
        return cartService.getTopProductsByPurchase();
    }
}
