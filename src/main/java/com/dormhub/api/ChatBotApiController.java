package com.dormhub.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.dormhub.models.Jurusan;
import com.dormhub.models.LaporanBarang;
import com.dormhub.models.LaporanUmum;
import com.dormhub.models.Mahasiswa;
import com.dormhub.models.User;
import com.dormhub.repositories.JurusanRepository;
import com.dormhub.repositories.LaporanBarangRepository;
import com.dormhub.repositories.LaporanUmumRepository;
import com.dormhub.repositories.MahasiswaRepository;
import com.dormhub.repositories.UserRepository;
import com.dormhub.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/chatbot")
@CrossOrigin(origins = "*")
public class ChatBotApiController {

    private static final Logger logger = LoggerFactory.getLogger(ChatBotApiController.class);

    @Value("${gemini.api.key}")
    private String geminiApiKey;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private MahasiswaRepository mahasiswaRepository;
    
    @Autowired
    private JurusanRepository jurusanRepository;
    
    @Autowired
    private LaporanUmumRepository laporanUmumRepository;
    
    @Autowired
    private LaporanBarangRepository laporanBarangRepository;
    
    public ChatBotApiController() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }
    
    @PostMapping("/message")
    public ResponseEntity<JsonNode> processMessage(@RequestBody JsonNode requestBody, @CookieValue(name = "jwt", required = false) String token) {
        try {
            logger.info("Received chatbot request");
            
            // Dapatkan pesan dari request body
            String userMessage = requestBody.get("message").asText();
            logger.debug("User message: {}", userMessage);
            
            // Get user info from JWT token
            User currentUser = null;
            Mahasiswa mahasiswa = null;
            String userSpecificInfo = "";
            
            if (token != null && !token.isEmpty()) {
                String email = jwtTokenProvider.getUsername(token);
                if (email != null) {
                    Optional<User> userOpt = userRepository.findByEmail(email);
                    if (userOpt.isPresent()) {
                        currentUser = userOpt.get();
                        
                        // Check if user is a student
                        if (currentUser.getLevelId() == 1) { // assuming 1 is for Mahasiswa
                            Optional<Mahasiswa> mahasiswaOpt = mahasiswaRepository.findByUserId(currentUser.getId());
                            if (mahasiswaOpt.isPresent()) {
                                mahasiswa = mahasiswaOpt.get();
                                
                                // Get jurusan info
                                Optional<Jurusan> jurusanOpt = jurusanRepository.findById(mahasiswa.getJurusanId());
                                String jurusanName = jurusanOpt.isPresent() ? jurusanOpt.get().getNama() : "tidak diketahui";
                                
                                // Count reports
                                List<LaporanUmum> laporanUmumList = laporanUmumRepository.findByMahasiswaId(mahasiswa.getId());
                                List<LaporanBarang> laporanBarangList = laporanBarangRepository.findByMahasiswaId(mahasiswa.getId());
                                
                                int totalLaporanUmum = laporanUmumList.size();
                                int totalLaporanBarang = laporanBarangList.size();
                                int totalLaporanKeluhan = (int) laporanUmumList.stream()
                                    .filter(l -> "Keluhan".equals(l.getJenis()))
                                    .count();
                                int totalLaporanIzin = (int) laporanUmumList.stream()
                                    .filter(l -> "Izin".equals(l.getJenis()))
                                    .count();
                                
                                // Add user-specific data to be included in the prompt
                                userSpecificInfo = String.format(
                                    "Informasi pengguna: Nama: %s, Email: %s, Jurusan: %s, " +
                                    "Nomor Kamar: %d, Nomor Kasur: %d, " +
                                    "Total Laporan Umum: %d, Total Laporan Keluhan: %d, Total Laporan Izin: %d, " +
                                    "Total Paket/Barang: %d",
                                    currentUser.getNamaLengkap(),
                                    currentUser.getEmail(),
                                    jurusanName,
                                    mahasiswa.getNoKamar(),
                                    mahasiswa.getNoKasur(),
                                    totalLaporanUmum,
                                    totalLaporanKeluhan,
                                    totalLaporanIzin,
                                    totalLaporanBarang
                                );
                            }
                        }
                    }
                }
            }
            
            // Tambahkan konteks untuk Gemini
            String fullPrompt = "Kamu adalah asisten DormHub, sebuah aplikasi asrama mahasiswa. Berikan jawaban singkat dan padat dalam Bahasa Indonesia. ";
            fullPrompt += "Informasi tentang DormHub: DormHub adalah aplikasi manajemen asrama yang memiliki fitur check-in, check-out, laporan keluhan, dan informasi kamar. ";
            
            // Add user-specific info if available
            if (!userSpecificInfo.isEmpty()) {
                fullPrompt += userSpecificInfo + ". ";
                fullPrompt += "Gunakan informasi pengguna ini untuk menjawab pertanyaan yang relevan. ";
                fullPrompt += "Jika ditanya tentang informasi spesifik pengguna seperti 'kamar saya berapa?' atau 'berapa jumlah laporan saya?', berikan jawaban berdasarkan data yang tersedia. ";
            }
            
            fullPrompt += "Jika ditanya tentang informasi kamar, saran untuk memeriksa menu 'Informasi Kamar'. ";
            fullPrompt += "Jika ditanya tentang laporan, saran untuk membuat laporan di menu 'Laporan' > 'Buat Laporan'. ";
            fullPrompt += "Berikut adalah pesan pengguna: " + userMessage;
            
            // Buat request body untuk Gemini API
            ObjectNode geminiRequestBody = objectMapper.createObjectNode();
            ObjectNode content = objectMapper.createObjectNode();
            ObjectNode part = objectMapper.createObjectNode();
            part.put("text", fullPrompt);
            
            // Struktur JSON untuk Gemini API v1
            content.set("parts", objectMapper.createArrayNode().add(part));
            geminiRequestBody.set("contents", objectMapper.createArrayNode().add(content));
            
            // Konfigurasi generasi
            ObjectNode generationConfig = objectMapper.createObjectNode();
            generationConfig.put("temperature", 0.7);
            generationConfig.put("maxOutputTokens", 150);
            geminiRequestBody.set("generationConfig", generationConfig);
            
            // Log request body
            logger.debug("Request body: {}", objectMapper.writeValueAsString(geminiRequestBody));
            
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Buat HTTP entity dengan body dan headers
            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(geminiRequestBody), headers);
            
            // Kirim request ke Gemini API
            String geminiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent?key=" + geminiApiKey;
            logger.info("Sending request to Gemini API");
            
            ResponseEntity<JsonNode> geminiResponse = restTemplate.exchange(
                geminiUrl,
                HttpMethod.POST,
                entity,
                JsonNode.class
            );
            
            // Log response
            logger.debug("Response status: {}", geminiResponse.getStatusCode());
            logger.debug("Response body: {}", geminiResponse.getBody());
            
            // Kembalikan response dari Gemini API
            return ResponseEntity.ok(geminiResponse.getBody());
        } catch (Exception e) {
            // Log error
            logger.error("Error processing chatbot message", e);
            
            // Buat response error
            ObjectNode errorResponse = objectMapper.createObjectNode();
            errorResponse.put("error", "Terjadi kesalahan saat memproses pesan");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/fallback")
    public ResponseEntity<JsonNode> fallbackResponse(@RequestBody JsonNode requestBody, @CookieValue(name = "jwt", required = false) String token) {
        try {
            logger.info("Using fallback response");
            
            String userMessage = requestBody.get("message").asText();
            logger.debug("User message for fallback: {}", userMessage);
            
            // Get user info from JWT token
            User currentUser = null;
            Mahasiswa mahasiswa = null;
            
            if (token != null && !token.isEmpty()) {
                String email = jwtTokenProvider.getUsername(token);
                if (email != null) {
                    Optional<User> userOpt = userRepository.findByEmail(email);
                    if (userOpt.isPresent()) {
                        currentUser = userOpt.get();
                        
                        // Check if user is a student
                        if (currentUser.getLevelId() == 1) { // assuming 1 is for Mahasiswa
                            Optional<Mahasiswa> mahasiswaOpt = mahasiswaRepository.findByUserId(currentUser.getId());
                            if (mahasiswaOpt.isPresent()) {
                                mahasiswa = mahasiswaOpt.get();
                            }
                        }
                    }
                }
            }
            
            ObjectNode responseNode = objectMapper.createObjectNode();
            ObjectNode candidate = objectMapper.createObjectNode();
            ObjectNode content = objectMapper.createObjectNode();
            ObjectNode part = objectMapper.createObjectNode();
            
            // Logika fallback dengan data user
            String response;
            String lowercaseMessage = userMessage.toLowerCase();
            
            if (lowercaseMessage.contains("kamar") && lowercaseMessage.contains("saya") && mahasiswa != null) {
                response = String.format("Nomor kamar Anda adalah %d dan nomor kasur Anda adalah %d.", 
                    mahasiswa.getNoKamar(), mahasiswa.getNoKasur());
            } else if ((lowercaseMessage.contains("paket") || lowercaseMessage.contains("barang")) && 
                      lowercaseMessage.contains("saya") && mahasiswa != null) {
                List<LaporanBarang> laporanBarangList = laporanBarangRepository.findByMahasiswaId(mahasiswa.getId());
                response = String.format("Anda memiliki %d paket/barang yang tercatat dalam sistem.", laporanBarangList.size());
            } else if (lowercaseMessage.contains("laporan") && lowercaseMessage.contains("saya") && mahasiswa != null) {
                List<LaporanUmum> laporanUmumList = laporanUmumRepository.findByMahasiswaId(mahasiswa.getId());
                response = String.format("Anda memiliki total %d laporan yang tercatat dalam sistem.", laporanUmumList.size());
            } else if (lowercaseMessage.contains("keluhan") && lowercaseMessage.contains("saya") && mahasiswa != null) {
                List<LaporanUmum> laporanKeluhanList = laporanUmumRepository.findByMahasiswaIdAndJenis(mahasiswa.getId(), "Keluhan");
                response = String.format("Anda memiliki %d laporan keluhan yang tercatat dalam sistem.", laporanKeluhanList.size());
            } else if (lowercaseMessage.contains("izin") && lowercaseMessage.contains("saya") && mahasiswa != null) {
                List<LaporanUmum> laporanIzinList = laporanUmumRepository.findByMahasiswaIdAndJenis(mahasiswa.getId(), "Izin");
                response = String.format("Anda memiliki %d laporan izin yang tercatat dalam sistem.", laporanIzinList.size());
            } else if (lowercaseMessage.contains("kamar")) {
                response = "Untuk informasi kamar, Anda bisa mengakses menu 'Informasi Kamar' di sidebar kiri.";
            } else if (lowercaseMessage.contains("laporan") || lowercaseMessage.contains("keluhan")) {
                response = "Untuk membuat laporan atau keluhan, silakan akses menu 'Laporan' > 'Buat Laporan' di sidebar kiri.";
            } else if (lowercaseMessage.contains("checkout") || lowercaseMessage.contains("check-out")) {
                response = "Untuk melakukan check-out, harap ikuti petunjuk di notifikasi pada dashboard Anda.";
            } else if (lowercaseMessage.contains("halo") || lowercaseMessage.contains("hai") || lowercaseMessage.contains("hi")) {
                if (currentUser != null) {
                    response = "Halo " + currentUser.getNamaLengkap() + "! Ada yang bisa saya bantu seputar layanan asrama?";
                } else {
                    response = "Halo! Ada yang bisa saya bantu seputar layanan asrama?";
                }
            } else {
                response = "Maaf, saya tidak dapat memahami pertanyaan Anda. Silakan hubungi admin untuk informasi lebih lanjut.";
            }
            
            // Struktur respons seperti Gemini API
            part.put("text", response);
            content.set("parts", objectMapper.createArrayNode().add(part));
            candidate.set("content", content);
            responseNode.set("candidates", objectMapper.createArrayNode().add(candidate));
            
            return ResponseEntity.ok(responseNode);
        } catch (Exception e) {
            logger.error("Error in fallback response", e);
            
            ObjectNode errorResponse = objectMapper.createObjectNode();
            errorResponse.put("error", "Terjadi kesalahan saat memproses pesan");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
} 