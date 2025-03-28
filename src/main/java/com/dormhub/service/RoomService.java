package com.dormhub.service;

import com.dormhub.repository.KonfigurasiRepository;
import com.dormhub.repository.MahasiswaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RoomService {

    @Autowired
    private MahasiswaRepository mahasiswaRepository;

    @Autowired
    private KonfigurasiRepository konfigurasiRepository;

    public int[] assignRoom() {
        
        int maxKasurPerKamar = Integer.parseInt(konfigurasiRepository.findKValueByKKey("web-kasur").orElseThrow(
            () -> new RuntimeException("Konfigurasi 'web-kasur' tidak ditemukan")
        ));
        
        int maxKamarPerLantai = Integer.parseInt(konfigurasiRepository.findKValueByKKey("web-kamar").orElseThrow(
            () -> new RuntimeException("Konfigurasi 'web-kamar' tidak ditemukan")
        ));
        
        int totalLantai = Integer.parseInt(konfigurasiRepository.findKValueByKKey("web-lantai").orElseThrow(
            () -> new RuntimeException("Konfigurasi 'web-lantai' tidak ditemukan")
        ));

        
        validateKonfigurasi(maxKasurPerKamar, maxKamarPerLantai, totalLantai);

        
        int lastRoomNumber = mahasiswaRepository.findLastRoomNumber();
        int lastFloor = getFloorFromRoomNumber(lastRoomNumber); // Lantai dari kamar terakhir
        int occupantsInLastRoom = mahasiswaRepository.countOccupantsInRoom(lastRoomNumber);

        
        return determineRoomAndBed(
            lastRoomNumber, lastFloor, occupantsInLastRoom,
            maxKasurPerKamar, maxKamarPerLantai, totalLantai
        );
    }

    private void validateKonfigurasi(int kasur, int kamar, int lantai) {
        if (kasur <= 0) {
            throw new IllegalArgumentException("Jumlah kasur per kamar harus lebih besar dari 0");
        }
        if (kamar <= 0) {
            throw new IllegalArgumentException("Jumlah kamar per lantai harus lebih besar dari 0");
        }
        if (lantai <= 0) {
            throw new IllegalArgumentException("Jumlah lantai harus lebih besar dari 0");
        }
    }

    private int getFloorFromRoomNumber(int roomNumber) {
        return roomNumber / 100; 
    }

    private int[] determineRoomAndBed(
        int lastRoomNumber, int lastFloor, int occupantsInLastRoom,
        int maxKasurPerKamar, int maxKamarPerLantai, int totalLantai
    ) {
        if (occupantsInLastRoom >= maxKasurPerKamar) {
            if (lastRoomNumber % 100 < maxKamarPerLantai) {
                return new int[]{lastRoomNumber + 1, 1}; 
            } else if (lastFloor < totalLantai) {
                int nextFloorRoom = (lastFloor + 1) * 100 + 1;
                return new int[]{nextFloorRoom, 1}; 
            } else {
                throw new RuntimeException("Semua kamar di semua lantai penuh");
            }
        } else {
            return new int[]{lastRoomNumber, occupantsInLastRoom + 1};
        }
    }
}
