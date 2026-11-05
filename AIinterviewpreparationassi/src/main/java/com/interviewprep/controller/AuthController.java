package com.interviewprep.controller;

import com.interviewprep.dto.AuthRequest;
import com.interviewprep.entity.User;
import com.interviewprep.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;

    public AuthController(AuthService auth) { this.auth = auth; }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody AuthRequest request) {
        User user = auth.register(request.email(), request.password());

        return ResponseEntity.ok(
            Map.of(
                "message", "Registration successful. Please log in.",
                "email", user.getEmail()
            )
        );
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest request, HttpServletRequest httpRequest) {
        User user = auth.login(request.email(), request.password());
        HttpSession old = httpRequest.getSession(false);
        if (old != null) old.invalidate();
        HttpSession session = httpRequest.getSession(true);
        session.setAttribute("userId", user.getId());
        session.setAttribute("email", user.getEmail());
        return ResponseEntity.ok(Map.of("message", "Login successful.", "email", user.getEmail()));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        return ResponseEntity.ok(Map.of("message", "Logged out."));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpSession session) {
        Object email = session.getAttribute("email");
        return email == null
                ? ResponseEntity.status(401).body(Map.of("error", "Not logged in."))
                : ResponseEntity.ok(Map.of("loggedIn", true, "email", email));
    }
}
