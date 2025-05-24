package com.dormhub.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.dormhub.model.ChatMessage;
import com.dormhub.model.ChatTitle;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    
    // Find all messages for a specific chat title, ordered by creation time
    List<ChatMessage> findByTitleOrderByCreatedAtAsc(ChatTitle title);
    
    // Find the last N messages for a chat title using correct JPQL syntax
    @Query("SELECT m FROM ChatMessage m WHERE m.title = :title ORDER BY m.createdAt DESC")
    List<ChatMessage> findLatestMessages(@Param("title") ChatTitle title, Pageable pageable);
} 