package org.com.drop.domain.auction.product.qna.controller;

import java.util.List;

import org.com.drop.domain.auction.product.qna.dto.ProductQnAAnswerRequest;
import org.com.drop.domain.auction.product.qna.dto.ProductQnAAnswerResponse;
import org.com.drop.domain.auction.product.qna.dto.ProductQnACreateRequest;
import org.com.drop.domain.auction.product.qna.dto.ProductQnAListResponse;
import org.com.drop.domain.auction.product.qna.dto.ProductQnAResponse;
import org.com.drop.domain.auction.product.qna.dto.ProductQnaCreateResponse;
import org.com.drop.domain.auction.product.qna.entity.Answer;
import org.com.drop.domain.auction.product.qna.entity.Question;
import org.com.drop.domain.auction.product.qna.service.QnAService;
import org.com.drop.domain.user.entity.User;
import org.com.drop.global.rsdata.RsData;
import org.com.drop.global.security.auth.LoginUser;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products/{productId}/qna")
public class QnAController {
	private final QnAService  qnAService;

	@PostMapping
	public RsData<ProductQnaCreateResponse> addQna(
		@LoginUser User actor,
		@PathVariable Long productId,
		@RequestBody @Valid
		ProductQnACreateRequest request) {
		Question question = qnAService.addQuestion(productId, request, actor);
		return new RsData<>(
			new ProductQnaCreateResponse(question)
		);
	}

	@PostMapping("/{qnaId}")
	public RsData<ProductQnAAnswerResponse> addAnswer(
		@LoginUser User actor,
		@PathVariable Long productId,
		@PathVariable Long qnaId,
		@RequestBody @Valid
		ProductQnAAnswerRequest request) {
		Answer answer = qnAService.addAnswer(productId, qnaId, request, actor);
		return new RsData<>(
			new ProductQnAAnswerResponse(answer)
		);
	}

	@DeleteMapping("/{qnaId}/{answerId}")
	public RsData<Void> deleteAnswer(
		@LoginUser User actor,
		@PathVariable Long qnaId,
		@PathVariable Long answerId
	) {
		qnAService.deleteAnswer(answerId, actor);
		return new RsData<>(null);
	}

	@GetMapping
	public RsData<ProductQnAListResponse> getQna(
		@PathVariable Long productId,
		Pageable pageable
	) {
		List<ProductQnAResponse> qnas = qnAService.getQna(productId, pageable);
		return new RsData<>(
			new ProductQnAListResponse(qnas)
		);
	}
}
