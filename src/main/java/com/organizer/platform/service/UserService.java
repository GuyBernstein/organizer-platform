package com.organizer.platform.service;


import com.organizer.platform.model.AppUser;
import com.organizer.platform.model.UserRole;
import com.organizer.platform.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static com.organizer.platform.model.AppUser.UserBuilder.anUser;

@Service
public class UserService {
    private static final String ADMIN_EMAIL = "guyu669@gmail.com";
    private final UserRepository repository;

    @Autowired
    public UserService(UserRepository repository) {
        this.repository = repository;
        initializeAdmin();
    }

    private void initializeAdmin() {
        repository.findByEmail(ADMIN_EMAIL).orElseGet(() -> {
            AppUser admin = anUser()
                    .email(ADMIN_EMAIL)
                    .role(UserRole.ADMIN)
                    .authorized(true)
                    .build();
            return repository.save(admin);
        });
    }

    public Optional<AppUser> findByEmail(String email) {
        return repository.findByEmail(email);
    }

    public Optional<AppUser> findByWhatsappNumber(String whatsappNumber) {
        return repository.findByWhatsappNumber(whatsappNumber);
    }

    public List<AppUser> findUnauthorizedUsers() {
        return repository.findByAuthorized(false);
    }

    public List<AppUser> findAll() {
        return repository.findAll();
    }

    public Optional<AppUser> findById(Long id) {
        return repository.findById(id);
    }

    public List<AppUser> findByAuthorized(boolean authorized) {
        return repository.findByAuthorized(authorized);
    }

    public AppUser save(AppUser appUser) {
        if (appUser == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        return repository.save(appUser);
    }

    public boolean isAuthorizedNumber(String whatsappNumber) {
        return findByWhatsappNumber(whatsappNumber)
                .map(AppUser::isAuthorized)
                .orElse(false);
    }

    public boolean isAdmin(String email) {
        return findByEmail(email)
                .map(appUser -> appUser.getRole() == UserRole.ADMIN)
                .orElse(false);
    }

    public void delete(AppUser appUser) {
        repository.delete(appUser);
    }

    public void deleteAll() {
        repository.deleteAll();
    }
}