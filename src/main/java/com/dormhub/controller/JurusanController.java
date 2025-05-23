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

    @PostMapping("/edit/{id}")
    public String updateJurusan(@PathVariable int id, @ModelAttribute Jurusan updatedJurusan, 
                               RedirectAttributes redirectAttributes, Model model, Principal principal) {
        // Validasi input kosong
        if (updatedJurusan.getNama() == null || updatedJurusan.getNama().trim().isEmpty()) {
            String email = principal.getName();
            User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User tidak ditemukan."));
            
            Map<String, String> konfigurasi = konfigurasiRepository.findAllAsMap().stream()
                .collect(Collectors.toMap(
                    entry -> entry.get("key"),
                    entry -> entry.get("value")
                ));

            model.addAttribute("konfigurasi", konfigurasi);
            model.addAttribute("user", user);
            model.addAttribute("jurusan", updatedJurusan);
            model.addAttribute("error", "Nama jurusan tidak boleh kosong");
            return "admin/Jurusan/edit";
        }

        Jurusan jurusan = jurusanService.findById(id);
        if (jurusan != null) {
            jurusan.setNama(updatedJurusan.getNama());
            jurusanService.saveJurusan(jurusan);
            redirectAttributes.addFlashAttribute("success", "Jurusan berhasil diperbarui");
        } else {
            redirectAttributes.addFlashAttribute("error", "Jurusan tidak ditemukan");
        }
        return "redirect:/admin/jurusan";
    }

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

    @PostMapping("/tambah")
    public String saveJurusan(@ModelAttribute Jurusan jurusan, RedirectAttributes redirectAttributes, Model model, Principal principal) {
        // Validasi input kosong
        if (jurusan.getNama() == null || jurusan.getNama().trim().isEmpty()) {
            String email = principal.getName();
            User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User tidak ditemukan."));
            
            Map<String, String> konfigurasi = konfigurasiRepository.findAllAsMap().stream()
                .collect(Collectors.toMap(
                    entry -> entry.get("key"),
                    entry -> entry.get("value")
                ));

            model.addAttribute("konfigurasi", konfigurasi);
            model.addAttribute("user", user);
            model.addAttribute("error", "Nama jurusan tidak boleh kosong");
            return "admin/Jurusan/tambah";
        }

        jurusanService.saveJurusan(jurusan);
        redirectAttributes.addFlashAttribute("success", "Jurusan berhasil ditambah");
        return "redirect:/admin/jurusan";
    }

    @GetMapping("/delete/{id}")
    public String deleteJurusan(@PathVariable int id, RedirectAttributes redirectAttributes) {
        try {
            Jurusan jurusan = jurusanService.findById(id);
            if (jurusan != null) {
                jurusanService.deleteJurusan(id);
                redirectAttributes.addFlashAttribute("success", "Jurusan berhasil dihapus");
            } else {
                redirectAttributes.addFlashAttribute("error", "Jurusan tidak ditemukan");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Tidak dapat menghapus jurusan karena sedang digunakan");
        }
        return "redirect:/admin/jurusan";
    }
}
