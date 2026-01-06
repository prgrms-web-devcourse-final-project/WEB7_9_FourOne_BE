package org.com.drop.domain.auction.list.repository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.com.drop.domain.auction.auction.entity.Auction.AuctionStatus;
import org.com.drop.domain.auction.auction.entity.QAuction;
import org.com.drop.domain.auction.bid.entity.QBid;
import org.com.drop.domain.auction.list.dto.SortType;
import org.com.drop.domain.auction.list.dto.request.AuctionSearchRequest;
import org.com.drop.domain.auction.product.entity.Product.Category;
import org.com.drop.domain.auction.product.entity.Product.SubCategory;
import org.com.drop.domain.auction.product.entity.QBookMark;
import org.com.drop.domain.auction.product.entity.QProduct;
import org.com.drop.domain.auction.product.entity.QProductImage;
import org.com.drop.global.util.CursorPaginationUtil;
import org.com.drop.global.util.CursorPaginationUtil.Cursor;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.NumberTemplate;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

/**
 * 경매 목록 QueryDSL 구현체
 */
@RequiredArgsConstructor
public class AuctionListRepositoryImpl implements AuctionListRepositoryCustom {

	private static final int DEFAULT_LIMIT = 10;

	private final JPAQueryFactory queryFactory;

	private final QAuction auction = QAuction.auction;
	private final QProduct product = QProduct.product;
	private final QProductImage productImage = QProductImage.productImage;
	private final QBid bid = QBid.bid;
	private final QBookMark bookMark = QBookMark.bookMark;

	@Override
	public List<AuctionItemDto> searchAuctions(AuctionSearchRequest request) {
		Cursor cursor = CursorPaginationUtil.decodeCursor(request.cursor());

		// 1. 첫 번째 이미지 URL
		Expression<String> firstImageUrl = new CaseBuilder()
			.when(JPAExpressions
				.select(productImage.id.count())
				.from(productImage)
				.where(productImage.product.eq(product))
				.gt(0L))
			.then(Expressions.stringTemplate(
				"({0})",
				JPAExpressions
					.select(productImage.imageUrl)
					.from(productImage)
					.where(productImage.product.eq(product))
					.orderBy(productImage.id.asc())
					.limit(1)
			))
			.otherwise("");

		// 2. 최고 입찰가
		NumberExpression<Integer> currentHighestBid = Expressions.numberTemplate(
			Integer.class,
			"COALESCE(({0}), {1})",
			JPAExpressions
				.select(bid.bidAmount.max().intValue())
				.from(bid)
				.where(bid.auction.eq(auction)),
			auction.startPrice
		);

		// 3. 입찰 수
		NumberExpression<Integer> bidCount = Expressions.numberTemplate(
			Integer.class,
			"COALESCE(({0}), 0)",
			JPAExpressions
				.select(bid.count().intValue())
				.from(bid)
				.where(bid.auction.eq(auction))
		);

		// 4. 인기 점수: 북마크 수 + 입찰 수
		NumberExpression<Integer> popularityScore = product.bookmarkCount.add(bidCount);

		JPAQuery<AuctionItemDto> query = queryFactory
			.select(Projections.constructor(
				AuctionItemDto.class,
				auction.id,
				product.id,
				product.name,
				firstImageUrl,
				auction.status,
				product.category,
				product.subcategory,
				auction.startPrice,
				currentHighestBid,
				auction.endAt,
				product.bookmarkCount,
				bidCount,
				product.createdAt,
				popularityScore
			))
			.from(auction)
			.join(auction.product, product)
			.where(
				product.deletedAt.isNull(),
				auction.deletedAt.isNull(),
				statusEq(request.status()),
				categoryEq(request.category()),
				subCategoryEq(request.subCategory()),
				keywordContains(request.keyword()),
				cursorCondition(cursor, request.sortType(), popularityScore)
			)
			.orderBy(getOrderSpecifier(request.sortType(), popularityScore))
			.limit(request.size() + 1L);

		return query.fetch();
	}

	@Override
	public String getNextCursor(List<AuctionItemDto> results, int size, SortType sortType) {
		if (results.size() <= size) {
			return null;
		}
		AuctionItemDto lastItem = results.get(size - 1);

		switch (sortType) {
			case POPULAR:
				return CursorPaginationUtil.encodePopularCursor(
					lastItem.getScore(),
					lastItem.getAuctionId()
				);
			case CLOSING:
				return CursorPaginationUtil.encodeCursor(
					lastItem.getEndAt(),
					lastItem.getAuctionId()
				);
			case NEWEST:
			default:
				return CursorPaginationUtil.encodeCursor(
					lastItem.getCreatedAt(),
					lastItem.getAuctionId()
				);
		}
	}

	@Override
	public Optional<AuctionDetailDto> findAuctionDetailById(Long auctionId) {
		List<String> imageUrls = queryFactory
			.select(productImage.imageUrl)
			.from(productImage)
			.join(productImage.product, product)
			.join(auction).on(auction.product.eq(product))
			.where(auction.id.eq(auctionId))
			.orderBy(productImage.id.asc())
			.fetch();

		NumberTemplate<Integer> currentHighestBid = Expressions.numberTemplate(
			Integer.class,
			"COALESCE(({0}), {1})",
			JPAExpressions
				.select(bid.bidAmount.max().intValue())
				.from(bid)
				.where(bid.auction.eq(auction)),
			auction.startPrice
		);

		NumberTemplate<Integer> totalBidCount = Expressions.numberTemplate(
			Integer.class,
			"COALESCE(({0}), 0)",
			JPAExpressions
				.select(bid.count().intValue())
				.from(bid)
				.where(bid.auction.eq(auction))
		);

		AuctionDetailDto result = queryFactory
			.select(Projections.constructor(
				AuctionDetailDto.class,
				auction.id,
				product.id,
				product.seller.id,
				product.seller.nickname,
				product.name,
				product.description,
				product.category,
				product.subcategory,
				auction.status,
				auction.startPrice,
				auction.buyNowPrice,
				auction.minBidStep,
				auction.startAt,
				auction.endAt,
				product.createdAt,
				currentHighestBid,
				totalBidCount,
				Expressions.constant(imageUrls)
			))
			.from(auction)
			.join(auction.product, product)
			.where(
				auction.id.eq(auctionId),
				product.deletedAt.isNull(),
				auction.deletedAt.isNull()
			)
			.fetchOne();

		return Optional.ofNullable(result);
	}

	@Override
	public Optional<CurrentHighestBidDto> findCurrentHighestBid(Long auctionId) {
		CurrentHighestBidDto result = queryFactory
			.select(Projections.constructor(
				CurrentHighestBidDto.class,
				bid.bidAmount.intValue(),
				bid.bidder.nickname,
				bid.createdAt
			))
			.from(bid)
			.where(bid.auction.id.eq(auctionId))
			.orderBy(bid.bidAmount.desc(), bid.createdAt.desc())
			.limit(1)
			.fetchOne();

		return Optional.ofNullable(result);
	}

	@Override
	public Optional<Integer> findAuctionStartPrice(Long auctionId) {
		Integer result = queryFactory
			.select(auction.startPrice)
			.from(auction)
			.where(
				auction.id.eq(auctionId),
				auction.deletedAt.isNull()
			)
			.fetchOne();

		return Optional.ofNullable(result);
	}

	@Override
	public List<AuctionItemDto> findEndingSoonAuctions(int limit) {
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime sixHoursLater = now.plusHours(6);
		int actualLimit = limit > 0 ? limit : DEFAULT_LIMIT;

		Expression<String> firstImageUrl = createFirstImageUrlExpression();
		NumberExpression<Integer> currentHighestBid = createCurrentHighestBidExpression();
		NumberExpression<Integer> bidCount = createBidCountExpression();
		NumberExpression<Integer> score = product.bookmarkCount.add(bidCount);

		return queryFactory
			.select(Projections.constructor(
				AuctionItemDto.class,
				auction.id,
				product.id,
				product.name,
				firstImageUrl,
				auction.status,
				product.category,
				product.subcategory,
				auction.startPrice,
				currentHighestBid,
				auction.endAt,
				product.bookmarkCount,
				bidCount,
				product.createdAt,
				score
			))
			.from(auction)
			.join(auction.product, product)
			.where(
				auction.status.eq(AuctionStatus.LIVE),
				auction.endAt.between(now, sixHoursLater),
				product.deletedAt.isNull(),
				auction.deletedAt.isNull()
			)
			.orderBy(auction.endAt.asc())
			.limit(actualLimit)
			.fetch();
	}

	@Override
	public List<AuctionItemDto> findPopularAuctions(int limit) {
		int actualLimit = limit > 0 ? limit : DEFAULT_LIMIT;

		Expression<String> firstImageUrl = createFirstImageUrlExpression();
		NumberExpression<Integer> currentHighestBid = createCurrentHighestBidExpression();
		NumberExpression<Integer> bidCount = createBidCountExpression();
		NumberExpression<Integer> popularityScore = product.bookmarkCount.add(bidCount);

		return queryFactory
			.select(Projections.constructor(
				AuctionItemDto.class,
				auction.id,
				product.id,
				product.name,
				firstImageUrl,
				auction.status,
				product.category,
				product.subcategory,
				auction.startPrice,
				currentHighestBid,
				auction.endAt,
				product.bookmarkCount,
				bidCount,
				product.createdAt,
				popularityScore
			))
			.from(auction)
			.join(auction.product, product)
			.where(
				auction.status.eq(AuctionStatus.LIVE),
				product.deletedAt.isNull(),
				auction.deletedAt.isNull()
			)
			.orderBy(popularityScore.desc(), auction.id.desc())
			.limit(actualLimit)
			.fetch();
	}

	@Override
	public List<BidHistoryDto> findBidHistory(Long auctionId, int limit) {
		int actualLimit = limit > 0 ? limit : DEFAULT_LIMIT;

		return queryFactory
			.select(Projections.constructor(
				BidHistoryDto.class,
				bid.id,
				bid.bidder.nickname,
				bid.bidAmount.intValue(),
				bid.createdAt
			))
			.from(bid)
			.where(bid.auction.id.eq(auctionId))
			.orderBy(bid.createdAt.desc())
			.limit(actualLimit)
			.fetch();
	}

	/**
	 * 특정 사용자가 찜한 모든 상품 ID 조회 (Batch Fetch)
	 */
	@Override
	public List<Long> findBookmarkedProductIdsByUserId(Long userId) {
		if (userId == null) {
			return Collections.emptyList();
		}

		return queryFactory
			.select(bookMark.product.id)
			.from(bookMark)
			.where(bookMark.user.id.eq(userId))
			.fetch();
	}

	// ==================== Common Expression Methods ====================

	private Expression<String> createFirstImageUrlExpression() {
		return new CaseBuilder()
			.when(JPAExpressions
				.select(productImage.id.count())
				.from(productImage)
				.where(productImage.product.eq(product))
				.gt(0L))
			.then(Expressions.stringTemplate(
				"({0})",
				JPAExpressions
					.select(productImage.imageUrl)
					.from(productImage)
					.where(productImage.product.eq(product))
					.orderBy(productImage.id.asc())
					.limit(1)
			))
			.otherwise("");
	}

	private NumberExpression<Integer> createCurrentHighestBidExpression() {
		return Expressions.numberTemplate(
			Integer.class,
			"COALESCE(({0}), {1})",
			JPAExpressions
				.select(bid.bidAmount.max().intValue())
				.from(bid)
				.where(bid.auction.eq(auction)),
			auction.startPrice
		);
	}

	private NumberExpression<Integer> createBidCountExpression() {
		return Expressions.numberTemplate(
			Integer.class,
			"COALESCE(({0}), 0)",
			JPAExpressions
				.select(bid.count().intValue())
				.from(bid)
				.where(bid.auction.eq(auction))
		);
	}

	// ==================== Helper Methods ====================

	private BooleanExpression statusEq(AuctionStatus status) {
		return status != null ? auction.status.eq(status) : null;
	}

	private BooleanExpression categoryEq(Category category) {
		return category != null ? product.category.eq(category) : null;
	}

	private BooleanExpression subCategoryEq(SubCategory subCategory) {
		return subCategory != null ? product.subcategory.eq(subCategory) : null;
	}

	private BooleanExpression keywordContains(String keyword) {
		if (keyword == null || keyword.isBlank()) {
			return null;
		}

		String[] keywords = keyword.trim().split("\\s+");
		BooleanBuilder builder = new BooleanBuilder();

		for (String kw : keywords) {
			if (!kw.isEmpty()) {
				builder.and(
					product.name.containsIgnoreCase(kw)
						.or(product.description.containsIgnoreCase(kw))
				);
			}
		}

		return builder.hasValue() ? Expressions.asBoolean(builder.getValue()) : null;
	}

	private BooleanExpression cursorCondition(
		Cursor cursor,
		SortType sortType,
		NumberExpression<Integer> popularityScore
	) {
		if (cursor == null) {
			return null;
		}

		if (sortType == SortType.POPULAR && cursor.isPopularCursor()) {
			return popularityScore.lt(cursor.score())
				.or(popularityScore.eq(cursor.score()).and(auction.id.lt(cursor.id())));
		} else if (sortType == SortType.CLOSING && cursor.isTimestampCursor()) {
			return auction.endAt.gt(cursor.timestamp())
				.or(auction.endAt.eq(cursor.timestamp()).and(auction.id.gt(cursor.id())));
		} else if (sortType == SortType.NEWEST && cursor.isTimestampCursor()) {
			return product.createdAt.lt(cursor.timestamp())
				.or(product.createdAt.eq(cursor.timestamp()).and(auction.id.lt(cursor.id())));
		}
		return null;
	}

	private OrderSpecifier<?>[] getOrderSpecifier(
		SortType sortType,
		NumberExpression<Integer> popularityScore
	) {
		switch (sortType) {
			case NEWEST:
				return new OrderSpecifier[] {
					product.createdAt.desc(),
					auction.id.desc()
				};
			case CLOSING:
				return new OrderSpecifier[] {
					auction.endAt.asc(),
					auction.id.asc()
				};
			case POPULAR:
				return new OrderSpecifier[] {
					popularityScore.desc(),
					auction.id.desc()
				};
			default:
				return new OrderSpecifier[] {
					product.createdAt.desc(),
					auction.id.desc()
				};
		}
	}
}
