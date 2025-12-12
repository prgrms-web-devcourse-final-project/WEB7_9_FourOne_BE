package org.com.drop.domain.auction.product.qna.repository;

import java.util.List;

import org.com.drop.domain.auction.product.qna.entity.Answer;
import org.com.drop.domain.auction.product.qna.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnswerRepository extends JpaRepository<Answer, Long> {
	List<Answer> findByQuestion(Question question);
}
