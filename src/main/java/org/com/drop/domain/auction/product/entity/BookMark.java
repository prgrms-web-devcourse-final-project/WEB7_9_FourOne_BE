package org.com.drop.domain.auction.product.entity;

import java.time.LocalDateTime;

import org.com.drop.domain.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
	name = "bookmarks",
	indexes = {
		@Index(name = "idx_user_id", columnList = "user_id"),
		@Index(name = "idx_product_id", columnList = "product_id")
	},
	uniqueConstraints = {@UniqueConstraint(name = "uk_user_product", columnNames = {"user_id", "product_id"})}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class BookMark {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "product_id", nullable = false)
	private Product product;

	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	public BookMark(User user, Product product) {
		this.user = user;
		this.product = product;
		this.createdAt = LocalDateTime.now();
	}
}
