package com.dormhub.controller;

import com.dormhub.model.User;
import com.dormhub.repository.KonfigurasiRepository;
import com.dormhub.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class ResetPasswordController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private KonfigurasiRepository konfigurasiRepository;

    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam("token") String token, Model model, RedirectAttributes redirectAttributes) {
        Optional<User> userOptional = userRepository.findByToken(token);

        if (!userOptional.isPresent()) {
            redirectAttributes.addFlashAttribute("error", "Token tidak valid atau telah digunakan.");
            return "redirect:/login";
        }

        Map<String, String> konfigurasi = konfigurasiRepository.findAllAsMap().stream()
            .collect(Collectors.toMap(
                entry -> entry.get("key"),
                entry -> entry.get("value")
            ));

        model.addAttribute("konfigurasi", konfigurasi);

        model.addAttribute("token", token);
        return "ResetPassword";
    }

    @PostMapping("/reset-password")
    public String handleResetPassword(
            @RequestParam("token") String token,
            @RequestParam("password") String password,
            @RequestParam("confirmPassword") String confirmPassword,
            RedirectAttributes redirectAttributes) {

        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Konfirmasi Password baru tidak sesuai.");
            return "redirect:/reset-password?token=" + token;
        }

        Optional<User> userOptional = userRepository.findByToken(token);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            user.setPassword(passwordEncoder.encode(password));
            user.setToken(null); // Hapus token setelah berhasil reset
            userRepository.save(user);

            redirectAttributes.addFlashAttribute("success", "Password berhasil diubah.");
            return "redirect:/login";
        } else {
            redirectAttributes.addFlashAttribute("error", "Token tidak valid atau telah digunakan.");
            return "redirect:/login";
        }
    }
}
