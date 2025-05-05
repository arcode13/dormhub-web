package com.dormhub.api;

import com.dormhub.model.SeniorResidence;
import com.dormhub.model.User;
import com.dormhub.repository.UserRepository;
import com.dormhub.service.SeniorResidenceService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.Map;

@RestController
@RequestMapping("/api/senior-residence")
public class InfoMahasiswaApiController {

    @Autowired
    private SeniorResidenceService seniorResidenceService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/info-mahasiswa")
    public ResponseEntity<?> getInfoMahasiswa() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        Optional<User> userOptional = userRepository.findByEmail(email);
        if (!userOptional.isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "User tidak ditemukan."));
        }

        User user = userOptional.get();
        int userId = user.getId();

        Optional<SeniorResidence> seniorResidenceOptional = seniorResidenceService.getSeniorResidenceByMahasiswaId(userId);
        if (!seniorResidenceOptional.isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Senior Residence tidak ditemukan."));
        }

        SeniorResidence seniorResidence = seniorResidenceOptional.get();
        return ResponseEntity.ok(seniorResidence);
    }
}
