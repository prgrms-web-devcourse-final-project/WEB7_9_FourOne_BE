package org.com.drop.domain.auction.bid;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.com.drop.domain.auction.auction.entity.Auction;
import org.com.drop.domain.auction.auction.repository.AuctionRepository;
import org.com.drop.domain.auction.bid.dto.request.BidRequestDto;
import org.com.drop.domain.auction.bid.entity.Bid;
import org.com.drop.domain.auction.bid.repository.BidRepository;
import org.com.drop.domain.auction.bid.service.BidService;
import org.com.drop.domain.auction.product.entity.Product;
import org.com.drop.domain.auction.product.repository.ProductRepository;
import org.com.drop.domain.user.entity.User;
import org.com.drop.domain.user.repository.UserRepository;
import org.com.drop.domain.user.service.UserService;
import org.com.drop.global.exception.ErrorCode;
import org.com.drop.global.exception.ServiceException;
import org.com.drop.scheduler.AuctionScheduler;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
public class BidIntegrationTest {

	@Autowired MockMvc mockMvc;

	ObjectMapper objectMapper = new ObjectMapper();

	@Autowired
	UserService userService;

	@Autowired
	BidRepository bidRepository;
	@Autowired
	UserRepository userRepository;
	@Autowired
	AuctionRepository auctionRepository;
	@Autowired
	ProductRepository productRepository;

	@Autowired
	BidService bidService;

	@Autowired
	private AuctionScheduler auctionScheduler;

	private User createUser(String email, String nickname) {
		return userRepository.save(User.builder()
			.email(email)
			.nickname(nickname)
			.password("pass")
			.loginType(User.LoginType.LOCAL)
			.role(User.UserRole.USER)
			.build());
	}

	private Product createProduct(User seller) {
		return productRepository.save(Product.builder()
			.seller(seller)
			.name("테스트상품")
			.description("설명")
			.category(Product.Category.FIGURE)
			.subcategory(Product.SubCategory.ETC)
			.createdAt(LocalDateTime.now())
			.bookmarkCount(0)
			.build());
	}

	private Auction createAuction(Product product, int startPrice, int step) {
		return auctionRepository.save(Auction.builder()
			.product(product)
			.startPrice(startPrice)
			.minBidStep(step)
			.startAt(LocalDateTime.now())
			.endAt(LocalDateTime.now().plusDays(1))
			.status(Auction.AuctionStatus.LIVE)
			.build());
	}

	private Auction createscheduledAuction(Product product, int startPrice, int step) {
		return auctionRepository.save(Auction.builder()
			.product(product)
			.startPrice(startPrice)
			.minBidStep(step)
			.startAt(LocalDateTime.now().minusMinutes(10))
			.endAt(LocalDateTime.now().plusHours(1))
			.status(Auction.AuctionStatus.SCHEDULED)
			.build());
	}


	@Test
	@DisplayName("시작가 보다 높은 금액으로 입찰하면 db에 저장 - 성공")
	void bid_success() throws Exception {
		User seller = createUser("seller@test.com", "판매자");
		Product product = createProduct(seller);

		Auction auction = createAuction(product, 1000, 100);

		String bidderEmail = "bidder@test.com";
		User bidder = createUser(bidderEmail, "입찰자");
		BidRequestDto biddto = new BidRequestDto(1500L);


		//when
		ResultActions result = mockMvc.perform(
			post("/api/v1/auctions/{auctionId}/bids", auction.getId())
				.contentType(String.valueOf(MediaType.APPLICATION_JSON))
				.content(objectMapper.writeValueAsString(biddto))
				.with(user(bidderEmail).roles("USER"))
				.with(csrf())
		);

		//then
		result.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.auctionId").value(auction.getId()))
			.andExpect(jsonPath("$.data.currentHighestBid").value(1500))
			.andExpect(jsonPath("$.data.isHighestBidder").value(true))
			.andExpect(jsonPath("$.data.bidTime").exists());


		//then
		Bid savedBid = bidRepository.findAll().get(0);
		assertThat(savedBid.getBidAmount()).isEqualTo(1500L);
		assertThat(savedBid.getBidder().getEmail()).isEqualTo(bidderEmail);
		assertThat(savedBid.getAuction().getId()).isEqualTo(auction.getId());
	}

	@Test
	@DisplayName("입찰 실패 - 시작가보다 낮은 금액으로 입찰하면 400 에러 발생")
	void place_bid_fail_low_amount() throws Exception {
		//given
		User seller = createUser("seller@test.com", "판매자");
		Product product = createProduct(seller);
		Auction auction = createAuction(product, 10000, 1000);

		String bidderEmail = "poor@test.com";
		User bidder = createUser(bidderEmail, "가난한입찰자");

		//when
		BidRequestDto badRequest = new BidRequestDto(5000L);

		ResultActions result = mockMvc.perform(
			post("/api/v1/auctions/{auctionId}/bids", auction.getId())
				.contentType(String.valueOf(MediaType.APPLICATION_JSON))
				.content(objectMapper.writeValueAsString(badRequest))
				.with(user(bidderEmail).roles("USER"))
				.with(csrf())
		);

		//then
		result.andExpect(status().is4xxClientError())
			.andDo(print());
	}

	@Test
	@DisplayName("경매 종료 후 낙찰 성공 - 시간이 지났을 때 최고가 입찰자가 낙찰자가 된다")
	void auction_closing_success() throws Exception {
		User seller = createUser("seller@test.com", "판매자");
		Product product = createProduct(seller);
		Auction auction = createAuction(product, 1000, 100);

		User loser = createUser("loser@test.com", "패배자");
		User winner = createUser("winner@test.com", "낙찰자");

		mockMvc.perform(post("/api/v1/auctions/{auctionId}/bids", auction.getId())
			.contentType(String.valueOf(MediaType.APPLICATION_JSON))
			.content(objectMapper.writeValueAsString(new BidRequestDto(1200L)))
			.with(user(loser.getEmail()).roles("USER")) // 리졸버용 이메일
			.with(csrf())
		).andExpect(status().isOk());

		mockMvc.perform(post("/api/v1/auctions/{auctionId}/bids", auction.getId())
			.contentType(String.valueOf(MediaType.APPLICATION_JSON))
			.content(objectMapper.writeValueAsString(new BidRequestDto(2000L)))
			.with(user(winner.getEmail()).roles("USER"))
			.with(csrf())
		).andExpect(status().isOk());


		auction.end(LocalDateTime.now().minusMinutes(1));
		// auction.setStatus(Auction.AuctionStatus.ENDED);
		auctionRepository.saveAndFlush(auction);

		Bid winningBid = bidRepository.findTopByAuction_IdOrderByBidAmountDesc(auction.getId())
			.orElseThrow(() -> new IllegalArgumentException("입찰 내역이 없습니다."));

		assertThat(winningBid.getBidder().getEmail()).isEqualTo(winner.getEmail());
		assertThat(winningBid.getBidAmount()).isEqualTo(2000L);
		Auction endedAuction = auctionRepository.findById(auction.getId()).get();
		assertThat(endedAuction.getEndAt()).isBefore(LocalDateTime.now());
	}

	@Test
	@DisplayName("시작 시간이 지난 경매는 상태가 SCHEDULED -> LIVE로 자동 변경되어야 한다")
	void auctionStartTest() {
		//given
		User seller = createUser("seller@test.com", "판매자");
		Product product = createProduct(seller);
		Auction auction = createscheduledAuction(product, 1000, 100);

		auctionRepository.save(auction);

		//when
		auctionScheduler.runAuctionScheduler();

		//then
		Auction updatedAuction = auctionRepository.findById(auction.getId()).orElseThrow();

		assertThat(updatedAuction.getStatus()).isEqualTo(Auction.AuctionStatus.LIVE);

		System.out.println("변경 확인 완료. 현재 상태: " + updatedAuction.getStatus());
	}

	@Test
	@Disabled
	@DisplayName("동시에 입찰이 들어와도 최고가 검증이 뚫리면 안 된다")
	void auctionLockTest2() throws Exception {
		//given
		User seller = createUser("seller@test.com", "판매자");
		Product product = createProduct(seller);
		Auction auction = createAuction(product, 1000, 100);
		User bidder1 = createUser("bidder1@test.com", "입찰자1");
		User bidder2 = createUser("bidder2@test.com", "입찰자2");

		auctionRepository.save(auction);

		int threads = 2;
		ExecutorService es = Executors.newFixedThreadPool(threads);

		CountDownLatch ready = new CountDownLatch(threads);
		CountDownLatch start = new CountDownLatch(1);
		CountDownLatch done  = new CountDownLatch(threads);

		List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

		// when
		es.submit(() -> {
			ready.countDown();
			await(start);
			try {
				bidService.placeBid(auction.getId(), bidder1.getId(), new BidRequestDto(1100L));
			} catch (Throwable t) {
				errors.add(t);
			} finally {
				done.countDown();
			}
		});

		es.submit(() -> {
			ready.countDown();
			await(start);
			try {
				bidService.placeBid(auction.getId(), bidder2.getId(), new BidRequestDto(1150L));
			} catch (Throwable t) {
				errors.add(t);
			} finally {
				done.countDown();
			}
		});

		ready.await();
		start.countDown();
		done.await();
		es.shutdown();

		// then
		List<Bid> bids = bidRepository.findTopByAuction_IdOrderByBidAmountDesc(auction.getId())
			.map(List::of)
			.orElseGet(List::of);

		assertThat(bids).hasSize(1);
		assertThat(bids.get(0).getBidAmount()).isEqualTo(1100L);

		assertThat(errors).hasSize(1);
		assertThat(errors.get(0)).isInstanceOf(ServiceException.class);
		ServiceException se = (ServiceException) errors.get(0);
		assertThat(se.getErrorCode()).isEqualTo(ErrorCode.AUCTION_BID_AMOUNT_TOO_LOW);
	}

	private void await(CountDownLatch latch) {
		try {
			latch.await();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}


	@Test
	@Disabled
	@DisplayName("동시에 30명이 같은 가격으로 입찰하면 1명만 성공하고 나머지는 실패해야 한다")
	void auctionLockTest() throws InterruptedException {
		// given
		User seller = createUser("seller@test.com", "판매자");
		Product product = createProduct(seller);
		Auction auction = createAuction(product, 1000, 100);
		auctionRepository.save(auction);

		int threadCount = 30;
		ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
		CountDownLatch latch = new CountDownLatch(threadCount);

		AtomicInteger successCount = new AtomicInteger();
		AtomicInteger failCount = new AtomicInteger();

		Long bidPrice = 1100L;
		BidRequestDto requestDto = new BidRequestDto(bidPrice);

		// when
		for (int i = 0; i < threadCount; i++) {
			User bidder = createUser("bidder" + i + "@test.com", "입찰자" + i);

			executorService.submit(() -> {
				try {
					bidService.placeBid(auction.getId(), bidder.getId(), requestDto);
					successCount.incrementAndGet();
				} catch (Exception e) {
					failCount.incrementAndGet();
				} finally {
					latch.countDown();
				}
			});
		}

		latch.await();

		// then
		Auction findAuction = auctionRepository.findById(auction.getId()).orElseThrow();
		assertThat(successCount.get()).isEqualTo(1);
		assertThat(failCount.get()).isEqualTo(threadCount - 1);
		assertThat(findAuction.getCurrentPrice()).isEqualTo(1100L);
	}

}
