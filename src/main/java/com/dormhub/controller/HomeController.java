package com.dormhub.controller;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;

import com.dormhub.repository.KonfigurasiRepository;

@Controller
public class HomeController {

    @Autowired
    private KonfigurasiRepository konfigurasiRepository;

    @GetMapping("/")
    public String homePage(Model model) {
        Map<String, String> konfigurasi = konfigurasiRepository.findAllAsMap().stream()
            .collect(Collectors.toMap(
                entry -> entry.get("key"),
                entry -> entry.get("value")
            ));

        model.addAttribute("konfigurasi", konfigurasi);

        return "Home";
    }
}