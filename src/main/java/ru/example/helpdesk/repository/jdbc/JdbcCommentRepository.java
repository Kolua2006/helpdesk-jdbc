package ru.example.helpdesk.repository.jdbc;

import ru.example.helpdesk.config.DatabaseConfig;
import ru.example.helpdesk.model.TicketComment;
import ru.example.helpdesk.repository.CommentRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class JdbcCommentRepository implements CommentRepository {

    @Override
    public TicketComment add(TicketComment comment) {
        String sql = """
                INSERT INTO ticket_comments(ticket_id, author_id, text, internal)
                VALUES (?, ?, ?, ?)
                RETURNING id, created_at
                """;
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, comment.getTicketId());
            ps.setLong(2, comment.getAuthorId());
            ps.setString(3, comment.getText());
            ps.setBoolean(4, comment.isInternal());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    comment.setId(rs.getLong("id"));
                    comment.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                }
            }
            return comment;
        } catch (SQLException e) {
            throw new RuntimeException("Не удалось добавить комментарий", e);
        }
    }

    @Override
    public List<TicketComment> findByTicketId(long ticketId) {
        String sql = """
                SELECT c.id, c.text, c.internal, c.created_at,
                       u.name AS author_name,
                       u.role AS author_role
                FROM ticket_comments c
                JOIN users u ON u.id = c.author_id
                WHERE c.ticket_id = ?
                ORDER BY c.created_at
                """;
        List<TicketComment> result = new ArrayList<>();
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, ticketId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TicketComment comment = new TicketComment();
                    comment.setId(rs.getLong("id"));
                    comment.setText(rs.getString("text"));
                    comment.setInternal(rs.getBoolean("internal"));
                    comment.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    result.add(comment);
                }
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка чтения комментариев", e);
        }
    }
}





