package org.com.drop.domain.auction.product.repository;

import java.util.Optional;

import org.com.drop.domain.auction.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
	Optional<Product> findByIdAndDeletedAtIsNull(Long id);

}
