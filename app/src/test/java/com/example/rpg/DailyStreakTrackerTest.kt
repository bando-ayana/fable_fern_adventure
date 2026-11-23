package com.example.rpg

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

class DailyStreakTrackerTest {

    @Test
    fun `first activity starts streak at day 1 with starter exp and mana bonus`() {
        val eval = DailyStreakTracker.evaluate(
            currentStreak = 0,
            longestStreak = 0,
            lastActivityEpochDay = -1L,
            todayEpochDay = 20_000L
        )
        assertTrue(eval.didAdvanceStreak)
        assertFalse(eval.wasReset)
        assertEquals(1, eval.newStreak)
        assertEquals(1, eval.newLongestStreak)
        assertEquals(20_000L, eval.newLastActivityEpochDay)
        assertEquals(15, eval.bonusHeroExp)
        assertEquals(6, eval.bonusMana)
    }

    @Test
    fun `same day repeat activity does not double count or grant duplicate rewards`() {
        val eval = DailyStreakTracker.evaluate(
            currentStreak = 3,
            longestStreak = 4,
            lastActivityEpochDay = 20_002L,
            todayEpochDay = 20_002L
        )
        assertFalse(eval.didAdvanceStreak)
        assertEquals(3, eval.newStreak)
        assertEquals(4, eval.newLongestStreak)
        assertEquals(0, eval.bonusHeroExp)
        assertEquals(0, eval.bonusMana)
        assertNull(eval.rewardMessage)
    }

    @Test
    fun `consecutive next day advances streak and scales bonus exp and mana`() {
        val day2 = DailyStreakTracker.evaluate(
            currentStreak = 1,
            longestStreak = 1,
            lastActivityEpochDay = 20_000L,
            todayEpochDay = 20_001L
        )
        assertTrue(day2.didAdvanceStreak)
        assertFalse(day2.wasReset)
        assertEquals(2, day2.newStreak)
        assertEquals(2, day2.newLongestStreak)
        assertEquals(35, day2.bonusHeroExp)
        assertEquals(14, day2.bonusMana)

        val day4 = DailyStreakTracker.evaluate(
            currentStreak = 3,
            longestStreak = 3,
            lastActivityEpochDay = 20_002L,
            todayEpochDay = 20_003L
        )
        assertEquals(4, day4.newStreak)
        assertEquals(55, day4.bonusHeroExp)
        assertEquals(22, day4.bonusMana)
    }

    @Test
    fun `missing one or more days resets streak to 1 while preserving longest streak`() {
        val eval = DailyStreakTracker.evaluate(
            currentStreak = 5,
            longestStreak = 5,
            lastActivityEpochDay = 20_000L,
            todayEpochDay = 20_002L
        )
        assertTrue(eval.didAdvanceStreak)
        assertTrue(eval.wasReset)
        assertEquals(1, eval.newStreak)
        assertEquals(5, eval.newLongestStreak)
        assertEquals(15, eval.bonusHeroExp)
        assertEquals(6, eval.bonusMana)
    }

    @Test
    fun `localEpochDay increments cleanly across calendar midnight in local timezone`() {
        val utc = TimeZone.getTimeZone("UTC")
        val beforeMidnight = 20_000L * 86_400_000L + 86_399_000L
        val afterMidnight = 20_001L * 86_400_000L + 1_000L
        assertEquals(20_000L, DailyStreakTracker.localEpochDay(beforeMidnight, utc))
        assertEquals(20_001L, DailyStreakTracker.localEpochDay(afterMidnight, utc))
    }
}
