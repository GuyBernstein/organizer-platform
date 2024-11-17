package com.organizer.platform.repository;


import com.organizer.platform.model.User.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmail(String email);
    Optional<AppUser> findByWhatsappNumber(String whatsappNumber);
    List<AppUser> findByAuthorized(boolean authorized);
}
