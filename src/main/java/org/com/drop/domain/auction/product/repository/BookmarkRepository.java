package org.com.drop.domain.auction.product.repository;

import java.util.Optional;

import org.com.drop.domain.auction.product.entity.BookMark;
import org.com.drop.domain.auction.product.entity.Product;
import org.com.drop.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookmarkRepository extends JpaRepository<BookMark, Long> {
	Optional<BookMark> findByProductAndUser(Product product, User user);

	@Query("SELECT b FROM BookMark b JOIN FETCH b.product p WHERE b.user = :user")
	Page<BookMark> findByUser(@Param("user") User user, Pageable pageable);
}
