package org.com.drop.domain.payment.payment.domain;

import java.time.LocalDateTime;

import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@DynamicUpdate
@Table(name = "payments")
public class Payment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "winners_id", nullable = false)
	private Long winnersId;

	@Column(name = "method_id")
	private Long methodId;

	@Column(name = "sellers_id", nullable = false)
	private Long sellersId;

	@Column(name = "toss_payment_key")
	private String tossPaymentKey;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private PaymentStatus status;

	@Column(name = "receipt")
	private String receipt;

	@Column(name = "requested_at", nullable = false)
	private LocalDateTime requestedAt;

	@Column(name = "approved_at")
	private LocalDateTime approvedAt;

	@Column(name = "fee")
	private Long fee;

	@Column(name = "net")
	private Long net;

	@Enumerated(EnumType.STRING)
	@Column(name = "card_company")
	private CardCompany cardCompany;

	@Builder
	private Payment(
		Long winnersId,
		Long sellersId,
		Long methodId,
		PaymentStatus status,
		LocalDateTime requestedAt,
		Long fee,
		Long net,
		CardCompany cardCompany
	) {
		this.winnersId = winnersId;
		this.sellersId = sellersId;
		this.methodId = methodId;
		this.status = status;
		this.requestedAt = requestedAt;
		this.fee = fee;
		this.net = net;
		this.cardCompany = cardCompany;
	}

	@PrePersist
	protected void prePersist() {
		if (this.requestedAt == null) {
			this.requestedAt = LocalDateTime.now();
		}
		if (this.status == null) {
			this.status = PaymentStatus.REQUESTED;
		}
	}

	/* =====================
	   도메인 상태 변경 메서드
	   ===================== */

	public void assignTossPaymentKey(String tossPaymentKey) {
		this.tossPaymentKey = tossPaymentKey;
	}

	public void markPaid() {
		this.status = PaymentStatus.PAID;
		this.approvedAt = LocalDateTime.now();
	}

	public void markFailed() {
		this.status = PaymentStatus.FAILED;
	}

	public void markCanceled() {
		this.status = PaymentStatus.CANCELED;
	}

	public void markExpired() {
		this.status = PaymentStatus.EXPIRED;
	}
}

