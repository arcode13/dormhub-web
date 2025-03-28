package com.dormhub.controller;

import com.dormhub.model.Mahasiswa;
import com.dormhub.model.SeniorResidence;
import com.dormhub.model.User;
import com.dormhub.repository.MahasiswaRepository;
import com.dormhub.repository.SeniorResidenceRepository;
import com.dormhub.repository.HelpDeskRepository;
import com.dormhub.repository.AdminRepository;
import com.dormhub.repository.UserRepository;
import com.dormhub.repository.LaporanBarangRepository;
import com.dormhub.repository.LaporanUmumRepository;
import com.dormhub.repository.KonfigurasiRepository;
import com.dormhub.service.LaporanService;
import com.dormhub.service.SeniorResidenceService;
import com.dormhub.model.HelpDesk;
import com.dormhub.model.Konfigurasi;
import com.dormhub.model.Admin;
import com.dormhub.model.LaporanBarang;
import com.dormhub.model.LaporanUmum;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class DashboardController {

    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

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
    private AdminRepository adminRepository;

    @Autowired
    private LaporanBarangRepository laporanBarangRepository;

    @Autowired
    private LaporanUmumRepository laporanUmumRepository;

    @Autowired
    private SeniorResidenceService seniorResidenceService;

    @Autowired
    private KonfigurasiRepository konfigurasiRepository;

    private String capitalize(String input) {
        if (input == null || input.isEmpty()) return input;
        return input.substring(0, 1).toUpperCase() + input.substring(1).toLowerCase();
    }    

    @GetMapping("/mahasiswa/dashboard")
    public String mahasiswaDashboard(Model model, RedirectAttributes redirectAttributes) {
        // Log awal masuk ke metode
        logger.info("Masuk ke metode mahasiswaDashboard.");

        // Ambil email dari user yang sedang login
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        logger.debug("Email pengguna yang login: {}", email);

        // Cari user berdasarkan email
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            int userId = user.getId();
            logger.debug("User ditemukan dengan ID: {}", userId);

            // Ambil jumlah laporan berdasarkan mahasiswa_id
            int jumlahLaporanIzin = laporanService.getJumlahLaporanIzinBulanIni(userId);
            int jumlahLaporanKeluhan = laporanService.getJumlahLaporanKeluhanBulanIni(userId);
            int totalLaporan = jumlahLaporanIzin + jumlahLaporanKeluhan;

            logger.debug("Jumlah laporan izin: {}", jumlahLaporanIzin);
            logger.debug("Jumlah laporan keluhan: {}", jumlahLaporanKeluhan);
            logger.debug("Total laporan: {}", totalLaporan);

            Optional<Mahasiswa> mahasiswaOptional = mahasiswaRepository.findByUserId(userId);
            if (mahasiswaOptional.isPresent()) {
                Mahasiswa mahasiswa = mahasiswaOptional.get();
                boolean isSeniorResidence = seniorResidenceRepository.existsByMahasiswaId(mahasiswa.getId());

                boolean isCheckinAvailable = false;
                boolean isCheckoutAvailable = false;
                boolean isCheckoutLate = false;
                
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM yyyy", new java.util.Locale("id", "ID"));
                String formattedCheckIn = (mahasiswa.getWaktuCheckin() != null) ? mahasiswa.getWaktuCheckin().format(formatter) : "Belum melakukan check-in";
                String formattedCheckOut = (mahasiswa.getWaktuCheckout() != null) ? mahasiswa.getWaktuCheckout().format(formatter) : "Belum melakukan check-out";
                
                String formattedMulaiTglCo = null;
                String formattedSelesaiTglCo = null;
                
                Optional<String> mulaiTglCoOptional = konfigurasiRepository.findKValueByKKey("web-mulai-tgl-co");
                Optional<String> selesaiTglCoOptional = konfigurasiRepository.findKValueByKKey("web-selesai-tgl-co");
                
                if (mulaiTglCoOptional.isPresent()) {
                    LocalDateTime batasMulai = LocalDateTime.parse(mulaiTglCoOptional.get() + "T00:00:00");
                    formattedMulaiTglCo = batasMulai.format(formatter); // Format ke tanggal Indonesia
                    isCheckinAvailable = LocalDateTime.now().isBefore(batasMulai.plusDays(7)) && mahasiswa.getIsCheckin() == 0;
                }
                
                if (selesaiTglCoOptional.isPresent()) {
                    LocalDateTime batasSelesai = LocalDateTime.parse(selesaiTglCoOptional.get() + "T23:59:59");
                    formattedSelesaiTglCo = batasSelesai.format(formatter); // Format ke tanggal Indonesia
                    isCheckoutAvailable = LocalDateTime.now().isAfter(batasSelesai.minusDays(7)) && mahasiswa.getIsCheckout() == 0;
                    isCheckoutLate = LocalDateTime.now().isAfter(batasSelesai) && mahasiswa.getIsCheckout() == 0;
                }
                
                model.addAttribute("isCheckinAvailable", isCheckinAvailable);
                model.addAttribute("isCheckoutAvailable", isCheckoutAvailable);
                model.addAttribute("isCheckoutLate", isCheckoutLate);
                
                model.addAttribute("formattedMulaiTglCo", formattedMulaiTglCo);
                model.addAttribute("formattedSelesaiTglCo", formattedSelesaiTglCo);

                mahasiswa.setWaktuCheckin(mahasiswa.getWaktuCheckin()); // Tetap gunakan getter/setter asli
                mahasiswa.setWaktuCheckout(mahasiswa.getWaktuCheckout());
                model.addAttribute("formattedCheckIn", formattedCheckIn);
                model.addAttribute("formattedCheckOut", formattedCheckOut);

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

            // Ambil laporan barang dengan status 'menunggu'
            List<LaporanBarang> laporanBarangList = laporanBarangRepository.findByMahasiswaIdAndStatus(userId, "menunggu");

            for (LaporanBarang laporan : laporanBarangList) {
                // Format tanggal
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM yyyy - HH:mm", new java.util.Locale("id", "ID"));
                laporan.setFormattedCreatedAt(laporan.getCreatedAt().format(formatter));

                // Ambil nama Help Desk
                Optional<HelpDesk> helpDeskOptional = helpDeskRepository.findById(laporan.getHelpdeskId());
                if (helpDeskOptional.isPresent()) {
                    HelpDesk helpDesk = helpDeskOptional.get();
                    laporan.setNamaLengkap(helpDesk.getUser().getNamaLengkap());
                }
            }

            Map<String, String> konfigurasi = konfigurasiRepository.findAllAsMap().stream()
                .collect(Collectors.toMap(
                    entry -> entry.get("key"),
                    entry -> entry.get("value")
                ));

            model.addAttribute("konfigurasi", konfigurasi);

            String ucapan = getUcapan();
            model.addAttribute("user", user);
            model.addAttribute("ucapan", ucapan);
            model.addAttribute("laporanIzinBulanIni", jumlahLaporanIzin);
            model.addAttribute("laporanKeluhanBulanIni", jumlahLaporanKeluhan);
            model.addAttribute("totalLaporan", totalLaporan);
            model.addAttribute("laporanBarangList", laporanBarangList);
        } else {
            logger.warn("User dengan email {} tidak ditemukan.", email);
            redirectAttributes.addFlashAttribute("error", "User tidak ditemukan.");
        }

        // Log akhir metode
        logger.info("Selesai menjalankan metode mahasiswaDashboard.");
        return "mahasiswa/Dashboard";
    }

    private String getUcapan() {
        LocalTime now = LocalTime.now();
        if (now.isBefore(LocalTime.NOON)) {
            return "Selamat Pagi";
        } else if (now.isBefore(LocalTime.of(18, 0))) {
            return "Selamat Siang";
        } else {
            return "Selamat Malam";
        }
    }

    @GetMapping("/mahasiswa/checkin")
    public String handleCheckin(RedirectAttributes redirectAttributes) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
    
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
    
            Optional<Mahasiswa> mahasiswaOptional = mahasiswaRepository.findByUserId(user.getId());
            if (mahasiswaOptional.isPresent()) {
                Mahasiswa mahasiswa = mahasiswaOptional.get();
    
                // Validasi jika sudah check-in
                if (mahasiswa.getIsCheckin() == 1) {
                    redirectAttributes.addFlashAttribute("error", "Anda sudah melakukan check-in sebelumnya.");
                    return "redirect:/mahasiswa/dashboard";
                }
    
                // Validasi jika sudah lebih dari 7 hari setelah tanggal mulai check-in
                Optional<String> mulaiTglCoOptional = konfigurasiRepository.findKValueByKKey("web-mulai-tgl-co");
                if (mulaiTglCoOptional.isPresent()) {
                    LocalDateTime batasMulai = LocalDateTime.parse(mulaiTglCoOptional.get() + "T00:00:00");
                    if (LocalDateTime.now().isAfter(batasMulai.plusDays(7))) {
                        redirectAttributes.addFlashAttribute("error", "Check-in tidak dapat dilakukan karena sudah melewati batas waktu 7 hari setelah tanggal mulai.");
                        return "redirect:/mahasiswa/dashboard";
                    }
                }
    
                // Set status check-in dan waktu check-in
                mahasiswa.setIsCheckin(1);
                mahasiswa.setWaktuCheckin(LocalDateTime.now());
                mahasiswaRepository.save(mahasiswa);
    
                redirectAttributes.addFlashAttribute("success", "Berhasil melakukan check-in");
                return "redirect:/mahasiswa/dashboard";
            }
        }
    
        redirectAttributes.addFlashAttribute("error", "Data mahasiswa tidak ditemukan.");
        return "redirect:/mahasiswa/dashboard";
    }      
    
    @GetMapping("/mahasiswa/checkout")
    public String handleCheckout(RedirectAttributes redirectAttributes) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
    
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
    
            Optional<Mahasiswa> mahasiswaOptional = mahasiswaRepository.findByUserId(user.getId());
            if (mahasiswaOptional.isPresent()) {
                Mahasiswa mahasiswa = mahasiswaOptional.get();
    
                // Validasi jika sudah check-out
                if (mahasiswa.getIsCheckout() == 1) {
                    redirectAttributes.addFlashAttribute("error", "Anda sudah melakukan check-out sebelumnya.");
                    return "redirect:/mahasiswa/dashboard";
                }
    
                // Ambil batas waktu dari konfigurasi
                Optional<String> selesaiTglCoOptional = konfigurasiRepository.findKValueByKKey("web-selesai-tgl-co");
                if (selesaiTglCoOptional.isPresent()) {
                    LocalDateTime batasWaktu = LocalDateTime.parse(selesaiTglCoOptional.get() + "T23:59:59");
                    if (LocalDateTime.now().isAfter(batasWaktu)) {
                        redirectAttributes.addFlashAttribute("error", "Check-out melewati batas waktu yang ditentukan.");
                        return "redirect:/mahasiswa/dashboard";
                    }
                }
    
                // Set status check-out dan waktu check-out
                mahasiswa.setIsCheckout(1);
                mahasiswa.setWaktuCheckout(LocalDateTime.now());
                mahasiswaRepository.save(mahasiswa);
    
                redirectAttributes.addFlashAttribute("success", "Berhasil melakukan check-out");
                return "redirect:/mahasiswa/dashboard";
            }
        }
    
        redirectAttributes.addFlashAttribute("error", "Data mahasiswa tidak ditemukan.");
        return "redirect:/mahasiswa/dashboard";
    }    

    @GetMapping("/senior-residence/dashboard")
    public String seniorResidenceDashboard(Model model, RedirectAttributes redirectAttributes) {
        // Log awal masuk ke metode
        logger.info("Masuk ke metode seniorResidenceDashboard.");

        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        logger.debug("Email pengguna yang login: {}", email);

        // Cari user berdasarkan email
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            int userId = user.getId();
            logger.debug("User ditemukan dengan ID: {}", userId);

            // Ambil jumlah laporan berdasarkan mahasiswa_id
            int jumlahLaporanIzin = laporanUmumRepository.getSemuaJumlahLaporanIzinBulanIni();
            int totalLaporanIzin = laporanUmumRepository.getTotalSemuaLaporanIzin();

            logger.debug("Jumlah laporan izin: {}", jumlahLaporanIzin);
            logger.debug("Total laporan: {}", totalLaporanIzin);

            // Ambil data Senior Residence
            SeniorResidence seniorResidence = seniorResidenceService.getSeniorResidenceByMahasiswaId(userId).orElse(null);
            if (seniorResidence == null) {
                redirectAttributes.addFlashAttribute("error", "Senior Residence tidak ditemukan.");
                return "redirect:/logout";
            }

            
            List<LaporanUmum> daftarLaporan = laporanUmumRepository.findAllIzin();
    
            
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

            String ucapan = getUcapan();
            model.addAttribute("user", user);
            model.addAttribute("ucapan", ucapan);
            model.addAttribute("laporanIzinBulanIni", jumlahLaporanIzin);
            model.addAttribute("totalLaporanIzin", totalLaporanIzin);
            model.addAttribute("daftarLaporan", daftarLaporan);
        } else {
            logger.warn("User dengan email {} tidak ditemukan.", email);
            redirectAttributes.addFlashAttribute("error", "User tidak ditemukan.");
        }

        // Log akhir metode
        logger.info("Selesai menjalankan metode seniorResidenceDashboard.");
        return "senior-residence/Dashboard";
    }

    @GetMapping("/mahasiswa/daftar-barang/{id}")
    public String updateStatusLaporanBarang(@PathVariable("id") int id, RedirectAttributes redirectAttributes) {
        Optional<LaporanBarang> laporanBarangOptional = laporanBarangRepository.findById(id);
        if (laporanBarangOptional.isPresent()) {
            LaporanBarang laporanBarang = laporanBarangOptional.get();
            laporanBarang.setStatus("Diterima");
            laporanBarang.setUpdatedAt(LocalDateTime.now());
            laporanBarangRepository.save(laporanBarang);

            // Pesan dinamis berdasarkan jenis barang
            String jenis = laporanBarang.getJenis();
            redirectAttributes.addFlashAttribute("success", "" + jenis + " berhasil diterima.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Barang tidak ditemukan.");
        }
        return "redirect:/mahasiswa/dashboard";
    }

    @GetMapping("/help-desk/dashboard")
    public String helpDeskDashboard(Model model, RedirectAttributes redirectAttributes) {
        // Log awal masuk ke metode
        logger.info("Masuk ke metode helpDeskDashboard.");
    
        // Ambil email dari user yang sedang login
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        logger.debug("Email pengguna yang login: {}", email);
    
        // Cari user berdasarkan email
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            int userId = user.getId();
            logger.debug("User ditemukan dengan ID: {}", userId);
    
            String ucapan = getUcapan();
            model.addAttribute("user", user);
            model.addAttribute("ucapan", ucapan);
    
            // Ambil daftar mahasiswa dari repository
            List<Mahasiswa> mahasiswaList = mahasiswaRepository.findAll();
            model.addAttribute("mahasiswaList", mahasiswaList);

            // 3. Dapatkan mahasiswa_id berdasarkan user_id
            Optional<HelpDesk> helpDeskOptional = helpDeskRepository.findByUserId(userId);
            if (!helpDeskOptional.isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Help Desk tidak ditemukan.");
                return "redirect:/logout";
            }

            HelpDesk helpDesk = helpDeskOptional.get();
            int helpDeskId = helpDesk.getId();
            
            List<LaporanBarang> laporanBarangList = laporanBarangRepository.findByHelpdeskId(helpDeskId);

            // **Format tanggal ke format Indonesia**
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM yyyy - HH:mm", new java.util.Locale("id", "ID"));
            for (LaporanBarang laporan : laporanBarangList) {
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

            model.addAttribute("laporanBarangList", laporanBarangList);
        } else {
            logger.warn("User dengan email {} tidak ditemukan.", email);
            redirectAttributes.addFlashAttribute("error", "User tidak ditemukan.");
        }
    
        return "help-desk/Dashboard";
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard(Model model, RedirectAttributes redirectAttributes) {
        // Log awal masuk ke metode
        logger.info("Masuk ke metode adminDashboard.");
    
        // Ambil email dari user yang sedang login
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        logger.debug("Email pengguna yang login: {}", email);
    
        // Cari user berdasarkan email
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            int userId = user.getId();
            logger.debug("User ditemukan dengan ID: {}", userId);

            // 3. Dapatkan mahasiswa_id berdasarkan user_id
            Optional<Admin> adminOptional = adminRepository.findByUserId(userId);
            if (!adminOptional.isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Admin tidak ditemukan.");
                return "redirect:/logout";
            }

            Map<String, String> konfigurasi = konfigurasiRepository.findAllAsMap().stream()
                .collect(Collectors.toMap(
                    entry -> entry.get("key"),
                    entry -> entry.get("value")
                ));
            
            model.addAttribute("konfigurasi", konfigurasi);

            int totalMahasiswa = userRepository.getAllMahasiswa();
            int totalSeniorResidence = userRepository.getAllSeniorResidence();
            int totalHelpDesk = userRepository.getAllHelpDesk();

            String ucapan = getUcapan();
            model.addAttribute("user", user);
            model.addAttribute("ucapan", ucapan);
            model.addAttribute("totalMahasiswa", totalMahasiswa);
            model.addAttribute("totalSeniorResidence", totalSeniorResidence);
            model.addAttribute("totalHelpDesk", totalHelpDesk);
        } else {
            logger.warn("User dengan email {} tidak ditemukan.", email);
            redirectAttributes.addFlashAttribute("error", "User tidak ditemukan.");
        }

        return "admin/Dashboard";
    }

    @PostMapping("/admin/update-konfigurasi")
    public String updateKonfigurasi(
            @RequestParam(value = "webFavicon", required = false) MultipartFile favicon,
            @RequestParam("webNamaWebsite") String namaWebsite,
            @RequestParam("webNamaGedung") String namaGedung,
            @RequestParam(value = "webLogo", required = false) MultipartFile logo,
            @RequestParam("webLantai") int lantai,
            @RequestParam("webKamar") int kamar,
            @RequestParam("webKasur") int kasur,
            @RequestParam("webMulaiTglCo") String mulaiTglCo,
            @RequestParam("webSelesaiTglCo") String selesaiTglCo,
            @RequestParam("webFooter") String footer,
            RedirectAttributes redirectAttributes) {
        try {
            // Variabel untuk menyimpan path file lama
            Path oldFaviconPath = null;
            Path oldLogoPath = null;

            // 1. Validasi dan simpan favicon jika ada
            if (favicon != null && !favicon.isEmpty()) {
                // Ambil file lama dari database
                Optional<String> oldFaviconOptional = konfigurasiRepository.findKValueByKKey("web-favicon");
                if (oldFaviconOptional.isPresent()) {
                    oldFaviconPath = Path.of("src/main/resources/static/assets/images/" + oldFaviconOptional.get());
                }

                // Validasi ukuran file maksimal 2 MB
                long maxFileSize = 2 * 1024 * 1024; // 2 MB
                if (favicon.getSize() > maxFileSize) {
                    redirectAttributes.addFlashAttribute("error", "Ukuran favicon maksimal 2 MB.");
                    return "redirect:/admin/dashboard";
                }

                // Validasi content type
                String contentType = favicon.getContentType();
                if (!"image/x-icon".equalsIgnoreCase(contentType)) {
                    redirectAttributes.addFlashAttribute("error", "Format favicon hanya boleh ICO.");
                    return "redirect:/admin/dashboard";
                }

                // Simpan file baru
                String originalFaviconName = favicon.getOriginalFilename();
                if (originalFaviconName == null || originalFaviconName.isBlank()) {
                    redirectAttributes.addFlashAttribute("error", "Nama favicon tidak valid.");
                    return "redirect:/admin/dashboard";
                }

                String sanitizedFaviconName = System.currentTimeMillis() + "_" + originalFaviconName.replaceAll("\\s+", "_").replaceAll("[^a-zA-Z0-9._-]", "");
                Path faviconPath = Path.of("src/main/resources/static/assets/images/" + sanitizedFaviconName);
                Files.write(faviconPath, favicon.getBytes());
                konfigurasiRepository.save(new Konfigurasi(1, "web-favicon", sanitizedFaviconName));

                // Hapus file lama setelah file baru berhasil disimpan
                if (oldFaviconPath != null) {
                    Files.deleteIfExists(oldFaviconPath);
                }
            }


            if (logo != null && !logo.isEmpty()) {
                // Ambil file lama dari database
                Optional<String> oldLogoOptional = konfigurasiRepository.findKValueByKKey("web-logo");
                if (oldLogoOptional.isPresent()) {
                    oldLogoPath = Path.of("src/main/resources/static/assets/images/" + oldLogoOptional.get());
                }

                // Validasi ukuran file maksimal 2 MB
                long maxFileSize = 2 * 1024 * 1024; // 2 MB
                if (logo.getSize() > maxFileSize) {
                    redirectAttributes.addFlashAttribute("error", "Ukuran logo maksimal 2 MB.");
                    return "redirect:/admin/dashboard";
                }

                // Validasi content type
                String contentType = logo.getContentType();
                if (!("image/jpeg".equalsIgnoreCase(contentType) || "image/png".equalsIgnoreCase(contentType))) {
                    redirectAttributes.addFlashAttribute("error", "Format logo hanya boleh JPG, JPEG, atau PNG.");
                    return "redirect:/admin/dashboard";
                }

                // Simpan file baru
                String originalLogoName = logo.getOriginalFilename();
                if (originalLogoName == null || originalLogoName.isBlank()) {
                    redirectAttributes.addFlashAttribute("error", "Nama logo tidak valid.");
                    return "redirect:/admin/dashboard";
                }

                String sanitizedLogoName = System.currentTimeMillis() + "_" + originalLogoName.replaceAll("\\s+", "_").replaceAll("[^a-zA-Z0-9._-]", "");
                Path logoPath = Path.of("src/main/resources/static/assets/images/" + sanitizedLogoName);
                Files.write(logoPath, logo.getBytes());
                konfigurasiRepository.save(new Konfigurasi(4, "web-logo", sanitizedLogoName));

                // Hapus file lama setelah file baru berhasil disimpan
                if (oldLogoPath != null) {
                    Files.deleteIfExists(oldLogoPath);
                }
            }
    
            // 3. Update data lain ke database
            konfigurasiRepository.save(new Konfigurasi(2, "web-nama-website", namaWebsite));
            konfigurasiRepository.save(new Konfigurasi(3, "web-nama-gedung", namaGedung));
            konfigurasiRepository.save(new Konfigurasi(5, "web-lantai", String.valueOf(lantai)));
            konfigurasiRepository.save(new Konfigurasi(6, "web-kamar", String.valueOf(kamar)));
            konfigurasiRepository.save(new Konfigurasi(7, "web-kasur", String.valueOf(kasur)));
            konfigurasiRepository.save(new Konfigurasi(8, "web-mulai-tgl-co", mulaiTglCo));
            konfigurasiRepository.save(new Konfigurasi(9, "web-selesai-tgl-co", selesaiTglCo));
            konfigurasiRepository.save(new Konfigurasi(10, "web-footer", footer));
    
            // 4. Berikan pesan sukses
            redirectAttributes.addFlashAttribute("success", "Konfigurasi berhasil diperbarui.");
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Terjadi kesalahan saat memperbarui konfigurasi.");
        }
    
        return "redirect:/admin/dashboard";
    }
}
