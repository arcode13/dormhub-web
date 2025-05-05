package com.dormhub.controller;

import com.dormhub.model.SeniorResidence;
import com.dormhub.model.User;
import com.dormhub.repository.KonfigurasiRepository;
import com.dormhub.repository.UserRepository;
import com.dormhub.service.SeniorResidenceService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class InfoMahasiswaController {

    @Autowired
    private SeniorResidenceService seniorResidenceService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private KonfigurasiRepository konfigurasiRepository;

    @GetMapping("/senior-residence/info-mahasiswa")
    public String infoMahasiswa(Model model, RedirectAttributes redirectAttributes) {
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            int userId = user.getId();
            
            SeniorResidence seniorResidence = seniorResidenceService.getSeniorResidenceByMahasiswaId(userId).orElse(null);
            if (seniorResidence == null) {
                redirectAttributes.addFlashAttribute("error", "Senior Residence tidak ditemukan.");
                return "redirect:/logout";
            }

            Map<String, String> konfigurasi = konfigurasiRepository.findAllAsMap().stream()
                .collect(Collectors.toMap(
                    entry -> entry.get("key"),
                    entry -> entry.get("value")
                ));

            model.addAttribute("konfigurasi", konfigurasi);
            
            model.addAttribute("user", user);
        } else {
            redirectAttributes.addFlashAttribute("error", "User tidak ditemukan.");
        }

        return "senior-residence/InfoMahasiswa";
    }
}
