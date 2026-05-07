package com.goldmonitor.controller;

import com.goldmonitor.dto.LoginRequest;
import com.goldmonitor.dto.LoginResponse;
import com.goldmonitor.dto.PasswordRequest;
import com.goldmonitor.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            LoginResponse response = userService.login(request.getUsername(), request.getPassword());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody PasswordRequest request, Principal principal) {
        try {
            userService.changePassword(principal.getName(), request.getOldPassword(), request.getNewPassword());
            return ResponseEntity.ok(Map.of("message", "密码修改成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
