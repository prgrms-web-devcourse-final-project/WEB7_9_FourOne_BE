package org.com.drop.domain.auction.bid.service;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.com.drop.domain.auction.auction.entity.Auction;
import org.com.drop.domain.auction.auction.repository.AuctionRepository;
import org.com.drop.domain.auction.bid.dto.request.BidRequestDto;
import org.com.drop.domain.auction.bid.dto.response.BuyNowResponseDto;
import org.com.drop.domain.auction.product.entity.Product;
import org.com.drop.domain.auction.product.repository.ProductRepository;
import org.com.drop.domain.user.entity.User;
import org.com.drop.domain.user.repository.UserRepository;
import org.com.drop.domain.winner.domain.Winner;
import org.com.drop.domain.winner.repository.WinnerRepository;
import org.com.drop.global.exception.ErrorCode;
import org.com.drop.global.exception.ServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class BuyNowServiceTest {

	@Autowired
	BuyNowService buyNowService;

	@Autowired
	BidService bidService;

	@Autowired
	UserRepository userRepository;
	@Autowired
	ProductRepository productRepository;
	@Autowired
	AuctionRepository auctionRepository;
	@Autowired
	WinnerRepository winnerRepository;

	LocalDateTime now = LocalDateTime.now();

	@DisplayName("즉시구매가_설정되지_않으면_예외가_발생한다")
	@Test
	void throwsExceptionWhenBuyNowPriceIsNotSet() {
		User seller = createDummyUser("seller");
		User buyer = createDummyUser("buyer");
		Product product = createDummyProduct(seller, "즉구없음");

		Auction auction = auctionRepository.saveAndFlush(
			new Auction(
				product,
				10_000,                 // startPrice
				null,                   // buyNowPrice
				1_000,                  // bidUnit
				LocalDateTime.now().minusMinutes(5), // 시작됨
				LocalDateTime.now().plusMinutes(10), // 아직 안 끝남
				Auction.AuctionStatus.LIVE
			)
		);

		assertThatThrownBy(() -> buyNowService.buyNow(auction.getId(), buyer.getId()))
			.isInstanceOf(ServiceException.class)
			.hasMessageContaining("즉시 구매가 불가능한 상품입니다.");
	}

	@DisplayName("즉시구매_성공하면_경매가_ENDED되고_Winner가_생성된다")
	@Test
	void endsAuctionAndCreatesWinnerWhenBuyNowSucceeds() {
		LocalDateTime now = LocalDateTime.now();

		User seller = userRepository.save(createDummyUser("seller"));
		User buyer = userRepository.save(createDummyUser("buyer"));

		Product product = productRepository.save(createDummyProduct(seller, "즉구성공"));

		Auction auction = auctionRepository.save(new Auction(
			product,
			10_000,
			50_000,
			1_000,
			now.minusMinutes(10),
			now.plusMinutes(10),
			Auction.AuctionStatus.LIVE
		));

		BuyNowResponseDto res = buyNowService.buyNow(auction.getId(), buyer.getId());

		assertThat(res.auctionId()).isEqualTo(auction.getId());
		assertThat(res.auctionStatus()).isEqualTo("ENDED");
		assertThat(res.finalPrice()).isEqualTo(50_000);
		assertThat(res.winnerId()).isNotNull();
		assertThat(res.winTime()).isNotNull();

		Auction updated = auctionRepository.findById(auction.getId()).orElseThrow();
		assertThat(updated.getStatus()).isEqualTo(Auction.AuctionStatus.ENDED);

		Winner winner = winnerRepository.findByAuction_Id(auction.getId()).orElseThrow();
		assertThat(winner.getFinalPrice()).isEqualTo(50_000);
		// Lazy 문제 나면 fetch join or ID만 검증
		assertThat(winner.getUserId()).isEqualTo(buyer.getId());
	}

	@DisplayName("즉시구매를_두번_호출하면_두번째는_예외이고_winner는_하나만_존재한다")
	@Test
	void throwsExceptionOnSecondBuyNowAndCreatesOnlyOneWinner() {
		User seller = createDummyUser("seller");
		User buyer = createDummyUser("buyer");
		Product product = createDummyProduct(seller, "즉구중복");

		Auction auction = auctionRepository.save(new Auction(
			product,
			10_000,
			50_000,
			1_000,
			LocalDateTime.now().minusMinutes(10),
			LocalDateTime.now().plusMinutes(10),
			Auction.AuctionStatus.LIVE
		));

		Auction saved = auctionRepository.findById(auction.getId()).orElseThrow();
		assertThat(saved.getBuyNowPrice()).isNotNull();

		buyNowService.buyNow(auction.getId(), buyer.getId());

		ServiceException ex = catchThrowableOfType(
			() -> buyNowService.buyNow(auction.getId(), buyer.getId()),
			ServiceException.class
		);

		assertThat(ex.getErrorCode()).isIn(
			ErrorCode.AUCTION_ALREADY_ENDED,
			ErrorCode.AUCTION_BUY_NOW_NOT_AVAILABLE,
			ErrorCode.AUCTION_NOT_LIVE
		);

		// countByAuctionId 있으면 그게 베스트
		List<Winner> winners = winnerRepository.findAllByAuction_Id(auction.getId());
		assertThat(winners).hasSize(1);
	}

	// ====== 테스트 헬퍼 ======

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

	private Product createDummyProduct(User seller, String name) {
		Product product = Product.builder()
			.seller(seller)
			.name(name)
			.description("테스트용 상품 설명입니다.")
			.category(Product.Category.STARGOODS)
			.subcategory(Product.SubCategory.ETC)
			.createdAt(LocalDateTime.now())
			.bookmarkCount(0)
			.build();
		return productRepository.save(product);
	}

	private Auction createLiveAuction(Product product) {
		LocalDateTime now = LocalDateTime.now();

		Auction auction = Auction.builder()
			.product(product)
			.startPrice(10_000)
			.minBidStep(1000)
			.status(Auction.AuctionStatus.LIVE)
			.startAt(now.minusMinutes(10))
			.endAt(now.plusMinutes(10)) // 기본은 아직 안 끝난 상태
			.build();

		return auctionRepository.save(auction);
	}

	private void placeBid(Auction auction, User bidder, long amount) {
		BidRequestDto dto = new BidRequestDto(amount); // 네 실제 DTO에 맞게 수정!
		bidService.placeBid(auction.getId(), bidder.getEmail(), dto);
	}
}
