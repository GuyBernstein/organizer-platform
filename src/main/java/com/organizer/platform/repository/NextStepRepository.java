package com.organizer.platform.repository;

import com.organizer.platform.model.organizedDTO.NextStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository interface for managing NextStep entities.
 * Extends JpaRepository to inherit basic CRUD operations and pagination support.
 *
 * @see NextStep
 * @see JpaRepository
 */
public interface NextStepRepository extends JpaRepository<NextStep, Long> {

    /**
     * Retrieves a NextStep entity by its name.
     * Following Spring Data JPA naming conventions for automatic query generation.
     *
     * @param name The name of the NextStep to find
     * @return An Optional containing the found NextStep or empty if not found
     * @throws org.springframework.dao.DataAccessException if there's an error accessing the database
     */
    Optional<NextStep> findByName(String name);
}