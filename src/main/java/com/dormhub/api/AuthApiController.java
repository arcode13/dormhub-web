package com.dormhub.api;

import com.dormhub.model.Jurusan;
import com.dormhub.model.User;
import com.dormhub.repository.UserRepository;
import com.dormhub.service.EmailService;
import com.dormhub.service.JurusanService;
import com.dormhub.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JurusanService jurusanService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    // ======= API LOGIN =======
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String password = request.get("password");

        User user = userService.findByEmail(email);
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email atau password salah"));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Login berhasil");
        response.put("user_id", user.getId());
        response.put("email", user.getEmail());
        response.put("level", user.getLevel().getNama());

        return ResponseEntity.ok(response);
    }

    // ======= API REGISTER =======
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, Object> request) {
        String email = (String) request.get("email");
        String password = (String) request.get("password");
        String nama = (String) request.get("nama");
        Integer jurusanId = (Integer) request.get("jurusanId");

        if (userRepository.findByEmail(email).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email sudah digunakan"));
        }

        Jurusan jurusan = jurusanService.findById(jurusanId);
        if (jurusan == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Jurusan tidak ditemukan"));
        }

        User newUser = new User();
        newUser.setEmail(email);
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setNama(nama);
        newUser.setJurusan(jurusan);

        userRepository.save(newUser);

        return ResponseEntity.ok(Map.of("message", "Registrasi berhasil"));
    }

    // ======= API FORGOT PASSWORD =======
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (!userOptional.isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email tidak ditemukan"));
        }

        User user = userOptional.get();
        String resetToken = UUID.randomUUID().toString();
        user.setToken(resetToken);
        userRepository.save(user);

        String resetUrl = "http://localhost:8080/reset-password?token=" + resetToken;

        try {
            emailService.sendResetPasswordEmail(user.getEmail(), resetUrl);
            return ResponseEntity.ok(Map.of("message", "Tautan reset password telah dikirim ke email Anda."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Gagal mengirim email reset password."));
        }
    }

    // ======= API RESET PASSWORD =======
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        String password = request.get("password");
        String confirmPassword = request.get("confirmPassword");

        if (!password.equals(confirmPassword)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Konfirmasi password tidak cocok."));
        }

        Optional<User> userOptional = userRepository.findByToken(token);

        if (!userOptional.isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Token tidak valid atau sudah digunakan."));
        }

        User user = userOptional.get();
        user.setPassword(passwordEncoder.encode(password));
        user.setToken(null); // Hapus token setelah reset berhasil
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Password berhasil diubah."));
    }
}