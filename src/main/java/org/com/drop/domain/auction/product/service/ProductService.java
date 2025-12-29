package org.com.drop.domain.auction.product.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.com.drop.domain.auction.auction.entity.Auction;
import org.com.drop.domain.auction.auction.repository.AuctionRepository;
import org.com.drop.domain.auction.product.dto.ProductCreateRequest;
import org.com.drop.domain.auction.product.dto.ProductSearchResponse;
import org.com.drop.domain.auction.product.entity.BookMark;
import org.com.drop.domain.auction.product.entity.Product;
import org.com.drop.domain.auction.product.entity.ProductImage;
import org.com.drop.domain.auction.product.repository.BookmarkRepository;
import org.com.drop.domain.auction.product.repository.ProductImageRepository;
import org.com.drop.domain.auction.product.repository.ProductRepository;
import org.com.drop.domain.user.entity.User;
import org.com.drop.global.aws.AmazonS3Client;
import org.com.drop.global.exception.ErrorCode;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

	private final ProductRepository productRepository;
	private final ProductImageRepository  productImageRepository;
	private final AuctionRepository auctionRepository;
	private final BookmarkRepository bookmarkRepository;
	private final AmazonS3Client amazonS3Client;
	public void validUser(Product product, User actor) {
		if (!product.getSeller().getId().equals(actor.getId())) {
			throw ErrorCode.USER_INACTIVE_USER
				.serviceException(
					"productId=%d, sellerId=%d, actorId=%d",
					product.getId(),
					product.getSeller().getId(),
					actor.getId()
				);
		}
	}

	public void validAuction(Product product) {

		Optional<Auction> auction =
			auctionRepository.findByProductId(product.getId());

		if (auction.isPresent()
			&& auction.get().getStartAt().isBefore(LocalDateTime.now())) {

			throw ErrorCode.PRODUCT_ALREADY_ON_AUCTION
				.serviceException(
					"productId=%d, auctionStartAt=%s",
					product.getId(),
					auction.get().getStartAt()
				);
		}
	}

	@Transactional
	@CacheEvict(
		value = "product:detail",
		allEntries = true
	)
	public Product addProduct(ProductCreateRequest request, User actor) {
		Product product = new Product(
			actor,
			request.name(),
			request.description(),
			request.category(),
			request.subCategory());
		productRepository.save(product);
		addProductImages(product, request.imagesFiles());

		return product;
	}

	@Transactional
	@CacheEvict(
		value = "product:detail",
		allEntries = true
	)
	public void addProductImages(Product product, List<String> imageUrls) {
		amazonS3Client.verifyImage(imageUrls);

		List<ProductImage> images = imageUrls.stream()
			.map(url -> new ProductImage(product, url))
			.toList();

		for (int i = 0; i < images.size(); i++) {
			ProductImage current = images.get(i);

			ProductImage prev = (i > 0) ? images.get(i - 1) : null;
			ProductImage next = (i < images.size() - 1) ? images.get(i + 1) : null;

			current.setHead(prev);
			current.setTail(next);
		}

		productImageRepository.saveAll(images);
	}

	public Product findProductById(Long id) {
		return productRepository.findByIdAndDeletedAtIsNull(id)
			.orElseThrow(() ->
				ErrorCode.PRODUCT_NOT_FOUND
					.serviceException("productId=%d", id)
			);
	}

	@Cacheable(
		value = "product:detail",
		key = "#id"
	)
	public ProductSearchResponse findProductWithImgById(Long id, User actor) {
		Product product = findProductById(id);
		validUser(product, actor);
		List<String> images = getSortedImageUrls(productImageRepository.findAllByProductId(product.getId()));

		return new ProductSearchResponse(product, images);
	}

	@CacheEvict(
		value = "product:detail",
		allEntries = true
	)
	public List<String> getSortedImageUrls(List<ProductImage> images) {
		Optional<ProductImage> start = images.stream()
			.filter(img -> img.getPreImg() == null).findFirst();

		if (start.isEmpty()) {
			return Collections.emptyList();
		}

		List<String> sortedUrls = new ArrayList<>();
		ProductImage current = start.get();

		while (current != null) {
			sortedUrls.add(amazonS3Client.getPresignedUrl(current.getImageUrl()));
			current = current.getTrailImg();

		}

		return sortedUrls;
	}

	@Transactional
	@CacheEvict(
		value = "product:detail",
		allEntries = true
	)
	public Product updateProduct(Long productId, ProductCreateRequest request, User actor) {
		Product product = findProductById(productId);
		validUser(product, actor);
		validAuction(product);
		deleteProductImage(product, actor);
		addProductImages(product, request.imagesFiles());
		product.update(request.name(), request.description(), request.category(), request.subCategory());
		return product;
	}

	@Transactional
	@CacheEvict(
		value = "product:detail",
		allEntries = true
	)
	public void deleteProduct(Long productId, User actor) {
		Product product = findProductById(productId);
		validUser(product, actor);
		validAuction(product);
		deleteProductImage(product, actor);
		product.setDeleted();
		productRepository.save(product);
	}

	@Transactional
	@CacheEvict(
		value = "product:detail",
		allEntries = true
	)
	public void deleteProductImage(Product product, User actor) {
		if (product.getSeller().getId().equals(actor.getId())) {
			List<ProductImage> keys = productImageRepository.deleteByProduct(product);
			for (ProductImage key : keys) {
				amazonS3Client.updateS3Tag(key.getImageUrl(), "deleted");
			}
		}
	}

	@Transactional
	public BookMark addBookmark(Long productId, User actor) {
		Product product = findProductById(productId);

		if (bookmarkRepository.findByProductAndUser(product, actor).isPresent()) {
			throw ErrorCode.PRODUCT_ALREADY_BOOKMARKED.serviceException("productId=%d", productId);
		}

		BookMark bookmark = new BookMark(actor, product);
		return bookmarkRepository.save(bookmark);
	}

	@Transactional
	public void deleteBookmark(Long productId, User actor) {
		Product product = findProductById(productId);
		BookMark bookMark = findBookmarkById(product, actor);
		bookmarkRepository.delete(bookMark);
	}

	public BookMark findBookmarkById(Product product, User actor) {
		return bookmarkRepository.findByProductAndUser(product, actor)
			.orElseThrow(() -> ErrorCode.USER_BOOKMARK_NOT_FOUND
				.serviceException("productId=%d", product.getId()));
	}
}
