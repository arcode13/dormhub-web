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
import com.dormhub.security.JwtUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.HashMap;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

// Import ChatTitle dan ChatMessage repositories
import com.dormhub.model.ChatMessage;
import com.dormhub.model.ChatTitle;
import com.dormhub.repository.ChatMessageRepository;
import com.dormhub.repository.ChatTitleRepository;

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
    
    @Autowired
    private com.dormhub.security.JwtUtil jwtUtil;
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Autowired
    private ChatTitleRepository chatTitleRepository;
    
    @Autowired
    private ChatMessageRepository chatMessageRepository;
    
    public ChatBotApiController() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }
    
    @PostMapping("/message")
    public ResponseEntity<JsonNode> processMessage(@RequestBody JsonNode requestBody, HttpServletRequest httpRequest) {
        try {
            logger.info("Received chatbot request");
            
            // Dapatkan pesan dari request body
            String userMessage = requestBody.get("message").asText();
            Integer chatTitleId = null;
            
            // Check if chatTitleId is provided
            if (requestBody.has("chatTitleId")) {
                chatTitleId = requestBody.get("chatTitleId").asInt();
                logger.debug("Chat title ID provided: {}", chatTitleId);
            }
            
            logger.debug("User message: {}", userMessage);
            
            // Cek apakah ini pertanyaan spesifik tentang data pengguna
            boolean isUserSpecificQuestion = false;
            boolean isSystemQuestion = false;
            
            // Kata kunci untuk pertanyaan spesifik pengguna
            String[] userSpecificKeywords = {"kamar", "lantai", "kasur", "paket", "barang", "laporan", 
                                            "keluhan", "izin", "jurusan", "nama", "saya", "berapa"};
            
            // Kata kunci untuk pertanyaan tentang sistem
            String[] systemKeywords = {"dormhub", "asrama", "gedung", "siapa kamu", "kamu siapa", 
                                      "anda siapa", "bisa apa", "check-in", "checkout"};
            
            // Perbaikan untuk kata kunci sederhana
            String lowercaseMessage = userMessage.toLowerCase().trim();
            String modifiedMessage = userMessage;
            
            // Konversi kata kunci sederhana menjadi pertanyaan lengkap
            if (lowercaseMessage.equals("kamar")) {
                modifiedMessage = "Kamar saya nomor berapa?";
                logger.debug("Modified simple keyword 'kamar' to full question");
                isUserSpecificQuestion = true;
            } else if (lowercaseMessage.equals("laporan") || lowercaseMessage.equals("lap")) {
                modifiedMessage = "Laporan saya ada berapa?";
                logger.debug("Modified simple keyword 'laporan' to full question");
                isUserSpecificQuestion = true;
            } else if (lowercaseMessage.equals("paket") || lowercaseMessage.equals("barang")) {
                modifiedMessage = "Paket saya ada berapa?";
                logger.debug("Modified simple keyword 'paket' to full question");
                isUserSpecificQuestion = true;
            } else if (lowercaseMessage.equals("nama")) {
                modifiedMessage = "Nama saya siapa?";
                logger.debug("Modified simple keyword 'nama' to full question");
                isUserSpecificQuestion = true;
            } else if (lowercaseMessage.equals("checkin") || lowercaseMessage.equals("check in")) {
                modifiedMessage = "Informasi tentang check in";
                logger.debug("Modified to check-in information request");
                isSystemQuestion = true;
            } else if (lowercaseMessage.equals("checkout") || lowercaseMessage.equals("check out")) {
                modifiedMessage = "Informasi tentang check out";
                logger.debug("Modified to check-out information request");
                isSystemQuestion = true;
            } else if (lowercaseMessage.equals("dormhub") || lowercaseMessage.equals("asrama")) {
                modifiedMessage = "Apa itu DormHub?";
                logger.debug("Modified to question about DormHub");
                isSystemQuestion = true;
            }
            
            // Cek apakah ada kata kunci spesifik pengguna dalam pesan
            if (!isUserSpecificQuestion) {
                for (String keyword : userSpecificKeywords) {
                    if (lowercaseMessage.contains(keyword)) {
                        isUserSpecificQuestion = true;
                        break;
                    }
                }
            }
            
            // Cek apakah ada kata kunci tentang sistem dalam pesan
            if (!isSystemQuestion) {
                for (String keyword : systemKeywords) {
                    if (lowercaseMessage.contains(keyword)) {
                        isSystemQuestion = true;
                        break;
                    }
                }
            }
            
            // Proses pertanyaan berdasarkan jenisnya
            if (isUserSpecificQuestion) {
                logger.info("Processing user-specific question with detail-query API");
                try {
                    // Gunakan detail-query API untuk pertanyaan spesifik tentang data pengguna
                    Map<String, String> requestMap = new HashMap<>();
                    requestMap.put("question", modifiedMessage);
                    
                    ResponseEntity<Map<String, String>> detailQueryResponse = detailQuery(requestMap, httpRequest);
                    Map<String, String> detailResult = detailQueryResponse.getBody();
                    
                    if (detailResult != null && detailResult.containsKey("answer") && 
                        !detailResult.get("answer").startsWith("Maaf, tidak dapat")) {
                        
                        // Dapatkan respons data
                        String dataResponse = detailResult.get("answer");
                        logger.debug("Got response from detail-query API: {}", dataResponse);
                        
                        // Gunakan Gemini untuk membuatnya lebih natural, tapi simpan data utamanya
                        String personalizedPrompt = String.format(
                            "Kamu adalah asisten DormHub, sebuah aplikasi asrama mahasiswa. Buat respons natural dan ramah dalam Bahasa Indonesia " +
                            "berdasarkan informasi berikut: '%s'. Pastikan semua data faktual tetap utuh dan akurat, " +
                            "tapi berikan respons yang ramah dan natural. Jangan memperkenalkan diri atau menggunakan format template. " +
                            "Langsung jawab dengan santai seperti obrolan asisten virtual yang ramah. " +
                            "Pengguna bertanya: '%s'", dataResponse, userMessage);
                        
                        // Kirim ke Gemini untuk naturalisasi respons
                        ObjectNode geminiRequestBody = objectMapper.createObjectNode();
                        ObjectNode content = objectMapper.createObjectNode();
                        ObjectNode part = objectMapper.createObjectNode();
                        part.put("text", personalizedPrompt);
                        
                        // Struktur JSON untuk Gemini API v1
                        content.set("parts", objectMapper.createArrayNode().add(part));
                        geminiRequestBody.set("contents", objectMapper.createArrayNode().add(content));
                        
                        // Konfigurasi generasi
                        ObjectNode generationConfig = objectMapper.createObjectNode();
                        generationConfig.put("temperature", 0.4); // Lower temperature for factual accuracy
                        generationConfig.put("maxOutputTokens", 200);
                        geminiRequestBody.set("generationConfig", generationConfig);
                        
                        // Set headers
                        HttpHeaders headers = new HttpHeaders();
                        headers.setContentType(MediaType.APPLICATION_JSON);
                        
                        // Buat HTTP entity dengan body dan headers
                        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(geminiRequestBody), headers);
                        
                        // Kirim request ke Gemini API
                        String geminiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent?key=" + geminiApiKey;
                        logger.info("Sending personalization request to Gemini API");
                        
                        ResponseEntity<JsonNode> geminiResponse = restTemplate.exchange(
                            geminiUrl,
                            HttpMethod.POST,
                            entity,
                            JsonNode.class
                        );
                        
                        // Save messages to database if chatTitleId is provided
                        if (chatTitleId != null) {
                            saveMessagesToDatabase(chatTitleId, userMessage, geminiResponse.getBody());
                        }
                        
                        return geminiResponse;
                    }
                } catch (Exception e) {
                    logger.warn("Error using detail-query API, falling back to direct Gemini call", e);
                }
            } else if (isSystemQuestion) {
                logger.info("Processing system question with general-query API");
                try {
                    // Gunakan general-query API untuk pertanyaan tentang sistem
                    Map<String, String> requestMap = new HashMap<>();
                    requestMap.put("question", modifiedMessage);
                    
                    ResponseEntity<Map<String, String>> generalQueryResponse = generalQuery(requestMap);
                    Map<String, String> generalResult = generalQueryResponse.getBody();
                    
                    if (generalResult != null && generalResult.containsKey("answer")) {
                        // Dapatkan respons data
                        String dataResponse = generalResult.get("answer");
                        logger.debug("Got response from general-query API: {}", dataResponse);
                        
                        // Gunakan Gemini untuk membuatnya lebih natural, tapi simpan data utamanya
                        String personalizedPrompt = String.format(
                            "Kamu adalah asisten DormHub, sebuah aplikasi asrama mahasiswa. Buat respons natural dan ramah dalam Bahasa Indonesia " +
                            "berdasarkan informasi berikut: '%s'. Pastikan semua data faktual tetap utuh dan akurat, " +
                            "tapi berikan respons yang ramah dan natural. Jangan memperkenalkan diri atau menggunakan format template. " +
                            "Langsung jawab dengan santai seperti obrolan asisten virtual yang ramah. " +
                            "Pengguna bertanya: '%s'", dataResponse, userMessage);
                        
                        // Kirim ke Gemini untuk naturalisasi respons
                        ObjectNode geminiRequestBody = objectMapper.createObjectNode();
                        ObjectNode content = objectMapper.createObjectNode();
                        ObjectNode part = objectMapper.createObjectNode();
                        part.put("text", personalizedPrompt);
                        
                        // Struktur JSON untuk Gemini API v1
                        content.set("parts", objectMapper.createArrayNode().add(part));
                        geminiRequestBody.set("contents", objectMapper.createArrayNode().add(content));
                        
                        // Konfigurasi generasi
                        ObjectNode generationConfig = objectMapper.createObjectNode();
                        generationConfig.put("temperature", 0.5); // Medium temperature for balance between accuracy and naturalness
                        generationConfig.put("maxOutputTokens", 200);
                        geminiRequestBody.set("generationConfig", generationConfig);
                        
                        // Set headers
                        HttpHeaders headers = new HttpHeaders();
                        headers.setContentType(MediaType.APPLICATION_JSON);
                        
                        // Buat HTTP entity dengan body dan headers
                        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(geminiRequestBody), headers);
                        
                        // Kirim request ke Gemini API
                        String geminiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent?key=" + geminiApiKey;
                        logger.info("Sending personalization request to Gemini API");
                        
                        ResponseEntity<JsonNode> geminiResponse = restTemplate.exchange(
                            geminiUrl,
                            HttpMethod.POST,
                            entity,
                            JsonNode.class
                        );
                        
                        // Save messages to database if chatTitleId is provided
                        if (chatTitleId != null) {
                            saveMessagesToDatabase(chatTitleId, userMessage, geminiResponse.getBody());
                        }
                        
                        return geminiResponse;
                    }
                } catch (Exception e) {
                    logger.warn("Error using general-query API, falling back to direct Gemini call", e);
                }
            }
            
            // Get user info from SecurityContextHolder first
            User currentUser = null;
            Mahasiswa mahasiswa = null;
            String userSpecificInfo = "";
            
            // Try SecurityContextHolder first
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
                String email = auth.getName();
                logger.info("Found authenticated user in SecurityContext: {}", email);
                
                Optional<User> userOpt = userRepository.findByEmail(email);
                if (userOpt.isPresent()) {
                    currentUser = userOpt.get();
                    logger.debug("Found user from SecurityContext: {}", currentUser.getNamaLengkap());
                    
                    // Check if user is a student
                    if (currentUser.getLevel().getId().intValue() == 1) { // assuming 1 is for Mahasiswa
                        Optional<Mahasiswa> mahasiswaOpt = mahasiswaRepository.findByUserId(currentUser.getId());
                        if (mahasiswaOpt.isPresent()) {
                            mahasiswa = mahasiswaOpt.get();
                            logger.debug("Found mahasiswa record from SecurityContext - kamar: {}, kasur: {}", 
                                mahasiswa.getNoKamar(), mahasiswa.getNoKasur());
                        }
                    }
                }
            } else {
                logger.warn("No authenticated user found in SecurityContext, trying cookies");
            }
            
            // If SecurityContextHolder fails, try cookies
            if (currentUser == null) {
                Cookie[] cookies = httpRequest.getCookies();
                if (cookies != null) {
                    // Try common JWT cookie names
                    String[] possibleNames = {"jwt_token", "jwt", "JWT", "access_token", "Authorization", "auth_token", "token", "JSESSIONID"};
                    String jwtToken = null;
                    String foundCookieName = null;
                    
                    for (Cookie cookie : cookies) {
                        logger.debug("Found cookie: {}={}", cookie.getName(), cookie.getValue());
                        for (String name : possibleNames) {
                            if (name.equals(cookie.getName())) {
                                jwtToken = cookie.getValue();
                                foundCookieName = name;
                                logger.info("Found potential JWT in cookie: {}", name);
                                break;
                            }
                        }
                        if (jwtToken != null) break;
                    }
                    
                    if (jwtToken != null) {
                        logger.info("Using JWT from cookie: {}", foundCookieName);
                        
                        // First try with JwtUtil
                        try {
                            if (jwtUtil.validateToken(jwtToken)) {
                                String email = jwtUtil.extractEmail(jwtToken);
                                logger.debug("JwtUtil extracted email: {}", email);
                                
                                if (email != null) {
                                    Optional<User> userOpt = userRepository.findByEmail(email);
                                    if (userOpt.isPresent()) {
                                        currentUser = userOpt.get();
                                        logger.debug("Found user with JwtUtil: {}", currentUser.getNamaLengkap());
                                        
                                        // Check if user is a student
                                        if (currentUser.getLevel().getId().intValue() == 1) { // assuming 1 is for Mahasiswa
                                            Optional<Mahasiswa> mahasiswaOpt = mahasiswaRepository.findByUserId(currentUser.getId());
                                            if (mahasiswaOpt.isPresent()) {
                                                mahasiswa = mahasiswaOpt.get();
                                                logger.debug("Found mahasiswa record with JwtUtil - kamar: {}, kasur: {}", 
                                                    mahasiswa.getNoKamar(), mahasiswa.getNoKasur());
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            logger.warn("JwtUtil failed to decode token: {}", e.getMessage());
                        }
                        
                        // If JwtUtil failed, try with JwtTokenProvider as backup
                        if (currentUser == null) {
                            try {
                                String email = jwtTokenProvider.getUsername(jwtToken);
                                logger.debug("Extracted email from token: {}", email);
                                
                                if (email != null) {
                                    Optional<User> userOpt = userRepository.findByEmail(email);
                                    if (userOpt.isPresent()) {
                                        currentUser = userOpt.get();
                                        logger.debug("Found user with JwtTokenProvider: {}", currentUser.getNamaLengkap());
                                        
                                        // Check if user is a student
                                        if (currentUser.getLevel().getId().intValue() == 1) { // assuming 1 is for Mahasiswa
                                            Optional<Mahasiswa> mahasiswaOpt = mahasiswaRepository.findByUserId(currentUser.getId());
                                            if (mahasiswaOpt.isPresent()) {
                                                mahasiswa = mahasiswaOpt.get();
                                                logger.debug("Found mahasiswa record with kamar: {}, kasur: {}", 
                                                    mahasiswa.getNoKamar(), mahasiswa.getNoKasur());
                                            }
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                logger.error("JwtTokenProvider failed to decode token: {}", e.getMessage());
                            }
                        }
                    }
                }
            }
            
            // Jika berhasil mendapatkan data mahasiswa, siapkan info spesifik pengguna
            if (mahasiswa != null && currentUser != null) {
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
                
                logger.info("Generated user-specific info for Gemini prompt");
            } else {
                logger.warn("Could not generate user-specific info - mahasiswa: {}, user: {}", 
                    (mahasiswa != null), (currentUser != null));
            }
            
            // Get configuration data
            Map<String, String> konfigurasi = new HashMap<>();
            try {
                List<Object[]> konfigurasiList = entityManager.createNativeQuery(
                    "SELECT k_key, k_value FROM konfigurasi"
                ).getResultList();
                
                for (Object[] row : konfigurasiList) {
                    konfigurasi.put((String) row[0], (String) row[1]);
                }
            } catch (Exception e) {
                logger.warn("Could not load configuration data", e);
            }
            
            // Get building name and other info from configuration
            String websiteName = konfigurasi.getOrDefault("web-nama-website", "DormHub");
            String buildingName = konfigurasi.getOrDefault("web-nama-gedung", "asrama");
            
            // Tambahkan konteks untuk Gemini
            String fullPrompt;
            
            if (isUserSpecificQuestion) {
                // Jika pertanyaan tentang data spesifik pengguna, tambahkan data pribadi ke prompt
                fullPrompt = String.format("Kamu adalah asisten %s, sebuah aplikasi asrama mahasiswa. Berikan jawaban natural dan ramah dalam Bahasa Indonesia. ", websiteName);
                fullPrompt += String.format("Informasi tentang %s: %s adalah aplikasi manajemen asrama bernama %s yang memiliki fitur check-in, check-out, laporan keluhan, dan informasi kamar. ", 
                    websiteName, websiteName, buildingName);
                
                // Add user-specific info if available
                if (!userSpecificInfo.isEmpty()) {
                    fullPrompt += userSpecificInfo + ". ";
                    fullPrompt += "Gunakan informasi pengguna ini untuk menjawab pertanyaan yang relevan. ";
                    fullPrompt += "Jika ditanya tentang informasi spesifik pengguna seperti 'kamar saya berapa?' atau 'berapa jumlah laporan saya?', berikan jawaban berdasarkan data yang tersedia. ";
                }
                
                fullPrompt += "Beri jawaban ramah dan natural seperti teman mengobrol, bukan seperti sistem formal. ";
                fullPrompt += "Jangan saran menggunakan menu-menu kecuali jika pengguna langsung bertanya tentang cara melakukan sesuatu. ";
            } else {
                // Untuk percakapan umum, gunakan prompt sederhana tanpa data pribadi
                fullPrompt = String.format("Kamu adalah asisten %s, sebuah aplikasi asrama mahasiswa bernama %s. Berikan jawaban natural dan ramah dalam Bahasa Indonesia. ", 
                    websiteName, buildingName);
                
                if (!userSpecificInfo.isEmpty()) {
                    fullPrompt += "Ini informasi tentang pengguna yang sedang kamu ajak bicara: " + userSpecificInfo + ". ";
                    fullPrompt += "Gunakan namanya dalam percakapan untuk sentuhan personal. ";
                }
                
                fullPrompt += "Kamu bisa mengobrol tentang berbagai topik dan menjawab pertanyaan umum pengguna. ";
                fullPrompt += "Beri respons yang natural, ramah dan personal seperti teman mengobrol. ";
                fullPrompt += "Hindari bahasa formal dan kaku. Gunakan bahasa yang santai namun tetap sopan. ";
            }
            
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
            generationConfig.put("temperature", isUserSpecificQuestion ? 0.4 : 0.7); // Lower temperature for data questions, higher for chat
            generationConfig.put("maxOutputTokens", isUserSpecificQuestion ? 200 : 300); // Longer responses for chat
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
            
            // Save messages to database if chatTitleId is provided
            if (chatTitleId != null) {
                saveMessagesToDatabase(chatTitleId, userMessage, geminiResponse.getBody());
            }
            
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
    
    /**
     * Helper method to save chat messages to database
     */
    private void saveMessagesToDatabase(Integer chatTitleId, String userMessage, JsonNode aiResponse) {
        try {
            // Find chat title
            Optional<ChatTitle> chatTitleOpt = chatTitleRepository.findById(chatTitleId);
            if (chatTitleOpt.isEmpty()) {
                logger.error("Chat title not found with ID: {}", chatTitleId);
                return;
            }
            
            ChatTitle chatTitle = chatTitleOpt.get();
            
            // Save user message
            ChatMessage userChatMessage = new ChatMessage();
            userChatMessage.setTitle(chatTitle);
            userChatMessage.setSenderType(ChatMessage.SenderType.user);
            userChatMessage.setMessageContent(userMessage);
            chatMessageRepository.save(userChatMessage);
            
            // Extract AI response text
            String aiResponseText = null;
            if (aiResponse != null && aiResponse.has("candidates") && aiResponse.get("candidates").isArray() && 
                aiResponse.get("candidates").size() > 0) {
                JsonNode candidate = aiResponse.get("candidates").get(0);
                if (candidate.has("content") && candidate.get("content").has("parts") && 
                    candidate.get("content").get("parts").isArray() && candidate.get("content").get("parts").size() > 0) {
                    JsonNode part = candidate.get("content").get("parts").get(0);
                    if (part.has("text")) {
                        aiResponseText = part.get("text").asText();
                    }
                }
            }
            
            if (aiResponseText != null) {
                // Save AI response
                ChatMessage aiChatMessage = new ChatMessage();
                aiChatMessage.setTitle(chatTitle);
                aiChatMessage.setSenderType(ChatMessage.SenderType.ai);
                aiChatMessage.setMessageContent(aiResponseText);
                chatMessageRepository.save(aiChatMessage);
                
                // Update chat title's updatedAt timestamp
                chatTitle.setUpdatedAt(LocalDateTime.now());
                chatTitleRepository.save(chatTitle);
                
                logger.debug("Saved chat messages to database for chat title ID: {}", chatTitleId);
            } else {
                logger.warn("Could not extract AI response text from response");
            }
        } catch (Exception e) {
            logger.error("Error saving chat messages to database", e);
        }
    }

    @PostMapping("/fallback")
    public ResponseEntity<JsonNode> fallbackResponse(@RequestBody JsonNode requestBody, HttpServletRequest httpRequest) {
        try {
            logger.info("Using fallback response");
            
            String userMessage = requestBody.get("message").asText();
            logger.debug("User message for fallback: {}", userMessage);
            
            // Get user info from SecurityContextHolder first
            User currentUser = null;
            Mahasiswa mahasiswa = null;
            
            // Try SecurityContextHolder first
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
                String email = auth.getName();
                logger.info("Found authenticated user in SecurityContext: {}", email);
                
                Optional<User> userOpt = userRepository.findByEmail(email);
                if (userOpt.isPresent()) {
                    currentUser = userOpt.get();
                    logger.debug("Found user from SecurityContext: {}", currentUser.getNamaLengkap());
                    
                    // Check if user is a student
                    if (currentUser.getLevel().getId().intValue() == 1) { // assuming 1 is for Mahasiswa
                        Optional<Mahasiswa> mahasiswaOpt = mahasiswaRepository.findByUserId(currentUser.getId());
                        if (mahasiswaOpt.isPresent()) {
                            mahasiswa = mahasiswaOpt.get();
                            logger.debug("Found mahasiswa record from SecurityContext - kamar: {}, kasur: {}", 
                                mahasiswa.getNoKamar(), mahasiswa.getNoKasur());
                        }
                    }
                }
            } else {
                logger.warn("No authenticated user found in SecurityContext, trying cookies");
            }
            
            // If SecurityContextHolder fails, try cookies
            if (currentUser == null) {
                Cookie[] cookies = httpRequest.getCookies();
                if (cookies != null) {
                    // Try common JWT cookie names
                    String[] possibleNames = {"jwt_token", "jwt", "JWT", "access_token", "Authorization", "auth_token", "token", "JSESSIONID"};
                    String jwtToken = null;
                    String foundCookieName = null;
                    
                    for (Cookie cookie : cookies) {
                        logger.debug("Found cookie: {}={}", cookie.getName(), cookie.getValue());
                        for (String name : possibleNames) {
                            if (name.equals(cookie.getName())) {
                                jwtToken = cookie.getValue();
                                foundCookieName = name;
                                logger.info("Found potential JWT in cookie: {}", name);
                                break;
                            }
                        }
                        if (jwtToken != null) break;
                    }
                    
                    if (jwtToken != null) {
                        logger.info("Using JWT from cookie: {}", foundCookieName);
                        
                        // First try with JwtUtil
                        try {
                            if (jwtUtil.validateToken(jwtToken)) {
                                String email = jwtUtil.extractEmail(jwtToken);
                                logger.debug("JwtUtil extracted email: {}", email);
                                
                                if (email != null) {
                                    Optional<User> userOpt = userRepository.findByEmail(email);
                                    if (userOpt.isPresent()) {
                                        currentUser = userOpt.get();
                                        logger.debug("Found user with JwtUtil: {}", currentUser.getNamaLengkap());
                                        
                                        // Check if user is a student
                                        if (currentUser.getLevel().getId().intValue() == 1) { // assuming 1 is for Mahasiswa
                                            Optional<Mahasiswa> mahasiswaOpt = mahasiswaRepository.findByUserId(currentUser.getId());
                                            if (mahasiswaOpt.isPresent()) {
                                                mahasiswa = mahasiswaOpt.get();
                                                logger.debug("Found mahasiswa record with JwtUtil - kamar: {}, kasur: {}", 
                                                    mahasiswa.getNoKamar(), mahasiswa.getNoKasur());
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            logger.warn("JwtUtil failed to decode token: {}", e.getMessage());
                        }
                        
                        // If JwtUtil failed, try with JwtTokenProvider as backup
                        if (currentUser == null) {
                            try {
                                String email = jwtTokenProvider.getUsername(jwtToken);
                                logger.debug("Extracted email from token: {}", email);
                                
                                if (email != null) {
                                    Optional<User> userOpt = userRepository.findByEmail(email);
                                    if (userOpt.isPresent()) {
                                        currentUser = userOpt.get();
                                        logger.debug("Found user with JwtTokenProvider: {}", currentUser.getNamaLengkap());
                                        
                                        // Check if user is a student
                                        if (currentUser.getLevel().getId().intValue() == 1) { // assuming 1 is for Mahasiswa
                                            Optional<Mahasiswa> mahasiswaOpt = mahasiswaRepository.findByUserId(currentUser.getId());
                                            if (mahasiswaOpt.isPresent()) {
                                                mahasiswa = mahasiswaOpt.get();
                                                logger.debug("Found mahasiswa record with kamar: {}, kasur: {}", 
                                                    mahasiswa.getNoKamar(), mahasiswa.getNoKasur());
                                            }
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                logger.error("JwtTokenProvider failed to decode token: {}", e.getMessage());
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
        
        // Get user info from SecurityContextHolder first
        User currentUser = null;
        Mahasiswa mahasiswa = null;
        
        // Try SecurityContextHolder first
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            String email = auth.getName();
            logger.info("Found authenticated user in SecurityContext: {}", email);
            
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isPresent()) {
                currentUser = userOpt.get();
                logger.debug("Found user from SecurityContext: {}", currentUser.getNamaLengkap());
                
                // Check if user is a student
                if (currentUser.getLevel().getId().intValue() == 1) { // assuming 1 is for Mahasiswa
                    Optional<Mahasiswa> mahasiswaOpt = mahasiswaRepository.findByUserId(currentUser.getId());
                    if (mahasiswaOpt.isPresent()) {
                        mahasiswa = mahasiswaOpt.get();
                        logger.debug("Found mahasiswa record from SecurityContext - kamar: {}, kasur: {}", 
                            mahasiswa.getNoKamar(), mahasiswa.getNoKasur());
                    }
                }
            }
        } else {
            logger.warn("No authenticated user found in SecurityContext, trying cookies");
        }
        
        // If SecurityContextHolder fails, try cookies
        if (currentUser == null) {
            Cookie[] cookies = httpRequest.getCookies();
            if (cookies != null) {
                // Try common JWT cookie names
                String[] possibleNames = {"jwt_token", "jwt", "JWT", "access_token", "Authorization", "auth_token", "token", "JSESSIONID"};
                String jwtToken = null;
                String foundCookieName = null;
                
                for (Cookie cookie : cookies) {
                    logger.debug("Found cookie: {}={}", cookie.getName(), cookie.getValue());
                    for (String name : possibleNames) {
                        if (name.equals(cookie.getName())) {
                            jwtToken = cookie.getValue();
                            foundCookieName = name;
                            logger.info("Found potential JWT in cookie: {}", name);
                            break;
                        }
                    }
                    if (jwtToken != null) break;
                }
                
                if (jwtToken != null) {
                    logger.info("Using JWT from cookie: {}", foundCookieName);
                    
                    // First try with JwtUtil
                    try {
                        if (jwtUtil.validateToken(jwtToken)) {
                            String email = jwtUtil.extractEmail(jwtToken);
                            logger.debug("JwtUtil extracted email: {}", email);
                            
                            if (email != null) {
                                Optional<User> userOpt = userRepository.findByEmail(email);
                                if (userOpt.isPresent()) {
                                    currentUser = userOpt.get();
                                    logger.debug("Found user with JwtUtil: {}", currentUser.getNamaLengkap());
                                    
                                    // Check if user is a student
                                    if (currentUser.getLevel().getId().intValue() == 1) { // assuming 1 is for Mahasiswa
                                        Optional<Mahasiswa> mahasiswaOpt = mahasiswaRepository.findByUserId(currentUser.getId());
                                        if (mahasiswaOpt.isPresent()) {
                                            mahasiswa = mahasiswaOpt.get();
                                            logger.debug("Found mahasiswa record with JwtUtil - kamar: {}, kasur: {}", 
                                                mahasiswa.getNoKamar(), mahasiswa.getNoKasur());
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        logger.warn("JwtUtil failed to decode token: {}", e.getMessage());
                    }
                    
                    // If JwtUtil failed, try with JwtTokenProvider as backup
                    if (currentUser == null) {
                        try {
                            String email = jwtTokenProvider.getUsername(jwtToken);
                            logger.debug("Extracted email from token: {}", email);
                            
                            if (email != null) {
                                Optional<User> userOpt = userRepository.findByEmail(email);
                                if (userOpt.isPresent()) {
                                    currentUser = userOpt.get();
                                    logger.debug("Found user with JwtTokenProvider: {}", currentUser.getNamaLengkap());
                                    
                                    // Check if user is a student
                                    if (currentUser.getLevel().getId().intValue() == 1) { // assuming 1 is for Mahasiswa
                                        Optional<Mahasiswa> mahasiswaOpt = mahasiswaRepository.findByUserId(currentUser.getId());
                                        if (mahasiswaOpt.isPresent()) {
                                            mahasiswa = mahasiswaOpt.get();
                                            logger.debug("Found mahasiswa record with kamar: {}, kasur: {}", 
                                                mahasiswa.getNoKamar(), mahasiswa.getNoKasur());
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            logger.error("JwtTokenProvider failed to decode token: {}", e.getMessage());
                        }
                    }
                }
            }
        }
        
        // If no user found, return early
        if (currentUser == null) {
            logger.warn("User not found after all authentication attempts");
            response.put("answer", "Maaf, tidak dapat menemukan data pengguna Anda. Silakan coba login kembali.");
            return ResponseEntity.ok(response);
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
                response.put("answer", 
                    "Maaf, saya tidak memahami pertanyaan Anda. Silakan tanyakan tentang kamar, laporan, atau paket Anda.");
            }
        } catch (Exception e) {
            logger.error("Error processing query", e);
            response.put("answer", "Terjadi kesalahan saat memproses pertanyaan Anda: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/detail-query")
    public ResponseEntity<Map<String, String>> detailQuery(@RequestBody Map<String, String> request, 
                                                         HttpServletRequest httpRequest) {
        Map<String, String> response = new HashMap<>();
        String question = request.get("question");
        
        logger.info("Received detail query: {}", question);
        
        // Default response
        response.put("answer", "Maaf, saya tidak dapat memahami pertanyaan Anda.");
        
        if (question == null || question.trim().isEmpty()) {
            return ResponseEntity.ok(response);
        }
        
        // Get user info from SecurityContextHolder first
        User currentUser = null;
        Mahasiswa mahasiswa = null;
        
        // Try SecurityContextHolder first
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            String email = auth.getName();
            logger.info("Found authenticated user in SecurityContext: {}", email);
            
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isPresent()) {
                currentUser = userOpt.get();
                logger.debug("Found user from SecurityContext: {}", currentUser.getNamaLengkap());
                
                // Check if user is a student
                if (currentUser.getLevel().getId().intValue() == 1) { // assuming 1 is for Mahasiswa
                    Optional<Mahasiswa> mahasiswaOpt = mahasiswaRepository.findByUserId(currentUser.getId());
                    if (mahasiswaOpt.isPresent()) {
                        mahasiswa = mahasiswaOpt.get();
                        logger.debug("Found mahasiswa record from SecurityContext - kamar: {}, kasur: {}", 
                            mahasiswa.getNoKamar(), mahasiswa.getNoKasur());
                    }
                }
            }
        } else {
            logger.warn("No authenticated user found in SecurityContext, trying cookies");
        }
        
        // If SecurityContextHolder fails, try cookies
        if (currentUser == null) {
            Cookie[] cookies = httpRequest.getCookies();
            if (cookies != null) {
                // Try common JWT cookie names
                String[] possibleNames = {"jwt_token", "jwt", "JWT", "access_token", "Authorization", "auth_token", "token", "JSESSIONID"};
                String jwtToken = null;
                String foundCookieName = null;
                
                for (Cookie cookie : cookies) {
                    logger.debug("Found cookie: {}={}", cookie.getName(), cookie.getValue());
                    for (String name : possibleNames) {
                        if (name.equals(cookie.getName())) {
                            jwtToken = cookie.getValue();
                            foundCookieName = name;
                            logger.info("Found potential JWT in cookie: {}", name);
                            break;
                        }
                    }
                    if (jwtToken != null) break;
                }
                
                if (jwtToken != null) {
                    logger.info("Using JWT from cookie: {}", foundCookieName);
                    
                    // First try with JwtUtil
                    try {
                        if (jwtUtil.validateToken(jwtToken)) {
                            String email = jwtUtil.extractEmail(jwtToken);
                            logger.debug("JwtUtil extracted email: {}", email);
                            
                            if (email != null) {
                                Optional<User> userOpt = userRepository.findByEmail(email);
                                if (userOpt.isPresent()) {
                                    currentUser = userOpt.get();
                                    logger.debug("Found user with JwtUtil: {}", currentUser.getNamaLengkap());
                                    
                                    // Check if user is a student
                                    if (currentUser.getLevel().getId().intValue() == 1) { // assuming 1 is for Mahasiswa
                                        Optional<Mahasiswa> mahasiswaOpt = mahasiswaRepository.findByUserId(currentUser.getId());
                                        if (mahasiswaOpt.isPresent()) {
                                            mahasiswa = mahasiswaOpt.get();
                                            logger.debug("Found mahasiswa record with JwtUtil - kamar: {}, kasur: {}", 
                                                mahasiswa.getNoKamar(), mahasiswa.getNoKasur());
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        logger.warn("JwtUtil failed to decode token: {}", e.getMessage());
                    }
                    
                    // If JwtUtil failed, try with JwtTokenProvider as backup
                    if (currentUser == null) {
                        try {
                            String email = jwtTokenProvider.getUsername(jwtToken);
                            logger.debug("Extracted email from token: {}", email);
                            
                            if (email != null) {
                                Optional<User> userOpt = userRepository.findByEmail(email);
                                if (userOpt.isPresent()) {
                                    currentUser = userOpt.get();
                                    logger.debug("Found user with JwtTokenProvider: {}", currentUser.getNamaLengkap());
                                    
                                    // Check if user is a student
                                    if (currentUser.getLevel().getId().intValue() == 1) { // assuming 1 is for Mahasiswa
                                        Optional<Mahasiswa> mahasiswaOpt = mahasiswaRepository.findByUserId(currentUser.getId());
                                        if (mahasiswaOpt.isPresent()) {
                                            mahasiswa = mahasiswaOpt.get();
                                            logger.debug("Found mahasiswa record with kamar: {}, kasur: {}", 
                                                mahasiswa.getNoKamar(), mahasiswa.getNoKasur());
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            logger.error("JwtTokenProvider failed to decode token: {}", e.getMessage());
                        }
                    }
                }
            }
        }
        
        // If no user found, return early
        if (currentUser == null) {
            logger.warn("User not found after all authentication attempts");
            response.put("answer", "Maaf, tidak dapat menemukan data pengguna Anda. Silakan coba login kembali.");
            return ResponseEntity.ok(response);
        }
        
        // Process specific questions based on keywords
        String lowercaseQuestion = question.toLowerCase();
        
        try {
            // Get configuration data for more detailed responses
            Map<String, String> konfigurasi = new HashMap<>();
            try {
                // Attempt to get configuration data (like building name, floor count, etc)
                List<Object[]> konfigurasiList = entityManager.createNativeQuery(
                    "SELECT k_key, k_value FROM konfigurasi"
                ).getResultList();
                
                for (Object[] row : konfigurasiList) {
                    konfigurasi.put((String) row[0], (String) row[1]);
                }
                logger.debug("Loaded configuration data: {}", konfigurasi);
            } catch (Exception e) {
                logger.warn("Could not load configuration data", e);
            }
            
            // Get building name from configuration
            String buildingName = konfigurasi.getOrDefault("web-nama-gedung", "asrama");
            String websiteName = konfigurasi.getOrDefault("web-nama-website", "DormHub");
            
            // Cek kata kunci sederhana dan ubah ke format yang lebih spesifik
            if (lowercaseQuestion.equals("kamar")) {
                lowercaseQuestion = "kamar saya";
                logger.info("Converting simple keyword 'kamar' to 'kamar saya'");
            } else if (lowercaseQuestion.equals("laporan") || lowercaseQuestion.equals("lap")) {
                lowercaseQuestion = "laporan saya";
                logger.info("Converting simple keyword 'laporan' to 'laporan saya'");
            } else if (lowercaseQuestion.equals("paket") || lowercaseQuestion.equals("barang")) {
                lowercaseQuestion = "paket saya";
                logger.info("Converting simple keyword 'paket/barang' to 'paket saya'");
            } else if (lowercaseQuestion.equals("nama")) {
                lowercaseQuestion = "nama saya";
                logger.info("Converting simple keyword 'nama' to 'nama saya'");
            } else if (lowercaseQuestion.equals("jurusan")) {
                lowercaseQuestion = "jurusan saya";
                logger.info("Converting simple keyword 'jurusan' to 'jurusan saya'");
            } else if (lowercaseQuestion.equals("keluhan")) {
                lowercaseQuestion = "keluhan saya";
                logger.info("Converting simple keyword 'keluhan' to 'keluhan saya'");
            } else if (lowercaseQuestion.equals("izin")) {
                lowercaseQuestion = "izin saya";
                logger.info("Converting simple keyword 'izin' to 'izin saya'");
            } else if (lowercaseQuestion.equals("checkout") || lowercaseQuestion.equals("check out")) {
                lowercaseQuestion = "tentang checkout";
                logger.info("Converting to checkout information request");
            } else if (lowercaseQuestion.equals("checkin") || lowercaseQuestion.equals("check in")) {
                lowercaseQuestion = "tentang checkin";
                logger.info("Converting to checkin information request");
            }
            
            if (lowercaseQuestion.contains("kamar") && (lowercaseQuestion.contains("saya") || lowercaseQuestion.contains("berapa") || lowercaseQuestion.contains("nomor"))) {
                if (mahasiswa != null) {
                    // Calculate floor based on room number convention (e.g., room 101 is on floor 1)
                    int lantai = mahasiswa.getNoKamar() / 100;
                    
                    // Create a more detailed and natural response
                    response.put("answer", String.format(
                        "Anda tinggal di %s lantai %d, kamar nomor %d, dan kasur nomor %d.",
                        buildingName, lantai, mahasiswa.getNoKamar(), mahasiswa.getNoKasur()
                    ));
                    logger.info("Responding with detailed room info for user ID: {}", currentUser.getId());
                } else {
                    response.put("answer", "Maaf, informasi kamar tidak tersedia. Anda mungkin perlu login kembali.");
                }
            } else if (lowercaseQuestion.contains("lantai") && (lowercaseQuestion.contains("saya") || lowercaseQuestion.contains("berapa"))) {
                if (mahasiswa != null) {
                    int lantai = mahasiswa.getNoKamar() / 100;
                    response.put("answer", String.format(
                        "Anda tinggal di lantai %d %s.",
                        lantai, buildingName
                    ));
                    logger.info("Responding with floor info for user ID: {}", currentUser.getId());
                } else {
                    response.put("answer", "Maaf, informasi lantai tidak tersedia. Anda mungkin perlu login kembali.");
                }
            } else if ((lowercaseQuestion.contains("paket") || lowercaseQuestion.contains("barang")) && 
                      (lowercaseQuestion.contains("saya") || lowercaseQuestion.contains("berapa") || lowercaseQuestion.contains("ada"))) {
                if (mahasiswa != null) {
                    List<LaporanBarang> laporanBarangList = laporanBarangRepository.findByMahasiswaIdAndStatus(mahasiswa.getId(), "menunggu");
                    
                    if (laporanBarangList.isEmpty()) {
                        response.put("answer", "Anda tidak memiliki paket/barang yang tercatat dalam sistem saat ini.");
                    } else if (laporanBarangList.size() == 1) {
                        LaporanBarang barang = laporanBarangList.get(0);
                        response.put("answer", String.format(
                            "Anda memiliki 1 %s yang tercatat dalam sistem. Status: %s",
                            barang.getJenis().toLowerCase(), barang.getStatus()
                        ));
                    } else {
                        response.put("answer", String.format(
                            "Anda memiliki %d paket/barang yang tercatat dalam sistem. Silakan cek menu 'Paket' untuk detail lengkapnya.",
                            laporanBarangList.size()
                        ));
                    }
                    logger.info("Responding with detailed package count for user ID: {}", currentUser.getId());
                } else {
                    response.put("answer", "Maaf, informasi paket tidak tersedia. Anda mungkin perlu login kembali.");
                }
            } else if (lowercaseQuestion.contains("laporan") && 
                      (lowercaseQuestion.contains("saya") || lowercaseQuestion.contains("berapa") || lowercaseQuestion.contains("ada"))) {
                if (mahasiswa != null) {
                    List<LaporanUmum> laporanUmumList = laporanUmumRepository.findAllByMahasiswaId(mahasiswa.getId());
                    int totalKeluhanCount = laporanUmumRepository.countLaporanKeluhan(mahasiswa.getId());
                    int totalIzinCount = laporanUmumRepository.countLaporanIzin(mahasiswa.getId());
                    
                    response.put("answer", String.format(
                        "Anda memiliki total %d laporan yang tercatat dalam sistem, terdiri dari %d laporan keluhan dan %d laporan izin.",
                        laporanUmumList.size(), totalKeluhanCount, totalIzinCount
                    ));
                    logger.info("Responding with detailed report count for user ID: {}", currentUser.getId());
                } else {
                    response.put("answer", "Maaf, informasi laporan tidak tersedia. Anda mungkin perlu login kembali.");
                }
            } else if (lowercaseQuestion.contains("keluhan") && 
                      (lowercaseQuestion.contains("saya") || lowercaseQuestion.contains("berapa") || lowercaseQuestion.contains("ada"))) {
                if (mahasiswa != null) {
                    int totalKeluhanCount = laporanUmumRepository.countLaporanKeluhan(mahasiswa.getId());
                    
                    if (totalKeluhanCount == 0) {
                        response.put("answer", "Anda belum pernah membuat laporan keluhan.");
                    } else {
                        response.put("answer", String.format(
                            "Anda memiliki %d laporan keluhan yang tercatat dalam sistem. Untuk melihat detail dan status keluhan, silakan cek menu 'Laporan'.",
                            totalKeluhanCount
                        ));
                    }
                    logger.info("Responding with detailed complaint count for user ID: {}", currentUser.getId());
                } else {
                    response.put("answer", "Maaf, informasi keluhan tidak tersedia. Anda mungkin perlu login kembali.");
                }
            } else if (lowercaseQuestion.contains("izin") && 
                      (lowercaseQuestion.contains("saya") || lowercaseQuestion.contains("berapa") || lowercaseQuestion.contains("ada"))) {
                if (mahasiswa != null) {
                    int totalIzinCount = laporanUmumRepository.countLaporanIzin(mahasiswa.getId());
                    
                    if (totalIzinCount == 0) {
                        response.put("answer", "Anda belum pernah membuat laporan izin.");
                    } else {
                        response.put("answer", String.format(
                            "Anda memiliki %d laporan izin yang tercatat dalam sistem. Untuk melihat detail dan status izin, silakan cek menu 'Laporan'.",
                            totalIzinCount
                        ));
                    }
                    logger.info("Responding with detailed permission count for user ID: {}", currentUser.getId());
                } else {
                    response.put("answer", "Maaf, informasi izin tidak tersedia. Anda mungkin perlu login kembali.");
                }
            } else if (lowercaseQuestion.contains("nama") && 
                      (lowercaseQuestion.contains("saya") || lowercaseQuestion.contains("siapa"))) {
                if (currentUser != null) {
                    response.put("answer", String.format(
                        "Nama Anda adalah %s. Anda terdaftar dengan email %s.", 
                        currentUser.getNamaLengkap(), currentUser.getEmail()
                    ));
                    logger.info("Responding with user name for user ID: {}", currentUser.getId());
                } else {
                    response.put("answer", "Maaf, informasi pengguna tidak tersedia. Anda mungkin perlu login kembali.");
                }
            } else if (lowercaseQuestion.contains("jurusan") && 
                      (lowercaseQuestion.contains("saya") || lowercaseQuestion.contains("apa"))) {
                if (mahasiswa != null && mahasiswa.getJurusan() != null) {
                    response.put("answer", String.format(
                        "Anda terdaftar di jurusan %s.", 
                        mahasiswa.getJurusan().getNama()
                    ));
                    logger.info("Responding with major info for user ID: {}", currentUser.getId());
                } else {
                    response.put("answer", "Maaf, informasi jurusan tidak tersedia.");
                }
            } else if ((lowercaseQuestion.contains("jadwal") || lowercaseQuestion.contains("tanggal") || lowercaseQuestion.contains("kapan")) && 
                      (lowercaseQuestion.contains("checkout") || lowercaseQuestion.contains("check out") || lowercaseQuestion.contains("keluar"))) {
                // Get check-out date from configuration
                String checkoutStartDate = konfigurasi.getOrDefault("web-mulai-tgl-co", "");
                String checkoutEndDate = konfigurasi.getOrDefault("web-selesai-tgl-co", "");
                
                if (!checkoutStartDate.isEmpty() && !checkoutEndDate.isEmpty()) {
                    response.put("answer", String.format(
                        "Jadwal check-out di %s adalah dari tanggal %s sampai dengan tanggal %s. Pastikan Anda telah menyelesaikan semua kewajiban sebelum check-out.",
                        buildingName, checkoutStartDate, checkoutEndDate
                    ));
                } else {
                    response.put("answer", String.format(
                        "Untuk informasi jadwal check-out, silakan perhatikan notifikasi pada dashboard Anda atau hubungi pihak %s.",
                        websiteName
                    ));
                }
                logger.info("Responding with check-out schedule info");
            } else if ((lowercaseQuestion.contains("checkin") || lowercaseQuestion.contains("check in") || lowercaseQuestion.contains("masuk"))) {
                response.put("answer", String.format(
                    "Untuk check-in di %s, Anda perlu login ke sistem dan mengikuti petunjuk pada halaman beranda. Check-in biasanya dilakukan pada awal semester atau periode tertentu sesuai kebijakan asrama.",
                    buildingName
                ));
                logger.info("Responding with check-in info");
            } else {
                // Use direct-query as fallback
                return directQuery(request, httpRequest);
            }
        } catch (Exception e) {
            logger.error("Error processing query", e);
            response.put("answer", "Terjadi kesalahan saat memproses pertanyaan Anda: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/general-query")
    public ResponseEntity<Map<String, String>> generalQuery(@RequestBody Map<String, String> request) {
        Map<String, String> response = new HashMap<>();
        String question = request.get("question");
        
        logger.info("Received general query: {}", question);
        
        // Default response
        response.put("answer", "Maaf, saya tidak dapat memahami pertanyaan Anda.");
        
        if (question == null || question.trim().isEmpty()) {
            return ResponseEntity.ok(response);
        }
        
        // Lowercase question for easier matching
        String lowercaseQuestion = question.toLowerCase();
        
        // Cek kata kunci sederhana dan ubah ke format yang lebih spesifik
        if (lowercaseQuestion.equals("dormhub") || lowercaseQuestion.equals("asrama")) {
            lowercaseQuestion = "tentang dormhub";
            logger.info("Converting simple keyword to 'tentang dormhub'");
        } else if (lowercaseQuestion.equals("siapa") || lowercaseQuestion.equals("kamu")) {
            lowercaseQuestion = "siapa kamu";
            logger.info("Converting simple keyword to 'siapa kamu'");
        } else if (lowercaseQuestion.equals("bisa") || lowercaseQuestion.equals("bantu")) {
            lowercaseQuestion = "bisa apa";
            logger.info("Converting simple keyword to 'bisa apa'");
        } else if (lowercaseQuestion.equals("checkout") || lowercaseQuestion.equals("check out")) {
            lowercaseQuestion = "tentang checkout";
            logger.info("Converting to checkout information request");
        } else if (lowercaseQuestion.equals("checkin") || lowercaseQuestion.equals("check in")) {
            lowercaseQuestion = "tentang checkin";
            logger.info("Converting to checkin information request");
        }
        
        try {
            // Get configuration data
            Map<String, String> konfigurasi = new HashMap<>();
            try {
                List<Object[]> konfigurasiList = entityManager.createNativeQuery(
                    "SELECT k_key, k_value FROM konfigurasi"
                ).getResultList();
                
                for (Object[] row : konfigurasiList) {
                    konfigurasi.put((String) row[0], (String) row[1]);
                }
            } catch (Exception e) {
                logger.warn("Could not load configuration data", e);
            }
            
            // Get building name and other info from configuration
            String websiteName = konfigurasi.getOrDefault("web-nama-website", "DormHub");
            String buildingName = konfigurasi.getOrDefault("web-nama-gedung", "asrama");
            
            // Handle general questions about the chatbot or system
            if (lowercaseQuestion.contains("siapa kamu") || 
                lowercaseQuestion.contains("kamu siapa") || 
                lowercaseQuestion.contains("siapa anda") ||
                lowercaseQuestion.contains("anda siapa") ||
                lowercaseQuestion.contains("siapa namamu") ||
                lowercaseQuestion.contains("chatbot")) {
                
                response.put("answer", String.format(
                    "Saya adalah asisten %s, chatbot yang membantu mahasiswa dengan informasi tentang asrama, laporan, dan paket. Saya dapat menjawab pertanyaan tentang data kamar, laporan, dan informasi lainnya yang Anda butuhkan.",
                    websiteName
                ));
            } 
            // Questions about what the bot can do
            else if (lowercaseQuestion.contains("bisa apa") || 
                     lowercaseQuestion.contains("bisa membantu") || 
                     lowercaseQuestion.contains("bisa melakukan") ||
                     lowercaseQuestion.contains("apa yang bisa") ||
                     lowercaseQuestion.contains("membantu apa")) {
                
                response.put("answer", String.format(
                    "Saya dapat membantu Anda dengan informasi tentang:\n" +
                    "- Nomor kamar dan kasur Anda\n" +
                    "- Jumlah dan status laporan Anda\n" +
                    "- Informasi paket/barang Anda\n" +
                    "- Informasi umum tentang %s\n" +
                    "Silakan tanyakan apa yang Anda butuhkan!",
                    websiteName
                ));
            }
            // Greetings
            else if (lowercaseQuestion.contains("halo") || 
                     lowercaseQuestion.contains("hai") || 
                     lowercaseQuestion.contains("hi") ||
                     lowercaseQuestion.contains("hello") ||
                     lowercaseQuestion.contains("pagi") ||
                     lowercaseQuestion.contains("siang") ||
                     lowercaseQuestion.contains("sore") ||
                     lowercaseQuestion.contains("malam")) {
                
                response.put("answer", String.format(
                    "Halo! Selamat datang di %s. Ada yang bisa saya bantu terkait informasi asrama, laporan, atau paket Anda?",
                    websiteName
                ));
            }
            // About the dormitory
            else if (lowercaseQuestion.contains("asrama") || 
                     lowercaseQuestion.contains("gedung") || 
                     lowercaseQuestion.contains("dormhub") ||
                     lowercaseQuestion.contains("dorm") ||
                     lowercaseQuestion.contains("hub") ||
                     lowercaseQuestion.contains("tentang dormhub")) {
                
                response.put("answer", String.format(
                    "%s adalah sistem manajemen asrama yang membantu mahasiswa dalam proses check-in, check-out, pengelolaan laporan, dan penerimaan paket di %s.",
                    websiteName, buildingName
                ));
            }
            // About the check-in process
            else if (lowercaseQuestion.contains("check-in") || 
                     lowercaseQuestion.contains("checkin") ||
                     lowercaseQuestion.contains("tentang checkin")) {
                
                response.put("answer", String.format(
                    "Untuk check-in di %s, Anda perlu login ke sistem dan mengikuti petunjuk pada halaman beranda. " +
                    "Check-in biasanya dilakukan pada awal semester atau periode tertentu sesuai kebijakan asrama.",
                    buildingName
                ));
            }
            // About the check-out process
            else if (lowercaseQuestion.contains("check-out") || 
                     lowercaseQuestion.contains("checkout") ||
                     lowercaseQuestion.contains("tentang checkout")) {
                
                // Get check-out date from configuration
                String checkoutStartDate = konfigurasi.getOrDefault("web-mulai-tgl-co", "");
                String checkoutEndDate = konfigurasi.getOrDefault("web-selesai-tgl-co", "");
                
                if (!checkoutStartDate.isEmpty() && !checkoutEndDate.isEmpty()) {
                    response.put("answer", String.format(
                        "Jadwal check-out di %s adalah dari tanggal %s sampai dengan tanggal %s. " +
                        "Pastikan Anda tidak memiliki tunggakan dan sudah menyelesaikan semua kewajiban sebelum melakukan check-out.",
                        buildingName, checkoutStartDate, checkoutEndDate
                    ));
                } else {
                    response.put("answer", 
                        "Untuk check-out, silakan perhatikan notifikasi pada dashboard Anda. " +
                        "Pastikan Anda tidak memiliki tunggakan dan sudah menyelesaikan semua kewajiban sebelum melakukan check-out."
                    );
                }
            }
            // About reports
            else if (lowercaseQuestion.contains("lapor") || 
                     lowercaseQuestion.contains("keluhan") ||
                     lowercaseQuestion.contains("izin")) {
                
                response.put("answer", 
                    "Untuk membuat laporan baru (keluhan atau izin), silakan akses menu 'Laporan' > 'Buat Laporan' di sidebar kiri. " +
                    "Anda dapat melihat status laporan yang sudah dibuat di menu 'Laporan' > 'Daftar Laporan'."
                );
            }
            // About packages
            else if (lowercaseQuestion.contains("paket") || 
                     lowercaseQuestion.contains("barang")) {
                
                response.put("answer", 
                    "Paket atau barang yang diterima oleh petugas helpdesk akan dicatat dalam sistem. " +
                    "Anda akan mendapatkan notifikasi di dashboard dan bisa melihat detail paket/barang Anda di sana."
                );
            }
            // About room information
            else if (lowercaseQuestion.contains("informasi kamar") || 
                     lowercaseQuestion.contains("info kamar")) {
                
                response.put("answer", 
                    "Untuk melihat informasi detail tentang kamar Anda, silakan akses menu 'Informasi Kamar' di sidebar kiri. " +
                    "Di sana Anda dapat melihat lokasi kamar, detail fasilitas, dan informasi lainnya."
                );
            }
            // Default response for other questions
            else {
                response.put("answer", 
                    "Maaf, saya tidak dapat memahami pertanyaan Anda. Silakan tanyakan tentang informasi kamar, laporan, paket, " +
                    "atau gunakan menu yang tersedia di dashboard untuk navigasi."
                );
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error processing general query", e);
            response.put("answer", "Terjadi kesalahan saat memproses pertanyaan Anda. Silakan coba lagi nanti.");
            return ResponseEntity.ok(response);
        }
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
            String[] possibleNames = {"jwt_token", "jwt", "JWT", "access_token", "Authorization", "auth_token", "token", "JSESSIONID"};
            
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
                
                // Try to decode with JwtTokenProvider
                try {
                    String email = jwtTokenProvider.getUsername(jwt);
                    response.put("decodedEmail", email);
                    
                    if (email != null) {
                        Optional<User> userOpt = userRepository.findByEmail(email);
                        if (userOpt.isPresent()) {
                            User user = userOpt.get();
                            response.put("userFound", true);
                            response.put("userName", user.getNamaLengkap());
                        }
                    }
                } catch (Exception e) {
                    response.put("decodeError", e.getMessage());
                }
                
                // Also try JwtUtil
                try {
                    if (jwtUtil.validateToken(jwt)) {
                        String emailFromJwtUtil = jwtUtil.extractEmail(jwt);
                        response.put("jwtUtilValidation", "success");
                        response.put("emailFromJwtUtil", emailFromJwtUtil);
                        
                        if (emailFromJwtUtil != null) {
                            Optional<User> userOpt = userRepository.findByEmail(emailFromJwtUtil);
                            if (userOpt.isPresent()) {
                                User user = userOpt.get();
                                response.put("userFoundFromJwtUtil", true);
                                response.put("userNameFromJwtUtil", user.getNamaLengkap());
                            }
                        }
                    } else {
                        response.put("jwtUtilValidation", "token invalid");
                    }
                } catch (Exception e) {
                    response.put("jwtUtilError", e.getMessage());
                }
                
                // Look at the raw token
                try {
                    String[] parts = jwt.split("\\.");
                    response.put("tokenParts", parts.length);
                    if (parts.length >= 2) {
                        response.put("tokenHeader", parts[0]);
                        response.put("tokenPayload", parts[1]);
                    }
                } catch (Exception e) {
                    response.put("tokenParsingError", e.getMessage());
                }
            } else {
                response.put("jwtFound", false);
            }
        }
        
        return ResponseEntity.ok(response);
    }
} 