package com.example.BackendApplication.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.BackendApplication.model.PullRequest;

public interface PullRequestReporitory extends JpaRepository<PullRequest, Long> {
    Optional<PullRequest> findByRepositoryAndPrNumber(Long repositoryId, Integer prNumber);
}
