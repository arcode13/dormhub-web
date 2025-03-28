package com.dormhub.service;

import com.dormhub.model.Jurusan;
import com.dormhub.repository.JurusanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class JurusanService {

    @Autowired
    private JurusanRepository jurusanRepository;

    public List<Jurusan> getAllJurusan() {
        return jurusanRepository.findAll();
    }

    public Jurusan findById(int jurusanId) {
        Optional<Jurusan> jurusan = jurusanRepository.findById(jurusanId);
        return jurusan.orElse(null); // Mengembalikan null jika tidak ditemukan
    }

    public Jurusan saveJurusan(Jurusan jurusan) {
        return jurusanRepository.save(jurusan);
    }
    
    public void deleteJurusan(int id) {
        jurusanRepository.deleteById(id);
    }
    
}
