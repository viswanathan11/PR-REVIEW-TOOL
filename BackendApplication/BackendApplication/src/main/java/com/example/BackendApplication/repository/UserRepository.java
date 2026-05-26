package com.example.BackendApplication.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.BackendApplication.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByGithubId(String githubId);
}
