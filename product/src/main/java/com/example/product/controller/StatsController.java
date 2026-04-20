package com.example.product.controller;

import com.example.product.repository.ProductRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stats")
@PreAuthorize("hasRole('PRODUCT_ADMIN')")
public class StatsController {

    private final ProductRepository productRepository;

    public StatsController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @GetMapping("/products/by-day")
    public List<Object[]> productsByDay() {
        return productRepository.countByDay();
    }

    @GetMapping("/products/by-creator")
    public List<Object[]> productsByCreator() {
        return productRepository.countByCreator();
    }

    @GetMapping("/summary")
    public Map<String, Object> summary() {
        long total = productRepository.count();
        List<Object[]> byCreator = productRepository.countByCreator();
        return Map.of(
                "totalProducts", total,
                "productCountByUser", byCreator
        );
    }
}
