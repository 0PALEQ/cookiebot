package com.cookiecraftmods.database;

public class UserRepository {
    private static final UserRepository INSTANCE = new UserRepository();
    private final DatabaseManager db = DatabaseManager.getInstance();

    private UserRepository() {}

    public static UserRepository getInstance() { return INSTANCE; }

    public int getCookies(String userId) {
        return db.cookies.getOrDefault(userId, 0);
    }

    public void addCookies(String userId, int amount) {
        db.cookies.merge(userId, amount, Integer::sum);
    }

    public String getBio(String userId) {
        return db.bios.get(userId);
    }

    public void setBio(String userId, String bio) {
        if (bio == null || bio.isBlank()) {
            db.bios.remove(userId);
        } else {
            db.bios.put(userId, bio);
        }
    }
}
