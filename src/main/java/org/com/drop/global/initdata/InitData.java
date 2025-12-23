package org.com.drop.global.initdata;

import java.time.LocalDateTime;

import org.com.drop.domain.admin.guide.entity.Guide;
import org.com.drop.domain.admin.guide.repository.GuideRepository;
import org.com.drop.domain.auction.auction.dto.AuctionCreateRequest;
import org.com.drop.domain.auction.auction.service.AuctionService;
import org.com.drop.domain.auction.product.entity.Product;
import org.com.drop.domain.auction.product.entity.ProductImage;
import org.com.drop.domain.auction.product.qna.dto.ProductQnAAnswerRequest;
import org.com.drop.domain.auction.product.qna.dto.ProductQnACreateRequest;
import org.com.drop.domain.auction.product.qna.service.QnAService;
import org.com.drop.domain.auction.product.repository.ProductRepository;
import org.com.drop.domain.auction.product.service.ProductService;
import org.com.drop.domain.user.entity.User;
import org.com.drop.domain.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
@Profile("!test")
public class InitData {

	private final UserRepository userRepository;
	private final ProductService productService;
	private final ProductRepository productRepository;
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
		if (userRepository.findByEmail("user1@example.com").isPresent()) {
			return;
		}
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

		String image = "b67103865cff09c2638b8e8e8551175b18db2253.jpg";

		Product product1 = new Product(
			user1,
			"상품1",
			"상품설명1",
			Product.Category.STARGOODS,
			Product.SubCategory.ACC
		);
		productRepository.save(product1);
		ProductImage productImage1 = new ProductImage(product1, image);

		Product product2 = new Product(
			user1,
			"상품1",
			"상품설명1",
			Product.Category.STARGOODS,
			Product.SubCategory.ACC
		);
		productRepository.save(product2);
		ProductImage productImage2 = new ProductImage(product2, image);

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
