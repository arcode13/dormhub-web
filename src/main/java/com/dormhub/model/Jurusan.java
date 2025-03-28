package com.dormhub.model;

import jakarta.persistence.*;

@Entity
@Table(name = "jurusan")
public class Jurusan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "nama", nullable = false)
    private String nama;

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNama() {
        return nama;
    }

    public void setNama(String nama) {
        this.nama = nama;
    }
}
