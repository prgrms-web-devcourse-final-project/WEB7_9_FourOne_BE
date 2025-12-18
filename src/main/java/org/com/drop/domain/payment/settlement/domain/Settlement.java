package org.com.drop.domain.payment.settlement.domain;

import java.time.LocalDateTime;

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
@Table(name = "settlement")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Settlement {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "payment_id", nullable = false)
	private Long paymentId;

	@Column(name = "seller_id", nullable = false)
	private Long sellerId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private SettlementStatus status;

	@Column(name = "fee")
	private Long fee;

	@Column(name = "net")
	private Long net;

	@Column(name = "hold_at")
	private LocalDateTime holdAt;

	@Column(name = "ready_at")
	private LocalDateTime readyAt;

	@Column(name = "paid_at")
	private LocalDateTime paidAt;
}
