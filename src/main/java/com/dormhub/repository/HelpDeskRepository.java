package com.dormhub.repository;

import com.dormhub.model.HelpDesk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HelpDeskRepository extends JpaRepository<HelpDesk, Integer> {
    Optional<HelpDesk> findByUserId(int userId);
}
