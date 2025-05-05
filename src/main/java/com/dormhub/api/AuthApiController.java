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
import com.dormhub.security.JwtUtil;

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

    private final JwtUtil jwtUtil;

    public AuthApiController(UserService userService,
                            UserRepository userRepository,
                            JurusanService jurusanService,
                            EmailService emailService,
                            BCryptPasswordEncoder passwordEncoder,
                            JwtUtil jwtUtil) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.jurusanService = jurusanService;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    // ======= API LOGIN =======
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String password = request.get("password");

        User user = userService.authenticate(email, password);
        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email atau password salah"));
        }

        String token = jwtUtil.generateToken(email);

        return ResponseEntity.ok(Map.of(
            "message", "Login berhasil",
            "token", token,
            "email", user.getEmail(),
            "level", user.getLevel().getNama(),
            "user_id", user.getId()
        ));
    }

    // ======= API REGISTER =======
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, Object> request) {
        try {
            String email = (String) request.get("email");
            String password = (String) request.get("password");
            String nama = (String) request.get("nama");
            String nomorHp = (String) request.get("nomorHp");
            String jenisKelamin = (String) request.get("jenisKelamin");
            Integer jurusanId = (Integer) request.get("jurusanId");

            Jurusan jurusan = jurusanService.findById(jurusanId);
            if (jurusan == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Jurusan tidak ditemukan"));
            }

            User newUser = new User();
            newUser.setEmail(email);
            newUser.setPassword(password);
            newUser.setNamaLengkap(nama);
            newUser.setNomorHp(nomorHp);
            newUser.setJenisKelamin(jenisKelamin);

            String result = userService.registerUser(newUser, jurusan);

            if (result.equals("Berhasil mendaftar")) {
                return ResponseEntity.ok(Map.of("message", "Registrasi berhasil"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", result));
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Terjadi kesalahan: " + e.getMessage()));
        }
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
        user.setToken(null);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Password berhasil diubah."));
    }
}