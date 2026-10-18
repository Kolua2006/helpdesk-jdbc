package ru.example.helpdesk;

import ru.example.helpdesk.config.DatabaseConfig;
import ru.example.helpdesk.model.Category;
import ru.example.helpdesk.model.Ticket;
import ru.example.helpdesk.model.TicketComment;
import ru.example.helpdesk.model.TicketPriority;
import ru.example.helpdesk.model.TicketStatus;
import ru.example.helpdesk.model.TicketStatusHistory;
import ru.example.helpdesk.model.User;
import ru.example.helpdesk.repository.jdbc.JdbcCatalogRepository;
import ru.example.helpdesk.repository.jdbc.JdbcCommentRepository;
import ru.example.helpdesk.repository.jdbc.JdbcTicketRepository;
import ru.example.helpdesk.repository.jdbc.JdbcUserRepository;
import ru.example.helpdesk.service.TicketService;
import java.util.Map;
import ru.example.helpdesk.repository.jdbc.JdbcRatingRepository;
import ru.example.helpdesk.model.TicketRating;

import java.sql.Connection;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        System.out.println("ДЕМОНСТРАЦИЯ РАБОТЫ HELP DESK\n");

        JdbcTicketRepository ticketRepo = new JdbcTicketRepository();
        JdbcUserRepository userRepo = new JdbcUserRepository();
        JdbcCatalogRepository catalogRepo = new JdbcCatalogRepository();
        JdbcCommentRepository commentRepo = new JdbcCommentRepository();
        TicketService ticketService = new TicketService(ticketRepo, userRepo);

        try {

            System.out.println("1. Подключение к PostgreSQL...");
            try (Connection conn = DatabaseConfig.getConnection()) {
                System.out.println("   Подключение успешно установлено!\n");
            }


            System.out.println("2. Поиск клиента по email 'client@example.com'...");
            User client = userRepo.findByEmail("client@example.com")
                    .orElseThrow(() -> new RuntimeException("Клиент не найден! Добавьте пользователя в БД."));
            System.out.println("   Клиент: " + client.getName() + " (id=" + client.getId() + ")\n");


            System.out.println("3. Список категорий:");
            List<Category> categories = catalogRepo.findAllCategories();
            for (Category c : categories) {
                System.out.println("   - " + c.getName() + " (id=" + c.getId() + ")");
            }
            long categoryId = categories.isEmpty() ? 1L : categories.get(0).getId();
            System.out.println();


            System.out.println("4. Создание новой заявки...");
            Ticket ticket = ticketService.createTicket(
                    "Не работает принтер",
                    "Принтер в кабинете 305 не печатает, мигает красная лампочка",
                    TicketPriority.MEDIUM,
                    client.getId(),
                    categoryId
            );
            System.out.println("   Заявка создана!\n");


            System.out.println("5. ID заявки из PostgreSQL: " + ticket.getId());
            System.out.println("   Время создания: " + ticket.getCreatedAt() + "\n");


            System.out.println("6. Поиск заявки через findById(" + ticket.getId() + "):");
            Ticket found = ticketRepo.findById(ticket.getId()).orElseThrow();
            System.out.println("   Тема: " + found.getTitle());
            System.out.println("   Статус: " + found.getStatus());
            System.out.println("   Приоритет: " + found.getPriority() + "\n");

            System.out.println("\nЗадание №2 ср Поиск заявок: Статус=NEW, Приоритет=MEDIUM");
            List<Ticket> filtered = ticketRepo.findByStatusAndPriority(TicketStatus.NEW, TicketPriority.MEDIUM);
            System.out.println("   Найдено заявок: " + filtered.size());
            for (Ticket t : filtered) {
                System.out.println("   - #" + t.getId() + ": " + t.getTitle() + " (" + t.getStatus() + ", " + t.getPriority() + ")");
            }

            System.out.println("\nЗадание №12 ср Универсальный поиск по слову 'принтер'");
            List<Ticket> searchResult = ticketRepo.searchByKeyword("принтер");
            System.out.println("   Найдено заявок: " + searchResult.size());
            for (Ticket t : searchResult) {
                System.out.println("   - #" + t.getId() + ": " + t.getTitle());
            }


            System.out.println("7. Назначение сотрудника поддержки...");
            List<User> allUsers = userRepo.findAll();
            User agent = allUsers.stream()
                    .filter(u -> u.getRole().toString().equals("SUPPORT_AGENT"))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Нет сотрудника поддержки!"));
            ticketService.assignTicket(ticket.getId(), agent.getId());
            System.out.println("   Исполнитель: " + agent.getName() + " (id=" + agent.getId() + ")\n");


            System.out.println("\nЗадание №7 ср Проверка ролей при назначении:");
            try {
                ticketService.assignTicket(ticket.getId(), client.getId());
                System.out.println("   ОШИБКА: Назначение прошло, но не должно было!");
            } catch (IllegalStateException e) {
                System.out.println("   Защита сработала: " + e.getMessage());
            }

            System.out.println("8. Добавление клиентского комментария...");
            TicketComment clientComment = new TicketComment();
            clientComment.setTicketId(ticket.getId());
            clientComment.setAuthorId(client.getId());
            clientComment.setText("Принтер всё ещё не работает, очень срочно!");
            clientComment.setInternal(false);
            commentRepo.add(clientComment);
            System.out.println("   Комментарий добавлен: \"" + clientComment.getText() + "\"\n");


            System.out.println("9. Смена статуса NEW -> IN_PROGRESS -> RESOLVED...");
            ticketService.changeStatus(ticket.getId(), TicketStatus.IN_PROGRESS, agent.getId());
            System.out.println("   Статус: IN_PROGRESS");
            ticketService.changeStatus(ticket.getId(), TicketStatus.RESOLVED, agent.getId());
            System.out.println("   Статус: RESOLVED\n");


            System.out.println("10. Добавление внутреннего комментария сотрудника...");
            TicketComment agentComment = new TicketComment();
            agentComment.setTicketId(ticket.getId());
            agentComment.setAuthorId(agent.getId());
            agentComment.setText("Заменил картридж, проблема решена");
            agentComment.setInternal(true);
            commentRepo.add(agentComment);
            System.out.println("    Комментарий: \"" + agentComment.getText() + "\" (внутренний)\n");


            System.out.println("11. Смена статуса RESOLVED -> CLOSED...");
            ticketService.changeStatus(ticket.getId(), TicketStatus.CLOSED, agent.getId());
            System.out.println("    Статус: CLOSED\n");


            System.out.println("12. История статусов заявки #" + ticket.getId() + ":");
            List<TicketStatusHistory> history = ticketRepo.findHistoryByTicketId(ticket.getId());
            for (TicketStatusHistory h : history) {
                System.out.println("    " + h.getOldStatus() + " -> " + h.getNewStatus()
                        + " (изменил пользователь id=" + h.getChangedById()
                        + ", время: " + h.getChangedAt() + ")");
            }
            System.out.println();

            System.out.println("\nЗадание №3 ср Дата последнего изменения заявки:");
            System.out.println("   updated_at: " + found.getUpdatedAt());


            System.out.println("13. JOIN-запрос: заявка с именами клиента, сотрудника и категории:");
            List<Ticket> ticketsWithNames = ticketRepo.findAllWithNames();
            for (Ticket t : ticketsWithNames) {
                if (t.getId().equals(ticket.getId())) {
                    System.out.println("    ID: " + t.getId());
                    System.out.println("    Тема: " + t.getTitle());
                    System.out.println("    Статус: " + t.getStatus());
                    System.out.println("    Клиент: (id=" + t.getCustomerId() + ")");
                    System.out.println("    Исполнитель: (id=" + t.getAssigneeId() + ")");
                    System.out.println("    Категория: (id=" + t.getCategoryId() + ")");
                    System.out.println("    Создана: " + t.getCreatedAt());
                }
            }
            System.out.println();


            System.out.println("14. Проверка сохранения в БД (повторный findById):");
            Ticket saved = ticketRepo.findById(ticket.getId()).orElseThrow();
            System.out.println("    Заявка #" + saved.getId() + " существует в БД!");
            System.out.println("    Тема: " + saved.getTitle());
            System.out.println("    Статус: " + saved.getStatus());
            System.out.println("    Исполнитель: " + saved.getAssigneeId());
            JdbcRatingRepository ratingRepo = new JdbcRatingRepository();

            System.out.println("\n15. Оценка качества работы сотрудника");
            try {
                ratingRepo.addRating(ticket.getId(), 5, client.getId());
                System.out.println("  Оценка 5 за заявку #" + ticket.getId() + " успешно сохранена");
            } catch (Exception e) {
                System.err.println("    Ошибка: " + e.getMessage());
            }

            System.out.println("\n16. Проверка бизнес-правила (оценка незакрытой заявки)");
            Ticket newTicket = ticketService.createTicket(
                    "Тестовая заявка для проверки",
                    "Эта заявка не должна быть оценена",
                    TicketPriority.LOW,
                    client.getId(),
                    categoryId
            );
            try {
                ratingRepo.addRating(newTicket.getId(), 4, client.getId());
                System.out.println("    ОШИБКА: Оценка сохранилась, но не должна была!");
            } catch (IllegalStateException e) {
                System.out.println("  Бизнес-правило сработало: " + e.getMessage());
            }

            System.out.println("\n17. Средний рейтинг сотрудников поддержки:");
            Map<String, Double> stats = ratingRepo.getAverageRatingsByAgent();
            if (stats.isEmpty()) {
                System.out.println("   Пока нет оценок.");
            } else {
                for (Map.Entry<String, Double> entry : stats.entrySet()) {
                    System.out.printf("   - %s: %.1f/5.0%n", entry.getKey(), entry.getValue());
                }
            }

            System.out.println("\n18. Заявки с низкой оценкой (≤3):");
            List<TicketRating> lowRated = ratingRepo.getLowRatedTickets();
            if (lowRated.isEmpty()) {
                System.out.println("   Нет заявок с низкой оценкой");
            } else {
                for (TicketRating r : lowRated) {
                    System.out.printf("   - Заявка #%d: оценка %d/5%n", r.getTicketId(), r.getRating());
                }
            }


        } catch (Exception e) {
            System.err.println("ОШИБКА: " + e.getMessage());
            e.printStackTrace();
        }
    }
}







