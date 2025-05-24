package com.dormhub.models;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "mahasiswa")
public class Mahasiswa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "jurusan_id")
    private Integer jurusanId;

    @Column(name = "no_kamar")
    private Integer noKamar;

    @Column(name = "no_kasur")
    private Integer noKasur;

    @Column(name = "is_checkin")
    private Integer isCheckin;

    @Column(name = "is_checkout")
    private Integer isCheckout;

    @Column(name = "waktu_checkin")
    private LocalDateTime waktuCheckin;

    @Column(name = "waktu_checkout")
    private LocalDateTime waktuCheckout;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getJurusanId() {
        return jurusanId;
    }

    public void setJurusanId(Integer jurusanId) {
        this.jurusanId = jurusanId;
    }

    public Integer getNoKamar() {
        return noKamar;
    }

    public void setNoKamar(Integer noKamar) {
        this.noKamar = noKamar;
    }

    public Integer getNoKasur() {
        return noKasur;
    }

    public void setNoKasur(Integer noKasur) {
        this.noKasur = noKasur;
    }

    public Integer getIsCheckin() {
        return isCheckin;
    }

    public void setIsCheckin(Integer isCheckin) {
        this.isCheckin = isCheckin;
    }

    public Integer getIsCheckout() {
        return isCheckout;
    }

    public void setIsCheckout(Integer isCheckout) {
        this.isCheckout = isCheckout;
    }

    public LocalDateTime getWaktuCheckin() {
        return waktuCheckin;
    }

    public void setWaktuCheckin(LocalDateTime waktuCheckin) {
        this.waktuCheckin = waktuCheckin;
    }

    public LocalDateTime getWaktuCheckout() {
        return waktuCheckout;
    }

    public void setWaktuCheckout(LocalDateTime waktuCheckout) {
        this.waktuCheckout = waktuCheckout;
    }
} 