package de.turnier;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TournamentManagerTest {

    @Test
    void addsOrdinaryPointValues() {
        assertEquals(15, TournamentManager.saturatedAdd(10, 5));
    }

    @Test
    void saturatesAtIntegerMaximumInsteadOfWrapping() {
        assertEquals(Integer.MAX_VALUE, TournamentManager.saturatedAdd(Integer.MAX_VALUE - 2, 10));
    }

    @Test
    void neverReturnsNegativePoints() {
        assertEquals(0, TournamentManager.saturatedAdd(3, -10));
    }

    @Test
    void acceptsZeroPointAward() {
        assertEquals(7, TournamentManager.saturatedAdd(7, 0));
    }
}
