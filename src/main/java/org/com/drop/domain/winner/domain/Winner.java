package org.com.drop.domain.winner.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "winners")
@Getter
@NoArgsConstructor
public class Winner {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "seller_id", nullable = false)
	private Long sellerId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "final_price", nullable = false)
	private Long finalPrice;
}
