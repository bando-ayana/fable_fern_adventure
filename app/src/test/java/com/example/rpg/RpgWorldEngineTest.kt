package com.example.rpg

import com.example.model.PuzzleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RpgWorldEngineTest {

    @Test
    fun allInteractablesAndCreaturesAreWalkableAndReachableFromSpawn() {
        val state = RpgWorldState()
        state.interactables.forEach { obj ->
            assertTrue(
                "Interactable ${obj.id} at (${obj.x}, ${obj.y}) must be walkable",
                RpgWorldEngine.isPointWalkable(obj.x, obj.y, radius = 0.2f)
            )
            assertTrue(
                "Interactable ${obj.id} at (${obj.x}, ${obj.y}) must be reachable from spawn",
                RpgWorldEngine.isPointReachableFromSpawn(obj.x, obj.y)
            )
        }
        state.creatures.forEach { creature ->
            assertTrue(
                "Creature ${creature.id} at (${creature.x}, ${creature.y}) must be walkable",
                RpgWorldEngine.isPointWalkable(creature.x, creature.y, radius = 0.2f)
            )
            assertTrue(
                "Creature ${creature.id} at (${creature.x}, ${creature.y}) must be reachable from spawn",
                RpgWorldEngine.isPointReachableFromSpawn(creature.x, creature.y)
            )
        }
        // All 7 mini-puzzles must be placed in the world for in-world access
        val worldPuzzles = state.interactables
            .filter { it.kind == InteractableKind.PUZZLE_PEDESTAL }
            .map { PuzzleType.fromId(it.targetId) }
            .toSet()
        assertEquals(PuzzleType.entries.toSet(), worldPuzzles)
    }

    @Test
    fun walkingWithJoystickMovesHeroAndSlidesAlongSolidObstacles() {
        val initial = RpgWorldState(heroX = 5.5f, heroY = 5.5f)
        val (movedRight, _) = RpgWorldEngine.stepWorld(
            state = initial,
            joyX = 1f,
            joyY = 0f,
            deltaSec = 0.04f,
            communityProsperity = 0,
            completedMissions = emptySet()
        )
        assertTrue(movedRight.heroX > initial.heroX)
        assertEquals(FacingDirection.RIGHT, movedRight.heroFacing)
        assertTrue(movedRight.isMoving)

        // Walking straight north into top border tree row stops at boundary without penetrating
        var clamped = RpgWorldState(heroX = 5.5f, heroY = 1.35f)
        repeat(20) {
            clamped = RpgWorldEngine.stepWorld(
                state = clamped,
                joyX = 0f,
                joyY = -1f,
                deltaSec = 0.04f,
                communityProsperity = 0,
                completedMissions = emptySet()
            ).first
        }
        assertTrue("Hero must not enter tree border at y < 1.0", clamped.heroY >= 1.0f)
    }

    @Test
    fun approachingWildCreatureTriggersAlertAndBattleEngagement() {
        // Place hero right next to Thornscale Stalker at (5.5f, 14.5f)
        val nearEnemy = RpgWorldState(heroX = 5.5f, heroY = 13.9f)
        val (stepped, engaged) = RpgWorldEngine.stepWorld(
            state = nearEnemy,
            joyX = 0f,
            joyY = 1f,
            deltaSec = 0.04f,
            communityProsperity = 0,
            completedMissions = emptySet()
        )
        assertNotNull("Expected melee engagement with Thornscale Stalker", engaged)
        assertEquals("mission_sunbridge", engaged?.missionId)

        // Cooldown prevents instant re-trigger on immediate retreat
        val cooledDown = RpgWorldEngine.applyBattleCooldownAll(stepped, cooldownSec = 4.0f)
        val (_, reEngaged) = RpgWorldEngine.stepWorld(
            state = cooledDown,
            joyX = 0f,
            joyY = 0f,
            deltaSec = 0.04f,
            communityProsperity = 0,
            completedMissions = emptySet()
        )
        assertEquals(null, reEngaged)
    }

    @Test
    fun findNearestTargetPrioritizesNearbyEnvironmentInteractablesAndWildCreatures() {
        val atFountain = RpgWorldState(heroX = 5.5f, heroY = 3.9f)
        val fountainTarget = RpgWorldEngine.findNearestTarget(
            state = atFountain,
            nectar = 15,
            communityProsperity = 0,
            completedMissions = emptySet()
        )
        assertEquals(WorldActionType.INTERACT_OBJECT, fountainTarget.actionType)
        assertEquals("mana_spring", fountainTarget.interactable?.id)

        val atEnemy = RpgWorldState(heroX = 5.5f, heroY = 14.2f)
        val enemyTarget = RpgWorldEngine.findNearestTarget(
            state = atEnemy,
            nectar = 15,
            communityProsperity = 0,
            completedMissions = emptySet()
        )
        assertEquals(WorldActionType.BATTLE_CREATURE, enemyTarget.actionType)
        assertEquals("wild_thornscale", enemyTarget.creature?.id)
        assertFalse(enemyTarget.label.isBlank())
    }

    @Test
    fun allPuzzleSpawnPointsAndPotentialClearingsAreWalkableAndReachable() {
        val spawns = RpgWorldEngine.puzzleSpawnPoints
        assertEquals(
            "Expected 7 active shrines plus 7 potential puzzle spawn clearings",
            14,
            spawns.size
        )
        assertEquals(7, spawns.count { it.isActiveShrine })
        assertEquals(7, spawns.count { !it.isActiveShrine })
        assertEquals(PuzzleType.entries.toSet(), spawns.filter { !it.isActiveShrine }.map { it.suggestedPuzzle }.toSet())

        spawns.forEach { spawn ->
            assertTrue(
                "Puzzle spawn ${spawn.id} at (${spawn.x}, ${spawn.y}) must be walkable",
                RpgWorldEngine.isPointWalkable(spawn.x, spawn.y, radius = 0.2f)
            )
            assertTrue(
                "Puzzle spawn ${spawn.id} at (${spawn.x}, ${spawn.y}) must be reachable from spawn",
                RpgWorldEngine.isPointReachableFromSpawn(spawn.x, spawn.y)
            )
        }
    }

    @Test
    fun computeMinimapRadarHighlightsNearbyInteractablesAndPotentialPuzzleSpawns() {
        // Near Solari Terrace spawn (5.5, 5.5): Mana fountain, Elder Nia, Forge, Campstone + Solari Brass Rune Ring
        val atSolari = RpgWorldState(heroX = 5.5f, heroY = 5.5f)
        val solariRadar = RpgWorldEngine.computeMinimapRadar(atSolari)
        val nearbyIds = solariRadar.nearbyInteractables.map { it.id }.toSet()
        assertTrue("mana_spring" in nearbyIds)
        assertTrue("npc_elder_nia" in nearbyIds)
        assertTrue("magitech_anvil" in nearbyIds)
        assertFalse("lantern_hollow_east" in nearbyIds)
        assertTrue(
            "Solari Brass Rune Ring should be highlighted on minimap from spawn",
            solariRadar.nearbyPuzzleSpawns.any { it.id == "potential_solari_ring" && !it.isActiveShrine }
        )

        // Move into Moss Hollow near (15.5, 5.0): should highlight active shrines & Moss Hollow potential clearing
        val atHollow = RpgWorldState(heroX = 15.5f, heroY = 5.0f, harvestedIds = setOf("bush_hollow_berries"))
        val hollowRadar = RpgWorldEngine.computeMinimapRadar(atHollow)
        val hollowObjIds = hollowRadar.nearbyInteractables.map { it.id }.toSet()
        assertTrue("lantern_hollow_west" in hollowObjIds)
        assertTrue("Harvested bush should be excluded from active nearby highlights", "bush_hollow_berries" !in hollowObjIds)
        assertTrue(hollowRadar.nearbyPuzzleSpawns.any { it.isActiveShrine && it.suggestedPuzzle == PuzzleType.FIREFLY_ECHO })
        assertTrue(hollowRadar.nearbyPuzzleSpawns.any { !it.isActiveShrine && it.id == "potential_hollow_clearing" })
    }

    @Test
    fun unbuiltBridgesBlockSouthAndEastSectionsUntilLowered() {
        val noBridges = emptySet<String>()
        val sunBridgeObj = RpgWorldEngine.worldInteractables.first { it.id == "landmark_sunbridge" }
        assertTrue(
            "North pier Sun-Bridge interactable must be reachable at start so player can repair it",
            RpgWorldEngine.isPointReachableFromSpawn(sunBridgeObj.x, sunBridgeObj.y, completedMissions = noBridges)
        )
        assertFalse(
            "Dew Lagoon chapter must be gated behind Lower Sun-Bridge before repair",
            RpgWorldEngine.isPointReachableFromSpawn(4.5f, 18.5f, completedMissions = noBridges)
        )
        assertFalse(
            "Eastern Orchard must be gated behind bridges before repair",
            RpgWorldEngine.isPointReachableFromSpawn(22.5f, 13.5f, completedMissions = noBridges)
        )

        // Walking straight south onto unbuilt SUN_BRIDGE_SITE (y=10) from north pier (5.5f, 9.6f) is blocked with toast
        var blockedState = RpgWorldState(heroX = 5.5f, heroY = 9.6f)
        repeat(2) {
            blockedState = RpgWorldEngine.stepWorld(
                state = blockedState,
                joyX = 0f,
                joyY = 1f,
                deltaSec = 0.05f,
                communityProsperity = 0,
                completedMissions = noBridges
            ).first
        }
        assertTrue("Hero must stay north of y=10 while Sun-Bridge is unbuilt", blockedState.heroY < 10.0f)
        assertNotNull("Expected helpful toast when trying to cross raised Sun-Bridge", blockedState.worldToast)

        // Completing bridge work lowers Lower Sun-Bridge, opening Dew Lagoon & Mirror Glade while keeping East Span closed
        val sunBridgeLowered = setOf("work_mission_sunbridge")
        assertTrue(
            "Dew Lagoon unlocks once Lower Sun-Bridge lowers",
            RpgWorldEngine.isPointReachableFromSpawn(4.5f, 18.5f, completedMissions = sunBridgeLowered)
        )
        assertTrue(
            "East Canopy Bridge Winch unlocks once Lower Sun-Bridge lowers",
            RpgWorldEngine.isPointReachableFromSpawn(18.2f, 13.5f, completedMissions = sunBridgeLowered)
        )
        assertFalse(
            "Eastern Orchard stays blocked until East Canopy Bridge lowers",
            RpgWorldEngine.isPointReachableFromSpawn(22.5f, 13.5f, completedMissions = sunBridgeLowered)
        )

        // Completing East Bridge / Aqueduct mission opens the entire eastern wing
        val allLowered = setOf("work_mission_sunbridge", "work_mission_aqueduct_gate")
        assertTrue(
            "Eastern Orchard opens once East Canopy Bridge lowers",
            RpgWorldEngine.isPointReachableFromSpawn(22.5f, 13.5f, completedMissions = allLowered)
        )
    }
}
