package org.com.drop.domain.auction.product.qna.entity;

import java.time.LocalDateTime;

import org.com.drop.domain.auction.product.entity.Product;
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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "product_qnas", indexes = {@Index(name = "idx_product_id", columnList = "product_id")})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Question {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "product_id", nullable = false)
	private Product product;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "question_user_id", nullable = false)
	private User questioner;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String question;

	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	public Question(Product product, User questioner, String question) {
		this.product = product;
		this.questioner = questioner;
		this.question = question;
		this.createdAt = LocalDateTime.now();
	}
}
