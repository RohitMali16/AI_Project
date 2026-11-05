package com.interviewprep.service;

import com.interviewprep.entity.User;
import com.interviewprep.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;

    public AuthService(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    public User register(String email, String password) {
        String normalizedEmail = email.trim().toLowerCase();
        if (users.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }
        return users.save(new User(normalizedEmail, encoder.encode(password)));
    }

    public User login(String email, String password) {
        User user = users.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password."));
        if (!encoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password.");
        }
        return user;
    }
}
