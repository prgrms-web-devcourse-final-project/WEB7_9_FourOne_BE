package org.com.drop.domain.payment.payment.service;

import static org.assertj.core.api.Assertions.*;
import static org.com.drop.domain.auction.product.entity.Product.Category.GAME;
import static org.com.drop.domain.auction.product.entity.Product.SubCategory.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.UUID;

import org.com.drop.config.TestRedissonConfig;
import org.com.drop.domain.auction.auction.entity.Auction;
import org.com.drop.domain.auction.auction.repository.AuctionRepository;
import org.com.drop.domain.auction.product.entity.Product;
import org.com.drop.domain.auction.product.repository.ProductRepository;
import org.com.drop.domain.payment.payment.domain.Payment;
import org.com.drop.domain.payment.payment.domain.PaymentStatus;
import org.com.drop.domain.payment.payment.infra.toss.TossPaymentsClient;
import org.com.drop.domain.payment.payment.infra.toss.util.CustomerKeyGenerator;
import org.com.drop.domain.payment.payment.repository.PaymentRepository;
import org.com.drop.domain.payment.settlement.domain.Settlement;
import org.com.drop.domain.payment.settlement.repository.SettlementRepository;
import org.com.drop.domain.user.entity.User;
import org.com.drop.domain.user.repository.UserRepository;
import org.com.drop.domain.winner.domain.Winner;
import org.com.drop.domain.winner.repository.WinnerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@Import(TestRedissonConfig.class)
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

	@MockitoBean
	TossPaymentsClient tossPaymentsClient;

	@MockitoBean
	CustomerKeyGenerator customerKeyGenerator;

	@MockitoBean
	RedissonClient redissonClient;

	@MockitoBean
	RLock rLock;

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

	private long countSettlementsByPaymentId(Long paymentId) {
		return settlementRepository.findAll().stream()
			.filter(s -> paymentId.equals(s.getPaymentId()))
			.count();
	}

	@BeforeEach
	void setUp() throws InterruptedException {
		when(redissonClient.getLock(anyString())).thenReturn(rLock);
		when(rLock.tryLock(anyLong(), anyLong(), any())).thenReturn(true);
		when(rLock.isHeldByCurrentThread()).thenReturn(true);
	}

	@Test
	void confirmPaymentByWebhook_isIdempotent_createOnlyOneSettlement_andKeepPaid() {
		// given
		User seller = createDummyUser("판매자1");

		Product product = productRepository.save(
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
				winnerRepository,
				redissonClient
			);

		long beforeCount = countSettlementsByPaymentId(payment.getId());

		// when
		service.confirmPaymentByWebhook("pk_test", winnerId, 1000L);
		service.confirmPaymentByWebhook("pk_test", winnerId, 1000L); // duplicate

		// then: payment 상태
		Payment saved = paymentRepository.findById(payment.getId()).orElseThrow();
		assertThat(saved.getStatus()).isEqualTo(PaymentStatus.PAID);

		// then: settlement 1개만 생성
		long afterCount = countSettlementsByPaymentId(payment.getId());
		assertThat(afterCount - beforeCount).isEqualTo(1);

		// then: settlement 내용 검증(가능하면 이런 것도 넣는 게 좋음)
		Settlement settlement = settlementRepository.findByPaymentId(payment.getId()).orElseThrow();
		assertThat(settlement.getPaymentId()).isEqualTo(payment.getId());
		assertThat(settlement.getSellerId()).isEqualTo(10L);
		assertThat(settlement.getNet()).isEqualTo(1000L);
		// fee/net 계산 정책에 따라 fee도 검증 가능
	}
}
