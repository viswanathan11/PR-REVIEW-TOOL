package com.example.BackendApplication.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.BackendApplication.model.Review;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    Optional<Review> findByPullRequestId(Long pullRequesId);

    @Query("SELECT r FROM Review r JOIN FETCH r.pullRequest pr JOIN FETCH pr.repository repo WHERE repo.user.id = :userId ORDER BY r.createdAt DESC")
    List<Review> findByUserId(Long userId);
}
