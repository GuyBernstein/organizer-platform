package com.organizer.platform.repository;

import com.organizer.platform.model.User.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing AppUser entities.
 * Provides methods for user-related database operations including authentication and authorization queries.
 *
 * @see AppUser
 * @see JpaRepository
 */
public interface UserRepository extends JpaRepository<AppUser, Long> {

    /**
     * Finds a user by their email address.
     * Commonly used for authentication and user lookup purposes.
     *
     * @param email The email address to search for
     * @return An Optional containing the found AppUser or empty if not found
     * @throws org.springframework.dao.DataAccessException if there's an error accessing the database
     */
    Optional<AppUser> findByEmail(String email);

    /**
     * Finds a user by their WhatsApp number.
     * Used for integrating WhatsApp functionality with user accounts.
     *
     * @param whatsappNumber The WhatsApp number to search for
     * @return An Optional containing the found AppUser or empty if not found
     * @throws org.springframework.dao.DataAccessException if there's an error accessing the database
     */
    Optional<AppUser> findByWhatsappNumber(String whatsappNumber);

    /**
     * Retrieves all users based on their authorization status.
     * Useful for administrative purposes and user management.
     *
     * @param authorized The authorization status to filter by
     * @return List of AppUser entities matching the authorization status
     * @throws org.springframework.dao.DataAccessException if there's an error accessing the database
     */
    List<AppUser> findByAuthorized(boolean authorized);
}