package com.dormhub.api;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dormhub.repository.KonfigurasiRepository;

@RestController
@RequestMapping("/api")
public class HomeApiController {

    @Autowired
    private KonfigurasiRepository konfigurasiRepository;

    @GetMapping("/home")
    public ResponseEntity<Map<String, String>> getHomeConfig() {
        Map<String, String> konfigurasi = konfigurasiRepository.findAllAsMap().stream()
            .collect(Collectors.toMap(
                entry -> entry.get("key"),
                entry -> entry.get("value")
            ));

        return ResponseEntity.ok(konfigurasi);
    }
}
