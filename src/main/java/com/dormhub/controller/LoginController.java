package com.dormhub.controller;

import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.dormhub.model.User;
import com.dormhub.repository.KonfigurasiRepository;
import com.dormhub.security.JwtUtil;
import com.dormhub.service.UserService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

@Controller
public class LoginController {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private KonfigurasiRepository konfigurasiRepository;
    
    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Menampilkan halaman login.
     */
    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error, Model model) {
        if (error != null) {
            model.addAttribute("error", "Email atau password salah.");
        }

        Map<String, String> konfigurasi = konfigurasiRepository.findAllAsMap().stream()
            .collect(Collectors.toMap(
                entry -> entry.get("key"),
                entry -> entry.get("value")
            ));

        model.addAttribute("konfigurasi", konfigurasi);

        return "Login";
    }

    /**
     * Proses login pengguna dan pengalihan ke dashboard sesuai level.
     *
     * @param email Email pengguna
     * @param password Password pengguna
     * @param model Model untuk view
     * @param response HttpServletResponse untuk menyimpan cookie JWT
     * @return Redirect ke dashboard sesuai level
     */
    @PostMapping("/login")
    public String login(
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            RedirectAttributes redirectAttributes, 
            Model model,
            HttpServletResponse response) {

        logger.debug("Login attempt dengan email: {}", email);

        System.out.println("Email: " + email); // Debug
        System.out.println("Password: " + password); // Debug

        User user = userService.authenticate(email, password);

        if (user != null) {
            logger.info("Login sukses untuk user dengan email: {}", email);
            
            // Generate JWT token
            String token = jwtUtil.generateToken(email);
            
            // Simpan token dalam cookie
            Cookie jwtCookie = new Cookie("jwt_token", token);
            jwtCookie.setPath("/");
            jwtCookie.setHttpOnly(true);
            jwtCookie.setMaxAge(86400); // 1 hari dalam detik
            response.addCookie(jwtCookie);
            
            String level = user.getLevel().getNama();
            logger.debug("User level: {}", level);

            switch (level.toLowerCase()) {
                case "mahasiswa":
                case "senior residence":
                    return "redirect:/mahasiswa/dashboard";
                case "admin":
                    return "redirect:/admin/dashboard";
                case "help desk":
                    return "redirect:/help-desk/dashboard";
                default:
                    redirectAttributes.addFlashAttribute("error", "Level tidak valid.");
                    return "redirect:/login";
            }
        } else {
            logger.warn("Login gagal untuk email: {}", email);
            redirectAttributes.addFlashAttribute("error", "Email atau password salah.");
            return "redirect:/login";
        }
    }

    /**
     * Proses logout pengguna dan menghapus cookie JWT.
     *
     * @param response HttpServletResponse untuk menghapus cookie JWT
     * @return Redirect ke halaman login
     */
    @GetMapping("/logout")
    public String logout(HttpServletResponse response) {
        // Hapus cookie JWT
        Cookie jwtCookie = new Cookie("jwt_token", null);
        jwtCookie.setPath("/");
        jwtCookie.setHttpOnly(true);
        jwtCookie.setMaxAge(0); // 0 berarti hapus cookie
        response.addCookie(jwtCookie);
        
        logger.info("User berhasil logout");
        return "redirect:/login";
    }
}
