package org.com.drop.domain.auction.product.controller;

import org.com.drop.domain.auction.product.dto.BookmarkCreateResponse;
import org.com.drop.domain.auction.product.dto.ProductCreateRequest;
import org.com.drop.domain.auction.product.dto.ProductCreateResponse;
import org.com.drop.domain.auction.product.entity.BookMark;
import org.com.drop.domain.auction.product.entity.Product;
import org.com.drop.domain.auction.product.service.ProductService;
import org.com.drop.domain.user.entity.User;
import org.com.drop.global.rsdata.RsData;
import org.com.drop.global.security.auth.LoginUser;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
public class ProductController {

	private final ProductService productService;

	@PostMapping
	public RsData<ProductCreateResponse> addProduct(
		@LoginUser User actor,
		@RequestBody @Valid ProductCreateRequest request
	) {
		Product product = productService.addProduct(request, actor);
		return new RsData<>(
			new ProductCreateResponse(product)
		);
	}

	@PutMapping("/{productId}")
	public RsData<ProductCreateResponse> updateProduct(
		@LoginUser User actor,
		@PathVariable Long productId,
		@RequestBody @Valid ProductCreateRequest request) {
		Product product = productService.updateProduct(productId, request, actor);
		return new RsData<>(
			new ProductCreateResponse(product)
		);
	}

	@DeleteMapping("/{productId}")
	public RsData<Void> deleteProduct(
		@LoginUser User actor,
		@PathVariable Long productId
	) {
		productService.deleteProduct(productId, actor);
		return new RsData<>(null);
	}

	@PostMapping("/{productId}/bookmarks")
	public RsData<BookmarkCreateResponse> addBookmark(
		@LoginUser User actor,
		@PathVariable Long productId
	) {
		BookMark bookMark = productService.addBookmark(productId, actor);
		return new RsData<>(
			new BookmarkCreateResponse(bookMark)
		);
	}

	@DeleteMapping("/{productId}/bookmarks")
	public RsData<BookmarkCreateResponse> deleteBookmark(
		@LoginUser User actor,
		@PathVariable Long productId) {
		productService.deleteBookmark(productId, actor);
		return new RsData<>(
			null
		);
	}

}
