package org.com.drop.domain.payment.payment.service;

import static org.assertj.core.api.Assertions.*;
import static org.com.drop.domain.auction.product.entity.Product.Category.GAME;
import static org.com.drop.domain.auction.product.entity.Product.SubCategory.*;

import java.time.LocalDateTime;
import java.util.UUID;

import org.com.drop.domain.auction.auction.entity.Auction;
import org.com.drop.domain.auction.auction.repository.AuctionRepository;
import org.com.drop.domain.auction.product.entity.Product;
import org.com.drop.domain.auction.product.repository.ProductRepository;
import org.com.drop.domain.payment.payment.domain.Payment;
import org.com.drop.domain.payment.payment.domain.PaymentStatus;
import org.com.drop.domain.payment.payment.infra.toss.TossPaymentsClient;
import org.com.drop.domain.payment.payment.infra.toss.util.CustomerKeyGenerator;
import org.com.drop.domain.payment.payment.repository.PaymentRepository;
import org.com.drop.domain.payment.settlement.repository.SettlementRepository;
import org.com.drop.domain.user.entity.User;
import org.com.drop.domain.user.repository.UserRepository;
import org.com.drop.domain.winner.domain.Winner;
import org.com.drop.domain.winner.repository.WinnerRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class PaymentServiceImplTest {
	@Autowired
	PaymentRepository paymentRepository;
	@Autowired
	SettlementRepository settlementRepository;
	@Autowired
	WinnerRepository winnerRepository;
	@MockitoBean
	SecurityFilterChain securityFilterChain;
	@Mock
	TossPaymentsClient tossPaymentsClient;
	@Mock
	CustomerKeyGenerator customerKeyGenerator;

	@Autowired
	ProductRepository productRepository;

	@Autowired
	AuctionRepository auctionRepository;

	@Autowired
	UserRepository userRepository;

	private User createDummyUser(String name) {
		User user = User.builder()
			.email(name + "+" + UUID.randomUUID() + "@example.com")
			.nickname(name + UUID.randomUUID())
			.password("12345678")
			.loginType(User.LoginType.LOCAL)
			.role(User.UserRole.USER)
			.createdAt(LocalDateTime.now())
			.penaltyCount(0)
			.build();
		return userRepository.save(user);
	}

	private Auction createLiveAuction(Product product) {
		LocalDateTime now = LocalDateTime.now();

		Auction auction = Auction.builder()
			.product(product)
			.startPrice(10_000)
			.minBidStep(1_000)
			.buyNowPrice(null)
			.startAt(now.minusHours(1))
			.endAt(now.plusHours(1))
			.status(Auction.AuctionStatus.LIVE)
			.build();

		return auctionRepository.save(auction);
	}

	@Test
	void createPayment_createsRequestedPaymentWithSellerId() {

		User seller = createDummyUser("판매자1");

		Product product	= productRepository.save(
			Product.builder()
				.seller(seller)
				.name("Test Product")
				.description("Test Description")
				.category(GAME)
				.subcategory(ETC)
				.createdAt(LocalDateTime.now())
				.bookmarkCount(0)
				.build()
		);

		Auction auction = createLiveAuction(product);

		Winner winner = new Winner();
		ReflectionTestUtils.setField(winner, "auction", auction);
		ReflectionTestUtils.setField(winner, "sellerId", 10L);
		ReflectionTestUtils.setField(winner, "userId", 20L);
		ReflectionTestUtils.setField(winner, "finalPrice", 1000L);
		winner = winnerRepository.save(winner);

		PaymentServiceImpl service =
			new PaymentServiceImpl(
				paymentRepository,
				settlementRepository,
				tossPaymentsClient,
				customerKeyGenerator,
				winnerRepository
			);

		Payment payment = service.createPayment(winner.getId(), 1000L);

		assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REQUESTED);
		assertThat(payment.getSellersId()).isEqualTo(10L);
		assertThat(payment.getFee()).isEqualTo(50L);
		assertThat(payment.getNet()).isEqualTo(1000L);
	}

	@Test
	void confirmPaymentByWebhook_isIdempotent_doNotDuplicateSettlement() {

		User seller = createDummyUser("판매자1");

		Product product	= productRepository.save(
			Product.builder()
				.seller(seller)
				.name("Test Product")
				.description("Test Description")
				.category(GAME)
				.subcategory(ETC)
				.createdAt(LocalDateTime.now())
				.bookmarkCount(0)
				.build()
		);
		Auction auction = createLiveAuction(product);

		Winner winner = new Winner();
		ReflectionTestUtils.setField(winner, "auction", auction);
		ReflectionTestUtils.setField(winner, "sellerId", 10L);
		ReflectionTestUtils.setField(winner, "userId", 20L);
		ReflectionTestUtils.setField(winner, "finalPrice", 1000L);
		winner = winnerRepository.save(winner);

		Long winnerId = winner.getId();

		Payment payment = paymentRepository.save(
			Payment.builder()
				.winnersId(winnerId)
				.sellersId(10L)
				.status(PaymentStatus.REQUESTED)
				.net(1000L)
				.requestedAt(LocalDateTime.now())
				.build()
		);

		PaymentServiceImpl service =
			new PaymentServiceImpl(
				paymentRepository,
				settlementRepository,
				tossPaymentsClient,
				customerKeyGenerator,
				winnerRepository
			);

		// first webhook
		service.confirmPaymentByWebhook("pk_test", winnerId, 1000L);

		// second webhook (duplicate)
		service.confirmPaymentByWebhook("pk_test", winnerId, 1000L);

		// then
		assertThat(paymentRepository.findById(payment.getId()))
			.get()
			.extracting(Payment::getStatus)
			.isEqualTo(PaymentStatus.PAID);

		assertThat(settlementRepository.findByPaymentId(payment.getId()))
			.isPresent();
	}
}

