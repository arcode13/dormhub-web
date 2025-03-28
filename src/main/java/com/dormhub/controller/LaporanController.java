package com.dormhub.controller;

import com.dormhub.model.LaporanBarang;
import com.dormhub.model.LaporanUmum;
import com.dormhub.model.Mahasiswa;
import com.dormhub.model.HelpDesk;
import com.dormhub.model.User;
import com.dormhub.service.LaporanService;
import com.dormhub.repository.UserRepository;
import com.dormhub.repository.LaporanBarangRepository;
import com.dormhub.repository.LaporanUmumRepository;
import com.dormhub.repository.HelpDeskRepository;
import com.dormhub.repository.MahasiswaRepository;
import com.dormhub.repository.SeniorResidenceRepository;
import com.dormhub.repository.KonfigurasiRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.List;
import java.util.Map;

@Controller
public class LaporanController {

    @Autowired
    private LaporanService laporanService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MahasiswaRepository mahasiswaRepository;

    @Autowired
    private SeniorResidenceRepository seniorResidenceRepository;

    @Autowired
    private HelpDeskRepository helpDeskRepository;

    @Autowired
    private LaporanUmumRepository laporanUmumRepository;

    @Autowired
    private LaporanBarangRepository laporanBarangRepository;

    @Autowired
    private KonfigurasiRepository konfigurasiRepository;

    private String capitalize(String input) {
        if (input == null || input.isEmpty()) return input;
        return input.substring(0, 1).toUpperCase() + input.substring(1).toLowerCase();
    }    

    @GetMapping("/mahasiswa/buat-laporan-umum")
    public String formBuatLaporan(Principal principal, Model model) {
        String email = principal.getName();
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User tidak ditemukan"));
        Mahasiswa mahasiswa = mahasiswaRepository.findByUserId(user.getId()).orElse(null);
    
        if (mahasiswa != null) {
            boolean isSeniorResidence = seniorResidenceRepository.existsByMahasiswaId(mahasiswa.getId());

            model.addAttribute("isSeniorResidence", isSeniorResidence);
            model.addAttribute("isCheckin", mahasiswa.getIsCheckin() == 1);
            model.addAttribute("isCheckout", mahasiswa.getIsCheckout() == 1);
            model.addAttribute("mahasiswa", mahasiswa);
        } else {
            model.addAttribute("isSeniorResidence", false);
            model.addAttribute("isCheckin", false);
            model.addAttribute("isCheckout", false);
            model.addAttribute("mahasiswa", false);
        }
    
        Map<String, String> konfigurasi = konfigurasiRepository.findAllAsMap().stream()
            .collect(Collectors.toMap(
                entry -> entry.get("key"),
                entry -> entry.get("value")
            ));

        model.addAttribute("konfigurasi", konfigurasi);

        model.addAttribute("user", user);
        return "mahasiswa/BuatLaporanUmum";
    }

    @PostMapping("/mahasiswa/buat-laporan-umum")
    public String buatLaporanUmum(
            @RequestParam("kategori") String kategori,
            @RequestParam("keterangan") String keterangan,
            @RequestParam(value = "buktiFoto", required = false) MultipartFile buktiFoto, // Ubah ke MultipartFile
            RedirectAttributes redirectAttributes,
            Model model) {

        try {
            // 1. Ambil email user yang login
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();

            // 2. Dapatkan user berdasarkan email
            Optional<User> userOptional = userRepository.findByEmail(email);
            if (!userOptional.isPresent()) {
                redirectAttributes.addFlashAttribute("error", "User tidak ditemukan.");
                return "redirect:/mahasiswa/buat-laporan-umum";
            }

            User user = userOptional.get();
            int userId = user.getId();

            // 3. Dapatkan mahasiswa_id berdasarkan user_id
            Optional<Mahasiswa> mahasiswaOptional = mahasiswaRepository.findByUserId(userId);
            if (!mahasiswaOptional.isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Mahasiswa tidak ditemukan.");
                return "redirect:/mahasiswa/buat-laporan-umum";
            }

            Mahasiswa mahasiswa = mahasiswaOptional.get();
            int mahasiswaId = mahasiswa.getId();

            // 4. Simpan laporan ke dalam tabel laporan_umum
            String buktiPath = null;

            if (buktiFoto != null && !buktiFoto.isEmpty()) {
                // Validasi ukuran file maksimal 2 MB
                long maxFileSize = 2 * 1024 * 1024; // 2 MB
                if (buktiFoto.getSize() > maxFileSize) {
                    redirectAttributes.addFlashAttribute("error", "Ukuran bukti foto maksimal 2 MB.");
                    return "redirect:/mahasiswa/dashboard";
                }

                // Validasi content type
                String contentType = buktiFoto.getContentType();
                if (contentType == null || 
                    (!contentType.equalsIgnoreCase("image/jpeg") &&
                     !contentType.equalsIgnoreCase("image/jpg") &&
                     !contentType.equalsIgnoreCase("image/png"))) {
                    redirectAttributes.addFlashAttribute("error", "Format bukti foto hanya boleh JPG, JPEG, atau PNG");
                    return "redirect:/mahasiswa/dashboard";
                }
            
                // Validasi nama file
                String originalFilename = buktiFoto.getOriginalFilename();
                if (originalFilename == null || originalFilename.isBlank()) {
                    redirectAttributes.addFlashAttribute("error", "Nama bukti foto tidak valid.");
                    return "redirect:/mahasiswa/dashboard";
                }
            
                // Simpan file dengan nama unik
                String fileName = System.currentTimeMillis() + "_" + originalFilename.replaceAll("\\s+", "_");
                Path uploadPath = Path.of("src/main/resources/static/assets/images/laporan-umum/" + fileName);
                Files.write(uploadPath, buktiFoto.getBytes());
                buktiPath = fileName;
            }

            // 5. Panggil service untuk menyimpan laporan
            laporanService.buatLaporanUmum(mahasiswaId, kategori, keterangan, buktiPath);

            redirectAttributes.addFlashAttribute("success", "Laporan berhasil dibuat");
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Terjadi kesalahan saat membuat laporan.");
            return "redirect:/mahasiswa/buat-laporan-umum";
        }

        return "redirect:/mahasiswa/daftar-laporan";
    }

    @GetMapping("/mahasiswa/daftar-laporan")
    public String daftarLaporanUmum(Model model, RedirectAttributes redirectAttributes) {
        try {
            // 1. Ambil email user yang login
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();

            // 2. Dapatkan user berdasarkan email
            Optional<User> userOptional = userRepository.findByEmail(email);
            if (!userOptional.isPresent()) {
                redirectAttributes.addFlashAttribute("error", "User tidak ditemukan.");
                return "redirect:/mahasiswa/daftar-laporan";
            }

            User user = userOptional.get();
            int userId = user.getId();

            // 3. Dapatkan mahasiswa_id berdasarkan user_id
            Optional<Mahasiswa> mahasiswaOptional = mahasiswaRepository.findByUserId(userId);
            if (!mahasiswaOptional.isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Mahasiswa tidak ditemukan.");
                return "redirect:/mahasiswa/daftar-laporan";
            }

            Mahasiswa mahasiswa = mahasiswaOptional.get();
            int mahasiswaId = mahasiswa.getId();

            // 4. Dapatkan daftar laporan umum dari database berdasarkan mahasiswa_id
            List<LaporanUmum> daftarLaporan = laporanUmumRepository.findAllByMahasiswaId(mahasiswaId);

            // **Format tanggal ke format Indonesia**
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM yyyy - HH:mm", new java.util.Locale("id", "ID"));
            for (LaporanUmum laporan : daftarLaporan) {
                String formattedDate = laporan.getCreatedAt().format(formatter);
                laporan.setStatus(capitalize(laporan.getStatus()));
                laporan.setFormattedCreatedAt(formattedDate);
            }

            boolean isSeniorResidence = seniorResidenceRepository.existsByMahasiswaId(mahasiswa.getId());
        
            Map<String, String> konfigurasi = konfigurasiRepository.findAllAsMap().stream()
                .collect(Collectors.toMap(
                    entry -> entry.get("key"),
                    entry -> entry.get("value")
                ));

            model.addAttribute("konfigurasi", konfigurasi);

            // 5. Tambahkan daftar laporan ke model agar bisa digunakan di view
            model.addAttribute("isSeniorResidence", isSeniorResidence);
            model.addAttribute("isCheckin", mahasiswa.getIsCheckin() == 1);
            model.addAttribute("isCheckout", mahasiswa.getIsCheckout() == 1);
            model.addAttribute("user", user);
            model.addAttribute("daftarLaporan", daftarLaporan);
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Terjadi kesalahan saat mengambil daftar laporan.");
            return "redirect:/mahasiswa/daftar-laporan";
        }

        return "mahasiswa/DaftarLaporanUmum";
    }

    @PostMapping("/help-desk/buat-laporan-barang")
    public String buatLaporanBarang(
            @RequestParam("mahasiswaId") int mahasiswaId,
            @RequestParam("jenis") String jenis,
            @RequestParam(value = "buktiFoto", required = false) MultipartFile buktiFoto,
            RedirectAttributes redirectAttributes,
            Principal principal) {
    
        try {
            // 1. Ambil email user yang login
            String email = principal.getName();
    
            // 2. Dapatkan user berdasarkan email
            Optional<User> userOptional = userRepository.findByEmail(email);
            if (!userOptional.isPresent()) {
                redirectAttributes.addFlashAttribute("error", "User tidak ditemukan.");
                return "redirect:/help-desk/dashboard";
            }

            User user = userOptional.get();
            int userId = user.getId();

            // 3. Dapatkan helpdesk_id berdasarkan user_id
            Optional<HelpDesk> helpDeskOptional = helpDeskRepository.findByUserId(userId);
            if (!helpDeskOptional.isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Mahasiswa tidak ditemukan.");
                return "redirect:/help-desk/dashboard";
            }

            HelpDesk helpDesk = helpDeskOptional.get();
            int helpDeskId = helpDesk.getId();
    
            // 3. Validasi mahasiswa
            Optional<Mahasiswa> mahasiswaOptional = mahasiswaRepository.findById(mahasiswaId);
            if (!mahasiswaOptional.isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Mahasiswa tidak ditemukan.");
                return "redirect:/help-desk/dashboard";
            }
    
            // 4. Simpan laporan ke dalam tabel laporan_barang
            String buktiPath = null;
    
            if (buktiFoto != null && !buktiFoto.isEmpty()) {
                // Validasi ukuran file maksimal 2 MB
                long maxFileSize = 2 * 1024 * 1024; // 2 MB
                if (buktiFoto.getSize() > maxFileSize) {
                    redirectAttributes.addFlashAttribute("error", "Ukuran bukti foto maksimal 2 MB.");
                    return "redirect:/help-desk/dashboard";
                }

                // Validasi content type
                String contentType = buktiFoto.getContentType();
                if (contentType == null || 
                    (!contentType.equalsIgnoreCase("image/jpeg") &&
                     !contentType.equalsIgnoreCase("image/jpg") &&
                     !contentType.equalsIgnoreCase("image/png"))) {
                    redirectAttributes.addFlashAttribute("error", "Format bukti foto hanya boleh JPG, JPEG, atau PNG");
                    return "redirect:/help-desk/dashboard";
                }
            
                // Validasi nama file
                String originalFilename = buktiFoto.getOriginalFilename();
                if (originalFilename == null || originalFilename.isBlank()) {
                    redirectAttributes.addFlashAttribute("error", "Nama bukti foto tidak valid.");
                    return "redirect:/help-desk/dashboard";
                }
            
                // Simpan file dengan nama unik
                String fileName = System.currentTimeMillis() + "_" + originalFilename.replaceAll("\\s+", "_");
                Path uploadPath = Path.of("src/main/resources/static/assets/images/laporan-barang/" + fileName);
                Files.write(uploadPath, buktiFoto.getBytes());
                buktiPath = fileName;
            }
    
            // 5. Simpan laporan ke database
            LaporanBarang laporanBarang = new LaporanBarang();
            laporanBarang.setHelpdeskId(helpDeskId);
            laporanBarang.setMahasiswaId(mahasiswaId);
            laporanBarang.setJenis(jenis);
            laporanBarang.setBuktiFoto(buktiPath);
            laporanBarang.setStatus("menunggu");
            laporanBarang.setCreatedAt(LocalDateTime.now());
            laporanBarang.setUpdatedAt(LocalDateTime.now());
    
            laporanBarangRepository.save(laporanBarang);
    
            // 6. Berikan pesan sukses
            redirectAttributes.addFlashAttribute("success", "Laporan barang berhasil dibuat");
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Terjadi kesalahan saat membuat laporan barang.");
        }
    
        return "redirect:/help-desk/dashboard";
    }

    @GetMapping("/help-desk/daftar-laporan")
    public String daftarLaporanUmumHelpDesk(Model model, RedirectAttributes redirectAttributes) {
        try {
            // 1. Ambil email user yang login
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
    
            // 2. Dapatkan user berdasarkan email
            Optional<User> userOptional = userRepository.findByEmail(email);
            if (!userOptional.isPresent()) {
                redirectAttributes.addFlashAttribute("error", "User tidak ditemukan.");
                return "redirect:/help-desk/dashboard";
            }
    
            User user = userOptional.get();
            int userId = user.getId();
    
            // Ambil daftar mahasiswa dari repository
            List<Mahasiswa> mahasiswaList = mahasiswaRepository.findAll();
            model.addAttribute("mahasiswaList", mahasiswaList);

            // 3. Dapatkan mahasiswa_id berdasarkan user_id
            Optional<HelpDesk> helpDeskOptional = helpDeskRepository.findByUserId(userId);
            if (!helpDeskOptional.isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Help Desk tidak ditemukan.");
                return "redirect:/logout";
            }

            // 3. Ambil data laporan dengan jenis keluhan
            List<LaporanUmum> daftarLaporan = laporanUmumRepository.findAllKeluhan();
    
            // **Format tanggal ke format Indonesia**
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM yyyy - HH:mm", new java.util.Locale("id", "ID"));
            for (LaporanUmum laporan : daftarLaporan) {
                String formattedDate = laporan.getCreatedAt().format(formatter);
                laporan.setStatus(capitalize(laporan.getStatus()));
                laporan.setFormattedCreatedAt(formattedDate);

                Optional<Mahasiswa> mahasiswaOptional = mahasiswaRepository.findById(laporan.getMahasiswaId());
                if (mahasiswaOptional.isPresent()) {
                    Mahasiswa mahasiswa = mahasiswaOptional.get();
                    laporan.setNamaLengkap(mahasiswa.getUser().getNamaLengkap());
                    laporan.setNoKamar(mahasiswa.getNoKamar());
                }
            }
    
            Map<String, String> konfigurasi = konfigurasiRepository.findAllAsMap().stream()
                .collect(Collectors.toMap(
                    entry -> entry.get("key"),
                    entry -> entry.get("value")
                ));

            model.addAttribute("konfigurasi", konfigurasi);

            // Tambahkan ke model untuk digunakan di view
            model.addAttribute("user", user);
            model.addAttribute("daftarLaporan", daftarLaporan);
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Terjadi kesalahan saat mengambil daftar laporan.");
            return "redirect:/help-desk/dashboard";
        }
    
        return "help-desk/DaftarLaporanUmum"; 
    }    

    @GetMapping("/help-desk/ubah-status/{laporanId}/{status}")
    public String ubahStatusLaporanUmumKKeluhan(
            @PathVariable("laporanId") int laporanId,
            @PathVariable("status") String status,
            RedirectAttributes redirectAttributes) {
        try {
            // Cari laporan berdasarkan ID
            Optional<LaporanUmum> laporanOptional = laporanUmumRepository.findById(laporanId);
            if (!laporanOptional.isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Laporan tidak ditemukan.");
                return "redirect:/help-desk/daftar-laporan";
            }

            LaporanUmum laporan = laporanOptional.get();

            // Perbarui status laporan
            laporan.setStatus(status.toLowerCase());
            laporan.setUpdatedAt(LocalDateTime.now());
            laporanUmumRepository.save(laporan);

            redirectAttributes.addFlashAttribute("success", "Status laporan berhasil diubah.");
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Terjadi kesalahan saat mengubah status laporan.");
        }

        return "redirect:/help-desk/daftar-laporan";
    }

    @GetMapping("/senior-residence/ubah-status/{laporanId}/{status}")
    public String ubahStatusLaporanUmumIzin(
            @PathVariable("laporanId") int laporanId,
            @PathVariable("status") String status,
            RedirectAttributes redirectAttributes) {
        try {
            // Cari laporan berdasarkan ID
            Optional<LaporanUmum> laporanOptional = laporanUmumRepository.findById(laporanId);
            if (!laporanOptional.isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Laporan tidak ditemukan.");
                return "redirect:/senior-residence/dashboard";
            }

            LaporanUmum laporan = laporanOptional.get();

            // Perbarui status laporan
            laporan.setStatus(status.toLowerCase());
            laporan.setUpdatedAt(LocalDateTime.now());
            laporanUmumRepository.save(laporan);

            redirectAttributes.addFlashAttribute("success", "Status laporan berhasil diubah.");
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Terjadi kesalahan saat mengubah status laporan.");
        }

        return "redirect:/senior-residence/dashboard";
    }
}