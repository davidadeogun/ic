package com.insurancechoice.backend.repository;

import com.insurancechoice.backend.entity.Problem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProblemRepository extends JpaRepository<Problem, Long> {
    /** All non-template problems that belong to a specific user */
    List<Problem> findByUserIdAndIsTemplateFalseOrderByUpdatedAtDesc(Long userId);
}
