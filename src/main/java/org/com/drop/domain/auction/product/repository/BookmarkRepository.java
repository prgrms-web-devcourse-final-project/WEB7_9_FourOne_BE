package org.com.drop.domain.auction.product.repository;

import java.util.Optional;

import org.com.drop.domain.auction.product.entity.BookMark;
import org.com.drop.domain.auction.product.entity.Product;
import org.com.drop.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookmarkRepository extends JpaRepository<BookMark, Long> {
	Optional<BookMark> findByProductAndUser(Product product, User user);
}
