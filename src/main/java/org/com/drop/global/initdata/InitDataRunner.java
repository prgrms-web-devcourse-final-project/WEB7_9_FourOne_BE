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
import org.com.drop.domain.auth.dto.LocalSignUpRequest;
import org.com.drop.domain.auth.service.AuthService;
import org.com.drop.domain.auth.store.VerificationCodeStore;
import org.com.drop.domain.notification.service.NotificationService;
import org.com.drop.domain.user.entity.User;
import org.com.drop.domain.user.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Profile("!test")
public class InitDataRunner implements ApplicationRunner {

	private final AuthService authService;
	private final VerificationCodeStore verificationCodeStore;
	private final UserRepository userRepository;
	private final ProductService productService;
	private final AuctionService auctionService;
	private final QnAService qnAService;
	private final GuideRepository  guideRepository;
	private final NotificationService notificationService;

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (userRepository.findByEmail("user1@example.com").isPresent()) {
			return;
		}
		//사용자 생성 (user1, user2, testUser)
		verificationCodeStore.markAsVerified("user1@example.com");
		LocalSignUpRequest localSignUpRequest = new LocalSignUpRequest(
			"user1@example.com", "Asdf1234!", "유저1"
		);
		authService.signup(localSignUpRequest);

		verificationCodeStore.markAsVerified("user2@example.com");
		localSignUpRequest = new LocalSignUpRequest(
			"user2@example.com", "Asdf1234!", "유저2"
		);
		authService.signup(localSignUpRequest);

		verificationCodeStore.markAsVerified("test@test.com");
		localSignUpRequest = new LocalSignUpRequest(
			"test@test.com", "Asdf1234!", "Test"
		);
		authService.signup(localSignUpRequest);

		User user1 = userRepository.findByEmail("user1@example.com").orElseThrow();
		User user2 = userRepository.findByEmail("user2@example.com").orElseThrow();
		User testUser = userRepository.findByEmail("test@test.com").orElseThrow();

		//상품 생성 (상품1, 상품2)

		List<String> image = List.of("b67103865cff09c2638b8e8e8551175b18db2253.jpg");

		ProductCreateRequest productCreateRequest =  new ProductCreateRequest(
			"상품1",
			"상품설명1",
			Product.Category.STARGOODS,
			Product.SubCategory.ACC,
			image

		);
		Product product1 = productService.addProduct(productCreateRequest, user1);

		productCreateRequest =  new ProductCreateRequest(
			"상품2",
			"상품설명2",
			Product.Category.STARGOODS,
			Product.SubCategory.ACC,
			image

		);
		Product product2 = productService.addProduct(productCreateRequest, user1);

		//경매 생성 (상품1, 상품 2)
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

		//QnA 생성
		qnAService.addQuestion(1L, new ProductQnACreateRequest("질문1"), user1);
		qnAService.addAnswer(1L, 1L, new ProductQnAAnswerRequest("답변1"), user1);
		qnAService.addAnswer(1L, 1L, new ProductQnAAnswerRequest("답변2"), user1);

		//가이드 생성
		guideRepository.save(new Guide("안내사항1"));
		guideRepository.save(new Guide("안내사항2"));

		//알림 생성
		notificationService.addNotification(user1, "user1 테스트 알림1");
		notificationService.addNotification(user1, "user1 테스트 알림2");
		notificationService.addNotification(user2, "user2 테스트 알림1");
		notificationService.addNotification(user2, "user2 테스트 알림2");
		notificationService.addNotification(testUser, "testUser 테스트 알림1");
		notificationService.addNotification(testUser, "testUser 테스트 알림2");
	}
}
