package com.dormhub.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.dormhub.models.LaporanUmum;

@Repository
public interface LaporanUmumRepository extends JpaRepository<LaporanUmum, Integer> {
    List<LaporanUmum> findByMahasiswaId(Integer mahasiswaId);
    List<LaporanUmum> findByMahasiswaIdAndJenis(Integer mahasiswaId, String jenis);
} 