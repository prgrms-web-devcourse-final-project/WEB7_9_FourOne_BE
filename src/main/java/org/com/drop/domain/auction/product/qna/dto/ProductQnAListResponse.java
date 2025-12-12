package org.com.drop.domain.auction.product.qna.dto;

import java.util.List;

public record ProductQnAListResponse(
	int totalCount,
	List<ProductQnAResponse> productQnAResponses) {

	public ProductQnAListResponse(List<ProductQnAResponse> productQnAResponses) {
		this(
			productQnAResponses.size(),
			productQnAResponses
		);
	}

}
