package com.dormhub.repository;

import com.dormhub.model.Jurusan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JurusanRepository extends JpaRepository<Jurusan, Integer> {
}
