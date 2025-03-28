package com.dormhub.controller;

import com.dormhub.model.Jurusan;
import com.dormhub.model.User;
import com.dormhub.repository.KonfigurasiRepository;
import com.dormhub.service.JurusanService;
import com.dormhub.service.UserService;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class RegisterController {

    @Autowired
    private UserService userService;

    @Autowired
    private JurusanService jurusanService;

    @Autowired
    private KonfigurasiRepository konfigurasiRepository;

    @GetMapping("/register")
    public String daftarPage(Model model) {
        Map<String, String> konfigurasi = konfigurasiRepository.findAllAsMap().stream()
            .collect(Collectors.toMap(
                entry -> entry.get("key"),
                entry -> entry.get("value")
            ));

        model.addAttribute("konfigurasi", konfigurasi);

        model.addAttribute("user", new User());
        model.addAttribute("jurusanList", jurusanService.getAllJurusan());
        return "Register";
    }

    @PostMapping("/register")
    public String daftarUser(
            @ModelAttribute User user,
            @RequestParam("jurusanId") int jurusanId,
            RedirectAttributes redirectAttributes, 
            Model model) {
        
        Jurusan jurusan = jurusanService.findById(jurusanId);
        if (jurusan == null) {
            model.addAttribute("error", "Jurusan tidak ditemukan.");
            model.addAttribute("user", user);
            model.addAttribute("jurusanList", jurusanService.getAllJurusan());
            return "register";
        }
    
        String result = userService.registerUser(user, jurusan);
        if (result.equals("Berhasil mendaftar")) {
            redirectAttributes.addFlashAttribute("success", "Daftar akun berhasil.");
            return "redirect:/login";
        } else {
            model.addAttribute("error", result);
            model.addAttribute("user", user); 
            model.addAttribute("jurusanList", jurusanService.getAllJurusan()); 
            model.addAttribute("selectedJurusan", jurusanId); 
            return "register";
        }
    }    
    
}
