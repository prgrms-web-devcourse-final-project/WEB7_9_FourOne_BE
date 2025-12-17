package org.com.drop.domain.payment.payment.domain;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;
import java.time.LocalDateTime;

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

