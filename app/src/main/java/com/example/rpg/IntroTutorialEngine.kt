package com.example.rpg

import com.example.data.RpgMissionCatalog
import com.example.model.PuzzleType
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

enum class IntroStage(
    val stepIndex: Int,
    val badge: String,
    val title: String,
    val instruction: String,
    val tip: String
) {
    CUTSCENE(
        stepIndex = 0,
        badge = "Story",
        title = "Welcome to Solari Canopy",
        instruction = "Learn why Solari needs an explorer and how to awaken the Canopy Crown.",
        tip = "Tap Next to read the tale or Skip Intro anytime."
    ),
    LESSON_MOVE_AND_LANTERN(
        stepIndex = 1,
        badge = "Lesson 1 of 3",
        title = "Walk & Light the Spore-Lantern",
        instruction = "Drag the golden stick on the left or tap on the mossy trail to walk east to the Spore-Lantern, then tap Light Lantern.",
        tip = "Lighting lanterns and foraging glowing bushes grants Sunbeam Nectar as you explore!"
    ),
    LESSON_PUZZLE_SHRINE(
        stepIndex = 2,
        badge = "Lesson 2 of 3",
        title = "Unlock a Puzzle Shrine",
        instruction = "Head east to the violet Firefly Training Shrine and solve Firefly Echo to earn XP and unlock it in the Puzzles tab.",
        tip = "Every puzzle shrine you discover in the RPG can be replayed anytime in the Puzzles tab for higher scores!"
    ),
    LESSON_WILD_CREATURE_BATTLE(
        stepIndex = 3,
        badge = "Lesson 3 of 3",
        title = "Battle or Soothe a Wild Creature",
        instruction = "Walk south into the meadow to challenge the Mossback Sproutling. Try Solar Strike, Arcane Pulse, or Soothe below 45% HP!",
        tip = "Wild battles grant Hero EXP, Magic Essence, Mana, and Loot to help rebuild Solari's Community Missions to 100%."
    ),
    GRADUATION_READY(
        stepIndex = 4,
        badge = "Ready!",
        title = "Explorer Training Complete!",
        instruction = "You have mastered walking, puzzle shrines, and wild creature battles. Enter the full Solari Canopy to rebuild the community and defeat the Shadowroot Sovereign!",
        tip = "Claim +20 Starter Sunbeam Nectar as your Solari Vanguard graduation gift."
    )
}

data class IntroTutorialState(
    val stage: IntroStage = IntroStage.CUTSCENE,
    val cutscenePage: Int = 0,
    val worldState: RpgWorldState = IntroTutorialEngine.initialWorldState(),
    val lanternLit: Boolean = false,
    val shrinePuzzleSolved: Boolean = false,
    val sproutlingDefeated: Boolean = false
)

object IntroTutorialEngine {
    const val MAP_WIDTH = 12
    const val MAP_HEIGHT = 10
    const val HERO_SPAWN_X = 2.5f
    const val HERO_SPAWN_Y = 3.5f

    private val terrain: Array<Array<WorldTile>> = buildTrainingTerrain()

    val trainingInteractables: List<WorldInteractable> = listOf(
        WorldInteractable(
            id = "intro_elder_nia",
            title = "Elder Nia (Mentor)",
            actionVerb = "Talk to Elder Nia",
            kind = InteractableKind.NPC_VILLAGER,
            x = 2.0f,
            y = 2.2f,
            dialogueText = "Follow the golden beacon! First light the Spore-Lantern, then solve the Firefly Shrine, and finally test your skills against the Mossback Sproutling."
        ),
        WorldInteractable(
            id = "intro_mana_spring",
            title = "Glade Mana Dewpool",
            actionVerb = "Restore Mana",
            kind = InteractableKind.MANA_FOUNTAIN,
            x = 2.0f,
            y = 5.5f
        ),
        WorldInteractable(
            id = "intro_lantern",
            title = "Training Spore-Lantern",
            actionVerb = "Light Lantern",
            kind = InteractableKind.SPORE_LANTERN,
            x = 6.0f,
            y = 3.5f,
            triggerRadius = 1.8f
        ),
        WorldInteractable(
            id = "intro_firefly_shrine",
            title = "Firefly Training Shrine",
            actionVerb = "Play Firefly Echo",
            kind = InteractableKind.PUZZLE_PEDESTAL,
            x = 9.5f,
            y = 3.5f,
            triggerRadius = 1.8f,
            targetId = PuzzleType.FIREFLY_ECHO.id
        )
    )

    fun initialCreatures(): List<WorldCreatureActor> = listOf(
        WorldCreatureActor(
            id = "intro_lumafern",
            name = "Lumafern",
            title = "Lantern-Antler Companion",
            x = 4.0f,
            y = 2.2f,
            homeX = 4.0f,
            homeY = 2.2f,
            patrolRadius = 0.6f,
            patrolAngle = 0.4f,
            speed = 0.55f,
            isWildEnemy = false,
            creatureId = "lumafern"
        ),
        WorldCreatureActor(
            id = "intro_mossback",
            name = "Mossback Sproutling",
            title = "Prickly Training Cub",
            x = 7.5f,
            y = 7.2f,
            homeX = 7.5f,
            homeY = 7.2f,
            patrolRadius = 0.9f,
            patrolAngle = 0.0f,
            speed = 0.65f,
            isWildEnemy = true,
            isBeautiful = false,
            missionId = RpgMissionCatalog.introMission.id,
            recommendedLevel = 1
        )
    )

    fun initialWorldState(): RpgWorldState = RpgWorldState(
        heroX = HERO_SPAWN_X,
        heroY = HERO_SPAWN_Y,
        heroFacing = FacingDirection.RIGHT,
        creatures = initialCreatures(),
        interactables = trainingInteractables
    )

    fun tileAt(tx: Int, ty: Int): WorldTile {
        if (tx !in 0 until MAP_WIDTH || ty !in 0 until MAP_HEIGHT) return WorldTile.CLIFF_TREE
        return terrain[ty][tx]
    }

    fun isPointWalkable(x: Float, y: Float, radius: Float = RpgWorldEngine.HERO_COLLISION_RADIUS): Boolean {
        val minTx = (x - radius).toInt()
        val maxTx = (x + radius).toInt()
        val minTy = (y - radius).toInt()
        val maxTy = (y + radius).toInt()
        if (x - radius < 0.1f || x + radius > MAP_WIDTH - 0.1f || y - radius < 0.1f || y + radius > MAP_HEIGHT - 0.1f) {
            return false
        }
        for (ty in minTy..maxTy) {
            for (tx in minTx..maxTx) {
                if (!RpgWorldEngine.isWalkableTile(tileAt(tx, ty))) return false
            }
        }
        return true
    }

    fun objectivePosition(state: IntroTutorialState): Pair<Float, Float>? = when (state.stage) {
        IntroStage.LESSON_MOVE_AND_LANTERN -> 6.0f to 3.5f
        IntroStage.LESSON_PUZZLE_SHRINE -> 9.5f to 3.5f
        IntroStage.LESSON_WILD_CREATURE_BATTLE -> {
            val cub = state.worldState.creatures.firstOrNull { it.id == "intro_mossback" }
            if (cub != null) cub.x to cub.y else 7.5f to 7.2f
        }
        IntroStage.GRADUATION_READY -> 2.0f to 2.2f
        IntroStage.CUTSCENE -> null
    }

    fun stepWorld(
        tutorialState: IntroTutorialState,
        joyX: Float,
        joyY: Float,
        deltaSec: Float
    ): Pair<RpgWorldState, WorldCreatureActor?> {
        val state = tutorialState.worldState
        val dt = deltaSec.coerceIn(0f, 0.05f)
        var vx = joyX
        var vy = joyY
        var nextTapTarget = state.tapTarget

        val joyMag = hypot(vx, vy)
        if (joyMag > 0.12f) {
            nextTapTarget = null
            if (joyMag > 1f) {
                vx /= joyMag
                vy /= joyMag
            }
        } else if (nextTapTarget != null) {
            val dx = nextTapTarget.first - state.heroX
            val dy = nextTapTarget.second - state.heroY
            val dist = hypot(dx, dy)
            if (dist <= 0.18f) {
                vx = 0f
                vy = 0f
                nextTapTarget = null
            } else {
                vx = dx / dist
                vy = dy / dist
            }
        } else {
            vx = 0f
            vy = 0f
        }

        val moving = hypot(vx, vy) > 0.08f
        var nextX = state.heroX
        var nextY = state.heroY
        var nextFacing = state.heroFacing
        var nextWalkCycle = state.walkCycle

        if (moving) {
            val stepX = vx * RpgWorldEngine.HERO_SPEED * dt
            val stepY = vy * RpgWorldEngine.HERO_SPEED * dt
            val candX = nextX + stepX
            if (isPointWalkable(candX, nextY)) nextX = candX
            val candY = nextY + stepY
            if (isPointWalkable(nextX, candY)) nextY = candY
            if (nextTapTarget != null && nextX == state.heroX && nextY == state.heroY) {
                nextTapTarget = null
            }
            nextFacing = when {
                kotlin.math.abs(vx) > kotlin.math.abs(vy) -> if (vx > 0f) FacingDirection.RIGHT else FacingDirection.LEFT
                vy > 0f -> FacingDirection.DOWN
                else -> FacingDirection.UP
            }
            nextWalkCycle = (state.walkCycle + dt * 10f) % (2f * Math.PI.toFloat())
        }

        var engagedCreature: WorldCreatureActor? = null
        val canEngageSproutling = tutorialState.stage == IntroStage.LESSON_WILD_CREATURE_BATTLE &&
            !tutorialState.sproutlingDefeated

        val updatedCreatures = state.creatures.map { actor ->
            val cooldown = (actor.battleCooldownSec - dt).coerceAtLeast(0f)
            val distToHero = hypot(nextX - actor.x, nextY - actor.y)
            val chasing = actor.isWildEnemy &&
                canEngageSproutling &&
                cooldown <= 0f &&
                distToHero <= 2.3f

            var targetX: Float
            var targetY: Float
            var nextAngle = actor.patrolAngle
            if (chasing) {
                targetX = nextX
                targetY = nextY
            } else {
                nextAngle = (actor.patrolAngle + dt * actor.speed * 0.7f) % (2f * Math.PI.toFloat())
                targetX = actor.homeX + cos(nextAngle) * actor.patrolRadius
                targetY = actor.homeY + sin(nextAngle) * (actor.patrolRadius * 0.65f)
            }

            val dx = targetX - actor.x
            val dy = targetY - actor.y
            val dist = hypot(dx, dy)
            var cx = actor.x
            var cy = actor.y
            var cFacing = actor.facing
            if (dist > 0.04f) {
                val ratio = ((actor.speed * dt) / dist).coerceAtMost(1f)
                val candX = cx + dx * ratio
                val candY = cy + dy * ratio
                if (isPointWalkable(candX, cy, radius = 0.22f)) cx = candX
                if (isPointWalkable(cx, candY, radius = 0.22f)) cy = candY
                cFacing = when {
                    kotlin.math.abs(dx) > kotlin.math.abs(dy) -> if (dx > 0f) FacingDirection.RIGHT else FacingDirection.LEFT
                    dy > 0f -> FacingDirection.DOWN
                    else -> FacingDirection.UP
                }
            }

            val postDist = hypot(nextX - cx, nextY - cy)
            if (engagedCreature == null &&
                actor.isWildEnemy &&
                canEngageSproutling &&
                cooldown <= 0f &&
                postDist <= RpgWorldEngine.CREATURE_MELEE_ENGAGE_RADIUS
            ) {
                engagedCreature = actor
            }

            actor.copy(
                x = cx,
                y = cy,
                patrolAngle = nextAngle,
                facing = cFacing,
                alertActive = chasing,
                walkPhase = (actor.walkPhase + dt * 5.2f) % (2f * Math.PI.toFloat()),
                battleCooldownSec = if (engagedCreature?.id == actor.id) 4.0f else cooldown
            )
        }

        val nextToast = state.worldToast?.let { toast ->
            val rem = toast.ttlSec - dt
            if (rem > 0f) toast.copy(ttlSec = rem) else null
        }

        return state.copy(
            heroX = nextX,
            heroY = nextY,
            heroFacing = nextFacing,
            isMoving = moving,
            walkCycle = nextWalkCycle,
            tapTarget = nextTapTarget,
            creatures = updatedCreatures,
            worldToast = nextToast
        ) to engagedCreature
    }

    fun findNearestTarget(tutorialState: IntroTutorialState): NearestWorldTarget {
        val world = tutorialState.worldState
        val completed = if (tutorialState.sproutlingDefeated) {
            setOf(RpgMissionCatalog.introMission.id)
        } else {
            emptySet()
        }

        val cub = world.creatures.firstOrNull {
            it.isWildEnemy && hypot(world.heroX - it.x, world.heroY - it.y) <= RpgWorldEngine.CREATURE_INTERACT_RADIUS
        }
        if (cub != null) {
            val label = if (tutorialState.sproutlingDefeated) "Spar Again" else "Battle Sproutling"
            return NearestWorldTarget(
                actionType = WorldActionType.BATTLE_CREATURE,
                label = label,
                creature = cub
            )
        }

        var bestObj: WorldInteractable? = null
        var bestDist = Float.MAX_VALUE
        for (obj in world.interactables) {
            val d = hypot(world.heroX - obj.x, world.heroY - obj.y)
            if (d <= obj.triggerRadius && d < bestDist) {
                bestObj = obj
                bestDist = d
            }
        }
        if (bestObj != null) {
            val label = when (bestObj.kind) {
                InteractableKind.SPORE_LANTERN ->
                    if (bestObj.id in world.litLanternIds) "Lantern Lit!" else "Light Lantern"
                else -> bestObj.actionVerb
            }
            return NearestWorldTarget(
                actionType = WorldActionType.INTERACT_OBJECT,
                label = label,
                interactable = bestObj
            )
        }

        val friendly = world.creatures.firstOrNull {
            !it.isWildEnemy && hypot(world.heroX - it.x, world.heroY - it.y) <= 1.75f
        }
        if (friendly != null && completed.isEmpty()) {
            return NearestWorldTarget(
                actionType = WorldActionType.INTERACT_OBJECT,
                label = "Pet ${friendly.name}",
                creature = friendly
            )
        }

        return NearestWorldTarget()
    }

    fun computeStage(
        inCutscene: Boolean,
        lanternLit: Boolean,
        shrinePuzzleSolved: Boolean,
        sproutlingDefeated: Boolean
    ): IntroStage = when {
        inCutscene -> IntroStage.CUTSCENE
        !lanternLit -> IntroStage.LESSON_MOVE_AND_LANTERN
        !shrinePuzzleSolved -> IntroStage.LESSON_PUZZLE_SHRINE
        !sproutlingDefeated -> IntroStage.LESSON_WILD_CREATURE_BATTLE
        else -> IntroStage.GRADUATION_READY
    }

    private fun buildTrainingTerrain(): Array<Array<WorldTile>> {
        // 12 wide x 10 tall sunlit Training Glade
        val rows = listOf(
            "TTTTTTTTTTTT", // 0
            "TWWWWGGGGGGT", // 1
            "TWWWWPPPPGGT", // 2
            "TWWWWPPPPPPT", // 3
            "TWWWWPPGGPPT", // 4
            "TGGGGPPGGGGT", // 5
            "T~GGGPPGGGGT", // 6
            "T~~GGPPSSGGT", // 7
            "T~~~GGGGGGGT", // 8
            "TTTTTTTTTTTT"  // 9
        )
        return Array(MAP_HEIGHT) { y ->
            val row = rows[y]
            Array(MAP_WIDTH) { x ->
                when (row[x]) {
                    'T' -> WorldTile.CLIFF_TREE
                    'W' -> WorldTile.SOLAR_WALKWAY
                    'P' -> WorldTile.MOSS_PATH
                    '~' -> WorldTile.WATER
                    'S' -> WorldTile.SHRINE_STONE
                    else -> WorldTile.GRASS
                }
            }
        }
    }
}
