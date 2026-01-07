package org.com.drop.domain.winner.domain;

import java.time.LocalDateTime;

import org.com.drop.domain.auction.auction.entity.Auction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
	name = "winners",
	uniqueConstraints = {
		@UniqueConstraint(columnNames = "auction_id")
	}
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Winner {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "auction_id", nullable = false)
	private Auction auction;

	@Column(name = "seller_id", nullable = false)
	private Long sellerId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "final_price", nullable = false)
	private Long finalPrice;

	@Column(name = "win_time")
	private LocalDateTime winTime;
}
