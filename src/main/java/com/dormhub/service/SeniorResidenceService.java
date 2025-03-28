package com.dormhub.service;

import com.dormhub.model.SeniorResidence;
import com.dormhub.repository.SeniorResidenceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SeniorResidenceService {

    @Autowired
    private SeniorResidenceRepository seniorResidenceRepository;

    /**
     * Mendapatkan data Senior Residence berdasarkan mahasiswa ID.
     *
     * @param mahasiswaId 
     * @return 
     */
    public Optional<SeniorResidence> getSeniorResidenceByMahasiswaId(int mahasiswaId) {
        return seniorResidenceRepository.findByMahasiswaId(mahasiswaId);
    }

    /**
     * Mendapatkan data Senior Residence berdasarkan ID.
     *
     * @param id 
     * @return 
     */
    public Optional<SeniorResidence> getSeniorResidenceById(int id) {
        return seniorResidenceRepository.findById(id);
    }

    /**
     * Menyimpan atau memperbarui data Senior Residence.
     *
     * @param seniorResidence 
     */
    public void saveSeniorResidence(SeniorResidence seniorResidence) {
        seniorResidenceRepository.save(seniorResidence);
    }

    public List<SeniorResidence> getAllSeniorResidence() {
        return seniorResidenceRepository.findAll();
    }
    
    public void deleteSeniorResidence(int id) {
        seniorResidenceRepository.deleteById(id);
    }
}
