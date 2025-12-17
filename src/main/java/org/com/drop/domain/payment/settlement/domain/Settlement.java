package org.com.drop.domain.payment.settlement.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "settlement")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Settlement {

	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
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
