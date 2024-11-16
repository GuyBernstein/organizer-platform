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
    private static final String ADMIN_WHATSAPP = "972509603888";

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
                    .whatsappNumber(ADMIN_WHATSAPP)
                    .role(UserRole.ADMIN)
                    .authorized(true)
                    .build();

            return repository.save(admin);
        });
    }

    public AppUser createUnauthorizedUser(String whatsappNumber) {
        return repository.findByWhatsappNumber(whatsappNumber)
                .orElseGet(() -> {

                    // Generate a temporary email using WhatsApp number
                    String tempEmail =
                            "whatsapp." + whatsappNumber.replaceAll("[^0-9]", "") + "@temp.platform.com";

                    AppUser newUser = AppUser.UserBuilder.anUser()
                            .whatsappNumber(whatsappNumber)
                            .email(tempEmail)  // Set the generated email
                            .role(UserRole.UNAUTHORIZED)
                            .authorized(false)
                            .build();

                    return repository.save(newUser);
                });
    }

    public AppUser restoreAdmin() {
        Optional<AppUser> adminUser = repository.findByEmail(ADMIN_EMAIL);

        if (adminUser.isPresent()) {
            return toAdmin(adminUser);
        } else {
            // If admin user somehow got deleted, recreate it
            AppUser admin = createAdmin();

            return repository.save(admin);
        }
    }

    private static AppUser createAdmin() {
        return AppUser.UserBuilder.anUser()
                .email(ADMIN_EMAIL)
                .whatsappNumber(ADMIN_WHATSAPP)
                .role(UserRole.ADMIN)
                .authorized(true)
                .build();
    }

    private AppUser toAdmin(Optional<AppUser> adminUser) {
        AppUser admin = adminUser.get();
        admin.setRole(UserRole.ADMIN);
        admin.setWhatsappNumber(ADMIN_WHATSAPP);
        admin.setAuthorized(true);
        return repository.save(admin);
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