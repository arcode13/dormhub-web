package com.dormhub.repository;

import com.dormhub.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailAndLevel_Id(String email, Integer levelId);

    Optional<User> findByToken(String token);

    @Query("SELECT COUNT(u) FROM User u WHERE u.level.id IN (1, 2)")
    int getAllMahasiswa();

    @Query("SELECT COUNT(u) FROM User u WHERE u.level.id = 2")
    int getAllSeniorResidence();

    @Query("SELECT COUNT(u) FROM User u WHERE u.level.id = 3")
    int getAllHelpDesk();
}
