package org.com.drop.domain.user.controller;

import org.com.drop.domain.auction.product.dto.ProductSearchResponse;
import org.com.drop.domain.auction.product.service.ProductService;
import org.com.drop.global.rsdata.RsData;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class UserController {
	private final ProductService productService;
	@GetMapping("products/{productId}")
	public RsData<ProductSearchResponse> getProduct(
		@PathVariable Long productId
	) {
		ProductSearchResponse product = productService.findProductWithImgById(productId);
		return new RsData<>(
			200,
			product
		);
	}
}
