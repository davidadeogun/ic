package com.insurancechoice.backend.repository;

import com.insurancechoice.backend.entity.GptCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GptCacheRepository extends JpaRepository<GptCache, Long> {

    Optional<GptCache> findByUserObjectIdAndProblemType(String userObjectId, int problemType);

    /** All cached entries for a user — used to find the other mode's cached risk values. */
    List<GptCache> findAllByUserObjectId(String userObjectId);

    /** Purge all cached responses for a user (called when profile changes). */
    void deleteAllByUserObjectId(String userObjectId);
}
