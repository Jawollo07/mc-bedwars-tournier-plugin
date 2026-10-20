package de.turnier;

/**
 * Pure point arithmetic helpers that can be tested without loading Bukkit/Paper classes.
 */
public final class PointScoring {

    private PointScoring() {
        // Utility class
    }

    /** Adds points without allowing integer overflow to wrap the leaderboard. */
    public static int saturatedAdd(int current, int amount) {
        long result = (long) current + amount;
        return (int) Math.max(0L, Math.min(Integer.MAX_VALUE, result));
    }
}
