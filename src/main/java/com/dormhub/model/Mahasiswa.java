package com.dormhub.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "mahasiswa")
public class Mahasiswa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "user_id", insertable = false, updatable = false) // Hindari duplikasi
    private int userId;

    @ManyToOne
    @JoinColumn(name = "jurusan_id", nullable = false)
    private Jurusan jurusan;

    @Column(name = "no_kamar", nullable = false)
    private int noKamar;

    @Column(name = "no_kasur", nullable = false)
    private int noKasur;

    @Column(name = "is_checkin", nullable = false)
    private int isCheckin;

    @Column(name = "is_checkout", nullable = false)
    private int isCheckout;

    @Column(name = "waktu_checkin")
    private LocalDateTime waktuCheckin;

    @Column(name = "waktu_checkout")
    private LocalDateTime waktuCheckout;

    
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public Jurusan getJurusan() {
        return jurusan;
    }

    public void setJurusan(Jurusan jurusan) {
        this.jurusan = jurusan;
    }

    public int getNoKamar() {
        return noKamar;
    }

    public void setNoKamar(int noKamar) {
        this.noKamar = noKamar;
    }

    public int getNoKasur() {
        return noKasur;
    }

    public void setNoKasur(int noKasur) {
        this.noKasur = noKasur;
    }

    public int getIsCheckin() {
        return isCheckin;
    }

    public void setIsCheckin(int isCheckin) {
        this.isCheckin = isCheckin;
    }

    public int getIsCheckout() {
        return isCheckout;
    }

    public void setIsCheckout(int isCheckout) {
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
