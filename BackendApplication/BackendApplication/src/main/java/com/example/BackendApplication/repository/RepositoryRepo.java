package com.example.BackendApplication.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.BackendApplication.model.Repository;

public interface RepositoryRepo extends JpaRepository<Repository, Long> {
    List<Repository> findByUserId(Long userId);

    Optional<Repository> findByUserIdGitHubRepoId(Long userId, String githubRepoId);

    Optional<Repository> findByFullName(String fullName);
}
