// User.java
package com.organizer.platform.model.User;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.organizer.platform.util.Dates;
import org.joda.time.LocalDateTime;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "app_user")
public class AppUser implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotNull
    @Column(nullable = false, updatable = false)
    private Date createdAt = Dates.nowUTC();

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonProperty("createdAt")
    public LocalDateTime calcCreatedAt() {
        return Dates.atLocalTime(createdAt);
    }

    @NotEmpty
    @Column(nullable = false, unique = true)
    private String email;

    @Column(length = 15)
    private String whatsappNumber;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private UserRole role = UserRole.UNAUTHORIZED;

    @Column(nullable = false)
    private boolean authorized = false;

    @Column(length = 100)
    private String name;

    // For storing URL/path
    @Column(length = 255)
    private String pictureUrl;

    public boolean isAdmin(){
        return role == UserRole.ADMIN;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPictureUrl() {
        return pictureUrl;
    }

    public void setPictureUrl(String pictureUrl) {
        this.pictureUrl = pictureUrl;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getWhatsappNumber() {
        return whatsappNumber;
    }

    public void setWhatsappNumber(String whatsappNumber) {
        this.whatsappNumber = whatsappNumber;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public boolean isAuthorized() {
        return authorized;
    }
    public boolean isUnauthorized() {
        return !authorized;
    }
    public boolean isNormalUser() {
        return role == UserRole.USER;
    }

    public boolean isUNAUTHORIZEDUser() {
        return role == UserRole.UNAUTHORIZED;
    }

    public void setAuthorized(boolean authorized) {
        this.authorized = authorized;
    }

    public static final class UserBuilder {
        private Long id;
        private Date createdAt = Dates.nowUTC();
        private @NotEmpty String email;
        private String whatsappNumber;
        private UserRole role;
        private boolean authorized;

        private String name;
        private String pictureUrl;

        private UserBuilder() {
        }

        public static UserBuilder anUser() {
            return new UserBuilder();
        }

        public UserBuilder id(Long id) {
            this.id = id;
            return this;
        }
        public UserBuilder name(String name) {
            this.name = name;
            return this;
        }
        public UserBuilder pictureUrl(String pictureUrl) {
            this.pictureUrl = pictureUrl;
            return this;
        }

        public UserBuilder createdAt(Date createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public UserBuilder email(String email) {
            this.email = email;
            return this;
        }

        public UserBuilder whatsappNumber(String whatsappNumber) {
            this.whatsappNumber = whatsappNumber;
            return this;
        }

        public UserBuilder role(UserRole role) {
            this.role = role;
            return this;
        }

        public UserBuilder authorized(boolean authorized) {
            this.authorized = authorized;
            return this;
        }

        public AppUser build() {
            AppUser appUser = new AppUser();
            appUser.setId(id);
            appUser.setCreatedAt(createdAt);
            appUser.setEmail(email);
            appUser.setWhatsappNumber(whatsappNumber);
            appUser.setRole(role);
            appUser.setAuthorized(authorized);
            appUser.setName(name);
            appUser.setPictureUrl(pictureUrl);
            return appUser;
        }
    }
}