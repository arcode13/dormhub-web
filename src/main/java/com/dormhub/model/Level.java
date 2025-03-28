package com.dormhub.model;

import jakarta.persistence.*;

@Entity
@Table(name = "level")
public class Level {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; 

    @Column(nullable = false)
    private String nama;

    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNama() {
        return nama;
    }

    public void setNama(String nama) {
        this.nama = nama;
    }
}
