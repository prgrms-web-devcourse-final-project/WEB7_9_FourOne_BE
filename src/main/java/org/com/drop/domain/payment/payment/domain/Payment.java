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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
@Table(name = "payments")
public class Payment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "winners_id", nullable = false)
	private Long winnersId;

	@Column(name = "method_id")
	private Long methoddI;

	@Column(name = "sellers_id", nullable = false)
	private Long sellersId;

	@Column(name = "toss_payment_key")
	private String tossPaymentKey;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private PaymentStatus status;

	@Column(name = "receipt")
	private String receipt;

	@Column(name = "requested_at")
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
}

