package com.example.aichatbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.aichatbot.entity.AbsenceRequest;

@Repository
public interface AbsenceRequestRepository extends JpaRepository<AbsenceRequest, Long> {
    // Add custom query methods if needed
}
