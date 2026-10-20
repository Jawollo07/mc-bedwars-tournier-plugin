package de.turnier;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TournamentManagerTest {

    @Test
    void addsOrdinaryPointValues() {
        assertEquals(15, PointScoring.saturatedAdd(10, 5));
    }

    @Test
    void saturatesAtIntegerMaximumInsteadOfWrapping() {
        assertEquals(Integer.MAX_VALUE, PointScoring.saturatedAdd(Integer.MAX_VALUE - 2, 10));
    }

    @Test
    void neverReturnsNegativePoints() {
        assertEquals(0, PointScoring.saturatedAdd(3, -10));
    }

    @Test
    void acceptsZeroPointAward() {
        assertEquals(7, PointScoring.saturatedAdd(7, 0));
    }
}
