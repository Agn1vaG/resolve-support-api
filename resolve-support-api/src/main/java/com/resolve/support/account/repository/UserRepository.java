package com.resolve.support.account.repository;

import com.resolve.support.account.entity.User;
import com.resolve.support.account.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByRole(UserRole role);

    Page<User> findAll(Pageable pageable);
}
