package org.com.drop.domain.auction.bid;

import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;

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

}
