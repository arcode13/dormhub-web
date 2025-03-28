package com.dormhub.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

public class LaporanBarangTest {

    @Test
    public void testGettersAndSetters() {
        // Arrange
        LaporanBarang laporan = new LaporanBarang();
        LocalDateTime now = LocalDateTime.now();

        // Act
        laporan.setId(1);
        laporan.setHelpdeskId(2);
        laporan.setMahasiswaId(3);
        laporan.setJenis("Paket");
        laporan.setBuktiFoto("foto.jpg");
        laporan.setStatus("menunggu");
        laporan.setCreatedAt(now);
        laporan.setUpdatedAt(now);
        laporan.setNamaLengkap("John Doe");
        laporan.setNoKamar(101);

        // Print results
        System.out.println("ID: " + laporan.getId());
        System.out.println("Helpdesk ID: " + laporan.getHelpdeskId());
        System.out.println("Mahasiswa ID: " + laporan.getMahasiswaId());
        System.out.println("Jenis: " + laporan.getJenis());
        System.out.println("Bukti Foto: " + laporan.getBuktiFoto());
        System.out.println("Status: " + laporan.getStatus());
        System.out.println("Created At: " + laporan.getCreatedAt());
        System.out.println("Updated At: " + laporan.getUpdatedAt());
        System.out.println("Nama Lengkap: " + laporan.getNamaLengkap());
        System.out.println("No Kamar: " + laporan.getNoKamar());

        // Assert
        Assertions.assertEquals(1, laporan.getId());
        Assertions.assertEquals(2, laporan.getHelpdeskId());
        Assertions.assertEquals(3, laporan.getMahasiswaId());
        Assertions.assertEquals("Paket", laporan.getJenis());
        Assertions.assertEquals("foto.jpg", laporan.getBuktiFoto());
        Assertions.assertEquals("menunggu", laporan.getStatus());
        Assertions.assertEquals(now, laporan.getCreatedAt());
        Assertions.assertEquals(now, laporan.getUpdatedAt());
        Assertions.assertEquals("John Doe", laporan.getNamaLengkap());
        Assertions.assertEquals(101, laporan.getNoKamar());
    }
}
