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

/**
 * This service manages user authentication and authorization in the platform.
 * It handles both OAuth2 (Google) users and WhatsApp-based users, providing
 * a dual authentication system that allows users to connect through either method.
 * The service maintains consistency between these two authentication paths and
 * manages user permissions through roles and authorization flags.
 */
@Service
public class UserService {
    // Hardcoded admin credentials ensure there's always at least one admin user
    // who can manage the platform, even if the database is reset
    public static final String ADMIN_EMAIL = "your@gmail.com";
    private static final String ADMIN_WHATSAPP = "9725-yourphone";
    private static final String ADMIN_PICTURE = "url-to-picture";
    private static final String ADMIN_NAME = "your-name";
    private static final boolean ADMIN_AUTHORIZED = true;
    private final UserRepository repository;

        @Autowired
    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    /**
     * Creates or updates a user based on their email address.
     * This method supports the WhatsApp-to-Google account linking process by allowing
     * temporary WhatsApp users to be upgraded to full users with Google credentials.
     * The role parameter determines the user's permissions in the system.
     */
    public void createUserFromEmail(String email, UserRole role, String phone) {
        repository.findByEmail(email)
                .ifPresentOrElse(
                        existingUser -> {
                            existingUser.setAuthorized(role != UserRole.UNAUTHORIZED);
                            existingUser.setRole(role);
                            existingUser.setWhatsappNumber(phone);
                            repository.save(existingUser);
                        },
                        () -> repository.save(toAuthorizedUser(phone, email, role))
                );
    }

    /**
     * Creates a new unauthorized user from Google OAuth2 data.
     * New users start unauthorized to prevent automatic access to the platform.
     * They need explicit authorization (usually by linking a WhatsApp number)
     * before gaining access to protected features.
     */
    public AppUser createUser(OAuth2User oauth2User) {
        return anUser()
                .name(oauth2User.getAttribute("name"))
                .pictureUrl(oauth2User.getAttribute("picture"))
                .email(oauth2User.getAttribute("email"))
                .authorized(false)
                .role(UserRole.UNAUTHORIZED)
                .build();
    }

    /**
     * Creates a fully authorized user with specified WhatsApp and email.
     * Used when we're confident about the user's identity (e.g., after
     * successful WhatsApp verification or admin approval).
     */
    private static AppUser toAuthorizedUser(String whatsappNumber, String email, UserRole role) {
        return AppUser.UserBuilder.anUser()
                .whatsappNumber(whatsappNumber)
                .email(email)
                .role(role)
                .authorized(role != UserRole.UNAUTHORIZED)
                .build();
    }

    /**
     * Creates a temporary unauthorized user from WhatsApp data.
     * Uses a temporary email to maintain unique user identification until
     * the user links their Google account. The temporary email format
     * helps identify users who haven't completed the full registration process.
     */
    private static AppUser toUnauthorizedUser(String whatsappNumber, String tempEmail) {
        return AppUser.UserBuilder.anUser()
                .whatsappNumber(whatsappNumber)
                .email(tempEmail)
                .role(UserRole.UNAUTHORIZED)
                .authorized(false)
                .build();
    }

    // Standard repository access methods
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

    /**
     * Saves a user while enforcing null checks for data integrity.
     * This is the primary method for persisting user changes and should be used
     * instead of directly accessing the repository to ensure consistent validation.
     */
    public AppUser save(AppUser appUser) {
        if (appUser == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        return repository.save(appUser);
    }

    /**
     * Handles the initial WhatsApp user registration process.
     * Returns true if the user is already authorized, false if they're new or unauthorized.
     * Creates temporary users with special email addresses to track their registration state
     * until they complete the Google account linking process.
     */
    public boolean processNewUser(String whatsappNumber) {
        return repository.findByWhatsappNumber(whatsappNumber)
                .map(user -> {
                    // Check if this is a fully registered user or still using a temporary email
                    if (!user.getEmail().endsWith("@temp.platform.com")) {
                        return user.isAuthorized();
                    }
                    return false;
                })
                .orElseGet(() -> {
                    // Create new temporary user with WhatsApp-based email
                    String tempEmail = "whatsapp." + whatsappNumber.replaceAll("[^0-9]", "")
                            + "@temp.platform.com";
                    repository.save(toUnauthorizedUser(whatsappNumber, tempEmail));
                    return false;
                });
    }

    /**
     * Checks if a user has admin privileges.
     * This is used for access control to administrative features and is separate
     * from the authorized flag to allow for fine-grained permission control.
     */
    public boolean isAdmin(String email) {
        return findByEmail(email)
                .map(appUser -> appUser.getRole() == UserRole.ADMIN)
                .orElse(false);
    }

    // User management operations
    public void delete(AppUser appUser) {
        repository.delete(appUser);
    }

    public void deleteById(Long userId) {
        findById(userId).ifPresent(this::delete);
    }

    public void deauthorize(Long userId){
        findById(userId).ifPresent(user -> {user.setAuthorized(false);
            repository.save(user);
        });
    }

    public void authorize(Long userId){
        findById(userId).ifPresent(user -> {
            user.setAuthorized(true);
            repository.save(user);
        });
    }

    public void changeRole(Long userId, UserRole userRole) {
        findById(userId).ifPresent(user -> {
            user.setRole(userRole);
            repository.save(user);
        });

    }

    /**
     * Ensures system integrity by creating users if they don't exist.
     * Special handling for admin user ensures there's always an admin account,
     * while regular users start unauthorized and need explicit approval.
     * This is typically called after successful OAuth2 authentication.
     *
     */
    public AppUser ensureUserExists(OAuth2User principal) {
        String email = principal.getAttribute("email");
        AppUser userResult;
        if (ADMIN_EMAIL.equals(email)) {
            userResult = repository.findByEmail(ADMIN_EMAIL).orElseGet(() -> {
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
        else {
            userResult = repository.findByEmail(email).orElseGet(() -> {
                AppUser user = anUser()
                        .name(principal.getAttribute("name"))
                        .pictureUrl(principal.getAttribute("picture"))
                        .email(email)
                        .role(UserRole.UNAUTHORIZED)
                        .authorized(false)
                        .build();

                return repository.save(user);
            });
        }
        return userResult;
    }

    /**
     * Completes the user authorization process by linking WhatsApp and email.
     * This is called after verifying both the Google account and WhatsApp number
     * belong to the same user, upgrading them to a fully authorized user.
     */
    public void authorizeUserFromPhone(String email, String whatsappNumber) {
        repository.findByEmail(email)
                .ifPresent(user -> {
                    user.setWhatsappNumber(whatsappNumber);
                    user.setAuthorized(true);
                    user.setRole(UserRole.USER);
                    repository.save(user);
                });
    }
}
