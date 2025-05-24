package com.dormhub.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.dormhub.models.LaporanBarang;

@Repository
public interface LaporanBarangRepository extends JpaRepository<LaporanBarang, Integer> {
    List<LaporanBarang> findByMahasiswaId(Integer mahasiswaId);
} 