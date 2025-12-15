package org.com.drop.domain.auction.bid.service;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.com.drop.domain.auction.auction.entity.Auction;
import org.com.drop.domain.auction.auction.repository.AuctionRepository;
import org.com.drop.domain.auction.bid.dto.request.BidRequestDto;
import org.com.drop.domain.auction.bid.entity.Bid;
import org.com.drop.domain.auction.bid.entity.Winner;
import org.com.drop.domain.auction.bid.repository.BidRepository;
import org.com.drop.domain.auction.bid.repository.WinnerRepository;
import org.com.drop.domain.auction.product.entity.Product;
import org.com.drop.domain.auction.product.repository.ProductRepository;
import org.com.drop.domain.user.entity.User;
import org.com.drop.domain.user.repository.UserRepository;
import org.com.drop.global.exception.ErrorCode;
import org.com.drop.global.exception.ServiceException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class WinnerServiceTest {

	@Autowired
	private BidService bidService;

	@Autowired
	private AuctionRepository auctionRepository;

	@Autowired
	private BidRepository bidRepository;

	@Autowired
	private WinnerService winnerService;

	@Autowired
	private WinnerRepository winnerRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ProductRepository productRepository;


	@Test
	void 경매종료시_최고입찰자가_낙찰된다() {
		// given
		User seller = createDummyUser("seller");
		User bidder1 = createDummyUser("bidder1");
		User bidder2 = createDummyUser("bidder2");

		Product product = createDummyProduct(seller, "상품1");

		Auction auction = auctionRepository.saveAndFlush(
			new Auction(
				product,
				10_000,                 // startPrice
				50_000,                 // buyNowPrice
				1_000,                  // bidUnit
				LocalDateTime.now().minusMinutes(20), // startAt
				LocalDateTime.now().minusMinutes(1),  // endAt (이미 종료됨)
				Auction.AuctionStatus.LIVE
			)
		);

		bidRepository.saveAndFlush(Bid.builder()
			.auction(auction)
			.bidder(bidder1)
			.bidAmount(11_000L)
			.createdAt(LocalDateTime.now().minusSeconds(2))
			.isAuto(false)
			.build());

		bidRepository.saveAndFlush(Bid.builder()
			.auction(auction)
			.bidder(bidder2)
			.bidAmount(12_000L)
			.createdAt(LocalDateTime.now().minusSeconds(1))
			.isAuto(false)
			.build());


		// when
		winnerService.finalizeAuction(auction.getId());

		// then
		Auction updated = auctionRepository.findById(auction.getId()).orElseThrow();
		assertThat(updated.getStatus()).isEqualTo(Auction.AuctionStatus.ENDED);

		Winner winner = winnerRepository.findByAuction_Id(auction.getId())
			.orElseThrow(); // findByAuctionId는 네 리포지토리 메서드에 맞게 변경

		assertThat(winner.getUser()).isEqualTo(bidder2);
		assertThat(winner.getFinalPrice()).isEqualTo(12_000);
	}

	@Test
	void 입찰이_없으면_Winner없이_경매만_ENDED상태가_된다() {
		// given
		User seller = createDummyUser("seller");
		Product product = createDummyProduct(seller, "입찰없음테스트상품");


		Auction auction = auctionRepository.saveAndFlush(
			new Auction(
				product,
				10_000,                 // startPrice
				50_000,                 // buyNowPrice
				1_000,                  // bidUnit
				LocalDateTime.now().minusMinutes(20), // startAt
				LocalDateTime.now().minusMinutes(1),  // endAt (이미 종료됨)
				Auction.AuctionStatus.LIVE
			)
		);


		// when
		winnerService.finalizeAuction(auction.getId());

		// then
		Auction updated = auctionRepository.findById(auction.getId())
			.orElseThrow();

		assertThat(updated.getStatus()).isEqualTo(Auction.AuctionStatus.ENDED);

		assertThat(winnerRepository.findByAuction_Id(auction.getId())).isEmpty();

	}

	@Test
	void 종료시간이_안지나면_경매는_종료되지_않고_Winner도_생성되지_않는다() {
		// given
		User seller = createDummyUser("seller");
		User bidder = createDummyUser("bidder");

		Product product = createDummyProduct(seller, "미종료테스트상품");

		Auction auction = auctionRepository.saveAndFlush(
			new Auction(
				product,
				10_000,
				50_000,
				1_000,
				LocalDateTime.now().minusMinutes(5),
				LocalDateTime.now().plusMinutes(5), // 아직 안 끝남
				Auction.AuctionStatus.LIVE
			)
		);

		bidRepository.saveAndFlush(Bid.builder()
			.auction(auction)
			.bidder(bidder)
			.bidAmount(11_000L)
			.createdAt(LocalDateTime.now())
			.isAuto(false)
			.build());


		// when
		winnerService.finalizeAuction(auction.getId());

		// then
		Auction updated = auctionRepository.findById(auction.getId())
			.orElseThrow();

		// 1) 상태는 여전히 LIVE 여야 한다
		assertThat(updated.getStatus()).isEqualTo(Auction.AuctionStatus.LIVE);

		// 2) Winner 가 생성되면 안 된다
		assertThat(winnerRepository.findByAuction_Id(auction.getId())).isEmpty();

	}

	@Test
	void 존재하지않는_경매ID면_AUCTION_NOT_FOUND_예외가_발생한다() {
		// given
		Long invalidAuctionId = 999999L; // 존재하지 않는 ID라고 가정

		// when & then
		assertThatThrownBy(() -> winnerService.finalizeAuction(invalidAuctionId))
			.isInstanceOf(ServiceException.class)
			.hasMessageContaining(ErrorCode.AUCTION_NOT_FOUND.getMessage());
	}

	@Test
	void finalizeAuction을_두번_호출해도_Winner는_한번만_생성된다() {
		// given
		User seller = createDummyUser("seller");
		User bidder1 = createDummyUser("bidder1");
		User bidder2 = createDummyUser("bidder2");


		Product product = createDummyProduct(seller, "멱등성테스트상품");
		Auction auction = auctionRepository.saveAndFlush(
			new Auction(
				product,
				10_000,
				50_000,
				1_000,
				LocalDateTime.now().minusMinutes(20),
				LocalDateTime.now().minusMinutes(1), // 이미 종료
				Auction.AuctionStatus.LIVE
			)
		);

		bidRepository.saveAndFlush(Bid.builder()
			.auction(auction)
			.bidder(bidder1)
			.bidAmount(11_000L)
			.createdAt(LocalDateTime.now().minusSeconds(2))
			.isAuto(false)
			.build());

		bidRepository.saveAndFlush(Bid.builder()
			.auction(auction)
			.bidder(bidder2)
			.bidAmount(12_000L)
			.createdAt(LocalDateTime.now().minusSeconds(1))
			.isAuto(false)
			.build());

		// when
		winnerService.finalizeAuction(auction.getId());
		winnerService.finalizeAuction(auction.getId());

		// then
		Auction updated = auctionRepository.findById(auction.getId()).orElseThrow();
		assertThat(updated.getStatus()).isEqualTo(Auction.AuctionStatus.ENDED);

		List<Winner> winners = winnerRepository.findAllByAuction_Id(auction.getId());
		assertThat(winners).hasSize(1);
		assertThat(winners.get(0).getUser()).isEqualTo(bidder2);
		assertThat(winners.get(0).getFinalPrice()).isEqualTo(12_000);

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
		bidService.placeBid(auction.getId(), bidder.getId(), dto);
	}
}
