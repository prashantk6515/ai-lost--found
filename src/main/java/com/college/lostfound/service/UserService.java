package com.college.lostfound.service;

import com.college.lostfound.dto.AuthRequest;
import com.college.lostfound.dto.AuthResponse;
import com.college.lostfound.dto.RegisterRequest;
import com.college.lostfound.model.User;
import com.college.lostfound.model.enums.Role;
import com.college.lostfound.repository.UserRepository;
import com.college.lostfound.security.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("Email is already registered: " + req.getEmail());
        }
        if (userRepository.existsByCollegeId(req.getCollegeId())) {
            throw new IllegalArgumentException("College ID is already registered: " + req.getCollegeId());
        }

        Role role = req.getRole() != null ? req.getRole() : Role.ROLE_STUDENT;

        User user = new User(
                req.getName(),
                req.getEmail().trim().toLowerCase(),
                req.getCollegeId().trim().toUpperCase(),
                passwordEncoder.encode(req.getPassword()),
                role,
                req.getPhone()
        );

        User saved = userRepository.save(user);

        String token = tokenProvider.generateTokenFromEmail(saved.getEmail());
        return new AuthResponse(token, saved.getId(), saved.getName(), saved.getEmail(),
                saved.getCollegeId(), saved.getRole(), saved.getPhone());
    }

    public AuthResponse login(AuthRequest req) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getEmail().trim().toLowerCase(), req.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = tokenProvider.generateToken(authentication);

        User user = userRepository.findByEmail(req.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return new AuthResponse(token, user.getId(), user.getName(), user.getEmail(),
                user.getCollegeId(), user.getRole(), user.getPhone());
    }

    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return userRepository.findByEmail(auth.getName()).orElse(null);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));
    }
}
