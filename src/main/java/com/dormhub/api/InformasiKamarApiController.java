package com.dormhub.api;

import com.dormhub.model.Mahasiswa;
import com.dormhub.model.User;
import com.dormhub.repository.MahasiswaRepository;
import com.dormhub.repository.SeniorResidenceRepository;
import com.dormhub.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/mahasiswa")
public class InformasiKamarApiController {

    @Autowired
    private MahasiswaRepository mahasiswaRepository;

    @Autowired
    private SeniorResidenceRepository seniorResidenceRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/informasi-kamar")
    public ResponseEntity<?> getInformasiKamar() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "User tidak ditemukan."));
        }

        User user = userOptional.get();
        Optional<Mahasiswa> mahasiswaOptional = mahasiswaRepository.findByUserId(user.getId());
        if (mahasiswaOptional.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Mahasiswa tidak ditemukan."));
        }

        Mahasiswa mahasiswa = mahasiswaOptional.get();
        boolean isSeniorResidence = seniorResidenceRepository.existsByMahasiswaId(mahasiswa.getId());
        int noKamar = mahasiswa.getNoKamar();

        List<Mahasiswa> mahasiswaSekamar = mahasiswaRepository.findByNoKamarWithUser(noKamar);

        return ResponseEntity.ok(Map.of(
            "user", user,
            "isSeniorResidence", isSeniorResidence,
            "isCheckin", mahasiswa.getIsCheckin() == 1,
            "isCheckout", mahasiswa.getIsCheckout() == 1,
            "mahasiswaSekamar", mahasiswaSekamar,
            "noKamar", noKamar
        ));
    }
}
