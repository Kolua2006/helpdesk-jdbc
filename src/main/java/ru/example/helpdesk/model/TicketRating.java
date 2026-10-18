package ru.example.helpdesk.model;

import java.time.LocalDateTime;

public class TicketRating {
    private Long id;
    private Long ticketId;
    private int rating;
    private Long ratedById;
    private LocalDateTime ratedAt;

    public TicketRating() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTicketId() { return ticketId; }
    public void setTicketId(Long ticketId) { this.ticketId = ticketId; }
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
    public Long getRatedById() { return ratedById; }
    public void setRatedById(Long ratedById) { this.ratedById = ratedById; }
    public LocalDateTime getRatedAt() { return ratedAt; }
    public void setRatedAt(LocalDateTime ratedAt) { this.ratedAt = ratedAt; }
}

