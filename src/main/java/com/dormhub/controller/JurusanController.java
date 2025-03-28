package com.dormhub.controller;

import com.dormhub.model.Jurusan;
import com.dormhub.model.User;
import com.dormhub.repository.KonfigurasiRepository;
import com.dormhub.repository.UserRepository;
import com.dormhub.service.JurusanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/jurusan")
public class JurusanController {

    @Autowired
    private JurusanService jurusanService;

    @Autowired
    private KonfigurasiRepository konfigurasiRepository;

    @Autowired
    private UserRepository userRepository;

    // Menampilkan halaman daftar jurusan
    @GetMapping
    public String getAllJurusan(Principal principal, Model model) {
        String email = principal.getName();
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User tidak ditemukan."));
    
        Map<String, String> konfigurasi = konfigurasiRepository.findAllAsMap().stream()
            .collect(Collectors.toMap(
                entry -> entry.get("key"),
                entry -> entry.get("value")
            ));

        model.addAttribute("konfigurasi", konfigurasi);

        model.addAttribute("user", user);
        model.addAttribute("currentTimeMillis", System.currentTimeMillis());

        List<Jurusan> jurusanList = jurusanService.getAllJurusan();
        model.addAttribute("jurusanList", jurusanList); 
        return "admin/Jurusan/index"; 
    }

    // Menampilkan halaman edit jurusan
    @GetMapping("/edit/{id}")
    public String editJurusanPage(@PathVariable int id, Principal principal, Model model) {
        String email = principal.getName();
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User tidak ditemukan."));
    
        Map<String, String> konfigurasi = konfigurasiRepository.findAllAsMap().stream()
            .collect(Collectors.toMap(
                entry -> entry.get("key"),
                entry -> entry.get("value")
            ));

        model.addAttribute("konfigurasi", konfigurasi);

        model.addAttribute("user", user);

        Jurusan jurusan = jurusanService.findById(id);
        if (jurusan == null) {
            return "redirect:/admin/jurusan"; 
        }
        model.addAttribute("jurusan", jurusan);
        return "admin/Jurusan/edit"; 
    }

    // Menyimpan hasil edit jurusan
    @PostMapping("/edit/{id}")
    public String updateJurusan(@PathVariable int id, @ModelAttribute Jurusan updatedJurusan) {
        Jurusan jurusan = jurusanService.findById(id);
        if (jurusan != null) {
            jurusan.setNama(updatedJurusan.getNama());
            jurusanService.saveJurusan(jurusan);
        }
        return "redirect:/admin/jurusan"; 
    }

    // Menampilkan halaman tambah jurusan
    @GetMapping("/tambah")
    public String tambahJurusanPage(Principal principal, Model model) {
        String email = principal.getName();
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User tidak ditemukan."));
    
        Map<String, String> konfigurasi = konfigurasiRepository.findAllAsMap().stream()
            .collect(Collectors.toMap(
                entry -> entry.get("key"),
                entry -> entry.get("value")
            ));

        model.addAttribute("konfigurasi", konfigurasi);

        model.addAttribute("user", user);
        
        model.addAttribute("jurusan", new Jurusan());
        return "admin/Jurusan/tambah"; 
    }

    // Menyimpan jurusan baru
    @PostMapping("/tambah")
    public String saveJurusan(@ModelAttribute Jurusan jurusan, RedirectAttributes redirectAttributes) {
        jurusanService.saveJurusan(jurusan);

        redirectAttributes.addFlashAttribute("success", "Jurusan berhasil ditambah");
        return "redirect:/admin/jurusan"; 
    }

    // Menghapus jurusan
    @GetMapping("/delete/{id}")
    public String deleteJurusan(@PathVariable int id) {
        jurusanService.deleteJurusan(id);
        return "redirect:/admin/jurusan"; 
    }
}
