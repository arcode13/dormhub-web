package com.dormhub.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class HomeController {

    private static final Logger logger = LoggerFactory.getLogger(HomeController.class);

    /**
     * Mengarahkan root path ke dashboard sesuai peran pengguna
     */
    @GetMapping("/")
    public String home() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))) {
            // Pengguna sudah login, arahkan berdasarkan peran
            if (auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
                logger.debug("Mengarahkan admin ke dashboard");
                return "redirect:/admin/dashboard";
            } else if (auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_HELP_DESK"))) {
                logger.debug("Mengarahkan help desk ke dashboard");
                return "redirect:/help-desk/dashboard";
            } else if (auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_MAHASISWA")) || 
                      auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_SENIOR_RESIDENCE"))) {
                logger.debug("Mengarahkan mahasiswa/senior residence ke dashboard");
                return "redirect:/mahasiswa/dashboard";
            }
        }
        
        // Belum login, arahkan ke halaman login
        logger.debug("Mengarahkan ke halaman login");
        return "redirect:/login";
    }
}