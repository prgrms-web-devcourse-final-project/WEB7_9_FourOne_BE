package org.com.drop.domain.auction.product.qna.service;

import org.com.drop.domain.auction.product.entity.Product;
import org.com.drop.domain.auction.product.qna.dto.ProductQnAAnswerRequest;
import org.com.drop.domain.auction.product.qna.dto.ProductQnACreateRequest;
import org.com.drop.domain.auction.product.qna.entity.Answer;
import org.com.drop.domain.auction.product.qna.entity.Question;
import org.com.drop.domain.auction.product.qna.repository.AnswerRepository;
import org.com.drop.domain.auction.product.qna.repository.QuestionRepository;
import org.com.drop.domain.auction.product.service.ProductService;
import org.com.drop.domain.user.entity.User;
import org.com.drop.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QnAService {
	private final QuestionRepository questionRepository;
	private final AnswerRepository answerRepository;
	private final ProductService productService;

	@Transactional
	public Question addQuestion(Long productId, ProductQnACreateRequest request, User actor) {
		Product product = productService.findProductById(productId);
		Question question = new Question(product, actor, request.question());
		return questionRepository.save(question);
	}

	@Transactional
	public Answer addAnswer(Long productId, Long qnaId, ProductQnAAnswerRequest request, User actor) {
		Product product = productService.findProductById(productId);
		Question question = questionFindById(qnaId);
		Answer answer = new Answer(product, question, actor, request.answer());
		return answerRepository.save(answer);
	}

	public Question questionFindById(Long qnaId) {
		return questionRepository.findById(qnaId)
			.orElseThrow(() ->
				ErrorCode.PRODUCT_QUESTION_NOT_FOUND
					.serviceException("qnaId=%d", qnaId)
			);
	}

	public Answer answerFindById(Long answerId) {
		return answerRepository.findById(answerId)
			.orElseThrow(() ->
				ErrorCode.PRODUCT_ANSWER_NOT_FOUND
					.serviceException("answerId=%d", answerId)
			);
	}


	@Transactional
	public void deleteAnswer(Long qnaId, User actor) {

		Answer answer = answerFindById(qnaId);

		if (!answer.getAnswerer().equals(actor)) {
			throw ErrorCode.USER_INACTIVE_USER
				.serviceException(
					"actorId=%d, answererId=%d",
					actor.getId(),
					answer.getAnswerer().getId()
				);
		}

		answer.delete();
	}
}
