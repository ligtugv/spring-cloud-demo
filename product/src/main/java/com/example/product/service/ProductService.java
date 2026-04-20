package com.example.product.service;

import com.example.product.entity.Product;
import com.example.product.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<Product> findAll() {
        return productRepository.findAll();
    }

    public Optional<Product> findById(Long id) {
        return productRepository.findById(id);
    }

    @Transactional
    public Product save(Product product, String createdBy) {
        if (product.getId() == null) {
            product.setCreatedBy(createdBy);
            if (product.getStock() == null || product.getStock() == 0) {
                product.setStock(10);
            }
        }
        return productRepository.save(product);
    }

    @Transactional
    public Product update(Long id, Product product) {
        return productRepository.findById(id)
                .map(existing -> {
                    existing.setName(product.getName());
                    return productRepository.save(existing);
                })
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
    }

    @Transactional
    public void deleteById(Long id) {
        if (!productRepository.existsById(id)) {
            throw new RuntimeException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
    }

    public boolean hasStock(Long productId, int quantity) {
        return productRepository.findById(productId)
                .map(p -> p.getStock() != null && p.getStock() >= quantity)
                .orElse(false);
    }

    @Transactional
    public void deductStock(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));
        int currentStock = product.getStock() != null ? product.getStock() : 0;
        if (currentStock < quantity) {
            throw new RuntimeException("库存不足：当前库存 " + currentStock + "，需要 " + quantity);
        }
        product.setStock(currentStock - quantity);
        productRepository.save(product);
    }

    @Transactional
    public void addStock(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));
        int currentStock = product.getStock() != null ? product.getStock() : 0;
        product.setStock(currentStock + quantity);
        productRepository.save(product);
    }
}
