package org.com.drop.domain.user.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
import org.com.drop.domain.user.dto.MyPageResponse;
import org.com.drop.domain.user.dto.MyProductPageResponse;
import org.com.drop.domain.user.dto.UpdateProfileRequest;
import org.com.drop.domain.user.dto.UpdateProfileResponse;
import org.com.drop.domain.user.entity.User;
import org.com.drop.domain.user.repository.UserRepository;
import org.com.drop.domain.winner.domain.Winner;
import org.com.drop.domain.winner.repository.WinnerRepository;
import org.com.drop.global.aws.AmazonS3Client;
import org.com.drop.global.exception.ErrorCode;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Primary
public class UserService implements UserDetailsService {

	private final UserRepository userRepository;
	private final AuctionRepository auctionRepository;
	private final BookmarkRepository bookmarkRepository;
	private final ProductRepository productRepository;
	private final ProductImageRepository productImageRepository;
	private final BidRepository bidRepository;
	private final WinnerRepository winnerRepository;
	private final AmazonS3Client amazonS3Client;

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		User user = userRepository.findByEmail(email)
			.orElseThrow(() -> new UsernameNotFoundException("해당 이메일의 유저를 찾을 수 없습니다: " + email));

		return org.springframework.security.core.userdetails.User.builder()
			.username(user.getEmail())
			.password(user.getPassword())
			.authorities("ROLE_" + user.getRole().name())
			.build();
	}

	@Transactional(readOnly = true)
	public User findUserByEmail(String email) {
		return userRepository.findByEmail(email)
			.orElseThrow(() ->
				ErrorCode.USER_NOT_FOUND
					.serviceException("email=%s", email)
			);
	}

	@Transactional(readOnly = true)
	public MyPageResponse getMe(User user) {
		return MyPageResponse.of(user);
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

	@Transactional
	public UpdateProfileResponse updateProfile(User user, UpdateProfileRequest dto) {
		if (!user.getNickname().equals(dto.nickname()) && userRepository.existsByNickname(dto.nickname())) {
			throw ErrorCode.USER_NICKNAME_CONFLICT
				.serviceException("이미 사용 중인 닉네임입니다: nickname=%s", dto.nickname());
		}

		String oldImageUrl = user.getUserProfile();
		String newImageUrl = dto.profileImageUrl();

		if (oldImageUrl != null && !oldImageUrl.equals(newImageUrl)) {
			amazonS3Client.updateS3Tag(oldImageUrl, "deleted");
		}

		user.updateProfile(dto.nickname(), newImageUrl);
		userRepository.save(user);

		return UpdateProfileResponse.of(user);
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

	@Transactional(readOnly = true)
	public MyProductPageResponse getMyProducts(User user, int page) {
		validatePageNumber(page);
		Pageable pageable = PageRequest.of(page - 1, 20);

		Page<Product> productPage = productRepository
			.findBySellerAndDeletedAtIsNullOrderByCreatedAtDesc(user, pageable);
		List<Product> products = productPage.getContent();

		List<Long> productIds = products.stream().map(Product::getId).toList();
		Map<Long, String> imageMap = getProductMainImageMapByIds(productIds);
		List<Auction> auctions = auctionRepository.findByProductInAndDeletedAtIsNullOrderByIdDesc(products);

		Map<Long, Auction> latestAuctionMap = auctions.stream()
			.collect(Collectors.toMap(
				a -> a.getProduct().getId(), a -> a, (e, r) -> e));

		Map<Long, Long> bidCountMap = bidRepository.countByAuctionIn(auctions).stream()
			.collect(Collectors.toMap(data -> (Long) data[0], data -> (Long) data[1]));

		List<MyProductPageResponse.MyProductResponse> dtoList = products.stream()
			.map(product -> {
				Auction auction = latestAuctionMap.get(product.getId());
				String status = (auction == null) ? "PENDING" : auction.getStatus().name();

				return new MyProductPageResponse.MyProductResponse(
					auction != null ? auction.getId() : null,
					product.getId(),
					product.getName(),
					imageMap.get(product.getId()),
					status,
					auction != null ? auction.getStartPrice() : 0,
					auction != null ? auction.getStartPrice() : 0,
					auction != null ? auction.getEndAt() : null,
					product.getBookmarkCount().longValue(),
					auction != null ? bidCountMap.getOrDefault(auction.getId(), 0L) : 0L,
					calculateRemainingTime(auction)
				);
			})
			.toList();

		return new MyProductPageResponse(
			productPage.getNumber() + 1,
			productPage.getTotalPages(),
			productPage.getTotalElements(),
			dtoList
		);
	}

	@Transactional(readOnly = true)
	public MyBidPageResponse getMyBids(User user, int page, String statusParam) {
		validatePageNumber(page);
		Pageable pageable = PageRequest.of(page - 1, 20);

		Page<Bid> bidPage = bidRepository.findMyLatestBidsPerAuction(user, pageable);
		List<Auction> auctions = bidPage.getContent().stream().map(Bid::getAuction).toList();

		Map<Long, Winner> winnerMap = winnerRepository.findByAuctionIn(auctions).stream()
			.collect(Collectors.toMap(w -> w.getAuction().getId(), w -> w));

		Map<Long, String> imageMap = getProductMainImageMapByIds(
			auctions.stream().map(a -> a.getProduct().getId()).toList()
		);

		List<MyBidPageResponse.MyBidResponse> dtoList = bidPage.getContent().stream()
			.map(bid -> {
				Auction auction = bid.getAuction();
				Winner winner = winnerMap.get(auction.getId());
				String bidStatus = determineStatusWithWinner(user, auction, winner);

				return new MyBidPageResponse.MyBidResponse(
					auction.getId(),
					auction.getProduct().getId(),
					auction.getProduct().getName(),
					imageMap.get(auction.getProduct().getId()),
					bid.getBidAmount().intValue(),
					(winner != null && winner.getUserId()
						.equals(user.getId())) ? winner.getFinalPrice().intValue() : null,
					bidStatus,
					auction.getEndAt()
				);
			})
			.toList();

		return new MyBidPageResponse(bidPage.getNumber() + 1, bidPage.getTotalPages(),
			bidPage.getTotalElements(), dtoList);
	}

	private String determineStatusWithWinner(User user, Auction auction, Winner winner) {
		if (auction.getStatus() == Auction.AuctionStatus.LIVE || auction.getEndAt().isAfter(LocalDateTime.now())) {
			return "ONGOING";
		}
		if (winner != null && winner.getUserId().equals(user.getId())) {
			return "WIN";
		}
		return "LOSE";
	}

	private long calculateRemainingTime(Auction auction) {
		if (auction == null || auction.getEndAt() == null) {
			return 0;
		}
		long seconds = java.time.Duration.between(LocalDateTime.now(), auction.getEndAt()).toSeconds();
		return Math.max(seconds, 0);
	}

	private void validatePageNumber(int page) {
		if (page < 1) {
			throw ErrorCode.USER_PAGE_OUT_OF_RANGE.serviceException("페이지 번호 오류: page=%d", page);
		}
	}

	private Map<Long, String> getProductMainImageMapByIds(List<Long> productIds) {
		if (productIds.isEmpty()) {
			return java.util.Collections.emptyMap();
		}

		return productImageRepository.findAllByProductIdIn(productIds).stream()
			.collect(Collectors.toMap(
				img -> img.getProduct().getId(),
				ProductImage::getImageUrl,
				(existing, replacement) -> existing
			));
	}
}
