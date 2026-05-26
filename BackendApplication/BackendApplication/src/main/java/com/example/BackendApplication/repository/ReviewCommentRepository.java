package com.example.BackendApplication.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.BackendApplication.model.ReviewComment;

public interface ReviewCommentRepository extends JpaRepository<ReviewComment, Long> {
    List<ReviewComment> finbyReviewId(Long reviewId);
}
