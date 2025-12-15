package org.com.drop.global.initdata;

import java.time.LocalDateTime;
import java.util.List;

import org.com.drop.domain.admin.guide.entity.Guide;
import org.com.drop.domain.admin.guide.repository.GuideRepository;
import org.com.drop.domain.auction.auction.dto.AuctionCreateRequest;
import org.com.drop.domain.auction.auction.service.AuctionService;
import org.com.drop.domain.auction.product.dto.ProductCreateRequest;
import org.com.drop.domain.auction.product.entity.Product;
import org.com.drop.domain.auction.product.qna.dto.ProductQnAAnswerRequest;
import org.com.drop.domain.auction.product.qna.dto.ProductQnACreateRequest;
import org.com.drop.domain.auction.product.qna.service.QnAService;
import org.com.drop.domain.auction.product.service.ProductService;
import org.com.drop.domain.user.entity.User;
import org.com.drop.domain.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class InitData {

	private final UserRepository userRepository;
	private final ProductService productService;
	private final AuctionService auctionService;
	private final QnAService qnAService;
	private final GuideRepository  guideRepository;
	@Autowired
	@Lazy
	private InitData self;

	@Bean
	ApplicationRunner baseInitData() {
		return args -> {
			self.work();
		};
	}

	@Transactional
	public void work() {

		User user1 = User.builder()
			.email("user1@example.com")
			.nickname("유저1")
			.password("12345678")
			.loginType(User.LoginType.LOCAL)
			.role(User.UserRole.USER)
			.createdAt(LocalDateTime.now())
			.penaltyCount(0)
			.build();
		userRepository.save(user1);

		User user2 = User.builder()
			.email("user2@example.com")
			.nickname("유저2")
			.password("12345678")
			.loginType(User.LoginType.LOCAL)
			.role(User.UserRole.USER)
			.createdAt(LocalDateTime.now())
			.penaltyCount(0)
			.build();
		userRepository.save(user2);

		List<String> images = List.of("img1.png", "img2.png");
		ProductCreateRequest productCreateRequest1 = new ProductCreateRequest(
			"상품명",
			"설명",
			Product.Category.STARGOODS,
			Product.SubCategory.ACC,
			images
		);
		Product product1 = productService.addProduct(productCreateRequest1, user1);

		ProductCreateRequest productCreateRequest2 = new ProductCreateRequest(
			"상품명",
			"설명",
			Product.Category.STARGOODS,
			Product.SubCategory.ACC,
			images
		);
		Product product2 = productService.addProduct(productCreateRequest2, user1);

		AuctionCreateRequest auctionCreateRequest1 = new AuctionCreateRequest(
			product1.getId(),
			1000,
			100000,
			10,
			LocalDateTime.now().plusSeconds(5),
			LocalDateTime.now().plusSeconds(10)
		);
		auctionService.addAuction(auctionCreateRequest1, user1);

		AuctionCreateRequest auctionCreateRequest2 = new AuctionCreateRequest(
			product2.getId(),
			1000,
			100000,
			10,
			LocalDateTime.now().plusDays(5),
			LocalDateTime.now().plusDays(10)
		);
		auctionService.addAuction(auctionCreateRequest2, user1);
		qnAService.addQuestion(1L, new ProductQnACreateRequest("질문1"), user1);
		qnAService.addAnswer(1L, 1L, new ProductQnAAnswerRequest("답변1"), user1);
		qnAService.addAnswer(1L, 1L, new ProductQnAAnswerRequest("답변2"), user1);
		guideRepository.save(new Guide("안내사항1"));
		guideRepository.save(new Guide("안내사항2"));
	}
}
