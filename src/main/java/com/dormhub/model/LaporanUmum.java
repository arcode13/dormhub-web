package com.dormhub.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "laporan_umum")
public class LaporanUmum {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "mahasiswa_id", nullable = false)
    private int mahasiswaId;

    @Column(name = "jenis", nullable = false)
    private String jenis;

    @Column(name = "alasan", nullable = false)
    private String alasan;

    @Column(name = "bukti_foto", nullable = true)
    private String buktiFoto;

    @Column(name = "status", nullable = false)
    private String status = "menunggu";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Transient
    private String namaLengkap;

    @Transient
    private int noKamar;

    @Transient
    private String formattedCreatedAt;
    
    public String getNamaLengkap() {
        return namaLengkap;
    }
    
    public void setNamaLengkap(String namaLengkap) {
        this.namaLengkap = namaLengkap;
    }
    
    public int getNoKamar() {
        return noKamar;
    }
    
    public void setNoKamar(int noKamar) {
        this.noKamar = noKamar;
    }

    
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getMahasiswaId() {
        return mahasiswaId;
    }

    public void setMahasiswaId(int mahasiswaId) {
        this.mahasiswaId = mahasiswaId;
    }

    public String getJenis() {
        return jenis;
    }

    public void setJenis(String jenis) {
        this.jenis = jenis;
    }

    public String getAlasan() {
        return alasan;
    }

    public void setAlasan(String alasan) {
        this.alasan = alasan;
    }

    public String getBuktiFoto() {
        return buktiFoto;
    }

    public void setBuktiFoto(String buktiFoto) {
        this.buktiFoto = buktiFoto;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getFormattedCreatedAt() {
        return formattedCreatedAt;
    }
    
    public void setFormattedCreatedAt(String formattedCreatedAt) {
        this.formattedCreatedAt = formattedCreatedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
