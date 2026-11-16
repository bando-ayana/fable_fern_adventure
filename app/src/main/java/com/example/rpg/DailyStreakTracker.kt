package com.example.rpg

import java.util.Calendar
import java.util.TimeZone

data class DailyStreakEvaluation(
    val newStreak: Int,
    val newLongestStreak: Int,
    val newLastActivityEpochDay: Long,
    val didAdvanceStreak: Boolean,
    val wasReset: Boolean,
    val bonusHeroExp: Int,
    val bonusMana: Int,
    val rewardMessage: String?
)

data class StreakRewardPreview(
    val nextDayStreak: Int,
    val bonusHeroExp: Int,
    val bonusMana: Int
)

object DailyStreakTracker {

    private const val MILLIS_PER_DAY = 86_400_000L

    fun localEpochDay(
        nowMillis: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): Long {
        val localCal = Calendar.getInstance(timeZone).apply {
            timeInMillis = nowMillis
        }
        val utcMidnight = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(
                localCal.get(Calendar.YEAR),
                localCal.get(Calendar.MONTH),
                localCal.get(Calendar.DAY_OF_MONTH),
                0,
                0,
                0
            )
            set(Calendar.MILLISECOND, 0)
        }
        return utcMidnight.timeInMillis / MILLIS_PER_DAY
    }

    fun evaluate(
        currentStreak: Int,
        longestStreak: Int,
        lastActivityEpochDay: Long,
        todayEpochDay: Long = localEpochDay()
    ): DailyStreakEvaluation {
        if (lastActivityEpochDay == todayEpochDay && currentStreak >= 1) {
            return DailyStreakEvaluation(
                newStreak = currentStreak,
                newLongestStreak = maxOf(longestStreak, currentStreak),
                newLastActivityEpochDay = lastActivityEpochDay,
                didAdvanceStreak = false,
                wasReset = false,
                bonusHeroExp = 0,
                bonusMana = 0,
                rewardMessage = null
            )
        }

        val isConsecutiveDay = lastActivityEpochDay >= 0L && lastActivityEpochDay == todayEpochDay - 1L
        val wasReset = lastActivityEpochDay >= 0L && lastActivityEpochDay < todayEpochDay - 1L
        val nextStreak = if (isConsecutiveDay) {
            currentStreak.coerceAtLeast(1) + 1
        } else {
            1
        }
        val nextLongest = maxOf(longestStreak, nextStreak)
        val (bonusExp, bonusMana) = rewardForStreakDay(nextStreak)

        val message = if (isConsecutiveDay) {
            "$nextStreak-Day Streak Bonus! +$bonusExp Hero EXP & +$bonusMana Mana"
        } else if (wasReset) {
            "Daily Streak Restarted (Day 1)! +$bonusExp Hero EXP & +$bonusMana Mana"
        } else {
            "Daily Sanctuary Streak Started (Day 1)! +$bonusExp Hero EXP & +$bonusMana Mana"
        }

        return DailyStreakEvaluation(
            newStreak = nextStreak,
            newLongestStreak = nextLongest,
            newLastActivityEpochDay = todayEpochDay,
            didAdvanceStreak = true,
            wasReset = wasReset,
            bonusHeroExp = bonusExp,
            bonusMana = bonusMana,
            rewardMessage = message
        )
    }

    fun rewardForStreakDay(streakDay: Int): Pair<Int, Int> {
        val clampedDay = streakDay.coerceAtLeast(1)
        if (clampedDay == 1) return 15 to 6
        val bonusExp = (15 + clampedDay * 10).coerceAtMost(75)
        val bonusMana = (6 + clampedDay * 4).coerceAtMost(28)
        return bonusExp to bonusMana
    }

    fun previewNextDayBonus(currentStreak: Int): StreakRewardPreview {
        val nextDay = currentStreak.coerceAtLeast(0) + 1
        val (exp, mana) = rewardForStreakDay(nextDay)
        return StreakRewardPreview(
            nextDayStreak = nextDay,
            bonusHeroExp = exp,
            bonusMana = mana
        )
    }
}
