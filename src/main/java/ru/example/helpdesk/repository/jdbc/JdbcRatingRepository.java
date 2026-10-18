package ru.example.helpdesk.repository.jdbc;

import ru.example.helpdesk.config.DatabaseConfig;
import ru.example.helpdesk.model.TicketRating;
import ru.example.helpdesk.model.TicketStatus;
import ru.example.helpdesk.repository.RatingRepository;

import java.sql.*;
import java.util.*;

public class JdbcRatingRepository implements RatingRepository {

    @Override
    public void addRating(long ticketId, int rating, long clientId) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Рейтинг должен быть от 1 до 5");
        }

        String checkSql = "SELECT status FROM tickets WHERE id = ? FOR UPDATE";
        String insertSql = "INSERT INTO ticket_ratings(ticket_id, rating, rated_by) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                TicketStatus status;
                try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
                    ps.setLong(1, ticketId);
                    ResultSet rs = ps.executeQuery();
                    if (!rs.next()) throw new RuntimeException("Заявка не найдена");
                    status = TicketStatus.valueOf(rs.getString("status"));
                }

                if (status != TicketStatus.CLOSED) {
                    throw new IllegalStateException(
                            "Оценить можно только закрытую заявку (текущий статус: " + status + ")"
                    );
                }

                try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                    ps.setLong(1, ticketId);
                    ps.setInt(2, rating);
                    ps.setLong(3, clientId);
                    ps.executeUpdate();
                }

                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка сохранения оценки", e);
        }
    }

    @Override
    public Map<String, Double> getAverageRatingsByAgent() {
        String sql = """
                SELECT u.name, AVG(tr.rating) as avg_rating
                FROM users u
                JOIN tickets t ON t.assignee_id = u.id
                JOIN ticket_ratings tr ON tr.ticket_id = t.id
                WHERE u.role = 'SUPPORT_AGENT'
                GROUP BY u.id, u.name
                ORDER BY avg_rating DESC
                """;
        Map<String, Double> result = new LinkedHashMap<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(rs.getString("name"), rs.getDouble("avg_rating"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка получения рейтингов", e);
        }
        return result;
    }

    @Override
    public List<TicketRating> getLowRatedTickets() {
        String sql = """
                SELECT tr.* FROM ticket_ratings tr
                JOIN tickets t ON t.id = tr.ticket_id
                WHERE tr.rating <= 3
                ORDER BY tr.rating ASC
                """;
        List<TicketRating> result = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                TicketRating r = new TicketRating();
                r.setId(rs.getLong("id"));
                r.setTicketId(rs.getLong("ticket_id"));
                r.setRating(rs.getInt("rating"));
                r.setRatedById(rs.getLong("rated_by"));
                r.setRatedAt(rs.getTimestamp("rated_at").toLocalDateTime());
                result.add(r);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка поиска низких оценок", e);
        }
        return result;
    }
}