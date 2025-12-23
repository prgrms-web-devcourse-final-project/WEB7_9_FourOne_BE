package org.com.drop.domain.auction.product.repository;

import java.util.Optional;

import org.com.drop.domain.auction.product.entity.Product;
import org.com.drop.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
	Optional<Product> findByIdAndDeletedAtIsNull(Long id);

	Page<Product> findBySellerAndDeletedAtIsNullOrderByCreatedAtDesc(User seller, Pageable pageable);
}
