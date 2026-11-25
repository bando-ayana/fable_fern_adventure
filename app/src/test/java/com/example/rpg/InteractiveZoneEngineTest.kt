package com.example.rpg

import com.example.data.JungleLoreCatalog
import com.example.data.RpgMissionCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InteractiveZoneEngineTest {

    @Test
    fun allChapterNonPuzzleChoicesProducePlayableZonesWithPointsAndLoot() {
        val nonPuzzleChoices = JungleLoreCatalog.biomes.flatMap { biome ->
            biome.storyChoices.filter { it.opensPuzzle == null }.map { biome to it }
        }
        assertEquals("Expected 6 non-puzzle chapter choices across 6 chapters", 6, nonPuzzleChoices.size)

        nonPuzzleChoices.forEach { (biome, choice) ->
            var state = InteractiveZoneEngine.forChapterChoice(biome, choice)
            assertEquals(ZoneKind.CHAPTER_ACTION, state.kind)
            assertEquals(3, state.workstations.size)
            assertTrue(
                "Hero spawn in ${state.zoneId} must be walkable",
                InteractiveZoneEngine.isPointWalkable(state, state.heroX, state.heroY)
            )
            state.workstations.forEach { ws ->
                assertTrue(
                    "Workstation ${ws.id} in ${state.zoneId} must be walkable",
                    InteractiveZoneEngine.isPointWalkable(state, ws.x, ws.y, bridgeLoweredOverride = true)
                )
                assertTrue(
                    "Workstation ${ws.id} in ${state.zoneId} must be reachable",
                    InteractiveZoneEngine.isPointReachable(state, ws.x, ws.y, bridgeLowered = true)
                )
                val outcome = InteractiveZoneEngine.completeWorkstation(state, ws.id)
                assertNotNull("Expected workstation ${ws.id} to complete", outcome)
                state = outcome!!.updatedState
            }
            assertTrue("${state.zoneId} should be cleared after completing all workstations", state.zoneCleared)
            assertTrue("${state.zoneId} must award Hero EXP", state.totalExpEarned > 0)
            assertTrue("${state.zoneId} must award Nectar", state.totalNectarEarned > 0)
            assertTrue("${state.zoneId} must award Magic", state.totalMagicEarned > 0)
            assertTrue("${state.zoneId} must award Loot", state.totalLootEarned.isNotEmpty())
        }
    }

    @Test
    fun dewSplashFloatingHydroPadValveFillsWaterChannelsAndRewardsLoot() {
        val dewBiome = JungleLoreCatalog.biomes.first { it.id == "dew_lagoon" }
        val dewSplash = dewBiome.storyChoices.first { it.id == "dew_splash" }
        var state = InteractiveZoneEngine.forChapterChoice(dewBiome, dewSplash)

        assertEquals(0f, state.waterFillProgress)
        val valveStation = state.workstations.first { it.fillsWaterChannel }
        assertTrue(InteractiveZoneEngine.isPointReachable(state, valveStation.x, valveStation.y, bridgeLowered = false))

        state.workstations.forEach { ws ->
            state = InteractiveZoneEngine.completeWorkstation(state, ws.id)!!.updatedState
        }
        assertEquals(1f, state.waterFillProgress)
        assertTrue(state.zoneCleared)
        assertTrue(state.totalLootEarned.size >= 2)
    }

    @Test
    fun allCommunityMissionsAndPatrolsHaveCompletePlayableWorkZones() {
        val allMissions = RpgMissionCatalog.communityMissions + RpgMissionCatalog.wildPatrols
        assertEquals(8, allMissions.size)

        allMissions.forEach { mission ->
            var state = InteractiveZoneEngine.forMission(mission)
            assertEquals(ZoneKind.COMMUNITY_MISSION, state.kind)
            assertEquals(3, state.workstations.size)
            assertTrue(
                "Hero spawn in ${mission.id} zone must be walkable",
                InteractiveZoneEngine.isPointWalkable(state, state.heroX, state.heroY)
            )

            // Sequentially complete bridge-lowering stations first (or any station without lowersBridgePlanksRequirement)
            state.workstations.forEach { ws ->
                val bridgeUp = state.bridgeFillProgress >= 0.99f
                assertTrue(
                    "Workstation ${ws.id} in ${mission.id} should be reachable at this step (bridgeUp=$bridgeUp)",
                    InteractiveZoneEngine.isPointReachable(state, ws.x, ws.y, bridgeLowered = bridgeUp)
                )
                state = InteractiveZoneEngine.completeWorkstation(state, ws.id)!!.updatedState
            }

            assertTrue("${mission.id} zone should clear", state.zoneCleared)
            assertTrue(state.totalExpEarned > 0)
            assertTrue(state.totalNectarEarned > 0)
            assertTrue(state.totalMagicEarned > 0)
            assertTrue("${mission.id} must grant Loot", state.totalLootEarned.isNotEmpty())
        }
    }

    @Test
    fun sunBridgeAndAqueductZonesGateCrossingUntilWinchOrValveLowersBridgePlanks() {
        val sunBridgeMission = RpgMissionCatalog.missionById("mission_sunbridge")!!
        var sunState = InteractiveZoneEngine.forMission(sunBridgeMission)
        val southPier = sunState.workstations.first { it.id == "sunbridge_ws_south_lock" }
        val winch = sunState.workstations.first { it.lowersBridgePlanks }

        assertFalse(
            "South pier should NOT be reachable before Lower Sun-Bridge planks drop",
            InteractiveZoneEngine.isPointReachable(sunState, southPier.x, southPier.y, bridgeLowered = false)
        )
        assertTrue(
            "Winch should be reachable from north bank",
            InteractiveZoneEngine.isPointReachable(sunState, winch.x, winch.y, bridgeLowered = false)
        )

        sunState = InteractiveZoneEngine.completeWorkstation(sunState, winch.id)!!.updatedState
        assertEquals(1f, sunState.bridgeFillProgress)
        assertTrue(
            "South pier MUST become reachable once winch lowers the bridge planks",
            InteractiveZoneEngine.isPointReachable(sunState, southPier.x, southPier.y, bridgeLowered = true)
        )

        val aqueductMission = RpgMissionCatalog.missionById("mission_aqueduct_gate")!!
        var aqState = InteractiveZoneEngine.forMission(aqueductMission)
        val eastGauge = aqState.workstations.first { it.id == "aq_ws_east_gauge" }
        val masterValve = aqState.workstations.first { it.lowersBridgePlanks && it.fillsWaterChannel }

        assertFalse(
            "East terrace gauge should NOT be reachable before East Span lowers",
            InteractiveZoneEngine.isPointReachable(aqState, eastGauge.x, eastGauge.y, bridgeLowered = false)
        )
        aqState = InteractiveZoneEngine.completeWorkstation(aqState, masterValve.id)!!.updatedState
        assertEquals(1f, aqState.bridgeFillProgress)
        assertEquals(1f, aqState.waterFillProgress)
        assertTrue(
            "East terrace gauge MUST become reachable once aqueduct valve lowers East Span",
            InteractiveZoneEngine.isPointReachable(aqState, eastGauge.x, eastGauge.y, bridgeLowered = true)
        )
    }
}
