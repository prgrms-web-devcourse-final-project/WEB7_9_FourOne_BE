package org.com.drop.domain.admin.guide.repository;

import java.util.List;

import org.com.drop.domain.admin.guide.entity.Guide;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GuideRepository extends JpaRepository<Guide, Long> {
	List<Guide> findAllByOrderByIdAsc();
}
