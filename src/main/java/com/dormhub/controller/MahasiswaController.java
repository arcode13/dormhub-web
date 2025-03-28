package com.dormhub.controller;

import com.dormhub.model.Mahasiswa;
import com.dormhub.service.MahasiswaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class MahasiswaController {

    @Autowired
    private MahasiswaService mahasiswaService;

    @GetMapping("/admin/mahasiswa")
    public String daftarMahasiswa(Model model) {

        List<Mahasiswa> mahasiswaList = mahasiswaService.getAllMahasiswa();
        
        model.addAttribute("mahasiswaList", mahasiswaList);
        return "admin/Mahasiswa/index";
    }
}
