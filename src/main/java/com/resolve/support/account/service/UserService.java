package com.resolve.support.account.service;

import com.resolve.support.account.dto.CreateUserRequest;
import com.resolve.support.account.dto.UpdateUserRoleRequest;
import com.resolve.support.account.entity.User;
import com.resolve.support.account.entity.UserRole;
import com.resolve.support.account.repository.UserRepository;
import com.resolve.support.exception.DuplicateEmailException;
import com.resolve.support.exception.InvalidRequestException;
import com.resolve.support.exception.ResourceNotFoundException;
import com.resolve.support.security.CurrentUserService;
import com.resolve.support.security.dto.RegisterRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserService currentUserService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       CurrentUserService currentUserService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.currentUserService = currentUserService;
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public User create(CreateUserRequest request) {
        return saveUser(request.name(), request.email(), request.password(),
                request.role() == null ? UserRole.USER : request.role());
    }

    @Transactional
    public User register(RegisterRequest request) {
        return saveUser(request.name(), request.email(), request.password(), UserRole.USER);
    }

    public User getById(Long id) {
        User current = currentUserService.getCurrentUser();
        if (current.getRole() != UserRole.ADMIN && !current.getId().equals(id)) {
            throw new AccessDeniedException("You may only view your own account.");
        }
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Page<User> getAll(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public User updateRole(Long id, UpdateUserRoleRequest request) {
        User user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
        if (request.role() == UserRole.ADMIN) {
            throw new InvalidRequestException("This endpoint may assign only USER or AGENT roles.");
        }
        User current = currentUserService.getCurrentUser();
        if (current.getId().equals(id) && user.getRole() == UserRole.ADMIN
                && request.role() != UserRole.ADMIN
                && userRepository.countByRole(UserRole.ADMIN) <= 1) {
            throw new InvalidRequestException("The last administrator cannot demote their own account.");
        }
        user.setRole(request.role());
        return userRepository.save(user);
    }

    private User saveUser(String name, String email, String rawPassword, UserRole role) {
        if (rawPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new InvalidRequestException("Password must be no more than 72 UTF-8 bytes.");
        }
        User user = new User(name, email, passwordEncoder.encode(rawPassword), role);
        if (userRepository.existsByEmail(user.getEmail())) throw new DuplicateEmailException();
        try {
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateEmailException();
        }
    }
}
