package com.dormhub.service;

import com.dormhub.model.LaporanUmum;
import com.dormhub.model.Mahasiswa;
import com.dormhub.repository.LaporanUmumRepository;
import com.dormhub.repository.MahasiswaRepository;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LaporanService {

    @Autowired
    private LaporanUmumRepository laporanUmumRepository;

    @Autowired
    private MahasiswaRepository mahasiswaRepository;

    
    public int getJumlahLaporanIzinBulanIni(int userId) {
        Mahasiswa mahasiswa = mahasiswaRepository.findByUserId(userId)
            .orElseThrow(() -> new IllegalArgumentException("Mahasiswa tidak ditemukan untuk userId: " + userId));
        
        return laporanUmumRepository.countLaporanIzin(mahasiswa.getId());
    }

    
    public int getJumlahLaporanKeluhanBulanIni(int userId) {
        Mahasiswa mahasiswa = mahasiswaRepository.findByUserId(userId)
            .orElseThrow(() -> new IllegalArgumentException("Mahasiswa tidak ditemukan untuk userId: " + userId));

        return laporanUmumRepository.countLaporanKeluhan(mahasiswa.getId());
    }

    public void buatLaporanUmum(int mahasiswaId, String jenis, String alasan, String buktiFoto) {
        LaporanUmum laporan = new LaporanUmum();
        laporan.setMahasiswaId(mahasiswaId);
        laporan.setJenis(jenis);
        laporan.setAlasan(alasan);
        laporan.setBuktiFoto(buktiFoto);
        laporan.setStatus("menunggu");
        laporan.setCreatedAt(LocalDateTime.now());
        laporan.setUpdatedAt(LocalDateTime.now());
        laporanUmumRepository.save(laporan);
    }
}
