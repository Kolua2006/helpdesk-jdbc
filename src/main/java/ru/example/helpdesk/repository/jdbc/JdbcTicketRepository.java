package ru.example.helpdesk.repository.jdbc;

import ru.example.helpdesk.config.DatabaseConfig;
import ru.example.helpdesk.model.Ticket;
import ru.example.helpdesk.model.TicketPriority;
import ru.example.helpdesk.model.TicketStatus;
import ru.example.helpdesk.model.TicketStatusHistory;
import ru.example.helpdesk.repository.TicketRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JdbcTicketRepository implements TicketRepository {

    @Override
    public Ticket save(Ticket ticket) {
        String sql = """
                INSERT INTO tickets(
                    title, description, status, priority,
                    customer_id, assignee_id, category_id
                )
                VALUES (?, ?, ?::ticket_status, ?::ticket_priority, ?, ?, ?)
                RETURNING id, created_at, updated_at
                """;
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, ticket.getTitle());
            ps.setString(2, ticket.getDescription());
            ps.setString(3, ticket.getStatus().name());
            ps.setString(4, ticket.getPriority().name());
            ps.setLong(5, ticket.getCustomerId());
            if (ticket.getAssigneeId() == null) {
                ps.setNull(6, Types.BIGINT);
            } else {
                ps.setLong(6, ticket.getAssigneeId());
            }
            if (ticket.getCategoryId() == null) {
                ps.setNull(7, Types.BIGINT);
            } else {
                ps.setLong(7, ticket.getCategoryId());
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ticket.setId(rs.getLong("id"));
                    ticket.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    ticket.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                }
            }
            return ticket;
        } catch (SQLException e) {
            throw new RuntimeException("Не удалось сохранить заявку", e);
        }
    }

    @Override
    public Optional<Ticket> findById(long id) {
        String sql = "SELECT * FROM tickets WHERE id = ?";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapTicket(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка поиска заявки", e);
        }
    }

    @Override
    public List<Ticket> findAll() {
        String sql = "SELECT * FROM tickets ORDER BY id";
        List<Ticket> result = new ArrayList<>();
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(mapTicket(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка чтения заявок", e);
        }
    }

    @Override
    public List<Ticket> findByStatus(TicketStatus status) {
        String sql = """
                SELECT * FROM tickets
                WHERE status = ?::ticket_status
                ORDER BY created_at DESC
                """;
        List<Ticket> result = new ArrayList<>();
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapTicket(rs));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка чтения заявок по статусу", e);
        }
    }

    @Override
    public List<Ticket> findByStatusAndPriority(TicketStatus status, TicketPriority priority) {
        String sql = """
                SELECT * FROM tickets
                WHERE status = ?::ticket_status
                  AND priority = ?::ticket_priority
                ORDER BY created_at DESC
                """;
        List<Ticket> result = new ArrayList<>();
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setString(2, priority.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapTicket(rs));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка поиска по статусу и приоритету", e);
        }
    }

    @Override
    public List<Ticket> searchByKeyword(String keyword) {
        String sql = """
                SELECT * FROM tickets
                WHERE title ILIKE ? OR description ILIKE ?
                ORDER BY created_at DESC
                """;
        List<Ticket> result = new ArrayList<>();
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            String pattern = "%" + keyword + "%";
            ps.setString(1, pattern);
            ps.setString(2, pattern);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapTicket(rs));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка универсального поиска", e);
        }
    }

    @Override
    public void update(Ticket ticket) {
        String sql = """
                UPDATE tickets
                SET title = ?, description = ?, status = ?::ticket_status,
                    priority = ?::ticket_priority, assignee_id = ?, category_id = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """;
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, ticket.getTitle());
            ps.setString(2, ticket.getDescription());
            ps.setString(3, ticket.getStatus().name());
            ps.setString(4, ticket.getPriority().name());
            if (ticket.getAssigneeId() == null) {
                ps.setNull(5, Types.BIGINT);
            } else {
                ps.setLong(5, ticket.getAssigneeId());
            }
            if (ticket.getCategoryId() == null) {
                ps.setNull(6, Types.BIGINT);
            } else {
                ps.setLong(6, ticket.getCategoryId());
            }
            ps.setLong(7, ticket.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка обновления заявки", e);
        }
    }

    @Override
    public boolean deleteById(long id) {
        String sql = "DELETE FROM tickets WHERE id = ?";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка удаления заявки", e);
        }
    }

    public List<Ticket> findAllWithNames() {
        String sql = """
                SELECT
                    t.*,
                    c.name AS category_name,
                    customer.name AS customer_name,
                    assignee.name AS assignee_name
                FROM tickets t
                JOIN users customer ON customer.id = t.customer_id
                LEFT JOIN users assignee ON assignee.id = t.assignee_id
                LEFT JOIN categories c ON c.id = t.category_id
                ORDER BY t.created_at DESC
                """;
        List<Ticket> result = new ArrayList<>();
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(mapTicket(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка чтения заявок с именами", e);
        }
    }

    public Map<TicketStatus, Long> countByStatus() {
        String sql = """
                SELECT status, COUNT(*) AS count
                FROM tickets
                GROUP BY status
                ORDER BY status
                """;
        Map<TicketStatus, Long> result = new HashMap<>();
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                TicketStatus status = TicketStatus.valueOf(rs.getString("status"));
                result.put(status, rs.getLong("count"));
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка отчёта по статусам", e);
        }
    }

    public Map<String, Long> countByCategory() {
        String sql = """
                SELECT c.name, COUNT(t.id) AS ticket_count
                FROM categories c
                LEFT JOIN tickets t ON t.category_id = c.id
                GROUP BY c.id, c.name
                ORDER BY ticket_count DESC
                """;
        Map<String, Long> result = new HashMap<>();
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(rs.getString("name"), rs.getLong("ticket_count"));
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка отчёта по категориям", e);
        }
    }

    public Map<String, Long> workloadByAgent() {
        String sql = """
                SELECT u.name,
                       COUNT(t.id) FILTER (
                           WHERE t.status IN ('NEW', 'IN_PROGRESS')
                       ) AS active_tickets
                FROM users u
                LEFT JOIN tickets t ON t.assignee_id = u.id
                WHERE u.role = 'SUPPORT_AGENT'
                GROUP BY u.id, u.name
                ORDER BY active_tickets DESC
                """;
        Map<String, Long> result = new HashMap<>();
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(rs.getString("name"), rs.getLong("active_tickets"));
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка отчёта по нагрузке", e);
        }
    }

    public List<TicketStatusHistory> findHistoryByTicketId(long ticketId) {
        String sql = """
                SELECT * FROM ticket_status_history
                WHERE ticket_id = ?
                ORDER BY changed_at
                """;
        List<TicketStatusHistory> result = new ArrayList<>();
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, ticketId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TicketStatusHistory history = new TicketStatusHistory();
                    history.setId(rs.getLong("id"));
                    history.setTicketId(rs.getLong("ticket_id"));
                    history.setOldStatus(TicketStatus.valueOf(rs.getString("old_status")));
                    history.setNewStatus(TicketStatus.valueOf(rs.getString("new_status")));
                    history.setChangedById(rs.getLong("changed_by_id"));
                    history.setChangedAt(rs.getTimestamp("changed_at").toLocalDateTime());
                    result.add(history);
                }
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка чтения истории статусов", e);
        }
    }

    private Ticket mapTicket(ResultSet rs) throws SQLException {
        Ticket ticket = new Ticket();
        ticket.setId(rs.getLong("id"));
        ticket.setTitle(rs.getString("title"));
        ticket.setDescription(rs.getString("description"));
        ticket.setStatus(TicketStatus.valueOf(rs.getString("status")));
        ticket.setPriority(TicketPriority.valueOf(rs.getString("priority")));
        ticket.setCustomerId(rs.getLong("customer_id"));
        long assigneeId = rs.getLong("assignee_id");
        ticket.setAssigneeId(rs.wasNull() ? null : assigneeId);
        long categoryId = rs.getLong("category_id");
        ticket.setCategoryId(rs.wasNull() ? null : categoryId);
        ticket.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        ticket.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        Timestamp closed = rs.getTimestamp("closed_at");
        ticket.setClosedAt(closed == null ? null : closed.toLocalDateTime());
        return ticket;
    }
}