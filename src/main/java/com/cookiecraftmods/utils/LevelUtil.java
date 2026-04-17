package com.cookiecraftmods.utils;

public final class LevelUtil {
    private LevelUtil() {}

    // Cumulative XP required to reach a given level (level 1 starts at 0xp)
    // Formula: xpForLevel(L) = 100 * L * (L - 1) / 2  -> 0, 100, 300, 600, 1000, ...
    public static int xpForLevel(int level) {
        if (level <= 1) return 0;
        long xp = 100L * level * (level - 1) / 2L;
        return (int) Math.min(Integer.MAX_VALUE, xp);
    }

    public static int levelForXp(int xp) {
        if (xp <= 0) return 1;
        // Solve L^2 - L - (2*xp/100) <= 0  => L = floor((1 + sqrt(1 + 8*xp/100)) / 2)
        double a = 1.0 + 8.0 * (xp / 100.0);
        int level = (int) Math.floor((1.0 + Math.sqrt(a)) / 2.0);
        return Math.max(1, level);
    }

    public static int xpIntoLevel(int xp) {
        int level = levelForXp(xp);
        return xp - xpForLevel(level);
    }

    public static int xpToNextLevel(int xp) {
        int level = levelForXp(xp);
        int nextReq = xpForLevel(level + 1) - xpForLevel(level);
        int into = xpIntoLevel(xp);
        return Math.max(0, nextReq - into);
    }

    public static String progressBar(int length, int current, int max) {
        if (length <= 0) length = 10;
        if (max <= 0) max = 1;
        double ratio = Math.max(0, Math.min(1, current / (double) max));
        int filled = (int) Math.round(ratio * length);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) sb.append(i < filled ? '█' : '░');
        return sb.toString();
    }
}
