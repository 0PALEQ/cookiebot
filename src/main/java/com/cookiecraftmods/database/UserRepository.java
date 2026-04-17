package com.cookiecraftmods.database;

import java.sql.*;
import java.time.LocalDate;

public class UserRepository {
    private static final UserRepository INSTANCE = new UserRepository();
    private final DatabaseManager db = DatabaseManager.getInstance();

    private UserRepository() {}

    public static UserRepository getInstance() { return INSTANCE; }

    public void ensureUser(long userId, String username) {
        String upsert = "INSERT INTO users (id, username) VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE username = VALUES(username)";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(upsert)) {
            ps.setLong(1, userId);
            ps.setString(2, username);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public UserProfile getProfile(long userId) {
        String q = "SELECT id, username, reputation, cookies, xp, last_daily, last_rep_given FROM users WHERE id = ?";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(q)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    UserProfile p = new UserProfile();
                    p.id = rs.getLong("id");
                    p.username = rs.getString("username");
                    p.reputation = rs.getInt("reputation");
                    p.cookies = rs.getInt("cookies");
                    p.xp = rs.getInt("xp");
                    Timestamp lastDaily = rs.getTimestamp("last_daily");
                    p.lastDaily = lastDaily;
                    Date d = rs.getDate("last_rep_given");
                    p.lastRepGiven = d == null ? null : d.toLocalDate();
                    return p;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public int getCookies(long userId) {
        String q = "SELECT cookies FROM users WHERE id = ?";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(q)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public void addCookies(long userId, int amount) {
        String q = "UPDATE users SET cookies = cookies + ? WHERE id = ?";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(q)) {
            ps.setInt(1, amount);
            ps.setLong(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addXp(long userId, int amount) {
        String q = "UPDATE users SET xp = xp + ? WHERE id = ?";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(q)) {
            ps.setInt(1, amount);
            ps.setLong(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean tryGiveRep(long giverId, long targetId) {
        if (giverId == targetId) return false;
        LocalDate today = LocalDate.now();
        // Check if giver can give rep today
        String check = "SELECT last_rep_given FROM users WHERE id = ?";
        try (Connection c = db.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(check)) {
                ps.setLong(1, giverId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        Date d = rs.getDate(1);
                        if (d != null && !d.toLocalDate().isBefore(today)) {
                            return false; // already gave today
                        }
                    }
                }
            }

            // Perform update in a transaction
            c.setAutoCommit(false);
            try (PreparedStatement up1 = c.prepareStatement("UPDATE users SET reputation = reputation + 1 WHERE id = ?");
                 PreparedStatement up2 = c.prepareStatement("UPDATE users SET last_rep_given = ? WHERE id = ?")) {
                up1.setLong(1, targetId);
                up1.executeUpdate();

                up2.setDate(1, Date.valueOf(today));
                up2.setLong(2, giverId);
                up2.executeUpdate();

                c.commit();
                return true;
            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean canClaimDaily(long userId) {
        String q = "SELECT last_daily FROM users WHERE id = ?";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(q)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Timestamp ts = rs.getTimestamp(1);
                    if (ts == null) return true;
                    long diffMs = System.currentTimeMillis() - ts.getTime();
                    return diffMs >= 24L * 60 * 60 * 1000; // 24h
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }

    public boolean claimDaily(long userId, int amount) {
        if (!canClaimDaily(userId)) return false;
        String q1 = "UPDATE users SET cookies = cookies + ?, last_daily = ? WHERE id = ?";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(q1)) {
            ps.setInt(1, amount);
            ps.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            ps.setLong(3, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static class UserProfile {
        public long id;
        public String username;
        public int reputation;
        public int cookies;
        public int xp;
        public Timestamp lastDaily;
        public LocalDate lastRepGiven;
    }
}
