package com.dormhub.model;

import jakarta.persistence.*;

@Entity
@Table(name = "senior_residence")
public class SeniorResidence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @OneToOne
    @JoinColumn(name = "mahasiswa_id", nullable = false)
    private Mahasiswa mahasiswa;

    // Getter dan Setter
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Mahasiswa getMahasiswa() {
        return mahasiswa;
    }

    public void setMahasiswa(Mahasiswa mahasiswa) {
        this.mahasiswa = mahasiswa;
    }
}
