package com.insurancechoice.backend.repository;

import com.insurancechoice.backend.entity.InsuranceProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InsuranceProfileRepository extends JpaRepository<InsuranceProfile, Long> {
    Optional<InsuranceProfile> findByUserObjectId(String userObjectId);
    boolean existsByUserObjectId(String userObjectId);
}
