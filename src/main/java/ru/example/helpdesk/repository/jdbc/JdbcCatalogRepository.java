package ru.example.helpdesk.repository.jdbc;

import ru.example.helpdesk.config.DatabaseConfig;
import ru.example.helpdesk.model.Category;
import ru.example.helpdesk.model.Department;
import ru.example.helpdesk.model.TicketPriority;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcCatalogRepository {

    public List<Category> findAllCategories() {
        String sql = "SELECT * FROM categories ORDER BY id";
        List<Category> result = new ArrayList<>();
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(mapCategory(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка чтения категорий", e);
        }
    }

    public Optional<Category> findCategoryById(long id) {
        String sql = "SELECT * FROM categories WHERE id = ?";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapCategory(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка поиска категории", e);
        }
    }

    public List<Department> findAllDepartments() {
        String sql = "SELECT * FROM departments ORDER BY id";
        List<Department> result = new ArrayList<>();
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(mapDepartment(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка чтения подразделений", e);
        }
    }

    public Optional<Department> findDepartmentById(long id) {
        String sql = "SELECT * FROM departments WHERE id = ?";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapDepartment(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка поиска подразделения", e);
        }
    }

    private Category mapCategory(ResultSet rs) throws SQLException {
        Category category = new Category();
        category.setId(rs.getLong("id"));
        category.setName(rs.getString("name"));
        category.setDescription(rs.getString("description"));
        category.setDefaultPriority(TicketPriority.valueOf(rs.getString("default_priority")));
        category.setActive(rs.getBoolean("active"));
        return category;
    }

    private Department mapDepartment(ResultSet rs) throws SQLException {
        Department department = new Department();
        department.setId(rs.getLong("id"));
        department.setName(rs.getString("name"));
        department.setDescription(rs.getString("description"));
        department.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return department;
    }
}

