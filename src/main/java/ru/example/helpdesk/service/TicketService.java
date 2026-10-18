package ru.example.helpdesk.service;

import ru.example.helpdesk.config.DatabaseConfig;
import ru.example.helpdesk.model.Ticket;
import ru.example.helpdesk.model.TicketPriority;
import ru.example.helpdesk.model.TicketStatus;
import ru.example.helpdesk.model.User;
import ru.example.helpdesk.repository.TicketRepository;
import ru.example.helpdesk.repository.UserRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TicketService {
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    public TicketService(TicketRepository ticketRepository, UserRepository userRepository) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
    }

    public Ticket createTicket(
            String title,
            String description,
            TicketPriority priority,
            long customerId,
            Long categoryId
    ) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Пустая тема заявки");
        }

        Ticket ticket = new Ticket();
        ticket.setTitle(title.trim());
        ticket.setDescription(description);
        ticket.setStatus(TicketStatus.NEW);
        ticket.setPriority(priority);
        ticket.setCustomerId(customerId);
        ticket.setCategoryId(categoryId);

        return ticketRepository.save(ticket);
    }

    public void changeStatus(
            long ticketId,
            TicketStatus newStatus,
            long changedByUserId
    ) {
        String selectSql = "SELECT status FROM tickets WHERE id = ? FOR UPDATE";

        String updateSql = """
                UPDATE tickets
                SET status = ?::ticket_status,
                    updated_at = CURRENT_TIMESTAMP,
                    closed_at = CASE
                        WHEN ?::ticket_status = 'CLOSED' THEN CURRENT_TIMESTAMP
                        ELSE closed_at
                    END
                WHERE id = ?
                """;

        String historySql = """
                INSERT INTO ticket_status_history(
                    ticket_id, old_status, new_status, changed_by_id
                )
                VALUES (?, ?::ticket_status, ?::ticket_status, ?)
                """;

        try (Connection connection = DatabaseConfig.getConnection()) {
            connection.setAutoCommit(false);
            try {
                TicketStatus oldStatus;

                try (PreparedStatement ps = connection.prepareStatement(selectSql)) {
                    ps.setLong(1, ticketId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalArgumentException("Заявка не найдена");
                        }
                        oldStatus = TicketStatus.valueOf(rs.getString("status"));
                    }
                }

                validateTransition(oldStatus, newStatus);

                try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
                    ps.setString(1, newStatus.name());
                    ps.setString(2, newStatus.name());
                    ps.setLong(3, ticketId);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = connection.prepareStatement(historySql)) {
                    ps.setLong(1, ticketId);
                    ps.setString(2, oldStatus.name());
                    ps.setString(3, newStatus.name());
                    ps.setLong(4, changedByUserId);
                    ps.executeUpdate();
                }

                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка изменения статуса", e);
        }
    }

    public void assignTicket(long ticketId, long agentId) {
        User agent = userRepository.findById(agentId)
                .orElseThrow(() -> new RuntimeException("Сотрудник не найден"));

        if (!agent.getRole().toString().equals("SUPPORT_AGENT")) {
            throw new IllegalStateException(
                    "Назначить заявку можно только сотруднику поддержки (SUPPORT_AGENT). Текущая роль: " + agent.getRole()
            );
        }

        String sql = "UPDATE tickets SET assignee_id = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, agentId);
            ps.setLong(2, ticketId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка назначения сотрудника", e);
        }
    }

    private void validateTransition(TicketStatus oldStatus, TicketStatus newStatus) {
        if (oldStatus == newStatus) {
            throw new IllegalArgumentException("Статус не изменился");
        }
        if (oldStatus == TicketStatus.CLOSED || oldStatus == TicketStatus.CANCELLED) {
            throw new IllegalArgumentException(
                    "Нельзя изменить статус закрытой или отменённой заявки");
        }
    }
}