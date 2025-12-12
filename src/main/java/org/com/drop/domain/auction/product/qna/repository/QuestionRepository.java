package org.com.drop.domain.auction.product.qna.repository;

import java.util.List;

import org.com.drop.domain.auction.product.entity.Product;
import org.com.drop.domain.auction.product.qna.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository extends JpaRepository<Question, Long> {
	List<Question> findByProductOrderById(Product product);
}
