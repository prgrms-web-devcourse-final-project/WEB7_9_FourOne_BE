// package org.com.drop.domain.payment.payment.service;
//
// import static org.assertj.core.api.Assertions.*;
// import static org.mockito.Mockito.*;
//
// import java.time.LocalDateTime;
//
// import org.com.drop.domain.payment.payment.domain.Payment;
// import org.com.drop.domain.payment.payment.domain.PaymentStatus;
// import org.com.drop.domain.payment.payment.infra.toss.TossPaymentsClient;
// import org.com.drop.domain.payment.payment.infra.toss.util.CustomerKeyGenerator;
// import org.com.drop.domain.payment.payment.repository.PaymentRepository;
// import org.com.drop.domain.payment.settlement.repository.SettlementRepository;
// import org.com.drop.domain.winner.domain.Winner;
// import org.com.drop.domain.winner.repository.WinnerRepository;
// import org.junit.jupiter.api.Test;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
// import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
// import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
// import org.springframework.boot.test.context.SpringBootTest;
// import org.springframework.security.web.SecurityFilterChain;
// import org.springframework.test.context.ActiveProfiles;
// import org.springframework.test.context.DynamicPropertyRegistry;
// import org.springframework.test.context.DynamicPropertySource;
// import org.springframework.test.context.bean.override.mockito.MockitoBean;
// import org.springframework.test.util.ReflectionTestUtils;
// import org.testcontainers.containers.MySQLContainer;
// import org.testcontainers.junit.jupiter.Container;
// import org.testcontainers.junit.jupiter.Testcontainers;
//
// @SpringBootTest(
// 	properties = {
// 		"springdoc.api-docs.enabled=false",
// 		"springdoc.swagger-ui.enabled=false"
// 	}
// )
// @Testcontainers
// @ActiveProfiles("test")
// @ImportAutoConfiguration(
// 	exclude = {
// 		WebMvcAutoConfiguration.class,
// 		SecurityAutoConfiguration.class
// 	}
// )
// class PaymentServiceImplTest {
//
// 	@Container
// 	static MySQLContainer<?> mysql =
// 		new MySQLContainer<>("mysql:8.0")
// 			.withDatabaseName("test")
// 			.withUsername("test")
// 			.withPassword("test");
// 	@Autowired
// 	PaymentRepository paymentRepository;
// 	@Autowired
// 	SettlementRepository settlementRepository;
// 	@Autowired
// 	WinnerRepository winnerRepository;
// 	@MockitoBean
// 	SecurityFilterChain securityFilterChain;
// 	TossPaymentsClient tossPaymentsClient = mock(TossPaymentsClient.class);
// 	CustomerKeyGenerator customerKeyGenerator = mock(CustomerKeyGenerator.class);
//
// 	@DynamicPropertySource
// 	static void overrideProps(DynamicPropertyRegistry registry) {
// 		registry.add("spring.datasource.url", mysql::getJdbcUrl);
// 		registry.add("spring.datasource.username", mysql::getUsername);
// 		registry.add("spring.datasource.password", mysql::getPassword);
// 		registry.add("spring.datasource.driver-class-name",
// 			() -> "com.mysql.cj.jdbc.Driver");
// 		registry.add("spring.jpa.hibernate.ddl-auto",
// 			() -> "create-drop");
// 	}
//
// 	@Test
// 	void createPayment_createsRequestedPaymentWithSellerId() {
//
// 		Winner winner = new Winner();
// 		ReflectionTestUtils.setField(winner, "sellerId", 10L);
// 		ReflectionTestUtils.setField(winner, "userId", 20L);
// 		ReflectionTestUtils.setField(winner, "finalPrice", 1000L);
// 		winner = winnerRepository.save(winner);
//
// 		PaymentServiceImpl service =
// 			new PaymentServiceImpl(
// 				paymentRepository,
// 				settlementRepository,
// 				tossPaymentsClient,
// 				customerKeyGenerator,
// 				winnerRepository
// 			);
//
// 		Payment payment = service.createPayment(winner.getId(), 1000L);
//
// 		assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REQUESTED);
// 		assertThat(payment.getSellersId()).isEqualTo(10L);
// 		assertThat(payment.getFee()).isEqualTo(50L);
// 		assertThat(payment.getNet()).isEqualTo(1000L);
// 	}
//
// 	@Test
// 	void confirmPaymentByWebhook_isIdempotent_doNotDuplicateSettlement() {
//
// 		Winner winner = new Winner();
// 		ReflectionTestUtils.setField(winner, "sellerId", 10L);
// 		ReflectionTestUtils.setField(winner, "userId", 20L);
// 		ReflectionTestUtils.setField(winner, "finalPrice", 1000L);
// 		winner = winnerRepository.save(winner);
//
// 		Long winnerId = winner.getId();
//
// 		Payment payment = paymentRepository.save(
// 			Payment.builder()
// 				.winnersId(winnerId)
// 				.sellersId(10L)
// 				.status(PaymentStatus.REQUESTED)
// 				.net(1000L)
// 				.requestedAt(LocalDateTime.now())
// 				.build()
// 		);
//
// 		PaymentServiceImpl service =
// 			new PaymentServiceImpl(
// 				paymentRepository,
// 				settlementRepository,
// 				tossPaymentsClient,
// 				customerKeyGenerator,
// 				winnerRepository
// 			);
//
// 		// first webhook
// 		service.confirmPaymentByWebhook("pk_test", winnerId, 1000L);
//
// 		// second webhook (duplicate)
// 		service.confirmPaymentByWebhook("pk_test", winnerId, 1000L);
//
// 		// then
// 		assertThat(paymentRepository.findById(payment.getId()))
// 			.get()
// 			.extracting(Payment::getStatus)
// 			.isEqualTo(PaymentStatus.PAID);
//
// 		assertThat(settlementRepository.findByPaymentId(payment.getId()))
// 			.isPresent();
// 	}
// }
//
