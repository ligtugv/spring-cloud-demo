package com.example.cart.service;

import com.example.cart.entity.CartItem;
import com.example.cart.entity.PurchaseRecord;
import com.example.cart.repository.CartItemRepository;
import com.example.cart.repository.PurchaseRecordRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class CartService {

    private static final String CART_KEY_PREFIX = "cart:";
    private static final String LOCK_KEY_PREFIX = "lock:cart:";
    private static final long CART_TTL_HOURS = 24;
    private static final long LOCK_TTL_SECONDS = 5;

    private final CartItemRepository cartItemRepository;
    private final PurchaseRecordRepository purchaseRecordRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final WebClient productWebClient;

    public CartService(CartItemRepository cartItemRepository,
                       PurchaseRecordRepository purchaseRecordRepository,
                       RedisTemplate<String, Object> redisTemplate,
                       WebClient productWebClient) {
        this.cartItemRepository = cartItemRepository;
        this.purchaseRecordRepository = purchaseRecordRepository;
        this.redisTemplate = redisTemplate;
        this.productWebClient = productWebClient;
    }

    public static class CheckoutResult {
        public List<PurchaseRecord> records = new ArrayList<>();
        public List<String> warnings = new ArrayList<>();

        public CheckoutResult() {}

        public CheckoutResult(List<PurchaseRecord> records, List<String> warnings) {
            this.records = records;
            this.warnings = warnings;
        }
    }

    public List<CartItem> getCart(String username) {
        String cacheKey = CART_KEY_PREFIX + username;
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof List<?> list) {
                @SuppressWarnings("unchecked")
                List<CartItem> items = (List<CartItem>) (List<?>) list;
                return items;
            }
        } catch (Exception e) {
            // Redis unavailable — fall through to DB
        }

        String lockKey = LOCK_KEY_PREFIX + username;
        boolean locked = acquireLock(lockKey);
        try {
            if (!locked) {
                Thread.sleep(100);
            }

            try {
                Object cached = redisTemplate.opsForValue().get(cacheKey);
                if (cached instanceof List<?> list) {
                    @SuppressWarnings("unchecked")
                    List<CartItem> items = (List<CartItem>) (List<?>) list;
                    return items;
                }
            } catch (Exception e) {
                // Redis unavailable — fall through to DB
            }

            List<CartItem> items = cartItemRepository.findByUsername(username);
            try {
                redisTemplate.opsForValue().set(cacheKey, items, CART_TTL_HOURS, TimeUnit.HOURS);
            } catch (Exception e) {
                // Redis write failed
            }
            return items;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return cartItemRepository.findByUsername(username);
        } finally {
            if (locked) {
                releaseLock(lockKey);
            }
        }
    }

    @Transactional
    public CartItem addToCart(String username, Long productId, String productName, BigDecimal price, int quantity) {
        CartItem existing = cartItemRepository.findByUsernameAndProductId(username, productId).orElse(null);
        CartItem saved;
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + quantity);
            saved = cartItemRepository.save(existing);
        } else {
            CartItem item = new CartItem();
            item.setUsername(username);
            item.setProductId(productId);
            item.setProductName(productName);
            item.setUnitPrice(price);
            item.setQuantity(quantity);
            saved = cartItemRepository.save(item);
        }
        invalidateCacheAfterCommit(username);
        return saved;
    }

    @Transactional
    public void removeFromCart(String username, Long productId) {
        cartItemRepository.findByUsernameAndProductId(username, productId)
                .ifPresent(item -> {
                    cartItemRepository.delete(item);
                    invalidateCacheAfterCommit(username);
                });
    }

    @Transactional
    public void updateQuantity(String username, Long productId, int quantity) {
        if (quantity <= 0) {
            removeFromCart(username, productId);
            return;
        }
        cartItemRepository.findByUsernameAndProductId(username, productId)
                .ifPresent(item -> {
                    item.setQuantity(quantity);
                    cartItemRepository.save(item);
                    invalidateCacheAfterCommit(username);
                });
    }

    @Transactional
    public CheckoutResult checkout(String username, String token) {
        List<CartItem> items = cartItemRepository.findByUsername(username);
        if (items.isEmpty()) return new CheckoutResult();

        List<PurchaseRecord> records = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<CartItem> validItems = new ArrayList<>();
        List<CartItem> staleItems = new ArrayList<>();

        for (CartItem item : items) {
            try {
                productWebClient.post()
                        .uri("/api/products/{id}/deduct", item.getProductId())
                        .header("Authorization", "Bearer " + token)
                        .bodyValue(Map.of("quantity", item.getQuantity()))
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block();

                PurchaseRecord record = new PurchaseRecord();
                record.setUsername(username);
                record.setProductId(item.getProductId());
                record.setProductName(item.getProductName());
                record.setQuantity(item.getQuantity());
                record.setTotalAmount(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
                record.setStatus("COMPLETED");
                records.add(record);
                validItems.add(item);
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : "Unknown error";
                if (msg.contains("404") || msg.contains("Not Found")) {
                    warnings.add("商品「" + item.getProductName() + "」已下架，已自动移除");
                    staleItems.add(item);
                } else if (msg.contains("库存不足")) {
                    warnings.add("商品「" + item.getProductName() + "」库存不足");
                } else {
                    warnings.add("商品「" + item.getProductName() + "」处理失败");
                }
            }
        }

        if (!validItems.isEmpty()) {
            purchaseRecordRepository.saveAll(records);
            cartItemRepository.deleteAll(validItems);
            invalidateCacheAfterCommit(username);
        }

        if (!staleItems.isEmpty()) {
            cartItemRepository.deleteAll(staleItems);
        }

        return new CheckoutResult(records, warnings);
    }

    public List<PurchaseRecord> getPurchaseHistory(String username) {
        return purchaseRecordRepository.findByUsernameOrderByCreatedAtDesc(username);
    }

    public List<Object[]> getTopUsersByPurchase() {
        return purchaseRecordRepository.findUserPurchaseStats();
    }

    public List<Object[]> getTopProductsByPurchase() {
        return purchaseRecordRepository.findProductPurchaseStats();
    }

    private void invalidateCacheAfterCommit(String username) {
        String cacheKey = CART_KEY_PREFIX + username;
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        redisTemplate.delete(cacheKey);
                    } catch (Exception e) {
                        // Redis unavailable
                    }
                }
            });
        } else {
            try {
                redisTemplate.delete(cacheKey);
            } catch (Exception e) {
                // Redis unavailable
            }
        }
    }

    private boolean acquireLock(String lockKey) {
        try {
            Boolean acquired = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, "1", LOCK_TTL_SECONDS, TimeUnit.SECONDS);
            return Boolean.TRUE.equals(acquired);
        } catch (Exception e) {
            return false;
        }
    }

    private void releaseLock(String lockKey) {
        try {
            redisTemplate.delete(lockKey);
        } catch (Exception e) {
            // Redis unavailable
        }
    }
}
