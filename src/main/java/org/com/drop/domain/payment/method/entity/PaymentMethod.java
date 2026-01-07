package org.com.drop.domain.payment.method.entity;

import java.time.LocalDateTime;

import org.com.drop.domain.payment.payment.domain.CardCompany;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "payment_methods")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentMethod {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Enumerated(EnumType.STRING)
	@Column(name = "card_company", nullable = false)
	private CardCompany cardCompany;

	@Column(name = "customer_key", nullable = false, unique = true)
	private String customerKey;

	@Column(name = "billing_key", nullable = false, unique = true)
	private String billingKey;

	@Column(name = "card_number_masked")
	private String cardNumberMasked;

	@Column(name = "card_name")
	private String cardName;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	public PaymentMethod(
		Long userId,
		CardCompany cardCompany,
		String customerKey,
		String billingKey,
		String cardNumberMasked,
		String cardName
	) {
		this.userId = userId;
		this.cardCompany = cardCompany;
		this.customerKey = customerKey;
		this.billingKey = billingKey;
		this.cardNumberMasked = cardNumberMasked;
		this.cardName = cardName;
	}

	@PrePersist
	protected void prePersist() {
		this.createdAt = LocalDateTime.now();
		this.updatedAt = this.createdAt;
	}

	@PreUpdate
	protected void preUpdate() {
		this.updatedAt = LocalDateTime.now();
	}
}
