package com.dormhub.model;

import jakarta.persistence.*;

@Entity
@Table(name = "konfigurasi")
public class Konfigurasi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "k_key", nullable = false, unique = true)
    private String kKey;

    @Column(name = "k_value", nullable = false)
    private String kValue;

    
    public Konfigurasi() {}


    public Konfigurasi(int id, String kKey, String kValue) {
        this.id = id;
        this.kKey = kKey;
        this.kValue = kValue;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getKKey() {
        return kKey;
    }

    public void setKKey(String kKey) {
        this.kKey = kKey;
    }

    public String getKValue() {
        return kValue;
    }

    public void setKValue(String kValue) {
        this.kValue = kValue;
    }
}