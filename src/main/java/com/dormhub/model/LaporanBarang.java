package com.dormhub.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "laporan_barang")
public class LaporanBarang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "helpdesk_id", nullable = false)
    private int helpdeskId;

    @Column(name = "mahasiswa_id", nullable = false)
    private int mahasiswaId;

    @Column(name = "jenis", nullable = false, length = 10)
    private String jenis;

    @Column(name = "bukti_foto", nullable = false)
    private String buktiFoto;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Transient
    private String formattedCreatedAt;

    @Transient
    private String namaLengkap;
    
    @Transient
    private int noKamar;
    
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

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getHelpdeskId() {
        return helpdeskId;
    }

    public void setHelpdeskId(int helpdeskId) {
        this.helpdeskId = helpdeskId;
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
