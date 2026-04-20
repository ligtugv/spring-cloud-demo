package com.example.product.controller;

import com.example.product.aop.OwnershipCheck;
import com.example.product.entity.Product;
import com.example.product.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'EDITOR', 'PRODUCT_ADMIN')")
    public List<Product> list() {
        return productService.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'EDITOR', 'PRODUCT_ADMIN')")
    public ResponseEntity<Product> get(@PathVariable("id") Long id) {
        return productService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('EDITOR', 'PRODUCT_ADMIN')")
    public Product create(@RequestBody Product product, @AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getClaimAsString("username");
        return productService.save(product, username);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('EDITOR', 'PRODUCT_ADMIN')")
    public ResponseEntity<Product> update(@PathVariable("id") Long id, @RequestBody Product product) {
        return ResponseEntity.ok(productService.update(id, product));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('EDITOR', 'PRODUCT_ADMIN')")
    @OwnershipCheck
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        productService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/stock")
    @PreAuthorize("hasAnyRole('USER', 'EDITOR', 'PRODUCT_ADMIN')")
    public ResponseEntity<?> checkStock(@PathVariable("id") Long id) {
        return productService.findById(id)
                .map(p -> ResponseEntity.ok().body(java.util.Map.of("productId", p.getId(), "stock", p.getStock() != null ? p.getStock() : 0)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/deduct")
    @PreAuthorize("hasAnyRole('USER', 'EDITOR', 'PRODUCT_ADMIN')")
    public ResponseEntity<?> deductStock(@PathVariable("id") Long id, @RequestBody java.util.Map<String, Object> body) {
        int quantity = Integer.parseInt(body.get("quantity").toString());
        productService.deductStock(id, quantity);
        return ResponseEntity.ok().body(java.util.Map.of("productId", id, "deducted", quantity));
    }

    @PostMapping("/{id}/add-stock")
    @PreAuthorize("hasAnyRole('EDITOR', 'PRODUCT_ADMIN')")
    public ResponseEntity<?> addStock(@PathVariable("id") Long id, @RequestBody java.util.Map<String, Object> body) {
        int quantity = Integer.parseInt(body.get("quantity").toString());
        productService.addStock(id, quantity);
        return ResponseEntity.ok().body(java.util.Map.of("productId", id, "added", quantity));
    }
}
