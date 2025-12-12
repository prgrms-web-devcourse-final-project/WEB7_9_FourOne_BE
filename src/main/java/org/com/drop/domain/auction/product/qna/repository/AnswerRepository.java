package org.com.drop.domain.auction.product.qna.repository;

import java.util.Optional;

import org.com.drop.domain.auction.product.qna.entity.Answer;
import org.com.drop.domain.auction.product.qna.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnswerRepository extends JpaRepository<Answer, Long> {
	Optional<Object> findByQuestion(Question question);
}
