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
    
    /**
     * Menemukan mahasiswa berdasarkan ID
     *
     * @param id ID mahasiswa
     * @return Objek Mahasiswa jika ditemukan, null jika tidak
     */
    public Mahasiswa findById(int id) {
        return mahasiswaRepository.findById(id).orElse(null);
    }
    
    /**
     * Menyimpan atau memperbarui data mahasiswa
     *
     * @param mahasiswa Objek Mahasiswa yang akan disimpan/diperbarui
     * @return Objek Mahasiswa yang telah disimpan
     */
    public Mahasiswa saveMahasiswa(Mahasiswa mahasiswa) {
        return mahasiswaRepository.save(mahasiswa);
    }
    
    /**
     * Menghapus data mahasiswa berdasarkan ID
     *
     * @param id ID mahasiswa yang akan dihapus
     */
    public void deleteMahasiswa(int id) {
        mahasiswaRepository.deleteById(id);
    }
}
