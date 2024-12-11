package com.organizer.platform.service.User;


import com.organizer.platform.model.User.AppUser;
import com.organizer.platform.model.User.UserRole;
import com.organizer.platform.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static com.organizer.platform.model.User.AppUser.UserBuilder.anUser;

@Service
public class UserService {
    public static final String ADMIN_EMAIL = "guyu669@gmail.com";
    private static final String ADMIN_WHATSAPP = "972509603888";
    private static final String ADMIN_PICTURE = "https://lh3.googleusercontent.com/a/ACg8ocJRoPwngyQ0tZOZ7ObgSXVLwkEAnWKs5HbapL8uA-Kh5V7_hoU=s96-c";
    private static final String ADMIN_NAME = "Guy Bernstein";
    private static final boolean ADMIN_AUTHORIZED = true;
    private final UserRepository repository;

    @Autowired
    public UserService(UserRepository repository) {
        this.repository = repository;
        initializeAdmin();
    }

    private AppUser initializeAdmin() {
        return repository.findByEmail(ADMIN_EMAIL).orElseGet(() -> {
            AppUser admin = anUser()
                    .name(ADMIN_NAME)
                    .pictureUrl(ADMIN_PICTURE)
                    .email(ADMIN_EMAIL)
                    .whatsappNumber(ADMIN_WHATSAPP)
                    .role(UserRole.ADMIN)
                    .authorized(ADMIN_AUTHORIZED)
                    .build();

            return repository.save(admin);
        });
    }

    public void createUserFromPhone(String email, UserRole role, String phone, String name) {
        repository.findByWhatsappNumber(phone)
                .ifPresentOrElse(
                        existingUser -> {
                            existingUser.setName(name);
                            existingUser.setAuthorized(role != UserRole.UNAUTHORIZED);
                            existingUser.setRole(role);
                            existingUser.setEmail(email);
                            repository.save(existingUser);
                        },
                        () -> repository.save(toAuthorizedUser(phone, email, role, name))
                );
    }

    public AppUser createUser(OAuth2User oauth2User) {
        return anUser()
                .name(oauth2User.getAttribute("name"))
                .pictureUrl(oauth2User.getAttribute("picture"))
                .email(oauth2User.getAttribute("email"))
                .authorized(false)
                .role(UserRole.UNAUTHORIZED)
                .build();
    }
    private static AppUser toAuthorizedUser(String whatsappNumber, String email, UserRole role, String name) {
        return AppUser.UserBuilder.anUser()
                .whatsappNumber(whatsappNumber)
                .name(name)
                .email(email)
                .role(role)
                .authorized(role != UserRole.UNAUTHORIZED)
                .build();
    }

    private static AppUser toUnauthorizedUser(String whatsappNumber, String tempEmail) {
        return AppUser.UserBuilder.anUser()
                .whatsappNumber(whatsappNumber)
                .email(tempEmail)
                .role(UserRole.UNAUTHORIZED)
                .authorized(false)
                .build();
    }


    public AppUser restoreAdmin() {
        return initializeAdmin();
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

    public boolean processNewUser(String whatsappNumber) {
        return repository.findByWhatsappNumber(whatsappNumber)
                .map(user -> {
                    // If user exists, check if authorized
                    if (!user.getEmail().endsWith("@temp.platform.com")) {
                        return user.isAuthorized();
                    }
                    // If it's a temp user, look for linked Google account
                    return false;
                })
                .orElseGet(() -> {
                    // User doesn't exist, create unauthorized temp user
                    String tempEmail = "whatsapp." + whatsappNumber.replaceAll("[^0-9]", "")
                            + "@temp.platform.com";
                    repository.save(toUnauthorizedUser(whatsappNumber, tempEmail));
                    return false;
                });
    }

    public boolean isAdmin(String email) {
        return findByEmail(email)
                .map(appUser -> appUser.getRole() == UserRole.ADMIN)
                .orElse(false);
    }

    public void delete(AppUser appUser) {
        repository.delete(appUser);
    }

    public void deleteById(Long userId) {
        findById(userId).ifPresent(this::delete);
    }

    public void deauthorize(Long userId){
        findById(userId).ifPresent(user -> user.setAuthorized(false));
    }
    public void authorize(Long userId){
        findById(userId).ifPresent(user -> user.setAuthorized(true));
    }
    public void changeRole(Long userId, UserRole userRole) {
        findById(userId).ifPresent(user -> user.setRole(userRole));
    }

    public void ensureUserExists(OAuth2User principal) {
        String email = principal.getAttribute("email");
        findByEmail(email)
                .orElseGet(() -> save(createUser(principal)));
    }
}