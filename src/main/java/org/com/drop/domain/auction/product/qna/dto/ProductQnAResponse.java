package org.com.drop.domain.auction.product.qna.dto;

import java.util.List;

import org.com.drop.domain.auction.product.qna.entity.Answer;
import org.com.drop.domain.auction.product.qna.entity.Question;

public record ProductQnAResponse(
	ProductQnaCreateResponse productQnaCreateResponse,
	List<ProductQnAAnswerResponse> answers) {

	public ProductQnAResponse(Question question, List<Answer> answers) {
		this(
			new ProductQnaCreateResponse(question),
			answers.stream()
				.map(ProductQnAAnswerResponse::new)
				.toList()
		);
	}
}
