package de.turnier;

import de.marcely.bedwars.api.arena.Team;

import java.util.Locale;

/** Resolves MBedwars colors independently of their translated/configured display token. */
final class BedwarsTeamResolver {
    private BedwarsTeamResolver() {}

    static Team resolve(String input) {
        if (input == null || input.isBlank()) return null;
        Team team = Team.getByName(input.trim());
        if (team != null) return team;

        // Older plugin versions persisted Team#getConfigName(), which can be a
        // localization token such as %Color_Blue% rather than the enum identifier.
        String normalized = input.trim();
        normalized = normalized.replaceFirst("(?i)^%?color_", "");
        normalized = normalized.replaceFirst("%$", "");
        normalized = normalized.replaceAll("[^A-Za-z0-9_]", "_")
                .toUpperCase(Locale.ROOT);
        try {
            return Team.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    static boolean same(String first, String second) {
        Team a = resolve(first);
        Team b = resolve(second);
        return a != null && a == b;
    }

    static boolean same(String stored, Team team) {
        return team != null && same(stored, team.name());
    }

    static String key(String input) {
        Team team = resolve(input);
        return team == null ? (input == null ? null : input.toLowerCase(Locale.ROOT))
                : team.name().toLowerCase(Locale.ROOT);
    }
}
