package com.dormhub.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.dormhub.model.Mahasiswa;
import com.dormhub.model.User;
import com.dormhub.repository.MahasiswaRepository;
import com.dormhub.repository.UserRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Controller
public class DummyDataController {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private MahasiswaRepository mahasiswaRepository;
    
    @GetMapping("/api/test/current-user")
    @ResponseBody
    public Map<String, Object> getCurrentUser() {
        Map<String, Object> response = new HashMap<>();
        
        // Get authenticated user from SecurityContext
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            String email = auth.getName();
            response.put("authenticated", true);
            response.put("email", email);
            
            // Get user from database
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                response.put("user", user.getNamaLengkap());
                response.put("userId", user.getId());
                response.put("level", user.getLevel().getId());
                
                // If user is a student, get student data
                if (user.getLevel().getId() == 1) {
                    Optional<Mahasiswa> mahasiswaOpt = mahasiswaRepository.findByUserId(user.getId());
                    if (mahasiswaOpt.isPresent()) {
                        Mahasiswa mahasiswa = mahasiswaOpt.get();
                        response.put("mahasiswa", true);
                        response.put("noKamar", mahasiswa.getNoKamar());
                        response.put("noKasur", mahasiswa.getNoKasur());
                        response.put("jurusan", mahasiswa.getJurusan().getNama());
                    } else {
                        response.put("mahasiswa", false);
                    }
                }
            } else {
                response.put("userFound", false);
            }
        } else {
            response.put("authenticated", false);
        }
        
        return response;
    }
    
    @GetMapping("/api/demo-chat")
    public String getDemoChatPage(Model model) {
        // Get authenticated user from SecurityContext
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            String email = auth.getName();
            
            // Get user from database
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                model.addAttribute("user", user);
                
                // If user is a student, get student data
                if (user.getLevel().getId() == 1) {
                    Optional<Mahasiswa> mahasiswaOpt = mahasiswaRepository.findByUserId(user.getId());
                    if (mahasiswaOpt.isPresent()) {
                        Mahasiswa mahasiswa = mahasiswaOpt.get();
                        model.addAttribute("mahasiswa", mahasiswa);
                        model.addAttribute("noKamar", mahasiswa.getNoKamar());
                        model.addAttribute("noKasur", mahasiswa.getNoKasur());
                        model.addAttribute("jurusan", mahasiswa.getJurusan().getNama());
                    }
                }
            }
        }
        
        return "demo-chat";
    }
} 