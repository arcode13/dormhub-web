package com.dormhub.repository;

import com.dormhub.model.SeniorResidence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SeniorResidenceRepository extends JpaRepository<SeniorResidence, Integer> {
    
    Optional<SeniorResidence> findByMahasiswaId(int mahasiswaId);

    
    Optional<SeniorResidence> findById(int id);

    boolean existsByMahasiswaId(int mahasiswaId);
}