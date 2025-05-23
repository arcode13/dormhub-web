package com.dormhub.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@RestController
@RequestMapping("/api/chatbot")
@CrossOrigin(origins = "*")
public class ChatBotApiController {

    private static final Logger logger = LoggerFactory.getLogger(ChatBotApiController.class);

    @Value("${gemini.api.key}")
    private String geminiApiKey;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    public ChatBotApiController() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }
    
    @PostMapping("/message")
    public ResponseEntity<JsonNode> processMessage(@RequestBody JsonNode requestBody) {
        try {
            logger.info("Received chatbot request");
            
            // Dapatkan pesan dari request body
            String userMessage = requestBody.get("message").asText();
            logger.debug("User message: {}", userMessage);
            
            // Tambahkan konteks untuk Gemini
            String fullPrompt = "Kamu adalah asisten DormHub, sebuah aplikasi asrama mahasiswa. Berikan jawaban singkat dan padat dalam Bahasa Indonesia. ";
            fullPrompt += "Informasi tentang DormHub: DormHub adalah aplikasi manajemen asrama yang memiliki fitur check-in, check-out, laporan keluhan, dan informasi kamar. ";
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
            String geminiUrl = "https://generativelanguage.googleapis.com/v1/models/gemini-pro:generateContent?key=" + geminiApiKey;
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
    public ResponseEntity<JsonNode> fallbackResponse(@RequestBody JsonNode requestBody) {
        try {
            logger.info("Using fallback response");
            
            String userMessage = requestBody.get("message").asText();
            logger.debug("User message for fallback: {}", userMessage);
            
            ObjectNode responseNode = objectMapper.createObjectNode();
            ObjectNode candidate = objectMapper.createObjectNode();
            ObjectNode content = objectMapper.createObjectNode();
            ObjectNode part = objectMapper.createObjectNode();
            
            // Logika fallback sederhana
            String response;
            String lowercaseMessage = userMessage.toLowerCase();
            
            if (lowercaseMessage.contains("kamar")) {
                response = "Untuk informasi kamar, Anda bisa mengakses menu 'Informasi Kamar' di sidebar kiri.";
            } else if (lowercaseMessage.contains("laporan") || lowercaseMessage.contains("keluhan")) {
                response = "Untuk membuat laporan atau keluhan, silakan akses menu 'Laporan' > 'Buat Laporan' di sidebar kiri.";
            } else if (lowercaseMessage.contains("checkout") || lowercaseMessage.contains("check-out")) {
                response = "Untuk melakukan check-out, harap ikuti petunjuk di notifikasi pada dashboard Anda.";
            } else if (lowercaseMessage.contains("halo") || lowercaseMessage.contains("hai") || lowercaseMessage.contains("hi")) {
                response = "Halo! Ada yang bisa saya bantu seputar layanan asrama?";
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