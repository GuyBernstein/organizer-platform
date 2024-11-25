package com.organizer.platform.repository;

import com.organizer.platform.model.organizedDTO.NextStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NextStepRepository extends JpaRepository<NextStep, Long> {
    Optional<NextStep> findByName(String name);
}
