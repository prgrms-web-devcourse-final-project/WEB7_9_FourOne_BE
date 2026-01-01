package org.com.drop.domain.auction.list.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.com.drop.domain.auction.auction.entity.Auction;
import org.com.drop.domain.auction.auction.entity.Auction.AuctionStatus;
import org.com.drop.domain.auction.bid.dto.response.BidHistoryResponse;
import org.com.drop.domain.auction.bid.entity.Bid;
import org.com.drop.domain.auction.bid.repository.BidRepository;
import org.com.drop.domain.auction.list.dto.SortType;
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
			.imageUrls(Arrays.asList("img1.jpg", "img2.jpg"))
			.build();

		highestBidDto = AuctionListRepositoryCustom.CurrentHighestBidDto.builder()
			.currentHighestBid(15000)
			.bidderNickname("입찰자")
			.bidTime(LocalDateTime.now())
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

		when(auctionListRepository.searchAuctions(request)).thenReturn(dtos);
		when(auctionListRepository.getNextCursor(dtos, 20, SortType.NEWEST)).thenReturn(null);
		when(auctionListRepository.isBookmarked(eq(1L), eq(1L))).thenReturn(true);

		// when
		AuctionCursorResponse response = auctionListService.getAuctions(request, testUser);

		// then
		assertThat(response).isNotNull();
		assertThat(response.getItems()).hasSize(1);
		assertThat(response.getItems().get(0).getAuctionId()).isEqualTo(1L);
		assertThat(response.getItems().get(0).getIsBookmarked()).isTrue();
		assertThat(response.getCursor()).isNull();
		assertThat(response.isHasNext()).isFalse();

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

		when(auctionListRepository.searchAuctions(request)).thenReturn(dtos);
		when(auctionListRepository.getNextCursor(dtos, 20, SortType.NEWEST)).thenReturn(null);
		// 비로그인 사용자는 isBookmarked 호출 안함

		// when
		AuctionCursorResponse response = auctionListService.getAuctions(request, null);

		// then
		assertThat(response).isNotNull();
		assertThat(response.getItems()).hasSize(1);
		assertThat(response.getItems().get(0).getIsBookmarked()).isFalse();
		verify(auctionListRepository, never()).isBookmarked(anyLong(), anyLong());
	}

	@Test
	@DisplayName("경매 목록 조회 - hasNext true")
	void getAuctions_hasNext_true() {
		// given
		AuctionSearchRequest request = AuctionSearchRequest.builder()
			.size(1)
			.build();

		// 2개의 항목 반환 (size보다 1개 더 많음 -> 다음 페이지 존재)
		AuctionListRepositoryCustom.AuctionItemDto itemDto2 = AuctionListRepositoryCustom.AuctionItemDto.builder()
			.auctionId(2L)
			.productId(2L)
			.name("테스트 상품2")
			.imageUrl("test2.jpg")
			.status(AuctionStatus.LIVE)
			.category(Product.Category.STARGOODS)
			.subCategory(Product.SubCategory.ACC)
			.currentHighestBid(20000)
			.startPrice(10000)
			.endAt(LocalDateTime.now().plusHours(2))
			.bookmarkCount(3)
			.bidCount(2)
			.createdAt(LocalDateTime.now())
			.score(5)
			.build();

		List<AuctionListRepositoryCustom.AuctionItemDto> dtos = Arrays.asList(itemDto, itemDto2);

		when(auctionListRepository.searchAuctions(request)).thenReturn(dtos);
		when(auctionListRepository.getNextCursor(dtos, 1, SortType.NEWEST)).thenReturn("encodedCursor");
		when(auctionListRepository.isBookmarked(eq(1L), eq(1L))).thenReturn(false);

		// when
		AuctionCursorResponse response = auctionListService.getAuctions(request, testUser);

		// then
		assertThat(response).isNotNull();
		assertThat(response.isHasNext()).isTrue();
		assertThat(response.getCursor()).isEqualTo("encodedCursor");
		assertThat(response.getItems()).hasSize(1); // 요청한 size(1)만큼만 반환
	}

	@Test
	@DisplayName("경매 상세 조회 - 성공")
	void getAuctionDetail_success() {
		// given
		Long auctionId = 1L;

		when(auctionListRepository.findAuctionDetailById(auctionId))
			.thenReturn(Optional.of(detailDto));
		when(auctionListRepository.isBookmarked(eq(1L), eq(1L)))
			.thenReturn(false);

		Bid bid = Bid.builder()
			.id(1L)
			.auction(Auction.builder().id(auctionId).build())
			.bidAmount(10000L)
			.bidder(User.builder().id(3L).nickname("입찰자").build())
			.createdAt(LocalDateTime.now())
			.isAuto(false)
			.build();
		Page<Bid> bidPage = new PageImpl<>(List.of(bid));
		when(bidRepository.findAllByAuctionId(eq(auctionId), any(PageRequest.class)))
			.thenReturn(bidPage);

		// when
		AuctionDetailResponse response = auctionListService.getAuctionDetail(auctionId, testUser);

		// then
		assertThat(response).isNotNull();
		assertThat(response.getAuctionId()).isEqualTo(1L);
		assertThat(response.getName()).isEqualTo("테스트 상품");
		assertThat(response.getCurrentHighestBid()).isEqualTo(10000);
		assertThat(response.getTotalBidCount()).isEqualTo(3);
		assertThat(response.getImageUrls()).hasSize(2);
		assertThat(response.getIsBookmarked()).isFalse();
		assertThat(response.getBidHistory()).hasSize(1);
		assertThat(response.getBidHistory().get(0).bidAmount()).isEqualTo(10000);
	}

	@Test
	@DisplayName("경매 상세 조회 - 경매 없음")
	void getAuctionDetail_auctionNotFound() {
		// given
		Long auctionId = 999L;
		when(auctionListRepository.findAuctionDetailById(auctionId))
			.thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> auctionListService.getAuctionDetail(auctionId, testUser))
			.isInstanceOf(ServiceException.class)
			.hasMessageContaining("경매를 찾을 수 없습니다");

		verify(bidRepository, never()).findAllByAuctionId(anyLong(), any());
	}

	@Test
	@DisplayName("현재 최고 입찰가 조회 - 입찰 있음")
	void getCurrentHighestBid_withBid() {
		// given
		Long auctionId = 1L;
		when(auctionListRepository.findCurrentHighestBid(auctionId))
			.thenReturn(Optional.of(highestBidDto));

		// when
		AuctionBidUpdate response = auctionListService.getCurrentHighestBid(auctionId);

		// then
		assertThat(response).isNotNull();
		assertThat(response.getCurrentHighestBid()).isEqualTo(15000);
		// 닉네임 마스킹 검증: "입찰자" -> "입찰***"
		assertThat(response.getBidderNickname()).isEqualTo("입찰***");
	}

	@Test
	@DisplayName("현재 최고 입찰가 조회 - 입찰 없음 (시작가 반환)")
	void getCurrentHighestBid_noBid() {
		// given
		Long auctionId = 1L;
		when(auctionListRepository.findCurrentHighestBid(auctionId))
			.thenReturn(Optional.empty());
		when(auctionListRepository.findAuctionStartPrice(auctionId))
			.thenReturn(Optional.of(5000));

		// when
		AuctionBidUpdate response = auctionListService.getCurrentHighestBid(auctionId);

		// then
		assertThat(response).isNotNull();
		assertThat(response.getCurrentHighestBid()).isEqualTo(5000);
		assertThat(response.getBidderNickname()).isNull();
	}

	@Test
	@DisplayName("현재 최고 입찰가 조회 - 경매 없음")
	void getCurrentHighestBid_auctionNotFound() {
		// given
		Long auctionId = 999L;
		when(auctionListRepository.findCurrentHighestBid(auctionId))
			.thenReturn(Optional.empty());
		when(auctionListRepository.findAuctionStartPrice(auctionId))
			.thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> auctionListService.getCurrentHighestBid(auctionId))
			.isInstanceOf(ServiceException.class)
			.hasMessageContaining("경매를 찾을 수 없습니다");
	}

	@Test
	@DisplayName("홈화면 조회 - 성공")
	void getHomeAuctions_success() {
		// given
		List<AuctionListRepositoryCustom.AuctionItemDto> endingSoon = List.of(itemDto);
		List<AuctionListRepositoryCustom.AuctionItemDto> popular = List.of(itemDto);

		when(auctionListRepository.findEndingSoonAuctions(10)).thenReturn(endingSoon);
		when(auctionListRepository.findPopularAuctions(10)).thenReturn(popular);
		when(auctionListRepository.isBookmarked(eq(1L), eq(1L))).thenReturn(true);

		// when
		AuctionHomeResponse response = auctionListService.getHomeAuctions(testUser);

		// then
		assertThat(response).isNotNull();
		assertThat(response.getEndingSoon()).hasSize(1);
		assertThat(response.getPopular()).hasSize(1);
		assertThat(response.getEndingSoon().get(0).getIsBookmarked()).isTrue();
		assertThat(response.getPopular().get(0).getIsBookmarked()).isTrue();
	}

	@Test
	@DisplayName("입찰 내역 조회 - 성공")
	void getBidHistory_success() {
		// given
		Long auctionId = 1L;
		int size = 10;

		Bid bid1 = Bid.builder()
			.id(1L)
			.auction(Auction.builder().id(auctionId).build())
			.bidAmount(10000L)
			.bidder(User.builder().id(3L).nickname("user1").build())
			.createdAt(LocalDateTime.now())
			.isAuto(false)
			.build();
		Bid bid2 = Bid.builder()
			.id(2L)
			.auction(Auction.builder().id(auctionId).build())
			.bidAmount(9000L)
			.bidder(User.builder().id(4L).nickname("user2").build())
			.createdAt(LocalDateTime.now().minusMinutes(5))
			.isAuto(false)
			.build();

		Page<Bid> bids = new PageImpl<>(List.of(bid1, bid2));
		when(bidRepository.findAllByAuctionId(eq(auctionId), any(PageRequest.class)))
			.thenReturn(bids);

		// when
		List<BidHistoryResponse> response = auctionListService.getBidHistory(auctionId, size);

		// then
		assertThat(response).hasSize(2);

		// BidHistoryResponse.from()이 닉네임 마스킹을 적용하는지 확인
		// user1 -> "us***", user2 -> "us***"
		assertThat(response.get(0).bidder()).isEqualTo("us***");
		assertThat(response.get(1).bidder()).isEqualTo("us***");

		assertThat(response.get(0).bidAmount()).isEqualTo(10000);
		assertThat(response.get(1).bidAmount()).isEqualTo(9000);
	}

	@Test
	@DisplayName("입찰 내역 조회 - 빈 목록")
	void getBidHistory_empty() {
		// given
		Long auctionId = 1L;
		int size = 10;

		Page<Bid> bids = new PageImpl<>(List.of());
		when(bidRepository.findAllByAuctionId(eq(auctionId), any(PageRequest.class)))
			.thenReturn(bids);

		// when
		List<BidHistoryResponse> response = auctionListService.getBidHistory(auctionId, size);

		// then
		assertThat(response).isEmpty();
	}
}
