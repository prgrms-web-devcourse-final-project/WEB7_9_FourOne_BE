package org.com.drop.domain.auction.product.controller;

import org.com.drop.domain.auction.product.dto.ProductCreateRequest;
import org.com.drop.domain.auction.product.dto.ProductCreateResponse;
import org.com.drop.domain.auction.product.entity.Product;
import org.com.drop.domain.auction.product.service.ProductService;
import org.com.drop.domain.user.entity.User;
import org.com.drop.domain.user.repository.UserRepository;
import org.com.drop.global.rsdata.RsData;
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
	//임시 설정
	private final UserRepository userRepository;

	@PostMapping
	public RsData<ProductCreateResponse> addProduct(
		@RequestBody
		@Valid
		ProductCreateRequest request) {
		//TODO : rq 구현 후 수정
		User actor = userRepository.findById(1L).get();
		Product product = productService.addProduct(request, actor);
		return new RsData<>(
			new ProductCreateResponse(product)
		);
	}

	@PutMapping("/{productId}")
	public RsData<ProductCreateResponse> updateProduct(
		@PathVariable
		Long productId,
		@RequestBody
		@Valid
		ProductCreateRequest request) {
		//TODO : rq 구현 후 수정
		User actor = userRepository.findById(1L).get();
		Product product = productService.updateProduct(productId, request, actor);
		return new RsData<>(
			new ProductCreateResponse(product)
		);
	}

	@DeleteMapping("/{productId}")
	public RsData<Void> deleteProduct(
		@PathVariable
		Long productId
	) {
		//TODO : rq 구현 후 수정
		User actor = userRepository.findById(1L).get();
		productService.deleteProduct(productId, actor);
		return new RsData<>(null);
	}

	// @PostMapping("/{productId}/qna")
	// public RsData<ProductQnaCreateResponse> addQna(
	// 	@PathVariable
	// 	@NotNull
	// 	Long productId,
	// 	@RequestBody
	// 	@Valid
	// 	ProductQnACreateRequest request) {
	// 	//TODO : rq 구현 후 수정
	// 	User actor = userRepository.findById(1L).get();
	// 	Question question = productService.addQuestion(request, actor);
	// 	return new RsData<>(
	// 		"SUCCESS",
	// 		"200",
	// 		"요청을 성공적으로 처리했습니다.",
	// 		new ProductQnaCreateResponse(question)
	// 	);
	// }

}
