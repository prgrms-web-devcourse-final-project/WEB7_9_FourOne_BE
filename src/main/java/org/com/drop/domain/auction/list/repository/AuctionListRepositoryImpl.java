package org.com.drop.domain.auction.list.repository;

import java.time.LocalDateTime;
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
		Cursor cursor = CursorPaginationUtil.decodeCursor(request.getCursor());

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
		NumberExpression<Integer> score = product.bookmarkCount.add(bidCount);

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
				score
			))
			.from(auction)
			.join(auction.product, product)
			.where(
				product.deletedAt.isNull(),
				auction.deletedAt.isNull(),
				statusEq(request.getStatus()),
				categoryEq(request.getCategory()),
				subCategoryEq(request.getSubCategory()),
				keywordContains(request.getKeyword()),
				cursorCondition(cursor, request.getSortType())
			)
			.orderBy(getOrderSpecifier(request.getSortType()))
			.limit(request.getSize() + 1L); // hasNext 확인을 위해 +1

		return query.fetch();
	}

	@Override
	public String getNextCursor(List<AuctionItemDto> results, int size) {
		if (results.size() <= size) {
			return null;
		}
		AuctionItemDto lastItem = results.get(size - 1);
		return CursorPaginationUtil.encodeCursor(lastItem.getCreatedAt(), lastItem.getAuctionId());
	}

	@Override
	public Optional<AuctionDetailDto> findAuctionDetailById(Long auctionId) {
		// 이미지 URL 리스트 조회
		List<String> imageUrls = queryFactory
			.select(productImage.imageUrl)
			.from(productImage)
			.join(productImage.product, product)
			.join(auction).on(auction.product.eq(product))
			.where(auction.id.eq(auctionId))
			.orderBy(productImage.id.asc())
			.fetch();

		// 최고 입찰가
		NumberTemplate<Integer> currentHighestBid = Expressions.numberTemplate(
			Integer.class,
			"COALESCE(({0}), {1})",
			JPAExpressions
				.select(bid.bidAmount.max().intValue())
				.from(bid)
				.where(bid.auction.eq(auction)),
			auction.startPrice
		);

		// 총 입찰 수
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

		// 첫 번째 이미지 URL
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

		// 최고 입찰가
		NumberExpression<Integer> currentHighestBid = Expressions.numberTemplate(
			Integer.class,
			"COALESCE(({0}), {1})",
			JPAExpressions
				.select(bid.bidAmount.max().intValue())
				.from(bid)
				.where(bid.auction.eq(auction)),
			auction.startPrice
		);

		// 입찰 수
		NumberExpression<Integer> bidCount = Expressions.numberTemplate(
			Integer.class,
			"COALESCE(({0}), 0)",
			JPAExpressions
				.select(bid.count().intValue())
				.from(bid)
				.where(bid.auction.eq(auction))
		);

		// 인기 점수
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

		// 첫 번째 이미지 URL
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

		// 입찰 수
		NumberExpression<Integer> bidCount = Expressions.numberTemplate(
			Integer.class,
			"COALESCE(({0}), 0)",
			JPAExpressions
				.select(bid.count().intValue())
				.from(bid)
				.where(bid.auction.eq(auction))
		);

		// 최고 입찰가
		NumberExpression<Integer> currentHighestBid = Expressions.numberTemplate(
			Integer.class,
			"COALESCE(({0}), {1})",
			JPAExpressions
				.select(bid.bidAmount.max().intValue())
				.from(bid)
				.where(bid.auction.eq(auction)),
			auction.startPrice
		);

		// 인기 점수
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

	@Override
	public boolean isBookmarked(Long productId, Long userId) {
		if (userId == null) {
			return false;
		}

		Integer count = queryFactory
			.selectOne()
			.from(bookMark)
			.where(
				bookMark.product.id.eq(productId),
				bookMark.user.id.eq(userId)
			)
			.fetchFirst();

		return count != null;
	}

	// ==================== Helper Methods ====================

	/**
	 * 상태 조건 (null이면 조건 제외)
	 */
	private BooleanExpression statusEq(AuctionStatus status) {
		return status != null ? auction.status.eq(status) : null;
	}

	/**
	 * 카테고리 조건 (null이면 조건 제외)
	 */
	private BooleanExpression categoryEq(Category category) {
		return category != null ? product.category.eq(category) : null;
	}

	/**
	 * 서브카테고리 조건 (null이면 조건 제외)
	 */
	private BooleanExpression subCategoryEq(SubCategory subCategory) {
		return subCategory != null ? product.subcategory.eq(subCategory) : null;
	}

	/**
	 * 키워드 검색 조건 (null이면 조건 제외)
	 */
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

	/**
	 * 커서 페이징 조건
	 */
	private BooleanExpression cursorCondition(Cursor cursor, SortType sortType) {
		if (cursor == null) {
			return null;
		}

		LocalDateTime timestamp = cursor.timestamp();
		Long id = cursor.id();

		if (sortType == SortType.NEWEST) {
			return product.createdAt.lt(timestamp)
				.or(product.createdAt.eq(timestamp).and(auction.id.lt(id)));
		} else if (sortType == SortType.CLOSING) {
			return auction.endAt.gt(timestamp)
				.or(auction.endAt.eq(timestamp).and(auction.id.gt(id)));
		} else if (sortType == SortType.POPULAR) {
			// 현재는 createdAt 기준으로 처리
			return product.createdAt.lt(timestamp)
				.or(product.createdAt.eq(timestamp).and(auction.id.lt(id)));
		} else {
			return product.createdAt.lt(timestamp)
				.or(product.createdAt.eq(timestamp).and(auction.id.lt(id)));
		}
	}

	/**
	 * 정렬 조건
	 */
	private OrderSpecifier<?>[] getOrderSpecifier(SortType sortType) {
		if (sortType == SortType.NEWEST) {
			return new OrderSpecifier[] {
				product.createdAt.desc(),
				auction.id.desc()
			};
		} else if (sortType == SortType.CLOSING) {
			return new OrderSpecifier[] {
				auction.endAt.asc(),
				auction.id.asc()
			};
		} else if (sortType == SortType.POPULAR) {
			// 인기 점수 계산
			NumberExpression<Integer> bidCount = Expressions.numberTemplate(
				Integer.class,
				"COALESCE(({0}), 0)",
				JPAExpressions
					.select(bid.count().intValue())
					.from(bid)
					.where(bid.auction.eq(auction))
			);
			NumberExpression<Integer> popularityScore = product.bookmarkCount.add(bidCount);
			return new OrderSpecifier[] {
				popularityScore.desc(),
				auction.id.desc()
			};
		} else {
			return new OrderSpecifier[] {
				product.createdAt.desc(),
				auction.id.desc()
			};
		}
	}
}
