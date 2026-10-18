package ru.example.helpdesk.repository;

import ru.example.helpdesk.model.TicketComment;
import java.util.List;

public interface CommentRepository {
    TicketComment add(TicketComment comment);
    List<TicketComment> findByTicketId(long ticketId);
}











