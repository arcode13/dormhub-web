package com.dormhub.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.dormhub.model.ChatTitle;
import com.dormhub.model.User;

@Repository
public interface ChatTitleRepository extends JpaRepository<ChatTitle, Integer> {
    
    // Find all chat titles for a specific user, ordered by last updated
    List<ChatTitle> findByUserOrderByUpdatedAtDesc(User user);
    
    // Find active chat title for a user (typically the most recent one)
    ChatTitle findFirstByUserOrderByUpdatedAtDesc(User user);
} 