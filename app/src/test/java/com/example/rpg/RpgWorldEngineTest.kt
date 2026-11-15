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
}