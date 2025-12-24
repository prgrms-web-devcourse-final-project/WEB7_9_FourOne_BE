package org.com.drop.domain.auction.list;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import org.com.drop.domain.auction.auction.entity.Auction;
import org.com.drop.domain.auction.auction.entity.Auction.AuctionStatus;
import org.com.drop.domain.auction.auction.repository.AuctionRepository;
import org.com.drop.domain.auction.bid.entity.Bid;
import org.com.drop.domain.auction.bid.repository.BidRepository;
import org.com.drop.domain.auction.product.entity.BookMark;
import org.com.drop.domain.auction.product.entity.Product;
import org.com.drop.domain.auction.product.entity.Product.Category;
import org.com.drop.domain.auction.product.entity.Product.SubCategory;
import org.com.drop.domain.auction.product.entity.ProductImage;
import org.com.drop.domain.auction.product.repository.BookmarkRepository;
import org.com.drop.domain.auction.product.repository.ProductImageRepository;
import org.com.drop.domain.auction.product.repository.ProductRepository;
import org.com.drop.domain.user.entity.User;
import org.com.drop.domain.user.repository.UserRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.Transactional;

import lombok.Builder;
import lombok.RequiredArgsConstructor;

@TestConfiguration
@RequiredArgsConstructor
public class TestDataSetup {

	private final UserRepository userRepository;
	private final ProductRepository productRepository;
	private final AuctionRepository auctionRepository;
	private final BidRepository bidRepository;
	private final ProductImageRepository productImageRepository;
	private final BookmarkRepository bookmarkRepository;

	@Bean
	@Transactional
	public AuctionTestData setupAuctionTestData() {

		// ✅ 테스트 전용 유저 직접 생성
		User seller = createUser("seller@test.com", "판매자");
		User bidder = createUser("bidder@test.com", "입찰자");
		User viewer = createUser("viewer@test.com", "조회자");

		LocalDateTime now = LocalDateTime.now();

		// 상품 생성
		Product starGoodsProduct = createProduct(seller, "스타굿즈 상품", Category.STARGOODS);
		Product figureProduct = createProduct(seller, "피규어 상품", Category.FIGURE);
		Product gameProduct = createProduct(seller, "게임 상품", Category.GAME);
		Product cdlpProduct = createProduct(seller, "CDLP 상품", Category.CDLP);

		// 경매 생성
		Auction liveEndingSoonAuction = createAuction(
			starGoodsProduct, 10000, now.minusHours(1), now.plusHours(3), AuctionStatus.LIVE);

		Auction liveLongAuction = createAuction(
			figureProduct, 15000, now.minusHours(1), now.plusDays(3), AuctionStatus.LIVE);

		Auction scheduledAuction = createAuction(
			gameProduct, 20000, now.plusHours(1), now.plusDays(5), AuctionStatus.SCHEDULED);

		Auction endedAuction = createAuction(
			cdlpProduct, 8000, now.minusDays(2), now.minusDays(1), AuctionStatus.ENDED);

		// 이미지
		createProductImage(starGoodsProduct, "star1.jpg");
		createProductImage(starGoodsProduct, "star2.jpg");

		// 입찰
		createBid(bidder, liveEndingSoonAuction, 25000L);
		Bid highestBid = createBid(viewer, liveEndingSoonAuction, 30000L);

		// 북마크
		createBookmark(viewer, starGoodsProduct);

		return AuctionTestData.builder()
			.seller(seller)
			.bidder(bidder)
			.viewer(viewer)
			.liveEndingSoonAuction(liveEndingSoonAuction)
			.liveLongAuction(liveLongAuction)
			.scheduledAuction(scheduledAuction)
			.endedAuction(endedAuction)
			.starGoodsProduct(starGoodsProduct)
			.highestBid(highestBid)
			.build();
	}

	/* ===== 헬퍼 메서드 ===== */

	private User createUser(String email, String nickname) {
		return userRepository.save(User.builder()
			.email(email)
			.nickname(nickname)
			.password("pass")
			.loginType(User.LoginType.LOCAL)
			.role(User.UserRole.USER)
			.createdAt(LocalDateTime.now())
			.penaltyCount(0)
			.build());
	}

	private Product createProduct(User seller, String name, Category category) {
		LocalDateTime now = LocalDateTime.now();

		return productRepository.save(Product.builder()
			.seller(seller)
			.name(name)
			.description(name + " 설명")
			.category(category)
			.subcategory(SubCategory.ACC)
			.bookmarkCount(0)
			.createdAt(now)
			.updatedAt(now)
			.build());
	}

	private Auction createAuction(
		Product product, int startPrice,
		LocalDateTime startAt, LocalDateTime endAt,
		AuctionStatus status
	) {
		return auctionRepository.save(Auction.builder()
			.product(product)
			.startPrice(startPrice)
			.minBidStep(1000)
			.buyNowPrice(100_000)
			.startAt(startAt)
			.endAt(endAt)
			.status(status)
			.bidCount(0)
			.build());
	}

	private void createProductImage(Product product, String url) {
		productImageRepository.save(ProductImage.builder()
			.product(product)
			.imageUrl(url)
			.build());
	}

	private Bid createBid(User bidder, Auction auction, Long amount) {
		Bid bid = bidRepository.save(Bid.builder()
			.auction(auction)
			.bidder(bidder)
			.bidAmount(amount)
			.createdAt(LocalDateTime.now())
			.isAuto(false)
			.build());
		auction.increaseBidCount();
		return bid;
	}

	private void createBookmark(User user, Product product) {
		bookmarkRepository.save(BookMark.builder()
			.user(user)
			.product(product)
			.createdAt(LocalDateTime.now())
			.build());

		try {
			Field f = Product.class.getDeclaredField("bookmarkCount");
			f.setAccessible(true);
			f.set(product, ((int) f.get(product)) + 1);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/* ===== DTO ===== */

	@Builder
	public static class AuctionTestData {
		private User seller;
		private User bidder;
		private User viewer;
		private Auction liveEndingSoonAuction;
		private Auction liveLongAuction;
		private Auction scheduledAuction;
		private Auction endedAuction;
		private Product starGoodsProduct;
		private Bid highestBid;

		public User seller() { return seller; }
		public User bidder() { return bidder; }
		public User viewer() { return viewer; }
		public Auction liveEndingSoonAuction() { return liveEndingSoonAuction; }
		public Auction liveLongAuction() { return liveLongAuction; }
		public Auction scheduledAuction() { return scheduledAuction; }
		public Auction endedAuction() { return endedAuction; }
		public Product starGoodsProduct() { return starGoodsProduct; }
		public Bid highestBid() { return highestBid; }
	}
}
