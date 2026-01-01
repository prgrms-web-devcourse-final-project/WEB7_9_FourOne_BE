package org.com.drop.domain.admin.guide.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "guides")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Guide {
	@Column(nullable = false)
	String content;
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	public Guide(String content) {
		this.content = content;
	}

	public void setContent(String content) {
		this.content = content;
	}
}

