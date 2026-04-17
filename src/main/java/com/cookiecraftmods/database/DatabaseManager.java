package com.cookiecraftmods.database;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class DatabaseManager {
    private static final DatabaseManager INSTANCE = new DatabaseManager();

    final ConcurrentMap<String, Integer> cookies = new ConcurrentHashMap<>();
    final ConcurrentMap<String, String> bios = new ConcurrentHashMap<>();

    private DatabaseManager() {}

    public static DatabaseManager getInstance() { return INSTANCE; }
}
