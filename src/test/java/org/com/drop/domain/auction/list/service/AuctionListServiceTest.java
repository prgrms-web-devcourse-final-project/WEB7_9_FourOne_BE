package org.com.drop.domain.auction.list.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
import org.com.drop.global.aws.AmazonS3Client;
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

	@Mock
	private AmazonS3Client amazonS3Client;

	@Mock
	private BookmarkCacheService bookmarkCacheService;

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

		itemDto = new AuctionListRepositoryCustom.AuctionItemDto(
			1L,
			1L,
			"테스트 상품",
			"test.jpg",
			AuctionStatus.LIVE,
			Product.Category.STARGOODS,
			Product.SubCategory.ACC,
			5000,
			10000,
			LocalDateTime.now().plusHours(1),
			5,
			3,
			LocalDateTime.now(),
			8
		);

		detailDto = new AuctionListRepositoryCustom.AuctionDetailDto(
			1L,
			1L,
			2L,
			"판매자",
			"테스트 상품",
			"상품 설명",
			Product.Category.STARGOODS,
			Product.SubCategory.ACC,
			AuctionStatus.LIVE,
			5000,
			20000,
			1000,
			LocalDateTime.now().minusHours(1),
			LocalDateTime.now().plusHours(1),
			LocalDateTime.now(),
			10000,
			3,
			Arrays.asList("img1.jpg", "img2.jpg")
		);

		highestBidDto = new AuctionListRepositoryCustom.CurrentHighestBidDto(
			15000,
			"입찰자",
			LocalDateTime.now()
		);
	}

	@Test
	@DisplayName("경매 목록 조회 - 성공 (로그인 사용자)")
	void getAuctions_success_withUser() {
		// given: Record 생성자로 객체 생성 (Builder 사용 X)
		AuctionSearchRequest request = new AuctionSearchRequest(
			null, null, null, null, SortType.NEWEST, null, 20
		);
		List<AuctionListRepositoryCustom.AuctionItemDto> dtos = Arrays.asList(itemDto);

		when(auctionListRepository.searchAuctions(request)).thenReturn(dtos);
		when(auctionListRepository.getNextCursor(dtos, 20, SortType.NEWEST)).thenReturn(null);

		// Redis 캐시 Mocking (찜한 상품 ID 목록 반환)
		when(bookmarkCacheService.getBookmarkedProductIds(1L)).thenReturn(Set.of(1L));

		// when
		AuctionCursorResponse response = auctionListService.getAuctions(request, testUser);

		// then
		assertThat(response).isNotNull();
		assertThat(response.items()).hasSize(1);
		assertThat(response.items().get(0).auctionId()).isEqualTo(1L);
		assertThat(response.items().get(0).isBookmarked()).isTrue(); // 찜 여부 확인
		assertThat(response.cursor()).isNull();
		assertThat(response.hasNext()).isFalse();

		// 캐시 서비스 호출 검증
		verify(bookmarkCacheService).getBookmarkedProductIds(1L);
	}

	@Test
	@DisplayName("경매 목록 조회 - 성공 (비로그인 사용자)")
	void getAuctions_success_anonymous() {
		// given
		AuctionSearchRequest request = new AuctionSearchRequest(
			null, null, null, null, SortType.NEWEST, null, 20
		);
		List<AuctionListRepositoryCustom.AuctionItemDto> dtos = Arrays.asList(itemDto);

		when(auctionListRepository.searchAuctions(request)).thenReturn(dtos);
		when(auctionListRepository.getNextCursor(dtos, 20, SortType.NEWEST)).thenReturn(null);
		// 비로그인 시 캐시 조회 X

		// when
		AuctionCursorResponse response = auctionListService.getAuctions(request, null);

		// then
		assertThat(response).isNotNull();
		assertThat(response.items()).hasSize(1);
		assertThat(response.items().get(0).isBookmarked()).isFalse(); // 비로그인은 항상 false
		verify(bookmarkCacheService, never()).getBookmarkedProductIds(any());
	}

	@Test
	@DisplayName("경매 목록 조회 - hasNext true")
	void getAuctions_hasNext_true() {
		// given (size=1)
		AuctionSearchRequest request = new AuctionSearchRequest(
			null, null, null, null, SortType.NEWEST, null, 1
		);

		AuctionListRepositoryCustom.AuctionItemDto itemDto2 = new AuctionListRepositoryCustom.AuctionItemDto(
			2L,
			2L,
			"테스트 상품2",
			"test2.jpg",
			AuctionStatus.LIVE,
			Product.Category.STARGOODS,
			Product.SubCategory.ACC,
			10000,
			20000,
			LocalDateTime.now().plusHours(2),
			3,
			2,
			LocalDateTime.now(),
			5
		);

		// size(1)보다 1개 더 많은 2개 반환 -> 다음 페이지 존재
		List<AuctionListRepositoryCustom.AuctionItemDto> dtos = Arrays.asList(itemDto, itemDto2);

		when(auctionListRepository.searchAuctions(request)).thenReturn(dtos);
		when(auctionListRepository.getNextCursor(dtos, 1, SortType.NEWEST)).thenReturn("encodedCursor");

		// 캐시 조회 (찜 내역 없음)
		when(bookmarkCacheService.getBookmarkedProductIds(1L)).thenReturn(Collections.emptySet());

		// when
		AuctionCursorResponse response = auctionListService.getAuctions(request, testUser);

		// then
		assertThat(response).isNotNull();
		assertThat(response.hasNext()).isTrue();
		assertThat(response.cursor()).isEqualTo("encodedCursor");
		assertThat(response.items()).hasSize(1); // 요청한 size(1)만큼만 반환
	}

	@Test
	@DisplayName("경매 상세 조회 - 성공")
	void getAuctionDetail_success() {
		// given
		Long auctionId = 1L;

		when(auctionListRepository.findAuctionDetailById(auctionId))
			.thenReturn(Optional.of(detailDto));

		// 캐시 조회 (찜 내역 없음)
		when(bookmarkCacheService.getBookmarkedProductIds(1L)).thenReturn(Collections.emptySet());

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
		assertThat(response.auctionId()).isEqualTo(1L);
		assertThat(response.name()).isEqualTo("테스트 상품");
		assertThat(response.currentHighestBid()).isEqualTo(10000);
		assertThat(response.totalBidCount()).isEqualTo(3);
		assertThat(response.imageUrls()).hasSize(2);
		assertThat(response.isBookmarked()).isFalse();
		assertThat(response.bidHistory()).hasSize(1);
		assertThat(response.bidHistory().get(0).bidAmount()).isEqualTo(10000);
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
		assertThat(response.currentHighestBid()).isEqualTo(15000);
		assertThat(response.bidderNickname()).isEqualTo("입찰***");
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
		assertThat(response.currentHighestBid()).isEqualTo(5000);
		assertThat(response.bidderNickname()).isNull();
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

		// 캐시 조회 (찜 내역 있음)
		when(bookmarkCacheService.getBookmarkedProductIds(1L)).thenReturn(Set.of(1L));

		// when
		AuctionHomeResponse response = auctionListService.getHomeAuctions(testUser);

		// then
		assertThat(response).isNotNull();
		assertThat(response.endingSoon()).hasSize(1);
		assertThat(response.popular()).hasSize(1);
		assertThat(response.endingSoon().get(0).isBookmarked()).isTrue();
		assertThat(response.popular().get(0).isBookmarked()).isTrue();
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

		// [수정] findAllByAuctionId로 검증
		when(bidRepository.findAllByAuctionId(eq(auctionId), any(PageRequest.class)))
			.thenReturn(bids);

		// when
		List<BidHistoryResponse> response = auctionListService.getBidHistory(auctionId, size);

		// then
		assertThat(response).hasSize(2);
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
