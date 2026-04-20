package com.example.product.repository;

import com.example.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("SELECT FUNCTION('DATE', p.createdAt) as date, COUNT(p) FROM Product p GROUP BY FUNCTION('DATE', p.createdAt) ORDER BY date DESC")
    List<Object[]> countByDay();

    @Query("SELECT p.createdBy, COUNT(p) FROM Product p WHERE p.createdBy IS NOT NULL GROUP BY p.createdBy ORDER BY COUNT(p) DESC")
    List<Object[]> countByCreator();

    long countByCreatedBy(String createdBy);
}
