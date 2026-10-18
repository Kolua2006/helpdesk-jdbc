package ru.example.helpdesk.repository;

import ru.example.helpdesk.model.TicketRating;
import java.util.List;
import java.util.Map;

public interface RatingRepository {
    void addRating(long ticketId, int rating, long clientId);
    Map<String, Double> getAverageRatingsByAgent();
    List<TicketRating> getLowRatedTickets();
}






