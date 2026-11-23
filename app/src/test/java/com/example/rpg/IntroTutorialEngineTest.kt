package com.example.rpg

import com.example.data.RpgMissionCatalog
import com.example.model.PuzzleType
import com.example.stateHolder.IntroTutorialState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IntroTutorialEngineTest {

    @Test
    fun spawnInteractablesCreaturesAndObjectivesAreWalkable() {
        assertTrue(
            "Hero spawn must be walkable",
            IntroTutorialEngine.isPointWalkable(IntroTutorialEngine.HERO_SPAWN_X, IntroTutorialEngine.HERO_SPAWN_Y)
        )
        val initial = IntroTutorialEngine.initialWorldState()
        initial.interactables.forEach { obj ->
            assertTrue(
                "Training interactable ${obj.id} at (${obj.x}, ${obj.y}) must be walkable",
                IntroTutorialEngine.isPointWalkable(obj.x, obj.y, radius = 0.2f)
            )
        }
        initial.creatures.forEach { actor ->
            assertTrue(
                "Training creature ${actor.id} at (${actor.x}, ${actor.y}) must be walkable",
                IntroTutorialEngine.isPointWalkable(actor.x, actor.y, radius = 0.2f)
            )
        }
        val shrine = initial.interactables.first { it.kind == InteractableKind.PUZZLE_PEDESTAL }
        assertEquals(PuzzleType.FIREFLY_ECHO.id, shrine.targetId)

        assertNull(IntroTutorialEngine.objectivePosition(IntroTutorialState(stage = IntroStage.CUTSCENE)))
        listOf(
            IntroStage.LESSON_MOVE_AND_LANTERN,
            IntroStage.LESSON_PUZZLE_SHRINE,
            IntroStage.LESSON_WILD_CREATURE_BATTLE,
            IntroStage.GRADUATION_READY
        ).forEach { stage ->
            val objPos = IntroTutorialEngine.objectivePosition(IntroTutorialState(stage = stage))
            assertNotNull("Stage $stage should have an objective marker", objPos)
            assertTrue(
                "Objective for $stage should be walkable",
                IntroTutorialEngine.isPointWalkable(objPos!!.first, objPos.second, radius = 0.2f)
            )
        }
    }

    @Test
    fun computeStageProgressesThroughIntroStagesInOrder() {
        assertEquals(
            IntroStage.CUTSCENE,
            IntroTutorialEngine.computeStage(
                inCutscene = true,
                lanternLit = false,
                shrinePuzzleSolved = false,
                sproutlingDefeated = false
            )
        )
        assertEquals(
            IntroStage.LESSON_MOVE_AND_LANTERN,
            IntroTutorialEngine.computeStage(
                inCutscene = false,
                lanternLit = false,
                shrinePuzzleSolved = false,
                sproutlingDefeated = false
            )
        )
        assertEquals(
            IntroStage.LESSON_PUZZLE_SHRINE,
            IntroTutorialEngine.computeStage(
                inCutscene = false,
                lanternLit = true,
                shrinePuzzleSolved = false,
                sproutlingDefeated = false
            )
        )
        assertEquals(
            IntroStage.LESSON_WILD_CREATURE_BATTLE,
            IntroTutorialEngine.computeStage(
                inCutscene = false,
                lanternLit = true,
                shrinePuzzleSolved = true,
                sproutlingDefeated = false
            )
        )
        assertEquals(
            IntroStage.GRADUATION_READY,
            IntroTutorialEngine.computeStage(
                inCutscene = false,
                lanternLit = true,
                shrinePuzzleSolved = true,
                sproutlingDefeated = true
            )
        )
    }

    @Test
    fun walkingAndBoundaryCollisionBehaveCorrectlyInTrainingGlade() {
        val state = IntroTutorialState(stage = IntroStage.LESSON_MOVE_AND_LANTERN)
        val (movedEast, _) = IntroTutorialEngine.stepWorld(
            tutorialState = state,
            joyX = 1f,
            joyY = 0f,
            deltaSec = 0.05f
        )
        assertTrue(movedEast.heroX > state.worldState.heroX)
        assertEquals(FacingDirection.RIGHT, movedEast.heroFacing)
        assertTrue(movedEast.isMoving)

        var boundary = IntroTutorialState(
            stage = IntroStage.LESSON_MOVE_AND_LANTERN,
            worldState = IntroTutorialEngine.initialWorldState().copy(heroX = 2.5f, heroY = 1.35f)
        )
        repeat(20) {
            boundary = boundary.copy(
                worldState = IntroTutorialEngine.stepWorld(boundary, 0f, -1f, 0.04f).first
            )
        }
        assertTrue("Hero must stop at cliff tree border", boundary.worldState.heroY >= 1.0f)
    }

    @Test
    fun mossbackSproutlingOnlyAutoEngagesDuringBattleLesson() {
        val nearCubWorld = IntroTutorialEngine.initialWorldState().copy(heroX = 7.5f, heroY = 6.95f)
        val duringLanternLesson = IntroTutorialState(
            stage = IntroStage.LESSON_MOVE_AND_LANTERN,
            worldState = nearCubWorld
        )
        val (_, engagedEarly) = IntroTutorialEngine.stepWorld(duringLanternLesson, 0f, 1f, 0.04f)
        assertNull("Sproutling should not ambush during Lesson 1", engagedEarly)

        val duringBattleLesson = IntroTutorialState(
            stage = IntroStage.LESSON_WILD_CREATURE_BATTLE,
            lanternLit = true,
            shrinePuzzleSolved = true,
            sproutlingDefeated = false,
            worldState = nearCubWorld
        )
        val (_, engagedLesson3) = IntroTutorialEngine.stepWorld(duringBattleLesson, 0f, 1f, 0.04f)
        assertNotNull("Sproutling should engage during Lesson 3", engagedLesson3)
        assertEquals(RpgMissionCatalog.introMission.id, engagedLesson3?.missionId)
    }

    @Test
    fun findNearestTargetDetectsLanternShrineAndSproutling() {
        val atLantern = IntroTutorialState(
            stage = IntroStage.LESSON_MOVE_AND_LANTERN,
            worldState = IntroTutorialEngine.initialWorldState().copy(heroX = 5.9f, heroY = 3.5f)
        )
        val lanternTarget = IntroTutorialEngine.findNearestTarget(atLantern)
        assertEquals(WorldActionType.INTERACT_OBJECT, lanternTarget.actionType)
        assertEquals("intro_lantern", lanternTarget.interactable?.id)

        val atShrine = IntroTutorialState(
            stage = IntroStage.LESSON_PUZZLE_SHRINE,
            lanternLit = true,
            worldState = IntroTutorialEngine.initialWorldState().copy(heroX = 9.4f, heroY = 3.5f)
        )
        val shrineTarget = IntroTutorialEngine.findNearestTarget(atShrine)
        assertEquals(WorldActionType.INTERACT_OBJECT, shrineTarget.actionType)
        assertEquals("intro_firefly_shrine", shrineTarget.interactable?.id)

        val atCub = IntroTutorialState(
            stage = IntroStage.LESSON_WILD_CREATURE_BATTLE,
            lanternLit = true,
            shrinePuzzleSolved = true,
            worldState = IntroTutorialEngine.initialWorldState().copy(heroX = 7.4f, heroY = 7.0f)
        )
        val cubTarget = IntroTutorialEngine.findNearestTarget(atCub)
        assertEquals(WorldActionType.BATTLE_CREATURE, cubTarget.actionType)
        assertEquals("intro_mossback", cubTarget.creature?.id)
        assertFalse(cubTarget.label.isBlank())
    }
}
