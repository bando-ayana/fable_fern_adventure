package com.example.ui

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.audio.FernfableAccessibilityManager
import com.example.audio.SpeechEngine
import com.example.data.local.PlayerProfileEntity
import com.example.model.LootItemType
import com.example.model.PuzzleCompletionResult
import com.example.puzzle.DailyChallengeEngine
import com.example.rpg.DailyStreakTracker
import com.example.ui.components.ProvideFernfableAccessibility
import com.example.ui.screens.PuzzleHubScreen
import com.example.ui.screens.PuzzlePlayScreen
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp")
class DailyChallengeUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var a11yManager: FernfableAccessibilityManager

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("fernfable_accessibility_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        a11yManager = FernfableAccessibilityManager(
            context = context,
            speechEngineFactory = {
                object : SpeechEngine {
                    override fun speak(text: String, utteranceId: String) = Unit
                    override fun stop() = Unit
                    override fun shutdown() = Unit
                }
            }
        )
    }

    @After
    fun tearDown() {
        a11yManager.release()
    }

    @Test
    fun puzzleHubDisplaysDailyChallengeCardTrackerAndLaunches() {
        var launchClicks = 0
        val uiState = FernfableUiState(
            profile = PlayerProfileEntity(
                dailyChallengesSolved = 3,
                dailyChallengeStreak = 2,
                lastDailyChallengeEpochDay = DailyStreakTracker.localEpochDay() - 1L
            )
        )

        composeRule.setContent {
            MaterialTheme {
                ProvideFernfableAccessibility(externalManager = a11yManager) {
                    PuzzleHubScreen(
                        uiState = uiState,
                        onSelectDifficulty = {},
                        onToggleGentleMode = {},
                        onStartPuzzle = { _, _ -> },
                        onStartDailyChallenge = { launchClicks++ }
                    )
                }
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("daily_challenge_card").assertIsDisplayed()
        composeRule.onNodeWithTag("daily_challenge_tracker").assertIsDisplayed()
        composeRule.onNodeWithTag("daily_challenge_status_pill")
            .assertTextContains("Reward Chest Ready", substring = true)
        composeRule.onNodeWithTag("daily_challenge_play_button")
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextContains("Solve Today's Challenge", substring = true)
            .performClick()

        assertEquals(1, launchClicks)
    }

    @Test
    fun puzzleHubShowsSolvedTodayStateAndSupportsCalmView() {
        val today = DailyStreakTracker.localEpochDay()
        a11yManager.setCalmMode(true)
        var launchClicks = 0
        val uiState = FernfableUiState(
            profile = PlayerProfileEntity(
                dailyChallengesSolved = 5,
                dailyChallengeStreak = 5,
                lastDailyChallengeEpochDay = today,
                dailyChallengeHistoryCsv = "$today:3"
            )
        )

        composeRule.setContent {
            MaterialTheme {
                ProvideFernfableAccessibility(externalManager = a11yManager) {
                    PuzzleHubScreen(
                        uiState = uiState,
                        onSelectDifficulty = {},
                        onToggleGentleMode = {},
                        onStartPuzzle = { _, _ -> },
                        onStartDailyChallenge = { launchClicks++ }
                    )
                }
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("daily_challenge_status_pill")
            .assertTextContains("Solved Today", substring = true)
        composeRule.onNodeWithTag("daily_challenge_play_button")
            .performScrollTo()
            .assertTextContains("Replay Today's Challenge", substring = true)
            .performClick()

        assertEquals(1, launchClicks)
    }

    @Test
    fun puzzlePlayCelebrationShowsDailyChallengeChestReward() {
        val today = DailyStreakTracker.localEpochDay()
        val spec = DailyChallengeEngine.forEpochDay(today)
        val result = PuzzleCompletionResult(
            puzzleType = spec.puzzleType,
            difficulty = spec.difficulty,
            completedLevel = spec.level,
            nextLevelUnlocked = spec.level + 1,
            starsEarned = 3,
            score = 420,
            nectarEarned = 22,
            moonstonesEarned = 2,
            skillXpEarned = 65,
            newBestScore = true,
            isDailyChallenge = true,
            dailyBonusNectar = spec.reward.nectar,
            dailyBonusShards = spec.reward.moonstones,
            dailyBonusMagic = spec.reward.magicEssence,
            dailyBonusLoot = LootItemType.SUNFIRE_CHARM,
            dailyRewardClaimedJustNow = true,
            newDailyChallengeStreak = 4
        )

        var returned = 0
        composeRule.setContent {
            MaterialTheme {
                ProvideFernfableAccessibility(externalManager = a11yManager) {
                    PuzzlePlayScreen(
                        puzzleType = spec.puzzleType,
                        level = spec.level,
                        difficulty = spec.difficulty,
                        gentleMode = true,
                        fireflyState = FireflyEchoUiState(),
                        mushroomState = null,
                        mushroomSelectedSlot = null,
                        fernMosaicState = null,
                        mosaicStatus = null,
                        canopySpotterState = null,
                        spotterStatus = null,
                        groveGridState = null,
                        selectedGridCell = null,
                        vineWeaverState = null,
                        spiritLockState = null,
                        completionResult = result,
                        isDailyChallenge = true,
                        dailyChallengeTitle = spec.curatedTitle,
                        onBack = { returned++ },
                        onRestartLevel = {},
                        onTapBlossom = {},
                        onReplayFireflyHint = {},
                        onTapMushroomSlot = {},
                        onUseMushroomHint = {},
                        onSelectMosaicSlot = {},
                        onChooseMosaicTile = {},
                        onUseMosaicHint = {},
                        onTapSpotterCharm = {},
                        onUseSpotterHint = {},
                        onSelectGridCell = {},
                        onPlaceGridRune = {},
                        onUseGridHint = {},
                        onRotateVineTile = { _, _ -> },
                        onUseVineHint = {},
                        onAppendDraftRune = {},
                        onRemoveLastDraftRune = {},
                        onSubmitSpiritDraft = {},
                        onUseSpiritHint = {},
                        onDismissCompletion = {}
                    )
                }
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("puzzle_celebration_card")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Daily Challenge Cleared!", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("daily_challenge_reward_chest")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Canopy Reward Chest Opened! (4d Streak)", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Sunfire Relic Charm", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("celebration_return_button")
            .performScrollTo()
            .performClick()

        assertEquals(1, returned)
    }
}
