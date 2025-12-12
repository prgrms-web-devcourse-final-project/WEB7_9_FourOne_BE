package org.com.drop.domain.auction.product.qna.repository;

import org.com.drop.domain.auction.product.qna.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository extends JpaRepository<Question, Long> {
}
