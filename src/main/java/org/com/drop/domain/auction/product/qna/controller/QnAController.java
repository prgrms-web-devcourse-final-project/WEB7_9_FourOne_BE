package org.com.drop.domain.auction.product.qna.controller;

import org.com.drop.domain.auction.product.qna.dto.ProductQnAAnswerRequest;
import org.com.drop.domain.auction.product.qna.dto.ProductQnAAnswerResponse;
import org.com.drop.domain.auction.product.qna.dto.ProductQnACreateRequest;
import org.com.drop.domain.auction.product.qna.dto.ProductQnaCreateResponse;
import org.com.drop.domain.auction.product.qna.entity.Answer;
import org.com.drop.domain.auction.product.qna.entity.Question;
import org.com.drop.domain.auction.product.qna.service.QnAService;
import org.com.drop.domain.user.entity.User;
import org.com.drop.domain.user.repository.UserRepository;
import org.com.drop.global.rsdata.RsData;
import org.springframework.web.bind.annotation.DeleteMapping;
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
	private final UserRepository userRepository; //임시
	private final QnAService  qnAService;

	@PostMapping
	public RsData<ProductQnaCreateResponse> addQna(
		@PathVariable
		Long productId,
		@RequestBody
		@Valid
		ProductQnACreateRequest request) {
		//TODO : rq 구현 후 수정
		User actor = userRepository.findById(1L).get();
		Question question = qnAService.addQuestion(productId, request, actor);
		return new RsData<>(
			new ProductQnaCreateResponse(question)
		);
	}

	@PostMapping("/{qnaId}")
	public RsData<ProductQnAAnswerResponse> addAnswer(
		@PathVariable
		Long productId,
		@PathVariable
		Long qnaId,
		@RequestBody
		@Valid
		ProductQnAAnswerRequest request) {
		//TODO : rq 구현 후 수정
		User actor = userRepository.findById(1L).get();
		Answer answer = qnAService.addAnswer(productId, qnaId, request, actor);
		return new RsData<>(
			new ProductQnAAnswerResponse(answer)
		);
	}

	@DeleteMapping("/{qnaId}")
	public RsData<Void> deleteAnswer(
		@PathVariable
		Long productId,
		@PathVariable
		Long qnaId
	) {
		//TODO : rq 구현 후 수정
		User actor = userRepository.findById(1L).get();
		qnAService.deleteAnswer(qnaId, actor);
		return new RsData<>(null);
	}
}
