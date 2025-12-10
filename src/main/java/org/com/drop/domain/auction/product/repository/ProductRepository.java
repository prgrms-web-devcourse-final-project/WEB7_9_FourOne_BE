package org.com.drop.domain.auction.product.repository;

import org.com.drop.domain.auction.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
