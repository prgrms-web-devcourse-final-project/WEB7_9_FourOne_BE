package org.com.drop.domain.auction.bid.entity;

import java.time.LocalDateTime;

import org.com.drop.domain.auction.auction.entity.Auction;
import org.com.drop.domain.user.entity.User;

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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
	name = "winners",
	uniqueConstraints = {
		@UniqueConstraint(columnNames = "auction_id")   // 경매당 낙찰자 1명 보장
	}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Winner {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "auction_id", nullable = false)
	private Auction auction;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "final_price", nullable = false)
	private Integer finalPrice;

	@Column(name = "win_time")
	private LocalDateTime winTime;
}
