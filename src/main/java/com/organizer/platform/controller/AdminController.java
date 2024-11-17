package com.organizer.platform.controller;

import com.organizer.platform.model.User.AppUser;
import com.organizer.platform.service.User.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserService userService;

    @Autowired
    public AdminController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/restore")
    public ResponseEntity<AppUser> restoreAdmin() {
        AppUser restoredAdmin = userService.restoreAdmin();
        return ResponseEntity.ok(restoredAdmin);
    }
}
