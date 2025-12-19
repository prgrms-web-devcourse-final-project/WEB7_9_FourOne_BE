package org.com.drop.domain.winner.repository;

import org.com.drop.domain.winner.domain.Winner;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WinnerRepository extends JpaRepository<Winner, Long> {
}
