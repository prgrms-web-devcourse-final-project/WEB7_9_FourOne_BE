package org.com.drop.domain.user.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.com.drop.domain.auction.auction.repository.AuctionRepository;
import org.com.drop.domain.auction.product.entity.BookMark;
import org.com.drop.domain.auction.product.entity.ProductImage;
import org.com.drop.domain.auction.product.repository.BookmarkRepository;
import org.com.drop.domain.auction.product.repository.ProductImageRepository;
import org.com.drop.domain.user.dto.MyAuctionListResponse;
import org.com.drop.domain.user.dto.MyBookmarkPageResponse;
import org.com.drop.domain.user.dto.MyPageResponse;
import org.com.drop.domain.user.entity.User;
import org.com.drop.domain.user.repository.UserRepository;
import org.com.drop.global.exception.ErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository userRepository;
	private final AuctionRepository auctionRepository;
	private final BookmarkRepository bookmarkRepository;
	private final ProductImageRepository productImageRepository;

	@Transactional(readOnly = true)
	public User findUserByEmail(String email) {
		return userRepository.findByEmail(email)
			.orElseThrow(() -> ErrorCode.USER_NOT_FOUND
				.serviceException("사용자를 찾을 수 없습니다: email=%s", email));
	}

	@Transactional(readOnly = true)
	public MyPageResponse getMe(User user) {
		return MyPageResponse.of(user);
	}

	// Todo: bid 엔티티 추가되면 이어 구현
	@Transactional(readOnly = true)
	public List<MyAuctionListResponse> getMyAuctions(User user, int page, String status) {
		return new ArrayList<>();
	}

	@Transactional(readOnly = true)
	public MyBookmarkPageResponse getMyBookmarks(User user, int page) {
		validatePageNumber(page);

		Pageable pageable = PageRequest.of(page - 1, 20);
		Page<BookMark> bookmarkPage = bookmarkRepository.findByUser(user, pageable);

		Map<Long, String> imageMap = getProductMainImageMap(bookmarkPage.getContent());

		List<MyBookmarkPageResponse.MyBookmarkResponse> dtoList = bookmarkPage.getContent().stream()
			.map(bm -> MyBookmarkPageResponse.MyBookmarkResponse.of(bm, imageMap.get(bm.getProduct().getId())))
			.toList();

		return new MyBookmarkPageResponse(page, bookmarkPage.getTotalElements(), dtoList);
	}

	private Map<Long, String> getProductMainImageMap(List<BookMark> bookmarks) {
		List<Long> productIds = bookmarks.stream().map(bm -> bm.getProduct().getId()).toList();

		return productImageRepository.findAllByProductIdIn(productIds).stream()
			.collect(Collectors.toMap(
				img -> img.getProduct().getId(),
				ProductImage::getImageUrl,
				(existing, replacement) -> existing
			));
	}

	private void validatePageNumber(int page) {
		if (page < 1) {
			throw ErrorCode.USER_PAGE_OUT_OF_RANGE.serviceException("페이지 번호 오류: page=%d", page);
		}
	}
}
