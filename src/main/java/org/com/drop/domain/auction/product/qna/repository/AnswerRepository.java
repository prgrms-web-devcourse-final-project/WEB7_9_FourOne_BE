package org.com.drop.domain.auction.product.qna.repository;

import org.com.drop.domain.auction.product.qna.entity.Answer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnswerRepository extends JpaRepository<Answer, Long> {
}
