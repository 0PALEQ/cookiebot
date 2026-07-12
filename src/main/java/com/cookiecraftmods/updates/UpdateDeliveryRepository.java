package com.cookiecraftmods.updates;

import com.cookiecraftmods.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UpdateDeliveryRepository {
    private final DatabaseManager database = DatabaseManager.getInstance();

    public DeliveryProgress get(long announcementId) {
        String sql = "SELECT next_part, message_ids, completed FROM update_deliveries WHERE announcement_id = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, announcementId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return new DeliveryProgress(0, List.of(), false);
                return new DeliveryProgress(
                        result.getInt("next_part"),
                        parseIds(result.getString("message_ids")),
                        result.getBoolean("completed")
                );
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not read update delivery state", exception);
        }
    }

    public void recordPart(long announcementId, int nextPart, List<String> messageIds) {
        String sql = "INSERT INTO update_deliveries (announcement_id, next_part, message_ids, completed) " +
                "VALUES (?, ?, ?, FALSE) ON DUPLICATE KEY UPDATE next_part = VALUES(next_part), " +
                "message_ids = VALUES(message_ids), updated_at = CURRENT_TIMESTAMP";
        execute(sql, announcementId, nextPart, String.join(",", messageIds));
    }

    public void markComplete(long announcementId, int partCount, List<String> messageIds) {
        String sql = "INSERT INTO update_deliveries (announcement_id, next_part, message_ids, completed) " +
                "VALUES (?, ?, ?, TRUE) ON DUPLICATE KEY UPDATE next_part = VALUES(next_part), " +
                "message_ids = VALUES(message_ids), completed = TRUE, updated_at = CURRENT_TIMESTAMP";
        execute(sql, announcementId, partCount, String.join(",", messageIds));
    }

    private void execute(String sql, long announcementId, int nextPart, String messageIds) {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, announcementId);
            statement.setInt(2, nextPart);
            statement.setString(3, messageIds);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not save update delivery state", exception);
        }
    }

    private List<String> parseIds(String value) {
        if (value == null || value.isBlank()) return new ArrayList<>();
        List<String> ids = new ArrayList<>();
        for (String id : value.split(",")) {
            if (!id.isBlank()) ids.add(id.trim());
        }
        return ids;
    }

    public record DeliveryProgress(int nextPart, List<String> messageIds, boolean completed) {}
}
