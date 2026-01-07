package org.com.drop.domain.auction.bid.service;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.UUID;

import org.com.drop.BaseIntegrationTest;
import org.com.drop.domain.auction.auction.entity.Auction;
import org.com.drop.domain.auction.auction.repository.AuctionRepository;
import org.com.drop.domain.auction.bid.dto.request.BidRequestDto;
import org.com.drop.domain.auction.bid.dto.response.BidResponseDto;
import org.com.drop.domain.auction.bid.repository.BidRepository;
import org.com.drop.domain.auction.product.entity.Product;
import org.com.drop.domain.auction.product.repository.ProductRepository;
import org.com.drop.domain.user.entity.User;
import org.com.drop.domain.user.repository.UserRepository;
import org.com.drop.global.exception.ServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class BidServiceTest extends BaseIntegrationTest {

	@Autowired
	private BidService bidService;

	@Autowired
	private AuctionRepository auctionRepository;

	@Autowired
	private BidRepository bidRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ProductRepository productRepository;

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
			.minBidStep(1_000)
			.buyNowPrice(null)
			.startAt(now.minusHours(1))        // 이미 시작
			.endAt(now.plusHours(1))           // 아직 안 끝남
			.status(Auction.AuctionStatus.LIVE)
			.build();

		return auctionRepository.save(auction);
	}

	// @Test
	// void 유효한_입찰이면_Bid가_저장되고_Auction_현재가가_갱신된다() {}

	@DisplayName("유효한_입찰일때_정상적으로_입찰_생성")
	@Test
	void createsBidSuccessfullyWhenBidIsValid() {
		//given
		User bidder = createDummyUser("입찰자1");
		User seller = createDummyUser("판매자1");
		Product product = createDummyProduct(seller, "테스트 상품");
		Auction auction = createLiveAuction(product);

		BidRequestDto requestDto = new BidRequestDto(15_000L); // 시작가 10_000 + minStep 1_000 이상

		//when
		BidResponseDto responseDto = bidService.placeBid(auction.getId(), bidder.getId(), requestDto);

		//then
		assertThat(responseDto.isHighestBidder()).isTrue();
		assertThat(responseDto.currentHighestBid()).isEqualTo(15_000L);

	}

	@DisplayName("최소입찰단위_미만이면_예외가_발생한다")
	@Test
	void throwsExceptionWhenBidAmountBelowBidUnit() {
		// given
		User user = createDummyUser("입찰자1");
		User seller = createDummyUser("판매자1");
		Product product = createDummyProduct(seller, "테스트 상품");
		Auction auction = createLiveAuction(product); // startPrice = 10_000, minBidStep = 1_000

		// 현재 최고가는 아직 시작가(10_000)라고 가정
		BidRequestDto requestDto = new BidRequestDto(10_500L); // 최소 입찰 가능 금액 11_000 미만

		// when & then
		assertThatThrownBy(() -> bidService.placeBid(auction.getId(), user.getId(), requestDto))
			.isInstanceOf(ServiceException.class)
			.hasMessageContaining("입찰 금액이 현재 최고가보다 낮거나 최소 입찰 단위를 충족하지 못했습니다.");

	}

	// @Test
	// void 현재_최고가_이하로_입찰하면_예외가_발생한다() {}
	//
	// @Test
	// void 종료된_경매에는_입찰할_수_없다() {}
	//
	// @Test
	// void 판매자는_자신의_경매에_입찰할_수_없다() {}
	//
	// @Test
	// void 존재하지않는_경매에_입찰하면_AUCTION_NOT_FOUND_예외가_발생한다() {}
	//
	// @Test
	// void 동일_사용자가_연속으로_더_높은_금액으로_입찰하면_정상적으로_현재가와_입찰내역이_갱신된다() {}

}
