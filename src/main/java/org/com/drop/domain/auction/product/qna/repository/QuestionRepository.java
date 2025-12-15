package org.com.drop.domain.auction.product.qna.repository;

import java.util.List;

import org.com.drop.domain.auction.product.entity.Product;
import org.com.drop.domain.auction.product.qna.entity.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository extends JpaRepository<Question, Long> {
	Page<Question> findByProduct(Product product, Pageable pageable);

	List<Question> findByProductOrderById(Product product);
}
