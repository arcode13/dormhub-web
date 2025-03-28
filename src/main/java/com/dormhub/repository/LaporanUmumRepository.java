package com.dormhub.repository;

import com.dormhub.model.LaporanUmum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LaporanUmumRepository extends JpaRepository<LaporanUmum, Integer> {

    /**
     * Menghitung jumlah laporan izin bulan ini berdasarkan mahasiswa_id.
     *
     * @param mahasiswaId 
     * @return 
     */
    @Query(value = "SELECT COUNT(*) FROM laporan_umum WHERE jenis = 'izin' AND mahasiswa_id = :mahasiswaId AND MONTH(created_at) = MONTH(CURRENT_DATE()) AND YEAR(created_at) = YEAR(CURRENT_DATE())", nativeQuery = true)
    int countLaporanIzin(int mahasiswaId);

    /**
     * Menghitung jumlah laporan keluhan bulan ini berdasarkan mahasiswa_id.
     *
     * @param mahasiswaId 
     * @return 
     */
    @Query(value = "SELECT COUNT(*) FROM laporan_umum WHERE jenis = 'keluhan' AND mahasiswa_id = :mahasiswaId AND MONTH(created_at) = MONTH(CURRENT_DATE()) AND YEAR(created_at) = YEAR(CURRENT_DATE())", nativeQuery = true)
    int countLaporanKeluhan(int mahasiswaId);

    List<LaporanUmum> findAllByMahasiswaId(int mahasiswaId);

    @Query("SELECT COUNT(l) FROM LaporanUmum l WHERE l.jenis = 'Izin' AND MONTH(l.createdAt) = MONTH(CURRENT_DATE) AND YEAR(l.createdAt) = YEAR(CURRENT_DATE)")
    int getSemuaJumlahLaporanIzinBulanIni();

    @Query("SELECT COUNT(l) FROM LaporanUmum l WHERE l.jenis = 'Izin'")
    int getTotalSemuaLaporanIzin();

    @Query("SELECT l FROM LaporanUmum l WHERE l.jenis = 'Keluhan'")
    List<LaporanUmum> findAllKeluhan();

    @Query("SELECT l FROM LaporanUmum l WHERE l.jenis = 'Izin'")
    List<LaporanUmum> findAllIzin();
}
