package com.testmgmt.service;

import com.testmgmt.entity.User;
import com.testmgmt.exception.GlobalExceptionHandler.*;
import com.testmgmt.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepo;

    @Transactional(readOnly = true)
    public User getProfile(String email) {
        return userRepo.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    @Transactional(readOnly = true)
    public User findById(UUID id) {
        return userRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<User> findAllActive() {
        return userRepo.findByActiveTrue();
    }

    @Transactional
    public User updateProfile(String email, String fullName, String team) {
        User user = getProfile(email);
        if (fullName != null) user.setFullName(fullName);
        if (team     != null) user.setTeam(team);
        return userRepo.save(user);
    }

    @Transactional
    public User updateRole(UUID userId, User.UserRole newRole) {
        User user = findById(userId);
        user.setRole(newRole);
        return userRepo.save(user);
    }

    @Transactional
    public void deactivate(UUID userId) {
        User user = findById(userId);
        user.setActive(false);
        userRepo.save(user);
    }
}
