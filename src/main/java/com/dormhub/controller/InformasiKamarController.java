package com.dormhub.controller;

import com.dormhub.model.Mahasiswa;
import com.dormhub.model.User;
import com.dormhub.repository.KonfigurasiRepository;
import com.dormhub.repository.MahasiswaRepository;
import com.dormhub.repository.SeniorResidenceRepository;
import com.dormhub.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class InformasiKamarController {

    @Autowired
    private MahasiswaRepository mahasiswaRepository;

    @Autowired
    private SeniorResidenceRepository seniorResidenceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private KonfigurasiRepository konfigurasiRepository;

    @GetMapping("/mahasiswa/informasi-kamar")
    public String informasiKamar(Model model, RedirectAttributes redirectAttributes) {
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            
            Optional<Mahasiswa> mahasiswaOptional = mahasiswaRepository.findByUserId(user.getId());
            if (mahasiswaOptional.isPresent()) {
                Mahasiswa mahasiswa = mahasiswaOptional.get();
                boolean isSeniorResidence = seniorResidenceRepository.existsByMahasiswaId(mahasiswa.getId());

                int noKamar = mahasiswa.getNoKamar();
                
                List<Mahasiswa> mahasiswaSekamar = mahasiswaRepository.findByNoKamarWithUser(noKamar);
                
                Map<String, String> konfigurasi = konfigurasiRepository.findAllAsMap().stream()
                    .collect(Collectors.toMap(
                        entry -> entry.get("key"),
                        entry -> entry.get("value")
                    ));

                model.addAttribute("konfigurasi", konfigurasi);

                model.addAttribute("isSeniorResidence", isSeniorResidence);
                model.addAttribute("isCheckin", mahasiswa.getIsCheckin() == 1);
                model.addAttribute("isCheckout", mahasiswa.getIsCheckout() == 1);
                model.addAttribute("user", user);
                model.addAttribute("mahasiswaSekamar", mahasiswaSekamar);
                model.addAttribute("noKamar", mahasiswa.getNoKamar());
            } else {
                redirectAttributes.addFlashAttribute("error", "Mahasiswa tidak ditemukan.");
            }
        } else {
            redirectAttributes.addFlashAttribute("error", "User tidak ditemukan.");
        }

        return "mahasiswa/InformasiKamar";
    }
}
