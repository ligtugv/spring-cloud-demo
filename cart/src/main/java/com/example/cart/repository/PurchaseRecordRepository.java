package com.example.cart.repository;

import com.example.cart.entity.PurchaseRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseRecordRepository extends JpaRepository<PurchaseRecord, Long> {
    List<PurchaseRecord> findByUsernameOrderByCreatedAtDesc(String username);

    @Query("SELECT p.username, COUNT(p), SUM(p.totalAmount) FROM PurchaseRecord p GROUP BY p.username ORDER BY SUM(p.totalAmount) DESC")
    List<Object[]> findUserPurchaseStats();

    @Query("SELECT p.productId, p.productName, COUNT(p), SUM(p.quantity), SUM(p.totalAmount) FROM PurchaseRecord p GROUP BY p.productId, p.productName ORDER BY COUNT(p) DESC")
    List<Object[]> findProductPurchaseStats();

    long countByUsername(String username);
}
