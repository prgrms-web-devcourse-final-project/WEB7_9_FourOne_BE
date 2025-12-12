package org.com.drop.domain.auction.product.repository;

import java.util.List;

import org.com.drop.domain.auction.product.entity.Product;
import org.com.drop.domain.auction.product.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
	List<ProductImage> findAllByProductId(Long productId);

	void deleteByProduct(Product product);
}
