package com.dormhub.controller;

import com.dormhub.model.SeniorResidence;
import com.dormhub.model.User;
import com.dormhub.service.SeniorResidenceService;
import com.dormhub.repository.KonfigurasiRepository;
import com.dormhub.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class SeniorResidenceController {

    @Autowired
    private SeniorResidenceService seniorResidenceService;
    
    @Autowired
    private KonfigurasiRepository konfigurasiRepository;
    
    @Autowired
    private UserRepository userRepository;

    @GetMapping("/admin/senior-residence")
    public String listSeniorResidence(Model model, RedirectAttributes redirectAttributes) {
        List<SeniorResidence> seniorResidenceList = seniorResidenceService.getAllSeniorResidence();
        model.addAttribute("seniorResidenceList", seniorResidenceList);
        
        Map<String, String> konfigurasi = konfigurasiRepository.findAllAsMap().stream()
            .collect(Collectors.toMap(
                entry -> entry.get("key"),
                entry -> entry.get("value")
            ));
        model.addAttribute("konfigurasi", konfigurasi);
        
        // Add current time for cache busting
        model.addAttribute("currentTimeMillis", System.currentTimeMillis());
        
        // Get logged in user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            model.addAttribute("user", user);
        } else {
            redirectAttributes.addFlashAttribute("error", "User tidak ditemukan");
            return "redirect:/logout";
        }

        return "admin/SeniorResidence/index";
    }

    @GetMapping("/admin/senior-residence/delete/{id}")
    public String deleteSeniorResidence(@PathVariable("id") int id, RedirectAttributes redirectAttributes) {
        try {
        seniorResidenceService.deleteSeniorResidence(id);
            redirectAttributes.addFlashAttribute("success", "Senior Residence berhasil dihapus.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Gagal menghapus Senior Residence: " + e.getMessage());
        }
        return "redirect:/admin/senior-residence";
    }
}
