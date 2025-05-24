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
import org.springframework.web.bind.annotation.GetMapping;

import com.dormhub.model.Jurusan;
import com.dormhub.model.LaporanBarang;
import com.dormhub.model.LaporanUmum;
import com.dormhub.model.Mahasiswa;
import com.dormhub.model.User;
import com.dormhub.repository.JurusanRepository;
import com.dormhub.repository.LaporanBarangRepository;
import com.dormhub.repository.LaporanUmumRepository;
import com.dormhub.repository.MahasiswaRepository;
import com.dormhub.repository.UserRepository;
import com.dormhub.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.HashMap;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

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
                        if (currentUser.getLevel().getId().intValue() == 1) { // assuming 1 is for Mahasiswa
                            Optional<Mahasiswa> mahasiswaOpt = mahasiswaRepository.findByUserId(currentUser.getId());
                            if (mahasiswaOpt.isPresent()) {
                                mahasiswa = mahasiswaOpt.get();
                                
                                // Get jurusan info directly from the mahasiswa entity
                                String jurusanName = mahasiswa.getJurusan().getNama();
                                
                                // Count reports
                                List<LaporanUmum> laporanUmumList = laporanUmumRepository.findAllByMahasiswaId(mahasiswa.getId());
                                List<LaporanBarang> laporanBarangList = laporanBarangRepository.findByMahasiswaIdAndStatus(mahasiswa.getId(), "menunggu");
                                
                                int totalLaporanUmum = laporanUmumList.size();
                                int totalLaporanBarang = laporanBarangList.size();
                                int totalLaporanKeluhan = laporanUmumRepository.countLaporanKeluhan(mahasiswa.getId());
                                int totalLaporanIzin = laporanUmumRepository.countLaporanIzin(mahasiswa.getId());
                                
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
                logger.debug("JWT token present: {}", token);
                String email = jwtTokenProvider.getUsername(token);
                logger.debug("Extracted email from token: {}", email);
                
                if (email != null) {
                    Optional<User> userOpt = userRepository.findByEmail(email);
                    if (userOpt.isPresent()) {
                        currentUser = userOpt.get();
                        logger.debug("Found user: {}", currentUser.getNamaLengkap());
                        
                        // Check if user is a student
                        if (currentUser.getLevel().getId().intValue() == 1) { // assuming 1 is for Mahasiswa
                            Optional<Mahasiswa> mahasiswaOpt = mahasiswaRepository.findByUserId(currentUser.getId());
                            if (mahasiswaOpt.isPresent()) {
                                mahasiswa = mahasiswaOpt.get();
                                logger.debug("Found mahasiswa record with kamar: {}, kasur: {}", 
                                    mahasiswa.getNoKamar(), mahasiswa.getNoKasur());
                            } else {
                                logger.warn("Mahasiswa record not found for user ID: {}", currentUser.getId());
                            }
                        } else {
                            logger.debug("User is not a student, level: {}", currentUser.getLevel().getId());
                        }
                    } else {
                        logger.warn("User not found for email: {}", email);
                    }
                } else {
                    logger.warn("Could not extract email from token");
                }
            } else {
                logger.warn("No JWT token provided");
            }
            
            ObjectNode responseNode = objectMapper.createObjectNode();
            ObjectNode candidate = objectMapper.createObjectNode();
            ObjectNode content = objectMapper.createObjectNode();
            ObjectNode part = objectMapper.createObjectNode();
            
            // Logika fallback dengan data user
            String response;
            String lowercaseMessage = userMessage.toLowerCase();
            
            if (lowercaseMessage.contains("nama") && lowercaseMessage.contains("saya") && currentUser != null) {
                response = String.format("Nama Anda adalah %s.", currentUser.getNamaLengkap());
                logger.debug("Responding with user name: {}", response);
            } else if (lowercaseMessage.contains("kamar") && lowercaseMessage.contains("saya") && mahasiswa != null) {
                response = String.format("Nomor kamar Anda adalah %d dan nomor kasur Anda adalah %d.", 
                    mahasiswa.getNoKamar(), mahasiswa.getNoKasur());
                logger.debug("Responding with room info: {}", response);
            } else if ((lowercaseMessage.contains("paket") || lowercaseMessage.contains("barang")) && 
                      lowercaseMessage.contains("saya") && mahasiswa != null) {
                List<LaporanBarang> laporanBarangList = laporanBarangRepository.findByMahasiswaIdAndStatus(mahasiswa.getId(), "menunggu");
                response = String.format("Anda memiliki %d paket/barang yang tercatat dalam sistem.", laporanBarangList.size());
                logger.debug("Responding with package count: {}", response);
            } else if (lowercaseMessage.contains("laporan") && lowercaseMessage.contains("saya") && mahasiswa != null) {
                List<LaporanUmum> laporanUmumList = laporanUmumRepository.findAllByMahasiswaId(mahasiswa.getId());
                response = String.format("Anda memiliki total %d laporan yang tercatat dalam sistem.", laporanUmumList.size());
                logger.debug("Responding with report count: {}", response);
            } else if (lowercaseMessage.contains("keluhan") && lowercaseMessage.contains("saya") && mahasiswa != null) {
                int totalKeluhanCount = laporanUmumRepository.countLaporanKeluhan(mahasiswa.getId());
                response = String.format("Anda memiliki %d laporan keluhan yang tercatat dalam sistem.", totalKeluhanCount);
                logger.debug("Responding with complaint count: {}", response);
            } else if (lowercaseMessage.contains("izin") && lowercaseMessage.contains("saya") && mahasiswa != null) {
                int totalIzinCount = laporanUmumRepository.countLaporanIzin(mahasiswa.getId());
                response = String.format("Anda memiliki %d laporan izin yang tercatat dalam sistem.", totalIzinCount);
                logger.debug("Responding with permission count: {}", response);
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

    @PostMapping("/direct-query")
    public ResponseEntity<Map<String, String>> directQuery(@RequestBody Map<String, String> request, 
                                                         HttpServletRequest httpRequest) {
        Map<String, String> response = new HashMap<>();
        String question = request.get("question");
        
        logger.info("Received direct query: {}", question);
        
        // Default response
        response.put("answer", "Maaf, saya tidak dapat memahami pertanyaan Anda.");
        
        if (question == null || question.trim().isEmpty()) {
            return ResponseEntity.ok(response);
        }
        
        // Get user info from any possible JWT token cookie
        User currentUser = null;
        Mahasiswa mahasiswa = null;
        
        Cookie[] cookies = httpRequest.getCookies();
        if (cookies != null) {
            // Try common JWT cookie names
            String[] possibleNames = {"jwt", "JWT", "access_token", "Authorization", "auth_token", "token", "JSESSIONID"};
            String jwtToken = null;
            
            for (Cookie cookie : cookies) {
                logger.debug("Found cookie: {}={}", cookie.getName(), cookie.getValue());
                for (String name : possibleNames) {
                    if (name.equals(cookie.getName())) {
                        jwtToken = cookie.getValue();
                        logger.info("Found potential JWT in cookie: {}", name);
                        break;
                    }
                }
                if (jwtToken != null) break;
            }
            
            if (jwtToken != null) {
                String email = jwtTokenProvider.getUsername(jwtToken);
                logger.debug("Extracted email from token: {}", email);
                
                if (email != null) {
                    Optional<User> userOpt = userRepository.findByEmail(email);
                    if (userOpt.isPresent()) {
                        currentUser = userOpt.get();
                        logger.debug("Found user: {}", currentUser.getNamaLengkap());
                        
                        // Check if user is a student
                        if (currentUser.getLevel().getId().intValue() == 1) { // assuming 1 is for Mahasiswa
                            Optional<Mahasiswa> mahasiswaOpt = mahasiswaRepository.findByUserId(currentUser.getId());
                            if (mahasiswaOpt.isPresent()) {
                                mahasiswa = mahasiswaOpt.get();
                                logger.debug("Found mahasiswa record with kamar: {}, kasur: {}", 
                                    mahasiswa.getNoKamar(), mahasiswa.getNoKasur());
                            } else {
                                logger.warn("Mahasiswa record not found for user ID: {}", currentUser.getId());
                                response.put("answer", "Maaf, data mahasiswa Anda tidak ditemukan.");
                                return ResponseEntity.ok(response);
                            }
                        }
                    }
                }
            } else {
                logger.warn("No JWT token found in cookies");
            }
        } else {
            logger.warn("No cookies found in the request");
        }
        
        // Process specific questions based on keywords
        String lowercaseQuestion = question.toLowerCase();
        
        try {
            if (lowercaseQuestion.contains("nama") && lowercaseQuestion.contains("saya")) {
                if (currentUser != null) {
                    response.put("answer", String.format("Nama Anda adalah %s.", currentUser.getNamaLengkap()));
                    logger.info("Responding with user name for user ID: {}", currentUser.getId());
                } else {
                    response.put("answer", "Maaf, informasi pengguna tidak tersedia. Anda mungkin perlu login kembali.");
                }
            } else if (lowercaseQuestion.contains("kamar") && lowercaseQuestion.contains("saya")) {
                if (mahasiswa != null) {
                    response.put("answer", String.format("Nomor kamar Anda adalah %d dan nomor kasur Anda adalah %d.", 
                        mahasiswa.getNoKamar(), mahasiswa.getNoKasur()));
                    logger.info("Responding with room info for user ID: {}", currentUser.getId());
                } else {
                    response.put("answer", "Maaf, informasi kamar tidak tersedia. Anda mungkin perlu login kembali.");
                }
            } else if ((lowercaseQuestion.contains("paket") || lowercaseQuestion.contains("barang")) && 
                      lowercaseQuestion.contains("saya")) {
                if (mahasiswa != null) {
                    List<LaporanBarang> laporanBarangList = laporanBarangRepository.findByMahasiswaIdAndStatus(mahasiswa.getId(), "menunggu");
                    response.put("answer", String.format("Anda memiliki %d paket/barang yang tercatat dalam sistem.", laporanBarangList.size()));
                    logger.info("Responding with package count for user ID: {}", currentUser.getId());
                } else {
                    response.put("answer", "Maaf, informasi paket tidak tersedia. Anda mungkin perlu login kembali.");
                }
            } else if (lowercaseQuestion.contains("laporan") && lowercaseQuestion.contains("saya")) {
                if (mahasiswa != null) {
                    List<LaporanUmum> laporanUmumList = laporanUmumRepository.findAllByMahasiswaId(mahasiswa.getId());
                    response.put("answer", String.format("Anda memiliki total %d laporan yang tercatat dalam sistem.", laporanUmumList.size()));
                    logger.info("Responding with report count for user ID: {}", currentUser.getId());
                } else {
                    response.put("answer", "Maaf, informasi laporan tidak tersedia. Anda mungkin perlu login kembali.");
                }
            } else if (lowercaseQuestion.contains("keluhan") && lowercaseQuestion.contains("saya")) {
                if (mahasiswa != null) {
                    int totalKeluhanCount = laporanUmumRepository.countLaporanKeluhan(mahasiswa.getId());
                    response.put("answer", String.format("Anda memiliki %d laporan keluhan yang tercatat dalam sistem.", totalKeluhanCount));
                    logger.info("Responding with complaint count for user ID: {}", currentUser.getId());
                } else {
                    response.put("answer", "Maaf, informasi keluhan tidak tersedia. Anda mungkin perlu login kembali.");
                }
            } else if (lowercaseQuestion.contains("izin") && lowercaseQuestion.contains("saya")) {
                if (mahasiswa != null) {
                    int totalIzinCount = laporanUmumRepository.countLaporanIzin(mahasiswa.getId());
                    response.put("answer", String.format("Anda memiliki %d laporan izin yang tercatat dalam sistem.", totalIzinCount));
                    logger.info("Responding with permission count for user ID: {}", currentUser.getId());
                } else {
                    response.put("answer", "Maaf, informasi izin tidak tersedia. Anda mungkin perlu login kembali.");
                }
            } else {
                // Default fallback for other questions
                response.put("answer", "Maaf, saya tidak memahami pertanyaan Anda. Silakan tanyakan tentang kamar, laporan, atau paket Anda.");
            }
        } catch (Exception e) {
            logger.error("Error processing query", e);
            response.put("answer", "Terjadi kesalahan saat memproses pertanyaan Anda: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/test-query")
    public ResponseEntity<Map<String, Object>> testQuery(@CookieValue(name = "jwt", required = false) String token) {
        Map<String, Object> response = new HashMap<>();
        
        // Log token untuk debugging
        logger.info("Test-query endpoint called with token present: {}", (token != null && !token.isEmpty()));
        
        if (token != null && !token.isEmpty()) {
            response.put("tokenPresent", true);
            
            try {
                // Decode token
                String email = jwtTokenProvider.getUsername(token);
                response.put("email", email);
                
                if (email != null) {
                    Optional<User> userOpt = userRepository.findByEmail(email);
                    if (userOpt.isPresent()) {
                        User user = userOpt.get();
                        response.put("user", user.getNamaLengkap());
                        response.put("userId", user.getId());
                        
                        if (user.getLevel().getId().intValue() == 1) { // Mahasiswa
                            Optional<Mahasiswa> mahasiswaOpt = mahasiswaRepository.findByUserId(user.getId());
                            if (mahasiswaOpt.isPresent()) {
                                Mahasiswa mahasiswa = mahasiswaOpt.get();
                                response.put("mahasiswaId", mahasiswa.getId());
                                response.put("kamar", mahasiswa.getNoKamar());
                                response.put("kasur", mahasiswa.getNoKasur());
                                
                                // Test queries
                                List<LaporanUmum> laporanList = laporanUmumRepository.findAllByMahasiswaId(mahasiswa.getId());
                                response.put("jumlahLaporan", laporanList.size());
                                
                                int keluhanCount = laporanUmumRepository.countLaporanKeluhan(mahasiswa.getId());
                                response.put("jumlahKeluhan", keluhanCount);
                                
                                int izinCount = laporanUmumRepository.countLaporanIzin(mahasiswa.getId());
                                response.put("jumlahIzin", izinCount);
                                
                                List<LaporanBarang> paketList = laporanBarangRepository.findByMahasiswaIdAndStatus(mahasiswa.getId(), "menunggu");
                                response.put("jumlahPaket", paketList.size());
                            } else {
                                response.put("error", "Data mahasiswa tidak ditemukan");
                            }
                        } else {
                            response.put("userLevel", user.getLevel().getNama());
                        }
                    } else {
                        response.put("error", "User tidak ditemukan");
                    }
                } else {
                    response.put("error", "Email tidak dapat diekstrak dari token");
                }
            } catch (Exception e) {
                response.put("error", "Error decoding token: " + e.getMessage());
                logger.error("Error decoding token", e);
            }
        } else {
            response.put("tokenPresent", false);
            response.put("error", "Token tidak ditemukan");
        }
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/check-cookies")
    public ResponseEntity<Map<String, Object>> checkCookies(HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        Cookie[] cookies = request.getCookies();
        response.put("hasCookies", cookies != null && cookies.length > 0);
        
        if (cookies != null) {
            Map<String, String> cookieMap = new HashMap<>();
            for (Cookie cookie : cookies) {
                cookieMap.put(cookie.getName(), cookie.getValue());
                logger.info("Cookie found: {} = {}", cookie.getName(), cookie.getValue());
            }
            response.put("cookies", cookieMap);
            
            // Try all possible cookies that might contain JWT
            String jwt = null;
            String cookieName = null;
            
            // Common JWT cookie names
            String[] possibleNames = {"jwt", "JWT", "access_token", "Authorization", "auth_token", "token", "JSESSIONID"};
            
            for (String name : possibleNames) {
                if (cookieMap.containsKey(name)) {
                    jwt = cookieMap.get(name);
                    cookieName = name;
                    break;
                }
            }
            
            if (jwt != null) {
                response.put("jwtFound", true);
                response.put("jwtCookieName", cookieName);
                
                // Try to decode
                try {
                    String email = jwtTokenProvider.getUsername(jwt);
                    response.put("decodedEmail", email);
                    
                    if (email != null) {
                        Optional<User> userOpt = userRepository.findByEmail(email);
                        if (userOpt.isPresent()) {
                            User user = userOpt.get();
                            response.put("userFound", true);
                            response.put("userName", user.getNamaLengkap());
                        } else {
                            response.put("userFound", false);
                        }
                    }
                } catch (Exception e) {
                    response.put("decodeError", e.getMessage());
                }
            } else {
                response.put("jwtFound", false);
            }
        }
        
        return ResponseEntity.ok(response);
    }
} 