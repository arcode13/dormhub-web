package com.dormhub.controller;

import com.dormhub.model.Jurusan;
import com.dormhub.model.Level;
import com.dormhub.model.Mahasiswa;
import com.dormhub.model.User;
import com.dormhub.repository.KonfigurasiRepository;
import com.dormhub.repository.UserRepository;
import com.dormhub.service.JurusanService;
import com.dormhub.service.MahasiswaService;
import com.dormhub.service.RoomService;
import com.dormhub.service.UserService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.io.IOException;
import java.util.UUID;

@Controller
public class MahasiswaController {

    private static final Logger logger = LoggerFactory.getLogger(MahasiswaController.class);

    @Autowired
    private MahasiswaService mahasiswaService;
    
    @Autowired
    private JurusanService jurusanService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private KonfigurasiRepository konfigurasiRepository;
    
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;
    
    @Autowired
    private RoomService roomService;

    @GetMapping("/admin/mahasiswa")
    public String daftarMahasiswa(Principal principal, Model model) {
        // Ambil data user yang login
        String email = principal.getName();
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User tidak ditemukan."));
        
        // Ambil data konfigurasi
        Map<String, String> konfigurasi = konfigurasiRepository.findAllAsMap().stream()
            .collect(Collectors.toMap(
                entry -> entry.get("key"),
                entry -> entry.get("value")
            ));
        
        model.addAttribute("konfigurasi", konfigurasi);
        model.addAttribute("user", user);
        model.addAttribute("currentTimeMillis", System.currentTimeMillis());

        List<Mahasiswa> mahasiswaList = mahasiswaService.getAllMahasiswa();
        model.addAttribute("mahasiswaList", mahasiswaList);
        return "admin/Mahasiswa/index";
    }
    
    @GetMapping("/admin/mahasiswa/tambah")
    public String tambahMahasiswaPage(Principal principal, Model model) {
        // Ambil data user yang login
        String email = principal.getName();
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User tidak ditemukan."));
        
        // Ambil data konfigurasi
        Map<String, String> konfigurasi = konfigurasiRepository.findAllAsMap().stream()
            .collect(Collectors.toMap(
                entry -> entry.get("key"),
                entry -> entry.get("value")
            ));
        
        model.addAttribute("konfigurasi", konfigurasi);
        model.addAttribute("user", user);
        model.addAttribute("currentTimeMillis", System.currentTimeMillis());
        
        // Ambil data jurusan untuk dropdown
        List<Jurusan> jurusanList = jurusanService.getAllJurusan();
        model.addAttribute("jurusanList", jurusanList);
        
        return "admin/Mahasiswa/tambah";
    }
    
    @PostMapping("/admin/mahasiswa/tambah")
    public String saveMahasiswa(
            @RequestParam("namaLengkap") String namaLengkap,
            @RequestParam("email") String email,
            @RequestParam("nomorHp") String nomorHp,
            @RequestParam("password") String password,
            @RequestParam("jurusanId") int jurusanId,
            @RequestParam("jenisKelamin") String jenisKelamin,
            @RequestParam(value = "image", required = false) MultipartFile imageFile,
            RedirectAttributes redirectAttributes,
            Principal principal, 
            Model model) {
        
        try {
            // Validasi input
            if (namaLengkap == null || namaLengkap.trim().isEmpty() || 
                email == null || email.trim().isEmpty() ||
                nomorHp == null || nomorHp.trim().isEmpty() ||
                password == null || password.trim().isEmpty()) {
                
                // Ambil data user yang login
                String currentUserEmail = principal.getName();
                User currentUser = userRepository.findByEmail(currentUserEmail).orElseThrow(() -> new RuntimeException("User tidak ditemukan."));
                
                // Ambil data konfigurasi
                Map<String, String> konfigurasi = konfigurasiRepository.findAllAsMap().stream()
                    .collect(Collectors.toMap(
                        entry -> entry.get("key"),
                        entry -> entry.get("value")
                    ));
                
                model.addAttribute("konfigurasi", konfigurasi);
                model.addAttribute("user", currentUser);
                model.addAttribute("currentTimeMillis", System.currentTimeMillis());
                
                // Ambil data jurusan untuk dropdown
                List<Jurusan> jurusanList = jurusanService.getAllJurusan();
                model.addAttribute("jurusanList", jurusanList);
                
                model.addAttribute("error", "Semua field harus diisi");
                return "admin/Mahasiswa/tambah";
            }
            
            // Cek apakah email sudah terdaftar
            if (userRepository.existsByEmail(email)) {
                // Ambil data user yang login
                String currentUserEmail = principal.getName();
                User currentUser = userRepository.findByEmail(currentUserEmail).orElseThrow(() -> new RuntimeException("User tidak ditemukan."));
                
                // Ambil data konfigurasi
                Map<String, String> konfigurasi = konfigurasiRepository.findAllAsMap().stream()
                    .collect(Collectors.toMap(
                        entry -> entry.get("key"),
                        entry -> entry.get("value")
                    ));
                
                model.addAttribute("konfigurasi", konfigurasi);
                model.addAttribute("user", currentUser);
                model.addAttribute("currentTimeMillis", System.currentTimeMillis());
                
                // Ambil data jurusan untuk dropdown
                List<Jurusan> jurusanList = jurusanService.getAllJurusan();
                model.addAttribute("jurusanList", jurusanList);
                
                model.addAttribute("error", "Email sudah terdaftar");
                return "admin/Mahasiswa/tambah";
            }
            
            // Ambil data jurusan
            Jurusan jurusan = jurusanService.findById(jurusanId);
            if (jurusan == null) {
                throw new RuntimeException("Jurusan tidak ditemukan");
            }
            
            // Buat user baru
            User newUser = new User();
            newUser.setNamaLengkap(namaLengkap);
            newUser.setEmail(email);
            newUser.setNomorHp(nomorHp);
            newUser.setPassword(passwordEncoder.encode(password));
            newUser.setJenisKelamin(jenisKelamin);
            
            // Set level mahasiswa
            Level level = new Level();
            level.setId(1); // ID 1 untuk Mahasiswa
            newUser.setLevel(level);
            
            // Upload foto profil jika ada
            if (imageFile != null && !imageFile.isEmpty()) {
                String fileName = UUID.randomUUID().toString() + "_" + imageFile.getOriginalFilename();
                Path path = Paths.get("src/main/resources/static/assets/images/users/" + fileName);
                Files.copy(imageFile.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
                newUser.setFotoProfil(fileName);
            }
            
            newUser.setCreatedAt(LocalDateTime.now());
            newUser.setUpdatedAt(LocalDateTime.now());
            
            // Simpan user
            User savedUser = userRepository.save(newUser);
            
            // Dapatkan nomor kamar dan kasur dari RoomService
            int[] roomAndBed = roomService.assignRoom();
            
            // Buat entitas Mahasiswa
            Mahasiswa mahasiswa = new Mahasiswa();
            mahasiswa.setUser(savedUser);
            mahasiswa.setJurusan(jurusan);
            mahasiswa.setNoKamar(roomAndBed[0]);
            mahasiswa.setNoKasur(roomAndBed[1]);
            mahasiswa.setIsCheckin(0);
            mahasiswa.setIsCheckout(0);
            
            // Simpan mahasiswa
            mahasiswaService.saveMahasiswa(mahasiswa);
            
            redirectAttributes.addFlashAttribute("success", "Mahasiswa berhasil ditambahkan");
            return "redirect:/admin/mahasiswa";
            
        } catch (Exception e) {
            logger.error("Terjadi kesalahan saat menambah mahasiswa: ", e);
            redirectAttributes.addFlashAttribute("error", "Terjadi kesalahan: " + e.getMessage());
            return "redirect:/admin/mahasiswa/tambah";
        }
    }
    
    @GetMapping("/admin/mahasiswa/edit/{id}")
    public String editMahasiswaPage(@PathVariable int id, Principal principal, Model model, RedirectAttributes redirectAttributes) {
        try {
            // Ambil data user yang login
            String email = principal.getName();
            User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User tidak ditemukan."));
            
            // Ambil data konfigurasi
            Map<String, String> konfigurasi = konfigurasiRepository.findAllAsMap().stream()
                .collect(Collectors.toMap(
                    entry -> entry.get("key"),
                    entry -> entry.get("value")
                ));
            
            model.addAttribute("konfigurasi", konfigurasi);
            model.addAttribute("user", user);
            model.addAttribute("currentTimeMillis", System.currentTimeMillis());
            
            // Ambil data mahasiswa
            Mahasiswa mahasiswa = mahasiswaService.findById(id);
            if (mahasiswa == null) {
                redirectAttributes.addFlashAttribute("error", "Mahasiswa tidak ditemukan");
                return "redirect:/admin/mahasiswa";
            }
            
            model.addAttribute("mahasiswa", mahasiswa);
            
            // Ambil data jurusan untuk dropdown
            List<Jurusan> jurusanList = jurusanService.getAllJurusan();
            model.addAttribute("jurusanList", jurusanList);
            
            return "admin/Mahasiswa/edit";
        } catch (Exception e) {
            logger.error("Terjadi kesalahan saat menampilkan halaman edit mahasiswa: ", e);
            redirectAttributes.addFlashAttribute("error", "Terjadi kesalahan: " + e.getMessage());
            return "redirect:/admin/mahasiswa";
        }
    }
    
    @PostMapping("/admin/mahasiswa/edit/{id}")
    public String updateMahasiswa(
            @PathVariable int id,
            @RequestParam("namaLengkap") String namaLengkap,
            @RequestParam("email") String email,
            @RequestParam("nomorHp") String nomorHp,
            @RequestParam(value = "password", required = false) String password,
            @RequestParam("jurusanId") int jurusanId,
            @RequestParam("noKamar") int noKamar,
            @RequestParam("noKasur") int noKasur,
            @RequestParam("jenisKelamin") String jenisKelamin,
            @RequestParam(value = "image", required = false) MultipartFile imageFile,
            RedirectAttributes redirectAttributes,
            Principal principal, 
            Model model) {
        
        try {
            // Validasi input
            if (namaLengkap == null || namaLengkap.trim().isEmpty() || 
                email == null || email.trim().isEmpty() ||
                nomorHp == null || nomorHp.trim().isEmpty()) {
                
                // Ambil data user yang login
                String currentUserEmail = principal.getName();
                User currentUser = userRepository.findByEmail(currentUserEmail).orElseThrow(() -> new RuntimeException("User tidak ditemukan."));
                
                // Ambil data konfigurasi
                Map<String, String> konfigurasi = konfigurasiRepository.findAllAsMap().stream()
                    .collect(Collectors.toMap(
                        entry -> entry.get("key"),
                        entry -> entry.get("value")
                    ));
                
                model.addAttribute("konfigurasi", konfigurasi);
                model.addAttribute("user", currentUser);
                model.addAttribute("currentTimeMillis", System.currentTimeMillis());
                
                // Ambil data mahasiswa
                Mahasiswa mahasiswa = mahasiswaService.findById(id);
                model.addAttribute("mahasiswa", mahasiswa);
                
                // Ambil data jurusan untuk dropdown
                List<Jurusan> jurusanList = jurusanService.getAllJurusan();
                model.addAttribute("jurusanList", jurusanList);
                
                model.addAttribute("error", "Nama Lengkap, Email, dan Nomor HP harus diisi");
                return "admin/Mahasiswa/edit";
            }
            
            // Ambil data mahasiswa
            Mahasiswa mahasiswa = mahasiswaService.findById(id);
            if (mahasiswa == null) {
                redirectAttributes.addFlashAttribute("error", "Mahasiswa tidak ditemukan");
                return "redirect:/admin/mahasiswa";
            }
            
            // Cek apakah email sudah terdaftar oleh user lain
            User existingUser = userRepository.findByEmail(email).orElse(null);
            if (existingUser != null && existingUser.getId() != mahasiswa.getUser().getId()) {
                // Ambil data user yang login
                String currentUserEmail = principal.getName();
                User currentUser = userRepository.findByEmail(currentUserEmail).orElseThrow(() -> new RuntimeException("User tidak ditemukan."));
                
                // Ambil data konfigurasi
                Map<String, String> konfigurasi = konfigurasiRepository.findAllAsMap().stream()
                    .collect(Collectors.toMap(
                        entry -> entry.get("key"),
                        entry -> entry.get("value")
                    ));
                
                model.addAttribute("konfigurasi", konfigurasi);
                model.addAttribute("user", currentUser);
                model.addAttribute("currentTimeMillis", System.currentTimeMillis());
                
                model.addAttribute("mahasiswa", mahasiswa);
                
                // Ambil data jurusan untuk dropdown
                List<Jurusan> jurusanList = jurusanService.getAllJurusan();
                model.addAttribute("jurusanList", jurusanList);
                
                model.addAttribute("error", "Email sudah terdaftar oleh pengguna lain");
                return "admin/Mahasiswa/edit";
            }
            
            // Ambil data jurusan
            Jurusan jurusan = jurusanService.findById(jurusanId);
            if (jurusan == null) {
                throw new RuntimeException("Jurusan tidak ditemukan");
            }
            
            // Update data user
            User userToUpdate = mahasiswa.getUser();
            userToUpdate.setNamaLengkap(namaLengkap);
            userToUpdate.setEmail(email);
            userToUpdate.setNomorHp(nomorHp);
            userToUpdate.setJenisKelamin(jenisKelamin);
            
            // Update password jika diisi
            if (password != null && !password.trim().isEmpty()) {
                userToUpdate.setPassword(passwordEncoder.encode(password));
            }
            
            // Upload foto profil jika ada
            if (imageFile != null && !imageFile.isEmpty()) {
                String fileName = UUID.randomUUID().toString() + "_" + imageFile.getOriginalFilename();
                Path path = Paths.get("src/main/resources/static/assets/images/users/" + fileName);
                Files.copy(imageFile.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
                userToUpdate.setFotoProfil(fileName);
            }
            
            userToUpdate.setUpdatedAt(LocalDateTime.now());
            
            // Simpan user
            userRepository.save(userToUpdate);
            
            // Update data mahasiswa
            mahasiswa.setJurusan(jurusan);
            mahasiswa.setNoKamar(noKamar);
            mahasiswa.setNoKasur(noKasur);
            
            // Simpan mahasiswa
            mahasiswaService.saveMahasiswa(mahasiswa);
            
            redirectAttributes.addFlashAttribute("success", "Mahasiswa berhasil diupdate");
            return "redirect:/admin/mahasiswa";
            
        } catch (Exception e) {
            logger.error("Terjadi kesalahan saat mengupdate mahasiswa: ", e);
            redirectAttributes.addFlashAttribute("error", "Terjadi kesalahan: " + e.getMessage());
            return "redirect:/admin/mahasiswa/edit/" + id;
        }
    }
    
    @GetMapping("/admin/mahasiswa/delete/{id}")
    public String deleteMahasiswa(@PathVariable int id, RedirectAttributes redirectAttributes) {
        try {
            // Ambil data mahasiswa
            Mahasiswa mahasiswa = mahasiswaService.findById(id);
            if (mahasiswa == null) {
                redirectAttributes.addFlashAttribute("error", "Mahasiswa tidak ditemukan");
                return "redirect:/admin/mahasiswa";
            }
            
            // Hapus mahasiswa terlebih dahulu (karena ada foreign key constraint)
            mahasiswaService.deleteMahasiswa(id);
            
            // Hapus user
            userRepository.deleteById(mahasiswa.getUser().getId());
            
            redirectAttributes.addFlashAttribute("success", "Mahasiswa berhasil dihapus");
            return "redirect:/admin/mahasiswa";
            
        } catch (Exception e) {
            logger.error("Terjadi kesalahan saat menghapus mahasiswa: ", e);
            redirectAttributes.addFlashAttribute("error", "Terjadi kesalahan: " + e.getMessage());
            return "redirect:/admin/mahasiswa";
        }
    }
}
