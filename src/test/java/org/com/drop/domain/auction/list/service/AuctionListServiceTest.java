package org.com.drop.domain.auction.list.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.com.drop.domain.auction.auction.entity.Auction.AuctionStatus;
import org.com.drop.domain.auction.bid.dto.response.BidHistoryResponse;
import org.com.drop.domain.auction.bid.entity.Bid;
import org.com.drop.domain.auction.bid.repository.BidRepository;
import org.com.drop.domain.auction.list.dto.request.AuctionSearchRequest;
import org.com.drop.domain.auction.list.dto.response.AuctionBidUpdate;
import org.com.drop.domain.auction.list.dto.response.AuctionCursorResponse;
import org.com.drop.domain.auction.list.dto.response.AuctionDetailResponse;
import org.com.drop.domain.auction.list.dto.response.AuctionHomeResponse;
import org.com.drop.domain.auction.list.repository.AuctionListRepository;
import org.com.drop.domain.auction.list.repository.AuctionListRepositoryCustom;
import org.com.drop.domain.auction.product.entity.Product;
import org.com.drop.domain.user.entity.User;
import org.com.drop.global.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuctionListService 단위 테스트")
class AuctionListServiceTest {

	@Mock
	private AuctionListRepository auctionListRepository;

	@Mock
	private BidRepository bidRepository;

	@InjectMocks
	private AuctionListService auctionListService;

	private User testUser;
	private AuctionListRepositoryCustom.AuctionItemDto itemDto;
	private AuctionListRepositoryCustom.AuctionDetailDto detailDto;
	private AuctionListRepositoryCustom.CurrentHighestBidDto highestBidDto;

	@BeforeEach
	void setUp() {
		testUser = User.builder()
			.id(1L)
			.nickname("testUser")
			.build();

		itemDto = AuctionListRepositoryCustom.AuctionItemDto.builder()
			.auctionId(1L)
			.productId(1L)
			.name("테스트 상품")
			.imageUrl("test.jpg")
			.status(AuctionStatus.LIVE)
			.category(Product.Category.STARGOODS)
			.subCategory(Product.SubCategory.ACC)
			.currentHighestBid(10000)
			.startPrice(5000)
			.endAt(LocalDateTime.now().plusHours(1))
			.bookmarkCount(5)
			.bidCount(3)
			.createdAt(LocalDateTime.now())
			.score(8)
			.build();

		detailDto = AuctionListRepositoryCustom.AuctionDetailDto.builder()
			.auctionId(1L)
			.productId(1L)
			.sellerId(2L)
			.sellerNickname("판매자")
			.name("테스트 상품")
			.description("상품 설명")
			.category(Product.Category.STARGOODS)
			.subCategory(Product.SubCategory.ACC)
			.status(AuctionStatus.LIVE)
			.startPrice(5000)
			.buyNowPrice(20000)
			.minBidStep(1000)
			.startAt(LocalDateTime.now().minusHours(1))
			.endAt(LocalDateTime.now().plusHours(1))
			.createdAt(LocalDateTime.now())
			.currentHighestBid(10000)
			.totalBidCount(3)
			.imageUrls(Arrays.asList("img1.jpg"))
			.build();

		highestBidDto = AuctionListRepositoryCustom.CurrentHighestBidDto.builder()
			.currentHighestBid(15000)
			.bidderNickname("입찰자")
			.build();
	}

	@Test
	@DisplayName("경매 목록 조회 - 성공 (로그인 사용자)")
	void getAuctions_success_withUser() {
		// given
		AuctionSearchRequest request = AuctionSearchRequest.builder()
			.size(20)
			.build();
		List<AuctionListRepositoryCustom.AuctionItemDto> dtos = Arrays.asList(itemDto);

		given(auctionListRepository.searchAuctions(request)).willReturn(dtos);
		given(auctionListRepository.getNextCursor(dtos, 20)).willReturn(null);
		given(auctionListRepository.isBookmarked(eq(1L), eq(1L))).willReturn(true);

		// when
		AuctionCursorResponse response = auctionListService.getAuctions(request, testUser);

		// then
		assertThat(response.getItems()).hasSize(1);
		assertThat(response.getItems().get(0).getIsBookmarked()).isTrue();
		verify(auctionListRepository).isBookmarked(eq(1L), eq(1L));
	}

	@Test
	@DisplayName("경매 목록 조회 - 성공 (비로그인 사용자)")
	void getAuctions_success_anonymous() {
		// given
		AuctionSearchRequest request = AuctionSearchRequest.builder()
			.size(20)
			.build();
		List<AuctionListRepositoryCustom.AuctionItemDto> dtos = Arrays.asList(itemDto);

		given(auctionListRepository.searchAuctions(request)).willReturn(dtos);
		given(auctionListRepository.getNextCursor(dtos, 20)).willReturn(null);

		// when
		AuctionCursorResponse response = auctionListService.getAuctions(request, null);

		// then
		assertThat(response.getItems()).hasSize(1);
		assertThat(response.getItems().get(0).getIsBookmarked()).isFalse();
	}

	@Test
	@DisplayName("경매 목록 조회 - hasNext true")
	void getAuctions_hasNext_true() {
		// given
		AuctionSearchRequest request = AuctionSearchRequest.builder().size(1).build();
		List<AuctionListRepositoryCustom.AuctionItemDto> dtos = Arrays.asList(itemDto, itemDto);

		given(auctionListRepository.searchAuctions(request)).willReturn(dtos);
		given(auctionListRepository.getNextCursor(dtos, 1)).willReturn("cursor");
		given(auctionListRepository.isBookmarked(eq(1L), eq(1L))).willReturn(false);

		// when
		AuctionCursorResponse response = auctionListService.getAuctions(request, testUser);

		// then
		assertThat(response.isHasNext()).isTrue();
		assertThat(response.getCursor()).isEqualTo("cursor");
	}

	@Test
	@DisplayName("경매 상세 조회 - 성공")
	void getAuctionDetail_success() {
		// given
		given(auctionListRepository.findAuctionDetailById(1L)).willReturn(Optional.of(detailDto));
		given(auctionListRepository.isBookmarked(eq(1L), eq(1L))).willReturn(false);

		Bid bid = Bid.builder()
			.id(1L)
			.bidAmount(10000L)
			.bidder(User.builder().nickname("입찰자").build())
			.createdAt(LocalDateTime.now())
			.build();
		Page<Bid> bidPage = new PageImpl<>(List.of(bid));
		given(bidRepository.findAllByAuctionId(eq(1L), any(PageRequest.class))).willReturn(bidPage);

		// when
		AuctionDetailResponse response = auctionListService.getAuctionDetail(1L, testUser);

		// then
		assertThat(response.getAuctionId()).isEqualTo(1L);
		assertThat(response.getIsBookmarked()).isFalse();
		assertThat(response.getBidHistory()).hasSize(1);
	}

	@Test
	@DisplayName("경매 상세 조회 - 경매 없음")
	void getAuctionDetail_auctionNotFound() {
		// given
		given(auctionListRepository.findAuctionDetailById(999L)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> auctionListService.getAuctionDetail(999L, testUser))
			.isInstanceOf(ServiceException.class)
			.hasMessageContaining("경매를 찾을 수 없습니다");
	}

	@Test
	@DisplayName("현재 최고 입찰가 조회 - 입찰 있음")
	void getCurrentHighestBid_withBid() {
		// given
		given(auctionListRepository.findCurrentHighestBid(1L)).willReturn(Optional.of(highestBidDto));

		// when
		AuctionBidUpdate response = auctionListService.getCurrentHighestBid(1L);

		// then
		assertThat(response.getCurrentHighestBid()).isEqualTo(15000);
		assertThat(response.getBidderNickname()).isEqualTo("입찰***");
	}

	@Test
	@DisplayName("현재 최고 입찰가 조회 - 입찰 없음")
	void getCurrentHighestBid_noBid() {
		// given
		given(auctionListRepository.findCurrentHighestBid(1L)).willReturn(Optional.empty());
		given(auctionListRepository.findAuctionStartPrice(1L)).willReturn(Optional.of(5000));

		// when
		AuctionBidUpdate response = auctionListService.getCurrentHighestBid(1L);

		// then
		assertThat(response.getCurrentHighestBid()).isEqualTo(5000);
		assertThat(response.getBidderNickname()).isNull();
	}

	@Test
	@DisplayName("홈화면 조회 - 성공")
	void getHomeAuctions_success() {
		// given
		List<AuctionListRepositoryCustom.AuctionItemDto> endingSoon = List.of(itemDto);
		List<AuctionListRepositoryCustom.AuctionItemDto> popular = List.of(itemDto);

		given(auctionListRepository.findEndingSoonAuctions(10)).willReturn(endingSoon);
		given(auctionListRepository.findPopularAuctions(10)).willReturn(popular);
		given(auctionListRepository.isBookmarked(eq(1L), eq(1L))).willReturn(false);

		// when
		AuctionHomeResponse response = auctionListService.getHomeAuctions(testUser);

		// then
		assertThat(response.getEndingSoon()).hasSize(1);
		assertThat(response.getPopular()).hasSize(1);
	}

	@Test
	@DisplayName("입찰 내역 조회")
	void getBidHistory_success() {
		// given
		Bid bid1 = Bid.builder()
			.id(1L)
			.bidAmount(10000L)
			.bidder(User.builder().nickname("user1").id(3L).build())
			.createdAt(LocalDateTime.now())
			.build();
		Bid bid2 = Bid.builder()
			.id(2L)
			.bidAmount(9000L)
			.bidder(User.builder().nickname("user2").id(4L).build())
			.createdAt(LocalDateTime.now().minusMinutes(5))
			.build();

		Page<Bid> bids = new PageImpl<>(List.of(bid1, bid2));
		given(bidRepository.findAllByAuctionId(eq(1L), any(PageRequest.class))).willReturn(bids);

		// when
		List<BidHistoryResponse> response = auctionListService.getBidHistory(1L, 10);

		// then
		assertThat(response).hasSize(2);
		assertThat(response.get(0).bidder()).isEqualTo("us***");  // user1 → "us***"
		assertThat(response.get(1).bidder()).isEqualTo("us***");  // user2 → "us***"
	}
}
