package com.dormhub.controller;

import com.dormhub.model.HelpDesk;
import com.dormhub.model.Level;
import com.dormhub.model.User;
import com.dormhub.repository.HelpDeskRepository;
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
public class HelpDeskController {

    @Autowired
    private HelpDeskRepository helpDeskRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LevelRepository levelRepository;

    @Autowired
    private KonfigurasiRepository konfigurasiRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @GetMapping("/admin/help-desk")
    public String listHelpDesk(Model model, RedirectAttributes redirectAttributes) {
        List<HelpDesk> helpDeskList = helpDeskRepository.findAll();
        model.addAttribute("helpDeskList", helpDeskList);

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

        return "admin/HelpDesk/index";
    }

    @GetMapping("/admin/help-desk/tambah")
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

        return "admin/HelpDesk/tambah";
    }

    @PostMapping("/admin/help-desk/tambah")
    public String addHelpDesk(
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
                return "redirect:/admin/help-desk/tambah";
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

            // Find Help Desk level
            Optional<Level> helpDeskLevelOpt = levelRepository.findByNama("Help Desk");
            if (!helpDeskLevelOpt.isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Level Help Desk tidak ditemukan");
                return "redirect:/admin/help-desk/tambah";
            }

            Level helpDeskLevel = helpDeskLevelOpt.get();
            user.setLevel(helpDeskLevel);

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

            // Create HelpDesk record
            HelpDesk helpDesk = new HelpDesk();
            helpDesk.setUser(user);
            helpDeskRepository.save(helpDesk);

            redirectAttributes.addFlashAttribute("success", "Help Desk berhasil ditambahkan");
            return "redirect:/admin/help-desk";
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "Gagal mengunggah foto profil: " + e.getMessage());
            return "redirect:/admin/help-desk/tambah";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Gagal menambahkan Help Desk: " + e.getMessage());
            return "redirect:/admin/help-desk/tambah";
        }
    }

    @GetMapping("/admin/help-desk/edit/{id}")
    public String showEditForm(@PathVariable("id") int id, Model model, RedirectAttributes redirectAttributes) {
        Optional<HelpDesk> helpDeskOpt = helpDeskRepository.findById(id);
        if (!helpDeskOpt.isPresent()) {
            redirectAttributes.addFlashAttribute("error", "Help Desk tidak ditemukan");
            return "redirect:/admin/help-desk";
        }

        HelpDesk helpDesk = helpDeskOpt.get();
        model.addAttribute("helpDesk", helpDesk);

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

        return "admin/HelpDesk/edit";
    }

    @PostMapping("/admin/help-desk/edit/{id}")
    public String updateHelpDesk(
            @PathVariable("id") int id,
            @RequestParam("namaLengkap") String namaLengkap,
            @RequestParam("email") String email,
            @RequestParam("nomorHp") String nomorHp,
            @RequestParam(value = "password", required = false) String password,
            @RequestParam("jenisKelamin") String jenisKelamin,
            @RequestParam(value = "image", required = false) MultipartFile image,
            RedirectAttributes redirectAttributes) {

        try {
            Optional<HelpDesk> helpDeskOpt = helpDeskRepository.findById(id);
            if (!helpDeskOpt.isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Help Desk tidak ditemukan");
                return "redirect:/admin/help-desk";
            }

            HelpDesk helpDesk = helpDeskOpt.get();
            User user = helpDesk.getUser();

            // Check if email already exists for other users
            Optional<User> existingUserOpt = userRepository.findByEmail(email);
            if (existingUserOpt.isPresent() && existingUserOpt.get().getId() != user.getId()) {
                redirectAttributes.addFlashAttribute("error", "Email sudah digunakan oleh pengguna lain");
                return "redirect:/admin/help-desk/edit/" + id;
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

            redirectAttributes.addFlashAttribute("success", "Help Desk berhasil diperbarui");
            return "redirect:/admin/help-desk";
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "Gagal mengunggah foto profil: " + e.getMessage());
            return "redirect:/admin/help-desk/edit/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Gagal memperbarui Help Desk: " + e.getMessage());
            return "redirect:/admin/help-desk/edit/" + id;
        }
    }

    @GetMapping("/admin/help-desk/delete/{id}")
    public String deleteHelpDesk(@PathVariable("id") int id, RedirectAttributes redirectAttributes) {
        try {
            Optional<HelpDesk> helpDeskOpt = helpDeskRepository.findById(id);
            if (!helpDeskOpt.isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Help Desk tidak ditemukan");
                return "redirect:/admin/help-desk";
            }

            HelpDesk helpDesk = helpDeskOpt.get();
            User user = helpDesk.getUser();
            
            // Delete help desk first to avoid foreign key constraints
            helpDeskRepository.delete(helpDesk);
            
            // Delete user
            userRepository.delete(user);

            redirectAttributes.addFlashAttribute("success", "Help Desk berhasil dihapus");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Gagal menghapus Help Desk: " + e.getMessage());
        }
        return "redirect:/admin/help-desk";
    }
} 