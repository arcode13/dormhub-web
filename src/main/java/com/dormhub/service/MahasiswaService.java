package com.dormhub.service;

import com.dormhub.model.Mahasiswa;
import com.dormhub.repository.MahasiswaRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MahasiswaService {

    @Autowired
    private MahasiswaRepository mahasiswaRepository;

    public List<Mahasiswa> getAllMahasiswa() {
        return mahasiswaRepository.findAll(); // Mengambil semua data mahasiswa
    }

    public Mahasiswa getMahasiswaByUserId(int userId) {
        return mahasiswaRepository.findByUserId(userId)
            .orElseThrow(() -> new IllegalArgumentException("Mahasiswa tidak ditemukan untuk userId: " + userId));
    }
}
