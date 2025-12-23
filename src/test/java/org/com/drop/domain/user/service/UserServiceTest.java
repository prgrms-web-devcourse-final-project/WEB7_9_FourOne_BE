package org.com.drop.domain.user.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.com.drop.domain.auction.auction.entity.Auction;
import org.com.drop.domain.auction.auction.repository.AuctionRepository;
import org.com.drop.domain.auction.bid.entity.Bid;
import org.com.drop.domain.auction.bid.repository.BidRepository;
import org.com.drop.domain.auction.product.entity.BookMark;
import org.com.drop.domain.auction.product.entity.Product;
import org.com.drop.domain.auction.product.entity.ProductImage;
import org.com.drop.domain.auction.product.repository.BookmarkRepository;
import org.com.drop.domain.auction.product.repository.ProductImageRepository;
import org.com.drop.domain.auction.product.repository.ProductRepository;
import org.com.drop.domain.user.dto.MyBidPageResponse;
import org.com.drop.domain.user.dto.MyBookmarkPageResponse;
import org.com.drop.domain.user.dto.MyProductPageResponse;
import org.com.drop.domain.user.dto.UpdateProfileRequest;
import org.com.drop.domain.user.dto.UpdateProfileResponse;
import org.com.drop.domain.user.entity.User;
import org.com.drop.domain.user.repository.UserRepository;
import org.com.drop.domain.winner.repository.WinnerRepository;
import org.com.drop.global.aws.AmazonS3Client;
import org.com.drop.global.exception.ErrorCode;
import org.com.drop.global.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 단위 테스트")
class UserServiceTest {

	@InjectMocks
	private UserService userService;

	@Mock
	private UserRepository userRepository;

	@Mock
	private AuctionRepository auctionRepository;

	@Mock
	private BookmarkRepository bookmarkRepository;

	@Mock
	private ProductRepository productRepository;

	@Mock
	private ProductImageRepository productImageRepository;

	@Mock
	private BidRepository bidRepository;

	@Mock
	private WinnerRepository winnerRepository;

	@Mock
	private AmazonS3Client amazonS3Client;

	private User mockUser;

	@BeforeEach
	void setUp() {
		mockUser = User.builder()
			.id(1L)
			.email("user@drop.com")
			.nickname("originalNick")
			.userProfile("old_image_url")
			.role(User.UserRole.USER)
			.password("password")
			.build();
	}

	@Nested
	@DisplayName("사용자 정보 로드 (Security)")
	class LoadUserTests {
		@Test
		@DisplayName("성공: 이메일로 UserDetails 로드")
		void loadUserByUsername_success() {
			// Given
			when(userRepository.findByEmail(mockUser.getEmail())).thenReturn(Optional.of(mockUser));

			// When
			UserDetails userDetails = userService.loadUserByUsername(mockUser.getEmail());

			// Then
			assertNotNull(userDetails);
			assertEquals(mockUser.getEmail(), userDetails.getUsername());
			assertTrue(userDetails.getAuthorities().stream()
				.anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
		}

		@Test
		@DisplayName("실패: 존재하지 않는 이메일인 경우")
		void loadUserByUsername_fail_notFound() {
			// Given
			when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

			// When & Then
			assertThrows(UsernameNotFoundException.class,
				() -> userService.loadUserByUsername("none@drop.com"));
		}
	}

	@Nested
	@DisplayName("프로필 수정")
	class UpdateProfileTests {
		@Test
		@DisplayName("성공: 닉네임과 프로필 이미지 수정")
		void updateProfile_success() {
			// Given
			UpdateProfileRequest request = new UpdateProfileRequest("newNick", "new_image_url");
			when(userRepository.existsByNickname("newNick")).thenReturn(false);

			// When
			UpdateProfileResponse response = userService.updateProfile(mockUser, request);

			// Then
			assertEquals("newNick", response.nickname());
			assertEquals("new_image_url", response.profileImageUrl());
			verify(amazonS3Client, times(1)).updateS3Tag("old_image_url", "deleted");
		}

		@Test
		@DisplayName("실패: 중복된 닉네임으로 수정 시도")
		void updateProfile_fail_duplicateNickname() {
			// Given
			UpdateProfileRequest request = new UpdateProfileRequest("takenNick", "new_url");
			when(userRepository.existsByNickname("takenNick")).thenReturn(true);

			// When & Then
			ServiceException exception = assertThrows(ServiceException.class,
				() -> userService.updateProfile(mockUser, request));
			assertEquals(ErrorCode.USER_NICKNAME_CONFLICT, exception.getErrorCode());
		}
	}

	@Nested
	@DisplayName("마이페이지 목록 조회")
	class GetMyPageListTests {

		@Test
		@DisplayName("성공: 내 찜 목록 페이징 조회")
		void getMyBookmarks_success() {
			// Given
			int page = 1;
			Product product = Product.builder().id(10L).name("상품").build();
			BookMark bookmark = BookMark.builder().product(product).build();
			Page<BookMark> bookmarkPage = new PageImpl<>(List.of(bookmark));

			when(bookmarkRepository.findByUser(eq(mockUser), any(Pageable.class))).thenReturn(bookmarkPage);

			ProductImage img = ProductImage.builder().product(product).imageUrl("img_url").build();
			when(productImageRepository.findAllByProductIdIn(anyList())).thenReturn(List.of(img));

			// When
			MyBookmarkPageResponse response = userService.getMyBookmarks(mockUser, page);

			// Then
			assertNotNull(response);
			assertEquals(1, response.bookmarks().size());
			assertEquals("img_url", response.bookmarks().get(0).productImageUrl());
		}

		@Test
		@DisplayName("실패: 페이지 번호가 1 미만일 때")
		void validatePageNumber_fail() {
			// When & Then
			ServiceException exception = assertThrows(ServiceException.class,
				() -> userService.getMyBookmarks(mockUser, 0));
			assertEquals(ErrorCode.USER_PAGE_OUT_OF_RANGE, exception.getErrorCode());
		}

		@Test
		@DisplayName("성공: 내 등록 상품 목록 조회 (경매 상태 포함)")
		void getMyProducts_success() {
			// Given
			Product product = Product.builder().id(10L).name("내 상품").bookmarkCount(5).build();
			Page<Product> productPage = new PageImpl<>(List.of(product));

			when(productRepository.findBySellerAndDeletedAtIsNullOrderByCreatedAtDesc(eq(mockUser), any(Pageable.class)))
				.thenReturn(productPage);

			Auction auction = Auction.builder()
				.id(100L).product(product).status(Auction.AuctionStatus.LIVE)
				.startPrice(1000).endAt(LocalDateTime.now().plusDays(1))
				.build();

			when(auctionRepository.findByProductInAndDeletedAtIsNullOrderByIdDesc(anyList()))
				.thenReturn(List.of(auction));
			when(bidRepository.countByAuctionIn(anyList())).thenReturn(Collections.emptyList());

			// When
			MyProductPageResponse response = userService.getMyProducts(mockUser, 1);

			// Then
			assertEquals(1, response.products().size());
			assertEquals("LIVE", response.products().get(0).status());
			assertTrue(response.products().get(0).remainingTimeSeconds() > 0);
		}
	}

	@Nested
	@DisplayName("내 입찰 내역 조회")
	class GetMyBidTests {
		@Test
		@DisplayName("성공: 입찰 목록 및 상태(낙찰/유찰/진행중) 확인")
		void getMyBids_success() {
			// Given
			Auction auction = Auction.builder()
				.id(100L).status(Auction.AuctionStatus.LIVE).endAt(LocalDateTime.now().plusHours(1))
				.product(Product.builder().id(10L).name("상품").build())
				.build();
			Bid bid = Bid.builder().auction(auction).bidAmount(5000L).build();
			Page<Bid> bidPage = new PageImpl<>(List.of(bid));

			when(bidRepository.findMyLatestBidsPerAuction(eq(mockUser), any(Pageable.class))).thenReturn(bidPage);
			when(winnerRepository.findByAuctionIn(anyList())).thenReturn(Collections.emptyList());

			// When
			MyBidPageResponse response = userService.getMyBids(mockUser, 1, "all");

			// Then
			assertEquals(1, response.auctions().size());
			assertEquals("ONGOING", response.auctions().get(0).status());
		}
	}
}
