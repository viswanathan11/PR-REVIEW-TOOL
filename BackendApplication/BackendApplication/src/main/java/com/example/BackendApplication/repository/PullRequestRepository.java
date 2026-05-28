package com.example.BackendApplication.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.BackendApplication.model.PullRequest;

public interface PullRequestRepository extends JpaRepository<PullRequest, Long> {
    Optional<PullRequest> findByRepositoryIdAndPrNumber(Long repositoryId, Integer prNumber);

    List<PullRequest> findByRepositoryId(Long repositoryId);
}
