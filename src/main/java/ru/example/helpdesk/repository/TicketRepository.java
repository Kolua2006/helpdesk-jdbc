package ru.example.helpdesk.repository;

import ru.example.helpdesk.model.Ticket;
import ru.example.helpdesk.model.TicketPriority;
import ru.example.helpdesk.model.TicketStatus;

import java.util.List;
import java.util.Optional;

public interface TicketRepository {
    Ticket save(Ticket ticket);

    Optional<Ticket> findById(long id);

    List<Ticket> findAll();

    List<Ticket> findByStatus(TicketStatus status);

    void update(Ticket ticket);

    boolean deleteById(long id);
    List<Ticket> findByStatusAndPriority(TicketStatus status, TicketPriority priority);
    List<Ticket> searchByKeyword(String keyword);
}




