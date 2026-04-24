package com.insurancechoice.backend.repository;

import com.insurancechoice.backend.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmail(String email);
    Optional<AppUser> findByUserObjectId(String userObjectId);
    boolean existsByEmail(String email);
}
