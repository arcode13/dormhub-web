package com.dormhub.api;

import com.dormhub.model.*;
import com.dormhub.repository.*;
import com.dormhub.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class DashboardApiController {

    @Autowired private UserRepository userRepository;
    @Autowired private MahasiswaRepository mahasiswaRepository;
    @Autowired private SeniorResidenceRepository seniorResidenceRepository;
    @Autowired private HelpDeskRepository helpDeskRepository;
    @Autowired private AdminRepository adminRepository;
    @Autowired private LaporanBarangRepository laporanBarangRepository;
    @Autowired private LaporanUmumRepository laporanUmumRepository;
    @Autowired private KonfigurasiRepository konfigurasiRepository;
    @Autowired private LaporanService laporanService;
    @Autowired private SeniorResidenceService seniorResidenceService;

    @GetMapping("/mahasiswa/dashboard")
    public ResponseEntity<?> mahasiswaDashboard(Authentication auth) {
        String email = auth.getName();
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) return ResponseEntity.status(404).body(Map.of("error", "User tidak ditemukan"));
        User user = userOpt.get();

        Optional<Mahasiswa> mhsOpt = mahasiswaRepository.findByUserId(user.getId());
        Mahasiswa mhs = mhsOpt.orElse(null);
        boolean isSenior = mhs != null && seniorResidenceRepository.existsByMahasiswaId(mhs.getId());

        int izin = laporanService.getJumlahLaporanIzinBulanIni(user.getId());
        int keluhan = laporanService.getJumlahLaporanKeluhanBulanIni(user.getId());
        int total = izin + keluhan;

        List<LaporanBarang> laporanBarang = laporanBarangRepository.findByMahasiswaIdAndStatus(user.getId(), "menunggu");
        laporanBarang.forEach(l -> l.setFormattedCreatedAt(formatDateTime(l.getCreatedAt())));

        Map<String, String> konfigurasi = konfigurasiRepository.findAllAsMap().stream()
            .collect(Collectors.toMap(e -> e.get("key"), e -> e.get("value")));

        return ResponseEntity.ok(Map.of(
            "user", user,
            "mahasiswa", mhs,
            "isSeniorResidence", isSenior,
            "laporanIzinBulanIni", izin,
            "laporanKeluhanBulanIni", keluhan,
            "totalLaporan", total,
            "laporanBarangList", laporanBarang,
            "konfigurasi", konfigurasi,
            "ucapan", getUcapan()
        ));
    }

    @PostMapping("/mahasiswa/checkin")
    public ResponseEntity<?> checkin(Authentication auth) {
        String email = auth.getName();
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) return ResponseEntity.status(404).body(Map.of("error", "User tidak ditemukan"));
        User user = userOpt.get();

        Optional<Mahasiswa> mhsOpt = mahasiswaRepository.findByUserId(user.getId());
        if (mhsOpt.isEmpty()) return ResponseEntity.status(404).body(Map.of("error", "Data mahasiswa tidak ditemukan"));

        Mahasiswa mhs = mhsOpt.get();
        if (mhs.getIsCheckin() == 1) return ResponseEntity.badRequest().body(Map.of("error", "Sudah check-in sebelumnya"));

        Optional<String> mulaiTglCo = konfigurasiRepository.findKValueByKKey("web-mulai-tgl-co");
        if (mulaiTglCo.isPresent()) {
            LocalDateTime batas = LocalDateTime.parse(mulaiTglCo.get() + "T00:00:00");
            if (LocalDateTime.now().isAfter(batas.plusDays(7))) {
                return ResponseEntity.badRequest().body(Map.of("error", "Batas waktu check-in terlewati"));
            }
        }

        mhs.setIsCheckin(1);
        mhs.setWaktuCheckin(LocalDateTime.now());
        mahasiswaRepository.save(mhs);

        return ResponseEntity.ok(Map.of("message", "Berhasil check-in"));
    }

    @PostMapping("/mahasiswa/checkout")
    public ResponseEntity<?> checkout(Authentication auth) {
        String email = auth.getName();
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) return ResponseEntity.status(404).body(Map.of("error", "User tidak ditemukan"));
        User user = userOpt.get();

        Optional<Mahasiswa> mhsOpt = mahasiswaRepository.findByUserId(user.getId());
        if (mhsOpt.isEmpty()) return ResponseEntity.status(404).body(Map.of("error", "Data mahasiswa tidak ditemukan"));

        Mahasiswa mhs = mhsOpt.get();
        if (mhs.getIsCheckout() == 1) return ResponseEntity.badRequest().body(Map.of("error", "Sudah check-out sebelumnya"));

        Optional<String> selesaiTglCo = konfigurasiRepository.findKValueByKKey("web-selesai-tgl-co");
        if (selesaiTglCo.isPresent()) {
            LocalDateTime batas = LocalDateTime.parse(selesaiTglCo.get() + "T23:59:59");
            if (LocalDateTime.now().isAfter(batas)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Batas waktu check-out terlewati"));
            }
        }

        mhs.setIsCheckout(1);
        mhs.setWaktuCheckout(LocalDateTime.now());
        mahasiswaRepository.save(mhs);

        return ResponseEntity.ok(Map.of("message", "Berhasil check-out"));
    }

    @PostMapping("/mahasiswa/daftar-barang/{id}/terima")
    public ResponseEntity<?> updateStatusLaporanBarang(@PathVariable("id") int id) {
        Optional<LaporanBarang> laporanBarangOptional = laporanBarangRepository.findById(id);
        if (laporanBarangOptional.isPresent()) {
            LaporanBarang laporanBarang = laporanBarangOptional.get();
            laporanBarang.setStatus("Diterima");
            laporanBarang.setUpdatedAt(LocalDateTime.now());
            laporanBarangRepository.save(laporanBarang);

            String jenis = laporanBarang.getJenis();
            return ResponseEntity.ok(Map.of("message", jenis + " berhasil diterima."));
        } else {
            return ResponseEntity.status(404).body(Map.of("error", "Barang tidak ditemukan."));
        }
    }

    @GetMapping("/senior-residence/dashboard")
    public ResponseEntity<?> seniorDashboard(Authentication auth) {
        String email = auth.getName();
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) return ResponseEntity.status(404).body(Map.of("error", "User tidak ditemukan"));
        User user = userOpt.get();

        SeniorResidence sr = seniorResidenceService.getSeniorResidenceByMahasiswaId(user.getId()).orElse(null);
        if (sr == null) return ResponseEntity.status(403).body(Map.of("error", "Bukan Senior Residence"));

        List<LaporanUmum> laporan = laporanUmumRepository.findAllIzin();
        laporan.forEach(l -> {
            l.setFormattedCreatedAt(formatDateTime(l.getCreatedAt()));
            mahasiswaRepository.findById(l.getMahasiswaId()).ifPresent(m -> {
                l.setNamaLengkap(m.getUser().getNamaLengkap());
                l.setNoKamar(m.getNoKamar());
            });
        });

        return ResponseEntity.ok(Map.of(
            "user", user,
            "laporanIzinBulanIni", laporanUmumRepository.getSemuaJumlahLaporanIzinBulanIni(),
            "totalLaporanIzin", laporanUmumRepository.getTotalSemuaLaporanIzin(),
            "daftarLaporan", laporan,
            "konfigurasi", getKonfigurasiMap(),
            "ucapan", getUcapan()
        ));
    }

    @GetMapping("/help-desk/dashboard")
    public ResponseEntity<?> helpDeskDashboard(Authentication auth) {
        String email = auth.getName();
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) return ResponseEntity.status(404).body(Map.of("error", "User tidak ditemukan"));
        User user = userOpt.get();

        Optional<HelpDesk> helpDeskOpt = helpDeskRepository.findByUserId(user.getId());
        if (helpDeskOpt.isEmpty()) return ResponseEntity.status(403).body(Map.of("error", "Bukan Help Desk"));

        List<LaporanBarang> laporan = laporanBarangRepository.findByHelpdeskId(helpDeskOpt.get().getId());
        laporan.forEach(l -> {
            l.setFormattedCreatedAt(formatDateTime(l.getCreatedAt()));
            mahasiswaRepository.findById(l.getMahasiswaId()).ifPresent(m -> {
                l.setNamaLengkap(m.getUser().getNamaLengkap());
                l.setNoKamar(m.getNoKamar());
            });
        });

        return ResponseEntity.ok(Map.of(
            "user", user,
            "laporanBarangList", laporan,
            "mahasiswaList", mahasiswaRepository.findAll(),
            "konfigurasi", getKonfigurasiMap(),
            "ucapan", getUcapan()
        ));
    }

    @GetMapping("/admin/dashboard")
    public ResponseEntity<?> adminDashboard(Authentication auth) {
        String email = auth.getName();
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) return ResponseEntity.status(404).body(Map.of("error", "User tidak ditemukan"));
        User user = userOpt.get();

        Optional<Admin> adminOpt = adminRepository.findByUserId(user.getId());
        if (adminOpt.isEmpty()) return ResponseEntity.status(403).body(Map.of("error", "Bukan Admin"));

        return ResponseEntity.ok(Map.of(
            "user", user,
            "totalMahasiswa", userRepository.getAllMahasiswa(),
            "totalSeniorResidence", userRepository.getAllSeniorResidence(),
            "totalHelpDesk", userRepository.getAllHelpDesk(),
            "konfigurasi", getKonfigurasiMap(),
            "ucapan", getUcapan()
        ));
    }

    @PostMapping("/admin/update-konfigurasi")
    public ResponseEntity<?> updateKonfigurasi(
            @RequestParam(value = "webFavicon", required = false) MultipartFile favicon,
            @RequestParam("webNamaWebsite") String namaWebsite,
            @RequestParam("webNamaGedung") String namaGedung,
            @RequestParam(value = "webLogo", required = false) MultipartFile logo,
            @RequestParam("webLantai") int lantai,
            @RequestParam("webKamar") int kamar,
            @RequestParam("webKasur") int kasur,
            @RequestParam("webMulaiTglCo") String mulaiTglCo,
            @RequestParam("webSelesaiTglCo") String selesaiTglCo,
            @RequestParam("webFooter") String footer
    ) {
        try {
            Path oldFaviconPath = null;
            Path oldLogoPath = null;

            if (favicon != null && !favicon.isEmpty()) {
                Optional<String> oldFaviconOptional = konfigurasiRepository.findKValueByKKey("web-favicon");
                if (oldFaviconOptional.isPresent()) {
                    oldFaviconPath = Path.of("src/main/resources/static/assets/images/" + oldFaviconOptional.get());
                }

                long maxFileSize = 2 * 1024 * 1024;
                if (favicon.getSize() > maxFileSize) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Ukuran favicon maksimal 2 MB."));
                }

                String contentType = favicon.getContentType();
                if (!"image/x-icon".equalsIgnoreCase(contentType)) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Format favicon hanya boleh ICO."));
                }

                String originalFaviconName = favicon.getOriginalFilename();
                String sanitizedFaviconName = System.currentTimeMillis() + "_" + originalFaviconName.replaceAll("\\s+", "_").replaceAll("[^a-zA-Z0-9._-]", "");
                Path faviconPath = Path.of("src/main/resources/static/assets/images/" + sanitizedFaviconName);
                Files.write(faviconPath, favicon.getBytes());
                konfigurasiRepository.save(new Konfigurasi(1, "web-favicon", sanitizedFaviconName));

                if (oldFaviconPath != null) Files.deleteIfExists(oldFaviconPath);
            }

            if (logo != null && !logo.isEmpty()) {
                Optional<String> oldLogoOptional = konfigurasiRepository.findKValueByKKey("web-logo");
                if (oldLogoOptional.isPresent()) {
                    oldLogoPath = Path.of("src/main/resources/static/assets/images/" + oldLogoOptional.get());
                }

                long maxFileSize = 2 * 1024 * 1024;
                if (logo.getSize() > maxFileSize) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Ukuran logo maksimal 2 MB."));
                }

                String contentType = logo.getContentType();
                if (!("image/jpeg".equalsIgnoreCase(contentType) || "image/png".equalsIgnoreCase(contentType))) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Format logo hanya boleh JPG, JPEG, atau PNG."));
                }

                String originalLogoName = logo.getOriginalFilename();
                String sanitizedLogoName = System.currentTimeMillis() + "_" + originalLogoName.replaceAll("\\s+", "_").replaceAll("[^a-zA-Z0-9._-]", "");
                Path logoPath = Path.of("src/main/resources/static/assets/images/" + sanitizedLogoName);
                Files.write(logoPath, logo.getBytes());
                konfigurasiRepository.save(new Konfigurasi(4, "web-logo", sanitizedLogoName));

                if (oldLogoPath != null) Files.deleteIfExists(oldLogoPath);
            }

            konfigurasiRepository.save(new Konfigurasi(2, "web-nama-website", namaWebsite));
            konfigurasiRepository.save(new Konfigurasi(3, "web-nama-gedung", namaGedung));
            konfigurasiRepository.save(new Konfigurasi(5, "web-lantai", String.valueOf(lantai)));
            konfigurasiRepository.save(new Konfigurasi(6, "web-kamar", String.valueOf(kamar)));
            konfigurasiRepository.save(new Konfigurasi(7, "web-kasur", String.valueOf(kasur)));
            konfigurasiRepository.save(new Konfigurasi(8, "web-mulai-tgl-co", mulaiTglCo));
            konfigurasiRepository.save(new Konfigurasi(9, "web-selesai-tgl-co", selesaiTglCo));
            konfigurasiRepository.save(new Konfigurasi(10, "web-footer", footer));

            return ResponseEntity.ok(Map.of("message", "Konfigurasi berhasil diperbarui."));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Terjadi kesalahan saat memperbarui konfigurasi."));
        }
    }

    private Map<String, String> getKonfigurasiMap() {
        return konfigurasiRepository.findAllAsMap().stream()
            .collect(Collectors.toMap(e -> e.get("key"), e -> e.get("value")));
    }

    private String formatDateTime(LocalDateTime dt) {
        return dt.format(DateTimeFormatter.ofPattern("d MMMM yyyy - HH:mm", new Locale("id", "ID")));
    }

    private String getUcapan() {
        LocalTime now = LocalTime.now();
        if (now.isBefore(LocalTime.NOON)) return "Selamat Pagi";
        if (now.isBefore(LocalTime.of(18, 0))) return "Selamat Siang";
        return "Selamat Malam";
    }
}