package com.dormhub.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dormhub.model.User;
import com.dormhub.repository.UserRepository;
import com.dormhub.security.JwtTokenProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    
    @Autowired
    private UserRepository userRepository;

    @GetMapping("/token")
    public ResponseEntity<Map<String, Object>> checkToken(@CookieValue(name = "jwt_token", required = false) String token) {
        Map<String, Object> response = new HashMap<>();
        
        response.put("hasToken", token != null && !token.isEmpty());
        
        if (token != null && !token.isEmpty()) {
            response.put("token", token);
            
            String email = jwtTokenProvider.getUsername(token);
            response.put("extractedEmail", email);
            
            if (email != null) {
                Optional<User> userOpt = userRepository.findByEmail(email);
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    response.put("userFound", true);
                    response.put("userName", user.getNamaLengkap());
                    response.put("userEmail", user.getEmail());
                    response.put("userLevel", user.getLevel().getId());
                } else {
                    response.put("userFound", false);
                }
            }
        }
        
        return ResponseEntity.ok(response);
    }
} 