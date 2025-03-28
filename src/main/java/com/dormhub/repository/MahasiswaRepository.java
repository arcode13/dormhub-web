package com.dormhub.repository;

import com.dormhub.model.Mahasiswa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MahasiswaRepository extends JpaRepository<Mahasiswa, Integer> {

    @Query("SELECT COALESCE(MAX(m.noKamar), 100) FROM Mahasiswa m")
    int findLastRoomNumber();

    @Query("SELECT COUNT(m) FROM Mahasiswa m WHERE m.noKamar = ?1")
    int countOccupantsInRoom(int noKamar);

    Optional<Mahasiswa> findByUserId(int userId);
    
    List<Mahasiswa> findByNoKamar(int noKamar);

    @Query("SELECT m FROM Mahasiswa m JOIN FETCH m.user WHERE m.noKamar = :noKamar")
    List<Mahasiswa> findByNoKamarWithUser(@Param("noKamar") int noKamar);
}
