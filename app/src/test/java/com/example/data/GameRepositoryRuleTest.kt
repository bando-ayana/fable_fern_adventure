package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.example.model.DifficultyTier
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

class GameRepositoryRuleTest : FirestoreEmulatorTestBase() {

    private fun uniqueEmail(prefix: String): String =
        "${prefix}_${UUID.randomUUID().toString().replace("-", "").take(10)}@test.com"

    @Test
    fun ensureSeeded_authenticatedOwner_createsProfileCreaturesAndPuzzleStats() = runBlocking {
        val email = uniqueEmail("alice_seed")
        val aliceUid = signInTestUser(email)
        val repository = GameRepository(firestore)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.ensureSeeded(aliceUid, "Alice Explorer", email)
        }

        val profile = withTimeout(DEFAULT_TIMEOUT_MS) { repository.getProfileOnce(aliceUid) }
        assertNotNull(profile)
        assertEquals("Zuri Vance", profile?.explorerName)
        assertEquals(1, profile?.heroLevel)

        val creatures = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeCreatures(aliceUid).first { it.size == JungleLoreCatalog.creatures.size }
        }
        assertTrue(creatures.first { it.creatureId == "lumafern" }.discovered)

        val stats = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observePuzzleStats(aliceUid).first { it.size == PuzzleType.entries.size }
        }
        assertEquals(PuzzleType.entries.size, stats.size)
    }

    @Test
    fun mutations_authenticatedOwner_updatesProfilePuzzleStatsAndCreatureBond() = runBlocking {
        val email = uniqueEmail("alice_mut")
        val aliceUid = signInTestUser(email)
        val repository = GameRepository(firestore)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.ensureSeeded(aliceUid, "Alice Explorer", email)
        }

        val completion = withTimeout(DEFAULT_TIMEOUT_MS) {
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

        val updatedProfile = withTimeout(DEFAULT_TIMEOUT_MS) { repository.getProfileOnce(aliceUid) }
        assertNotNull(updatedProfile)
        assertTrue((updatedProfile?.totalPuzzlesSolved ?: 0) >= 1)
        assertTrue((updatedProfile?.logicXp ?: 0) > 0)

        withTimeout(DEFAULT_TIMEOUT_MS) {
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

        val postQuestProfile = withTimeout(DEFAULT_TIMEOUT_MS) { repository.getProfileOnce(aliceUid) }
        assertEquals("kofi", postQuestProfile?.selectedHeroId)
        assertFalse(postQuestProfile?.gentleMode ?: true)
        assertTrue("moss_hollow" in (postQuestProfile?.completedQuestIds ?: emptySet()))
    }

    @Test
    fun crossUserAccess_bobReadingAliceProfile_failsWithPermissionDenied() = runBlocking {
        val aliceEmail = uniqueEmail("alice_iso")
        val bobEmail = uniqueEmail("bob_iso")
        val aliceUid = signInTestUser(aliceEmail)
        val aliceRepo = GameRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.ensureSeeded(aliceUid, "Alice Explorer", aliceEmail)
        }

        signInTestUser(bobEmail)
        val bobRepo = GameRepository(firestore)
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                bobRepo.getProfileOnce(aliceUid)
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
        val repository = GameRepository(firestore)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
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
