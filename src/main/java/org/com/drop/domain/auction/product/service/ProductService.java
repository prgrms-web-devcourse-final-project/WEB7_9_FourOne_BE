package org.com.drop.domain.auction.product.service;

import org.com.drop.domain.auction.product.dto.AuctionCreateRequest;
import org.com.drop.domain.auction.product.entity.Product;
import org.com.drop.domain.auction.product.repository.ProductRepository;
import org.com.drop.domain.user.entity.User;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService {

	private final ProductRepository productRepository;
	public Product addProduct(AuctionCreateRequest request, User actor) {
		Product product = new Product(
			actor,
			request.name(),
			request.description(),
			request.category(),
			request.subCategory());
		return productRepository.save(product);
	}
}
