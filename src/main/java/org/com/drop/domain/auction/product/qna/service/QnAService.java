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
import org.com.drop.global.exception.ServiceException;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QnAService {
	private final QuestionRepository questionRepository;
	private final AnswerRepository answerRepository;
	private final ProductService productService;

	public Question addQuestion(Long productId, ProductQnACreateRequest request, User actor) {
		Product product = productService.findProductById(productId);
		Question question = new Question(product, actor, request.question());
		return questionRepository.save(question);
	}

	public Answer addAnswer(Long productId, Long qnaId, ProductQnAAnswerRequest request, User actor) {
		Product product = productService.findProductById(productId);
		Question question = questionFindById(qnaId);
		Answer answer = new Answer(product, question, actor, request.answer());
		return answerRepository.save(answer);
	}

	public Question questionFindById(Long qnaId) {
		return questionRepository.findById(qnaId)
			.orElseThrow(() -> new ServiceException(ErrorCode.PRODUCT_QUESTION_NOT_FOUND));
	}
}
