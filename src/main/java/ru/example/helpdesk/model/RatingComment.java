package ru.example.helpdesk.model;

import java.time.LocalDateTime;

public class RatingComment {
    private Long id;
    private Long ratingId;
    private String commentText;
    private LocalDateTime createdAt;

    public RatingComment() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getRatingId() { return ratingId; }
    public void setRatingId(Long ratingId) { this.ratingId = ratingId; }
    public String getCommentText() { return commentText; }
    public void setCommentText(String commentText) { this.commentText = commentText; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}




