package com.dormhub.controller;

import com.dormhub.model.Mahasiswa;
import com.dormhub.repository.KonfigurasiRepository;
import com.dormhub.repository.MahasiswaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class ApiController {

    @Autowired
    private MahasiswaRepository mahasiswaRepository;

    @Autowired
    private KonfigurasiRepository konfigurasiRepository;

    @GetMapping("/api/konfigurasi")
    public ResponseEntity<?> getKonfigurasi() {
        try {
            List<Map<String, String>> konfigurasi = konfigurasiRepository.findAllAsMap();
            return ResponseEntity.ok(konfigurasi);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body("Terjadi kesalahan saat memuat konfigurasi : " + e.getMessage());
        }
    }

    @GetMapping("/api/mahasiswa")
    public List<Mahasiswa> getMahasiswaByKamar(@RequestParam("noKamar") int noKamar) {
        return mahasiswaRepository.findByNoKamarWithUser(noKamar);
    }
}
