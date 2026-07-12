package com.cookiecraftmods.database;

import com.cookiecraftmods.config.Config;
import io.github.cdimascio.dotenv.Dotenv;

import java.sql.*;

public class DatabaseManager {
    private static final DatabaseManager INSTANCE = new DatabaseManager();

    private final String url;
    private final String user;
    private final String pass;


    static {
        try {
            Class.forName("org.mariadb.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MariaDB driver not found in classpath", e);
        }
    }
    private DatabaseManager() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().ignoreIfMalformed().load();
        this.url = firstNonBlank(System.getenv("DB_URL"), dotenv.get("DB_URL"));
        this.user = firstNonBlank(System.getenv("DB_USER"), dotenv.get("DB_USER"));
        this.pass = firstNonBlank(System.getenv("DB_PASS"), dotenv.get("DB_PASS"));

        if (url == null || url.isBlank()) {
            throw new IllegalStateException("DB_URL not configured in environment or .env file");
        }

        initSchema();
    }

    public static DatabaseManager getInstance() { return INSTANCE; }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, pass);
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a;
        return (b != null && !b.isBlank()) ? b : null;
    }

    private void initSchema() {
        // Create only the users table that we need, if it does not exist
        String sql = "CREATE TABLE IF NOT EXISTS users (" +
                "id BIGINT PRIMARY KEY," +
                "username VARCHAR(100)," +
                "reputation INT DEFAULT 0," +
                "cookies INT DEFAULT 0," +
                "xp INT DEFAULT 0," +
                "last_daily DATETIME NULL," +
                "created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP()," +
                "last_rep_given DATE NULL" +
                ")";
        try (Connection c = getConnection(); Statement st = c.createStatement()) {
            st.execute(sql);
            st.execute("CREATE TABLE IF NOT EXISTS update_deliveries (" +
                    "announcement_id BIGINT PRIMARY KEY," +
                    "next_part INT NOT NULL DEFAULT 0," +
                    "message_ids TEXT NULL," +
                    "completed BOOLEAN NOT NULL DEFAULT FALSE," +
                    "updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                    ")");
        } catch (SQLException e) {
            System.err.println("[CookieBot] Failed to initialize database schema: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
