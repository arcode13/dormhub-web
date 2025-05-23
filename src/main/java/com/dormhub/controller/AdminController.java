package com.dormhub.controller;

import com.dormhub.model.Admin;
import com.dormhub.model.Level;
import com.dormhub.model.User;
import com.dormhub.repository.AdminRepository;
import com.dormhub.repository.KonfigurasiRepository;
import com.dormhub.repository.LevelRepository;
import com.dormhub.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
public class AdminController {

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LevelRepository levelRepository;

    @Autowired
    private KonfigurasiRepository konfigurasiRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @GetMapping("/admin/admin")
    public String listAdmin(Model model, RedirectAttributes redirectAttributes) {
        List<Admin> adminList = adminRepository.findAll();
        model.addAttribute("adminList", adminList);

        Map<String, String> konfigurasi = konfigurasiRepository.findAllAsMap().stream()
                .collect(Collectors.toMap(
                        entry -> entry.get("key"),
                        entry -> entry.get("value")
                ));
        model.addAttribute("konfigurasi", konfigurasi);

        // Add current time for cache busting
        model.addAttribute("currentTimeMillis", System.currentTimeMillis());

        // Get logged in user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            model.addAttribute("user", user);
        } else {
            redirectAttributes.addFlashAttribute("error", "User tidak ditemukan");
            return "redirect:/logout";
        }

        return "admin/Admin/index";
    }

    @GetMapping("/admin/admin/tambah")
    public String showAddForm(Model model, RedirectAttributes redirectAttributes) {
        Map<String, String> konfigurasi = konfigurasiRepository.findAllAsMap().stream()
                .collect(Collectors.toMap(
                        entry -> entry.get("key"),
                        entry -> entry.get("value")
                ));
        model.addAttribute("konfigurasi", konfigurasi);
        model.addAttribute("currentTimeMillis", System.currentTimeMillis());

        // Get logged in user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            model.addAttribute("user", user);
        } else {
            redirectAttributes.addFlashAttribute("error", "User tidak ditemukan");
            return "redirect:/logout";
        }

        return "admin/Admin/tambah";
    }

    @PostMapping("/admin/admin/tambah")
    public String addAdmin(
            @RequestParam("namaLengkap") String namaLengkap,
            @RequestParam("email") String email,
            @RequestParam("nomorHp") String nomorHp,
            @RequestParam("password") String password,
            @RequestParam("jenisKelamin") String jenisKelamin,
            @RequestParam(value = "image", required = false) MultipartFile image,
            RedirectAttributes redirectAttributes) {

        try {
            // Check if email already exists
            if (userRepository.findByEmail(email).isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Email sudah terdaftar");
                return "redirect:/admin/admin/tambah";
            }

            // Create new user
            User user = new User();
            user.setNamaLengkap(namaLengkap);
            user.setEmail(email);
            user.setNomorHp(nomorHp);
            user.setPassword(passwordEncoder.encode(password));
            user.setJenisKelamin(jenisKelamin);
            
            // Set created_at and updated_at timestamps
            LocalDateTime now = LocalDateTime.now();
            user.setCreatedAt(now);
            user.setUpdatedAt(now);

            // Find Admin level
            Optional<Level> adminLevelOpt = levelRepository.findByNama("Admin");
            if (!adminLevelOpt.isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Level Admin tidak ditemukan");
                return "redirect:/admin/admin/tambah";
            }

            Level adminLevel = adminLevelOpt.get();
            user.setLevel(adminLevel);

            // Handle profile image upload
            if (image != null && !image.isEmpty()) {
                String originalFilename = StringUtils.cleanPath(image.getOriginalFilename());
                // Generate unique filename to prevent overwriting
                String uniqueFilename = UUID.randomUUID().toString() + "_" + originalFilename;
                
                // Set the upload path
                Path uploadPath = Paths.get("src/main/resources/static/assets/images/users");
                
                // Ensure directory exists
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }
                
                // Save the file
                Path destinationFile = uploadPath.resolve(uniqueFilename);
                Files.copy(image.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);
                
                // Set the profile image in user
                user.setFotoProfil(uniqueFilename);
            }

            // Save user
            userRepository.save(user);

            // Create Admin record
            Admin admin = new Admin();
            admin.setUser(user);
            adminRepository.save(admin);

            redirectAttributes.addFlashAttribute("success", "Admin berhasil ditambahkan");
            return "redirect:/admin/admin";
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "Gagal mengunggah foto profil: " + e.getMessage());
            return "redirect:/admin/admin/tambah";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Gagal menambahkan Admin: " + e.getMessage());
            return "redirect:/admin/admin/tambah";
        }
    }

    @GetMapping("/admin/admin/edit/{id}")
    public String showEditForm(@PathVariable("id") int id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Admin> adminOpt = adminRepository.findById(id);
        if (!adminOpt.isPresent()) {
            redirectAttributes.addFlashAttribute("error", "Admin tidak ditemukan");
            return "redirect:/admin/admin";
        }

        Admin admin = adminOpt.get();
        model.addAttribute("admin", admin);

        Map<String, String> konfigurasi = konfigurasiRepository.findAllAsMap().stream()
                .collect(Collectors.toMap(
                        entry -> entry.get("key"),
                        entry -> entry.get("value")
                ));
        model.addAttribute("konfigurasi", konfigurasi);
        model.addAttribute("currentTimeMillis", System.currentTimeMillis());

        // Get logged in user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            model.addAttribute("user", user);
        } else {
            redirectAttributes.addFlashAttribute("error", "User tidak ditemukan");
            return "redirect:/logout";
        }

        return "admin/Admin/edit";
    }

    @PostMapping("/admin/admin/edit/{id}")
    public String updateAdmin(
            @PathVariable("id") int id,
            @RequestParam("namaLengkap") String namaLengkap,
            @RequestParam("email") String email,
            @RequestParam("nomorHp") String nomorHp,
            @RequestParam(value = "password", required = false) String password,
            @RequestParam("jenisKelamin") String jenisKelamin,
            @RequestParam(value = "image", required = false) MultipartFile image,
            RedirectAttributes redirectAttributes) {

        try {
            Optional<Admin> adminOpt = adminRepository.findById(id);
            if (!adminOpt.isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Admin tidak ditemukan");
                return "redirect:/admin/admin";
            }

            Admin admin = adminOpt.get();
            User user = admin.getUser();

            // Check if email already exists for other users
            Optional<User> existingUserOpt = userRepository.findByEmail(email);
            if (existingUserOpt.isPresent() && existingUserOpt.get().getId() != user.getId()) {
                redirectAttributes.addFlashAttribute("error", "Email sudah digunakan oleh pengguna lain");
                return "redirect:/admin/admin/edit/" + id;
            }

            // Update user details
            user.setNamaLengkap(namaLengkap);
            user.setEmail(email);
            user.setNomorHp(nomorHp);
            user.setJenisKelamin(jenisKelamin);
            user.setUpdatedAt(LocalDateTime.now());

            // Update password if provided
            if (password != null && !password.isEmpty()) {
                user.setPassword(passwordEncoder.encode(password));
            }

            // Handle profile image upload
            if (image != null && !image.isEmpty()) {
                String originalFilename = StringUtils.cleanPath(image.getOriginalFilename());
                // Generate unique filename to prevent overwriting
                String uniqueFilename = UUID.randomUUID().toString() + "_" + originalFilename;
                
                // Set the upload path
                Path uploadPath = Paths.get("src/main/resources/static/assets/images/users");
                
                // Ensure directory exists
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }
                
                // Save the file
                Path destinationFile = uploadPath.resolve(uniqueFilename);
                Files.copy(image.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);
                
                // Set the profile image in user
                user.setFotoProfil(uniqueFilename);
            }

            // Save user
            userRepository.save(user);

            redirectAttributes.addFlashAttribute("success", "Admin berhasil diperbarui");
            return "redirect:/admin/admin";
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "Gagal mengunggah foto profil: " + e.getMessage());
            return "redirect:/admin/admin/edit/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Gagal memperbarui Admin: " + e.getMessage());
            return "redirect:/admin/admin/edit/" + id;
        }
    }

    @GetMapping("/admin/admin/delete/{id}")
    public String deleteAdmin(@PathVariable("id") int id, RedirectAttributes redirectAttributes) {
        try {
            Optional<Admin> adminOpt = adminRepository.findById(id);
            if (!adminOpt.isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Admin tidak ditemukan");
                return "redirect:/admin/admin";
            }

            Admin admin = adminOpt.get();
            User user = admin.getUser();
            
            // Delete admin first to avoid foreign key constraints
            adminRepository.delete(admin);
            
            // Delete user
            userRepository.delete(user);

            redirectAttributes.addFlashAttribute("success", "Admin berhasil dihapus");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Gagal menghapus Admin: " + e.getMessage());
        }
        return "redirect:/admin/admin";
    }
} 