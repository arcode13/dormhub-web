package com.dormhub.api;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dormhub.model.ChatMessage;
import com.dormhub.model.ChatMessage.SenderType;
import com.dormhub.model.ChatTitle;
import com.dormhub.model.User;
import com.dormhub.repository.ChatMessageRepository;
import com.dormhub.repository.ChatTitleRepository;
import com.dormhub.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@RestController
@RequestMapping("/api/chat")
public class ChatHistoryApiController {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatHistoryApiController.class);
    
    @Autowired
    private ChatTitleRepository chatTitleRepository;
    
    @Autowired
    private ChatMessageRepository chatMessageRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @GetMapping("/titles")
    public ResponseEntity<?> getChatTitles() {
        try {
            // Get current authenticated user
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }
            
            Optional<User> userOpt = userRepository.findByEmail(auth.getName());
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "User not found"));
            }
            
            User user = userOpt.get();
            List<ChatTitle> titles = chatTitleRepository.findByUserOrderByUpdatedAtDesc(user);
            
            // Transform to simplified format
            List<Map<String, Object>> result = titles.stream()
                .map(title -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", title.getId());
                    item.put("title", title.getTitle());
                    item.put("createdAt", title.getCreatedAt());
                    item.put("updatedAt", title.getUpdatedAt());
                    return item;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error getting chat titles", e);
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }
    
    @GetMapping("/titles/{id}/messages")
    public ResponseEntity<?> getChatMessages(@PathVariable("id") Integer titleId) {
        try {
            // Get current authenticated user
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }
            
            Optional<User> userOpt = userRepository.findByEmail(auth.getName());
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "User not found"));
            }
            
            User user = userOpt.get();
            Optional<ChatTitle> titleOpt = chatTitleRepository.findById(titleId);
            
            if (titleOpt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "Chat title not found"));
            }
            
            ChatTitle title = titleOpt.get();
            
            // Security check - only allow user to access their own chats
            if (title.getUser() == null || title.getUser().getId() != user.getId()) {
                return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
            }
            
            List<ChatMessage> messages = chatMessageRepository.findByTitleOrderByCreatedAtAsc(title);
            
            // Transform to simplified format
            List<Map<String, Object>> result = messages.stream()
                .map(message -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", message.getId());
                    item.put("senderType", message.getSenderType().toString());
                    item.put("messageContent", message.getMessageContent());
                    item.put("createdAt", message.getCreatedAt());
                    return item;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error getting chat messages", e);
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }
    
    @GetMapping("/current")
    public ResponseEntity<?> getCurrentChatTitle() {
        try {
            // Get current authenticated user
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }
            
            Optional<User> userOpt = userRepository.findByEmail(auth.getName());
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "User not found"));
            }
            
            User user = userOpt.get();
            ChatTitle title = chatTitleRepository.findFirstByUserOrderByUpdatedAtDesc(user);
            
            if (title == null) {
                // No existing chat, return null
                return ResponseEntity.ok(null);
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("id", title.getId());
            result.put("title", title.getTitle());
            result.put("createdAt", title.getCreatedAt());
            result.put("updatedAt", title.getUpdatedAt());
            
            // Also get messages for this title
            List<ChatMessage> messages = chatMessageRepository.findByTitleOrderByCreatedAtAsc(title);
            
            // Transform to simplified format
            List<Map<String, Object>> messagesList = messages.stream()
                .map(message -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", message.getId());
                    item.put("senderType", message.getSenderType().toString());
                    item.put("messageContent", message.getMessageContent());
                    item.put("createdAt", message.getCreatedAt());
                    return item;
                })
                .collect(Collectors.toList());
                
            result.put("messages", messagesList);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error getting current chat title", e);
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }
    
    @PostMapping("/new")
    public ResponseEntity<?> createNewChat(@RequestBody Map<String, String> request) {
        try {
            // Get current authenticated user
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }
            
            Optional<User> userOpt = userRepository.findByEmail(auth.getName());
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "User not found"));
            }
            
            User user = userOpt.get();
            
            // Create a new chat title
            String title = request.getOrDefault("title", "Chat " + LocalDateTime.now().toString());
            
            ChatTitle chatTitle = new ChatTitle();
            chatTitle.setUser(user);
            chatTitle.setTitle(title);
            
            // Save to database
            chatTitle = chatTitleRepository.save(chatTitle);
            
            // Create welcome message
            ChatMessage welcomeMessage = new ChatMessage();
            welcomeMessage.setTitle(chatTitle);
            welcomeMessage.setSenderType(SenderType.ai);
            welcomeMessage.setMessageContent("Halo! Saya asisten DormHub. Ada yang bisa saya bantu hari ini?");
            chatMessageRepository.save(welcomeMessage);
            
            // Return the new chat title with messages
            Map<String, Object> result = new HashMap<>();
            result.put("id", chatTitle.getId());
            result.put("title", chatTitle.getTitle());
            result.put("createdAt", chatTitle.getCreatedAt());
            result.put("updatedAt", chatTitle.getUpdatedAt());
            
            // Add the welcome message
            Map<String, Object> messageItem = new HashMap<>();
            messageItem.put("id", welcomeMessage.getId());
            messageItem.put("senderType", welcomeMessage.getSenderType().toString());
            messageItem.put("messageContent", welcomeMessage.getMessageContent());
            messageItem.put("createdAt", welcomeMessage.getCreatedAt());
            
            result.put("messages", List.of(messageItem));
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error creating new chat", e);
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }
    
    @PostMapping("/save-messages")
    public ResponseEntity<?> saveMessages(@RequestBody Map<String, Object> request) {
        try {
            // Get current authenticated user
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }
            
            Optional<User> userOpt = userRepository.findByEmail(auth.getName());
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "User not found"));
            }
            
            User user = userOpt.get();
            
            // Get title ID from request
            Integer titleId = (Integer) request.get("titleId");
            if (titleId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Title ID is required"));
            }
            
            Optional<ChatTitle> titleOpt = chatTitleRepository.findById(titleId);
            if (titleOpt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "Chat title not found"));
            }
            
            ChatTitle title = titleOpt.get();
            
            // Security check - only allow user to access their own chats
            if (title.getUser() == null || title.getUser().getId() != user.getId()) {
                return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
            }
            
            // Get messages from request
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> messagesData = (List<Map<String, Object>>) request.get("messages");
            if (messagesData == null || messagesData.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Messages are required"));
            }
            
            // Save each message
            for (Map<String, Object> messageData : messagesData) {
                String content = (String) messageData.get("content");
                String senderTypeStr = (String) messageData.get("senderType");
                
                if (content == null || senderTypeStr == null) {
                    continue; // Skip invalid messages
                }
                
                SenderType senderType;
                try {
                    senderType = SenderType.valueOf(senderTypeStr);
                } catch (Exception e) {
                    continue; // Skip invalid sender type
                }
                
                ChatMessage message = new ChatMessage();
                message.setTitle(title);
                message.setSenderType(senderType);
                message.setMessageContent(content);
                
                chatMessageRepository.save(message);
            }
            
            // Update title's updatedAt timestamp
            title.setUpdatedAt(LocalDateTime.now());
            chatTitleRepository.save(title);
            
            return ResponseEntity.ok(Map.of("success", true));
            
        } catch (Exception e) {
            logger.error("Error saving messages", e);
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }
} 