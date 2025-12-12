package org.com.drop.domain.auction.product.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.com.drop.domain.auction.auction.entity.Auction;
import org.com.drop.domain.auction.auction.repository.AuctionRepository;
import org.com.drop.domain.auction.product.dto.ProductCreateRequest;
import org.com.drop.domain.auction.product.entity.Product;
import org.com.drop.domain.auction.product.entity.ProductImage;
import org.com.drop.domain.auction.product.repository.ProductImageRepository;
import org.com.drop.domain.auction.product.repository.ProductRepository;
import org.com.drop.domain.user.entity.User;
import org.com.drop.global.exception.ErrorCode;
import org.com.drop.global.exception.ServiceException;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService {

	private final ProductRepository productRepository;
	private final ProductImageRepository  productImageRepository;
	private final AuctionRepository auctionRepository;

	public void validUser(Product product, User actor) {
		if (!product.getSeller().getId().equals(actor.getId())) {
			throw new ServiceException(ErrorCode.USER_INACTIVE_USER);
		}
	}

	public void validAuction(Product product) {

		Optional<Auction> auction = auctionRepository.findByProductId(product.getId());

		if (auction.isPresent()
			&& auction.get().getStartAt().isBefore(LocalDateTime.now())) {
			throw new ServiceException(ErrorCode.PRODUCT_ALREADY_ON_AUCTION);
		}
	}


	public Product addProduct(ProductCreateRequest request, User actor) {
		Product product = new Product(
			actor,
			request.name(),
			request.description(),
			request.category(),
			request.subCategory());
		addProductImages(product, request.imagesFiles());
		return productRepository.save(product);
	}

	public List<ProductImage> addProductImages(Product product, List<String> imageUrls) {

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

		return images;
	}



	public Product findProductById(Long id) {
		return productRepository.findByIdAndDeletedAtIsNull(id)
			.orElseThrow(() -> new ServiceException(ErrorCode.PRODUCT_NOT_FOUND));
	}

	public Product updateProduct(Long productId, ProductCreateRequest request, User actor) {
		Product product = findProductById(productId);
		validUser(product, actor);
		validAuction(product);
		deleteProductImage(product, actor);
		addProductImages(product, request.imagesFiles());
		product.update(request.name(), request.description(), request.category(), request.subCategory());
		return product;
	}

	public void deleteProduct(Long productId, User actor) {
		Product product = findProductById(productId);
		validUser(product, actor);
		validAuction(product);
		deleteProductImage(product, actor);
		product.setDeleted();
		productRepository.save(product);
	}

	public void deleteProductImage(Product product, User actor) {
		if (product.getSeller().getId().equals(actor.getId())) {
			productImageRepository.deleteByProduct(product);
		}
	}
}
