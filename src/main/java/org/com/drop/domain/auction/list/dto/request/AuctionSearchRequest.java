package org.com.drop.domain.auction.list.dto.request;

import java.util.Objects;

import org.com.drop.domain.auction.auction.entity.Auction.AuctionStatus;
import org.com.drop.domain.auction.list.dto.SortType;
import org.com.drop.domain.auction.product.entity.Product.Category;
import org.com.drop.domain.auction.product.entity.Product.SubCategory;
import org.com.drop.global.exception.ErrorCode;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

/**
 * 경매 리스트 조회 요청 DTO
 */
@Getter
@Builder
public class AuctionSearchRequest {

	private static final int DEFAULT_SIZE = 20;
	private static final int MAX_SIZE = 50;

	private final AuctionStatus status;
	private final Category category;
	private final SubCategory subCategory;

	@Size(min = 2, max = 20, message = "검색어는 2자 이상 20자 이하로 입력해주세요")
	@Pattern(regexp = "^[가-힣a-zA-Z0-9\\s]*$", message = "검색어는 한글, 영문, 숫자, 공백만 입력 가능합니다")
	private final String keyword;

	private final SortType sortType;
	private final String cursor;

	@Min(value = 1, message = "size는 최소 1 이상이어야 합니다.")
	@Max(value = MAX_SIZE, message = "size는 최대 50 이하여야 합니다.")
	private final Integer size;

	@Builder
	public AuctionSearchRequest(
		AuctionStatus status,
		Category category,
		SubCategory subCategory,
		String keyword,
		SortType sortType,
		String cursor,
		Integer size
	) {
		this.status = status;
		this.category = category;
		this.subCategory = subCategory;
		this.keyword = validateAndTrimKeyword(keyword);
		this.sortType = Objects.requireNonNullElse(sortType, SortType.NEWEST);
		this.cursor = cursor != null ? cursor.trim() : null;
		this.size = (size != null && size >= 1 && size <= MAX_SIZE) ? size : DEFAULT_SIZE;
	}

	private String validateAndTrimKeyword(String keyword) {
		if (keyword == null || keyword.isBlank()) {
			return null;
		}

		String trimmedKeyword = keyword.trim();
		if (trimmedKeyword.length() < 2 || trimmedKeyword.length() > 20) {
			throw ErrorCode.AUCTION_INVALID_SEARCH_KEYWORD.serviceException("검색어는 2~20자여야 합니다.");
		}

		return trimmedKeyword;
	}
}
