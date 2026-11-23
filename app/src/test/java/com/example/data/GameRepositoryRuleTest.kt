package com.example.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.base.FirestoreEmulatorTestBase
import com.example.data.local.FernfableDatabase
import com.example.model.DifficultyTier
import com.example.model.LootItemType
import com.example.model.PuzzleType
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

class GameRepositoryRuleTest : FirestoreEmulatorTestBase() {

    private fun uniqueEmail(prefix: String): String =
        "${prefix}_${UUID.randomUUID().toString().replace("-", "").take(10)}@test.com"

    private fun createRepository(): GameRepository {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return GameRepository(
            db = firestore,
            localDatabase = FernfableDatabase.getInstance(context),
            context = context
        )
    }

    @Test
    fun ensureSeeded_authenticatedOwner_createsProfileCreaturesAndPuzzleStats() = runBlocking {
        val email = uniqueEmail("alice_seed")
        val aliceUid = signInTestUser(email)
        val repository = createRepository()

        withTimeout(DEFAULT_TIMEOUT_MS.milliseconds) {
            repository.ensureSeeded(aliceUid, "Alice Explorer", email)
        }

        val profile = withTimeout(DEFAULT_TIMEOUT_MS.milliseconds) { repository.getProfile(aliceUid) }
        assertNotNull(profile)
        assertEquals("Zuri Vance", profile?.explorerName)
        assertEquals(1, profile?.heroLevel)
        assertFalse(profile?.introCompleted ?: true)

        val creatures = withTimeout(FLOW_TIMEOUT_MS.milliseconds) {
            repository.observeCreatures(aliceUid).first { it.size == JungleLoreCatalog.creatures.size }
        }
        assertTrue(creatures.first { it.creatureId == "lumafern" }.discovered)

        val stats = withTimeout(FLOW_TIMEOUT_MS.milliseconds) {
            repository.observePuzzleStats(aliceUid).first { it.size == PuzzleType.entries.size }
        }
        assertEquals(PuzzleType.entries.size, stats.size)
        assertTrue(stats.all { !it.accessed })
    }

    @Test
    fun mutations_authenticatedOwner_updatesProfilePuzzleStatsAndCreatureBond() = runBlocking {
        val email = uniqueEmail("alice_mut")
        val aliceUid = signInTestUser(email)
        val repository = createRepository()

        withTimeout(DEFAULT_TIMEOUT_MS.milliseconds) {
            repository.ensureSeeded(aliceUid, "Alice Explorer", email)
        }

        withTimeout(DEFAULT_TIMEOUT_MS.milliseconds) {
            repository.markPuzzleAccessed(aliceUid, PuzzleType.VINE_WEAVER)
        }
        val vineStatsAfterAccess = withTimeout(DEFAULT_TIMEOUT_MS.milliseconds) {
            repository.getPuzzleStatsOnce(aliceUid, PuzzleType.VINE_WEAVER.id)
        }
        assertTrue(vineStatsAfterAccess?.accessed == true)
        val spotterStatsUntouched = withTimeout(DEFAULT_TIMEOUT_MS.milliseconds) {
            repository.getPuzzleStatsOnce(aliceUid, PuzzleType.CANOPY_SPOTTER.id)
        }
        assertFalse(spotterStatsUntouched?.accessed ?: true)

        val completion = withTimeout(DEFAULT_TIMEOUT_MS.milliseconds) {
            repository.recordPuzzleSolved(
                userId = aliceUid,
                puzzleType = PuzzleType.MUSHROOM_MELODY,
                difficulty = DifficultyTier.SPROUT,
                levelPlayed = 1,
                starsEarned = 3,
                rawScore = 150
            )
        }
        assertTrue(completion.nectarEarned > 0)

        val replayBetterScore = withTimeout(DEFAULT_TIMEOUT_MS.milliseconds) {
            repository.recordPuzzleSolved(
                userId = aliceUid,
                puzzleType = PuzzleType.MUSHROOM_MELODY,
                difficulty = DifficultyTier.ELDER,
                levelPlayed = 1,
                starsEarned = 3,
                rawScore = 260
            )
        }
        assertTrue(replayBetterScore.newBestScore)
        val mushroomStats = withTimeout(DEFAULT_TIMEOUT_MS.milliseconds) {
            repository.getPuzzleStatsOnce(aliceUid, PuzzleType.MUSHROOM_MELODY.id)
        }
        assertTrue(mushroomStats?.accessed == true)
        assertEquals(260, mushroomStats?.bestScore)

        val updatedProfile = withTimeout(DEFAULT_TIMEOUT_MS.milliseconds) { repository.getProfile(aliceUid) }
        assertNotNull(updatedProfile)
        assertTrue((updatedProfile?.totalPuzzlesSolved ?: 0) >= 2)
        assertTrue((updatedProfile?.logicXp ?: 0) > 0)

        withTimeout(DEFAULT_TIMEOUT_MS.milliseconds) {
            repository.recordStoryChoice(
                userId = aliceUid,
                biomeId = "moss_hollow",
                creatureId = "lumafern",
                rewardNectar = 12,
                bondGain = 20,
                rewardExp = 35
            )
            repository.selectHero(aliceUid, "kofi")
            repository.setGentleMode(aliceUid, false)
        }

        val mission = RpgMissionCatalog.communityMissions.first()
        val missionSummary = withTimeout(DEFAULT_TIMEOUT_MS.milliseconds) {
            repository.recordMissionOutcome(
                userId = aliceUid,
                mission = mission,
                victory = true,
                calmedSpirit = false,
                remainingMana = 24,
                usedLootInBattle = mapOf(LootItemType.SUNBERRY_TONIC to 1)
            )
        }
        assertTrue(missionSummary.victory)
        assertTrue(missionSummary.expGained > 0)
        assertTrue(missionSummary.magicGained > 0)
        assertEquals(20, missionSummary.prosperityGained)

        val brewed = withTimeout(DEFAULT_TIMEOUT_MS.milliseconds) {
            repository.brewBattleLoot(aliceUid, LootItemType.SUNFIRE_CHARM)
        }
        assertTrue(brewed)

        val maxMana = withTimeout(DEFAULT_TIMEOUT_MS.milliseconds) {
            repository.restAtSanctuary(aliceUid)
        }

        val postQuestProfile = withTimeout(DEFAULT_TIMEOUT_MS.milliseconds) { repository.getProfile(aliceUid) }
        assertEquals("kofi", postQuestProfile?.selectedHeroId)
        assertFalse(postQuestProfile?.gentleMode ?: true)
        assertTrue("moss_hollow" in (postQuestProfile?.completedQuestIds ?: emptySet()))
        assertTrue(mission.id in (postQuestProfile?.completedMissionIds ?: emptySet()))
        assertEquals(20, postQuestProfile?.communityProsperity)
        assertEquals(maxMana, postQuestProfile?.mana)
        assertTrue((postQuestProfile?.magicEssence ?: 0) > 0)
        val parsedLootBeforeForage = RpgMissionCatalog.parseLootBag(postQuestProfile?.lootBagCsv.orEmpty())
        val tonicsBefore = parsedLootBeforeForage[LootItemType.SUNBERRY_TONIC] ?: 0
        val nectarBeforeForage = postQuestProfile?.nectar ?: 0

        withTimeout(DEFAULT_TIMEOUT_MS.milliseconds) {
            repository.forageWorldResource(aliceUid, nectarGain = 5, lootItem = LootItemType.SUNBERRY_TONIC)
        }
        val postForageProfile = withTimeout(DEFAULT_TIMEOUT_MS.milliseconds) { repository.getProfile(aliceUid) }
        assertEquals(nectarBeforeForage + 5, postForageProfile?.nectar)
        val parsedLoot = RpgMissionCatalog.parseLootBag(postForageProfile?.lootBagCsv.orEmpty())
        assertTrue((parsedLoot[LootItemType.SUNFIRE_CHARM] ?: 0) >= 1)
        assertEquals(tonicsBefore + 1, parsedLoot[LootItemType.SUNBERRY_TONIC] ?: 0)

        val day1 = withTimeout(DEFAULT_TIMEOUT_MS.milliseconds) {
            repository.recordDailyActivity(aliceUid, todayEpochDay = 20_400L)
        }
        assertTrue(day1.didAdvanceStreak)
        assertEquals(1, day1.newStreak)

        val repeatSameDay = withTimeout(DEFAULT_TIMEOUT_MS.milliseconds) {
            repository.recordDailyActivity(aliceUid, todayEpochDay = 20_400L)
        }
        assertFalse(repeatSameDay.didAdvanceStreak)

        val day2 = withTimeout(DEFAULT_TIMEOUT_MS.milliseconds) {
            repository.recordDailyActivity(aliceUid, todayEpochDay = 20_401L)
        }
        assertTrue(day2.didAdvanceStreak)
        assertEquals(2, day2.newStreak)
        assertEquals(35, day2.bonusHeroExp)
        assertEquals(14, day2.bonusMana)

        val streakProfile = withTimeout(DEFAULT_TIMEOUT_MS.milliseconds) { repository.getProfile(aliceUid) }
        assertEquals(2, streakProfile?.dailyStreak)
        assertEquals(2, streakProfile?.longestDailyStreak)
        assertEquals(20_401L, streakProfile?.lastActivityEpochDay)

        val nectarBeforeGraduation = streakProfile?.nectar ?: 0
        withTimeout(DEFAULT_TIMEOUT_MS.milliseconds) {
            repository.markIntroCompleted(aliceUid, bonusNectar = 20)
        }
        val graduatedProfile = withTimeout(DEFAULT_TIMEOUT_MS.milliseconds) { repository.getProfile(aliceUid) }
        assertTrue(graduatedProfile?.introCompleted ?: false)
        assertEquals(nectarBeforeGraduation + 20, graduatedProfile?.nectar)

        withTimeout(DEFAULT_TIMEOUT_MS.milliseconds) {
            repository.resetIntroTutorial(aliceUid)
        }
        val resetProfile = withTimeout(DEFAULT_TIMEOUT_MS.milliseconds) { repository.getProfile(aliceUid) }
        assertFalse(resetProfile?.introCompleted ?: true)
        assertEquals(nectarBeforeGraduation + 20, resetProfile?.nectar)
    }

    @Test
    fun crossUserAccess_bobReadingAliceProfile_failsWithPermissionDenied() = runBlocking {
        val aliceEmail = uniqueEmail("alice_iso")
        val bobEmail = uniqueEmail("bob_iso")
        val aliceUid = signInTestUser(aliceEmail)
        val aliceRepo = createRepository()
        withTimeout(DEFAULT_TIMEOUT_MS.milliseconds) {
            aliceRepo.ensureSeeded(aliceUid, "Alice Explorer", aliceEmail)
        }

        signInTestUser(bobEmail)
        val bobRepo = createRepository()
        try {
            withTimeout(DEFAULT_TIMEOUT_MS.milliseconds) {
                bobRepo.getProfile(aliceUid)
            }
            fail("Expected PERMISSION_DENIED when Bob reads Alice's profile")
        } catch (e: Exception) {
            val fsException = e.unwrapFirestoreException()
            assertNotNull("Expected FirebaseFirestoreException, got $e", fsException)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsException?.code)
        }
    }

    @Test
    fun unauthenticatedAccess_observeProfile_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = createRepository()

        try {
            withTimeout(FLOW_TIMEOUT_MS.milliseconds) {
                repository.observeProfile("alice_unauth_test").first()
            }
            fail("Expected PERMISSION_DENIED when observing profile while signed out")
        } catch (e: Exception) {
            val fsException = e.unwrapFirestoreException()
            assertNotNull("Expected nested FirebaseFirestoreException, got $e", fsException)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsException?.code)
        }
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
