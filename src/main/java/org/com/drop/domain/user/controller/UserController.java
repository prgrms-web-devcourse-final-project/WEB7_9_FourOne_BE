package org.com.drop.domain.user.controller;

import java.util.List;

import org.com.drop.domain.auction.product.dto.ProductSearchResponse;
import org.com.drop.domain.auction.product.service.ProductService;
import org.com.drop.domain.payment.method.dto.CardResponse;
import org.com.drop.domain.payment.method.dto.CardResponseList;
import org.com.drop.domain.payment.method.dto.RegisterCardRequest;
import org.com.drop.domain.payment.method.entity.PaymentMethod;
import org.com.drop.domain.payment.method.service.PaymentMethodService;
import org.com.drop.domain.user.dto.MyBidPageResponse;
import org.com.drop.domain.user.dto.MyBookmarkPageResponse;
import org.com.drop.domain.user.dto.MyPageResponse;
import org.com.drop.domain.user.dto.MyProductPageResponse;
import org.com.drop.domain.user.dto.UpdateProfileRequest;
import org.com.drop.domain.user.dto.UpdateProfileResponse;
import org.com.drop.domain.user.entity.User;
import org.com.drop.domain.user.service.UserService;
import org.com.drop.global.aws.AmazonS3Client;
import org.com.drop.global.aws.ImageType;
import org.com.drop.global.aws.PreSignedUrlListRequest;
import org.com.drop.global.rsdata.RsData;
import org.com.drop.global.security.auth.LoginUser;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class UserController {

	private final ProductService productService;
	private final UserService userService;
	private final AmazonS3Client amazonS3Client;
	private final PaymentMethodService paymentMethodService;

	@GetMapping("products/{productId}")
	public RsData<ProductSearchResponse> getProduct(
		@LoginUser User actor,
		@PathVariable Long productId
	) {
		ProductSearchResponse response = productService.findProductWithImgById(productId);
		productService.validUser(response.sellerId(), actor);
		return new RsData<>(response);
	}

	@GetMapping("user/me")
	public RsData<MyPageResponse> me(
		@LoginUser User user) {

		MyPageResponse response = userService.getMe(user);
		return new RsData<>(response);
	}

	@GetMapping("/me/products")
	public RsData<MyProductPageResponse> getMyProducts(
		@LoginUser User user,
		@RequestParam(defaultValue = "1") int page) {

		MyProductPageResponse response = userService.getMyProducts(user, page);
		return new RsData<>(response);
	}

	@GetMapping("/me/bids")
	public RsData<MyBidPageResponse> getMyBids(
		@LoginUser User user,
		@RequestParam(defaultValue = "1") int page,
		@RequestParam(defaultValue = "ALL") String status) {

		MyBidPageResponse response = userService.getMyBids(user, page, status);
		return new RsData<>(response);
	}

	@GetMapping("user/me/bookmarks")
	public RsData<MyBookmarkPageResponse> getMyBookmarks(
		@LoginUser User user,
		@RequestParam(defaultValue = "1") int page) {

		MyBookmarkPageResponse response = userService.getMyBookmarks(user, page);
		return new RsData<>(response);
	}

	@PatchMapping("user/me/profile")
	public RsData<UpdateProfileResponse> updateProfile(
		@LoginUser User user,
		@Valid @RequestBody UpdateProfileRequest dto) {

		UpdateProfileResponse response = userService.updateProfile(user, dto);
		return new RsData<>(response);
	}

	@PostMapping("/user/me/profile/img")
	public RsData<List<String>> getProfileImageUrl(
		@LoginUser User actor,
		@Valid @RequestBody PreSignedUrlListRequest preSignedUrlRequest
	) {
		List<String> url = amazonS3Client.createPresignedUrls(preSignedUrlRequest, actor, ImageType.PROFILE);
		return new RsData<>(url);
	}

	@PostMapping("/user/me/paymentMethods")
	public RsData<CardResponse> register(
		@LoginUser User user,
		@Valid @RequestBody RegisterCardRequest request
	) {
		PaymentMethod paymentMethod =  paymentMethodService.registerCard(user.getId(), request);
		return new RsData<>(
			new CardResponse(paymentMethod)
		);
	}

	@GetMapping("/user/me/paymentMethods")
	public RsData<CardResponseList> cardList(@LoginUser User user) {
		List<PaymentMethod> paymentMethods = paymentMethodService.getCards(user.getId());
		return new RsData<>(CardResponseList.from(paymentMethods));
	}

	@DeleteMapping("/user/me/paymentMethods/{paymentMethodId}")
	public RsData<Void> delete(
		@LoginUser User user,
		@PathVariable Long paymentMethodId
	) {
		paymentMethodService.deleteCard(user.getId(), paymentMethodId);
		return new RsData<>(null);
	}
}
