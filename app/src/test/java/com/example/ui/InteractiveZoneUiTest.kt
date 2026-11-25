package com.example.ui

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.audio.FernfableAccessibilityManager
import com.example.audio.SpeechEngine
import com.example.data.JungleLoreCatalog
import com.example.data.RpgMissionCatalog
import com.example.model.StoryChoice
import com.example.rpg.InteractiveZoneEngine
import com.example.ui.components.ProvideFernfableAccessibility
import com.example.ui.screens.BiomeEncounterSheet
import com.example.ui.screens.InteractiveZoneScreen
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp")
class InteractiveZoneUiTest {

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
    fun dewLagoonHydroPadValveChoiceDisplaysPlayAreaCueAndRoutesToInteractiveZone() {
        val dewBiome = JungleLoreCatalog.biomes.first { it.id == "dew_lagoon" }
        var pickedChoice: StoryChoice? = null

        composeRule.setContent {
            MaterialTheme {
                ProvideFernfableAccessibility(externalManager = a11yManager) {
                    BiomeEncounterSheet(
                        biome = dewBiome,
                        bondLevel = 1,
                        isQuestCompleted = false,
                        encounterFeedback = null,
                        onDismiss = {},
                        onChooseStoryOption = { _, choice -> pickedChoice = choice },
                        onLaunchPuzzle = {}
                    )
                }
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("story_choice_dew_splash")
            .performScrollTo()
            .assertTextContains("Enter Area", substring = true)
            .performClick()

        composeRule.waitForIdle()
        assertEquals("dew_splash", pickedChoice?.id)
        assertNull(pickedChoice?.opensPuzzle)
    }

    @Test
    fun interactiveZoneScreenAllowsAvatarWorkForPointsAndLootThenUnlocksConfront() {
        val sunBridgeMission = RpgMissionCatalog.missionById("mission_sunbridge")!!
        var state by mutableStateOf(InteractiveZoneEngine.forMission(sunBridgeMission))
        var confronted = false

        composeRule.setContent {
            MaterialTheme {
                ProvideFernfableAccessibility(externalManager = a11yManager) {
                    InteractiveZoneScreen(
                        zoneState = state,
                        heroId = "zuri",
                        onStepFrame = { jx, jy, dt ->
                            state = InteractiveZoneEngine.stepZone(state, jx, jy, dt)
                        },
                        onSetTapTarget = { x, y ->
                            state = state.copy(tapTarget = x to y)
                        },
                        onPerformWork = { stationId ->
                            state = InteractiveZoneEngine.completeWorkstation(state, stationId)!!.updatedState
                        },
                        onConfrontCreature = { confronted = true },
                        onLaunchChapterPuzzle = {},
                        onExitZone = {}
                    )
                }
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("interactive_zone_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("zone_progress_chip").assertTextContains("0/3", substring = true)

        // Walk hero adjacent to station 1 (North Pier Anchor), complete it, then station 2 (winch lowers bridge), then station 3 (south pier)
        state.workstations.forEach { ws ->
            state = state.copy(heroX = ws.x, heroY = ws.y, tapTarget = null)
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("zone_work_button")
                .assertIsDisplayed()
                .performClick()
            composeRule.waitForIdle()
        }

        composeRule.onNodeWithTag("zone_progress_chip").assertTextContains("Cleared!", substring = true)
        composeRule.onNodeWithTag("zone_clear_card").assertIsDisplayed()
        composeRule.onNodeWithTag("zone_rewards_banner").assertTextContains("Loot", substring = true)
        composeRule.onNodeWithTag("zone_confront_button")
            .assertIsDisplayed()
            .performClick()

        composeRule.waitForIdle()
        assertTrue("Expected Confront button to launch creature confrontation", confronted)
    }
}
