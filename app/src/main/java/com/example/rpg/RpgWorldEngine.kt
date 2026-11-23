package com.example.rpg

import com.example.data.JungleLoreCatalog
import com.example.data.RpgMissionCatalog
import com.example.model.PuzzleType
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

enum class WorldTile {
    GRASS,
    MOSS_PATH,
    SOLAR_WALKWAY,
    WATER,
    CLIFF_TREE,
    SUN_BRIDGE_SITE,
    AQUEDUCT_CHANNEL,
    SHRINE_STONE
}

enum class FacingDirection {
    DOWN, UP, LEFT, RIGHT
}

enum class WorldZone(
    val id: String,
    val displayName: String,
    val subtitle: String
) {
    SOLARI_TERRACE("solari_terrace", "Solari Village Terrace", "Magitech homes, Mana Spring & Forge"),
    MOSS_HOLLOW("moss_hollow", "Whispering Moss Hollow", "Spore-lanterns, glowberries & Lumafern"),
    DEW_LAGOON("dew_lagoon", "Lower Sun-Bridges & Lagoon", "River crossings, Thornscale territory & Dewdrake"),
    MIRROR_GLADE("mirror_glade", "Mirror Glade & Aqueducts", "Healing quartz, Prism Stag & Mirefang Basin"),
    MOONFLOWER_ORCHARD("moonflower_orchard", "Moonflower Orchard", "Star-petal terraces, Aureowl & Orchid Lynx"),
    HEARTWOOD_PEAKS("heartwood_peaks", "Sky-Peak & Heartwood Shrine", "Storm spires, Phoenix peak & Shadowroot Sovereign")
}

enum class InteractableKind {
    MANA_FOUNTAIN,
    MAGITECH_ANVIL,
    HERO_CAMPSTONE,
    NPC_VILLAGER,
    FRIENDLY_CREATURE,
    SPORE_LANTERN,
    HARVEST_NECTAR,
    HARVEST_TONIC,
    PUZZLE_PEDESTAL,
    CHAPTER_BEACON,
    COMMUNITY_LANDMARK
}

data class WorldInteractable(
    val id: String,
    val title: String,
    val actionVerb: String,
    val kind: InteractableKind,
    val x: Float,
    val y: Float,
    val triggerRadius: Float = 1.65f,
    val targetId: String = "",
    val dialogueText: String = ""
)

data class WorldCreatureActor(
    val id: String,
    val name: String,
    val title: String,
    val x: Float,
    val y: Float,
    val homeX: Float,
    val homeY: Float,
    val patrolRadius: Float,
    val patrolAngle: Float,
    val speed: Float,
    val facing: FacingDirection = FacingDirection.DOWN,
    val isWildEnemy: Boolean,
    val isBeautiful: Boolean = false,
    val missionId: String? = null,
    val creatureId: String? = null,
    val recommendedLevel: Int = 1,
    val alertActive: Boolean = false,
    val walkPhase: Float = 0f,
    val battleCooldownSec: Float = 0f
)

data class WorldToast(
    val id: Int,
    val text: String,
    val x: Float,
    val y: Float,
    val ttlSec: Float = 2.4f
)

enum class WorldActionType {
    NONE,
    BATTLE_CREATURE,
    INTERACT_OBJECT
}

data class NearestWorldTarget(
    val actionType: WorldActionType = WorldActionType.NONE,
    val label: String = "",
    val interactable: WorldInteractable? = null,
    val creature: WorldCreatureActor? = null
)

data class PuzzleSpawnPoint(
    val id: String,
    val title: String,
    val x: Float,
    val y: Float,
    val suggestedPuzzle: PuzzleType,
    val isActiveShrine: Boolean
)

data class MinimapRadarSnapshot(
    val nearbyInteractables: List<WorldInteractable>,
    val nearbyPuzzleSpawns: List<PuzzleSpawnPoint>,
    val nearbyEnemies: List<WorldCreatureActor>
)

data class RpgWorldState(
    val heroX: Float = 5.5f,
    val heroY: Float = 5.5f,
    val heroFacing: FacingDirection = FacingDirection.DOWN,
    val isMoving: Boolean = false,
    val walkCycle: Float = 0f,
    val tapTarget: Pair<Float, Float>? = null,
    val creatures: List<WorldCreatureActor> = RpgWorldEngine.initialCreatures(),
    val interactables: List<WorldInteractable> = RpgWorldEngine.worldInteractables,
    val litLanternIds: Set<String> = emptySet(),
    val harvestedIds: Set<String> = emptySet(),
    val dialogueSpeaker: String? = null,
    val dialogueBody: String? = null,
    val worldToast: WorldToast? = null,
    val toastSeq: Int = 0
) {
    val activeZone: WorldZone
        get() = RpgWorldEngine.zoneAt(heroX, heroY)
}

object RpgWorldEngine {
    const val MAP_WIDTH = 28
    const val MAP_HEIGHT = 22
    const val HERO_SPEED = 4.35f
    const val HERO_COLLISION_RADIUS = 0.28f
    const val CREATURE_AGGRO_RADIUS = 2.5f
    const val CREATURE_MELEE_ENGAGE_RADIUS = 0.88f
    const val CREATURE_INTERACT_RADIUS = 1.95f
    const val MINIMAP_NEARBY_RADIUS = 5.5f

    private val terrain: Array<Array<WorldTile>> = buildTerrainGrid()

    val worldInteractables: List<WorldInteractable> = listOf(
        // Solari Village Terrace (Safe Home)
        WorldInteractable(
            id = "mana_spring",
            title = "Solari Mana Fountain",
            actionVerb = "Restore Mana",
            kind = InteractableKind.MANA_FOUNTAIN,
            x = 5.5f,
            y = 3.5f
        ),
        WorldInteractable(
            id = "magitech_anvil",
            title = "Brass Magitech Forge",
            actionVerb = "Open Forge",
            kind = InteractableKind.MAGITECH_ANVIL,
            x = 2.5f,
            y = 5.5f
        ),
        WorldInteractable(
            id = "hero_campstone",
            title = "Vanguard Campstone",
            actionVerb = "Switch Hero",
            kind = InteractableKind.HERO_CAMPSTONE,
            x = 7.5f,
            y = 3.5f
        ),
        WorldInteractable(
            id = "npc_elder_nia",
            title = "Elder Nia (Solari Architect)",
            actionVerb = "Talk",
            kind = InteractableKind.NPC_VILLAGER,
            x = 3.5f,
            y = 3.5f,
            dialogueText = "Walk east into Moss Hollow to light spore-lanterns and solve shrines, or head south across the river ford to rebuild our Sun-Bridges!"
        ),
        WorldInteractable(
            id = "npc_kwame",
            title = "Engineer Kwame",
            actionVerb = "Talk",
            kind = InteractableKind.NPC_VILLAGER,
            x = 8.5f,
            y = 7.5f,
            dialogueText = "Every Community Mission you finish transforms this living world—restored bridges, quartz clinics, and shining aqueducts appear right on the map!"
        ),

        // Whispering Moss Hollow (North-Center)
        WorldInteractable(
            id = "lantern_hollow_west",
            title = "Hollow Spore-Lantern",
            actionVerb = "Light Lantern",
            kind = InteractableKind.SPORE_LANTERN,
            x = 12.5f,
            y = 3.5f
        ),
        WorldInteractable(
            id = "lantern_hollow_east",
            title = "Canopy Relay Lantern",
            actionVerb = "Light Lantern",
            kind = InteractableKind.SPORE_LANTERN,
            x = 17.5f,
            y = 6.5f
        ),
        WorldInteractable(
            id = "bush_hollow_berries",
            title = "Golden Glowberry Bush",
            actionVerb = "Harvest Nectar",
            kind = InteractableKind.HARVEST_NECTAR,
            x = 13.5f,
            y = 7.5f
        ),
        WorldInteractable(
            id = "puzzle_firefly_echo",
            title = "Lantern Blossom Shrine",
            actionVerb = "Play Firefly Echo",
            kind = InteractableKind.PUZZLE_PEDESTAL,
            x = 15.5f,
            y = 3.5f,
            targetId = PuzzleType.FIREFLY_ECHO.id
        ),
        WorldInteractable(
            id = "puzzle_mushroom_melody",
            title = "Singing Mushroom Log",
            actionVerb = "Play Mushroom Melody",
            kind = InteractableKind.PUZZLE_PEDESTAL,
            x = 17.5f,
            y = 3.5f,
            targetId = PuzzleType.MUSHROOM_MELODY.id
        ),
        WorldInteractable(
            id = "chapter_moss_hollow",
            title = "Ch. 1: Moss Hollow Altar",
            actionVerb = "Chapter 1 Story",
            kind = InteractableKind.CHAPTER_BEACON,
            x = 15.5f,
            y = 6.5f,
            targetId = "moss_hollow"
        ),

        // Lower Sun-Bridges & Dew Lagoon (South-West)
        WorldInteractable(
            id = "landmark_sunbridge",
            title = "Lower Sun-Bridge Site",
            actionVerb = "Inspect Bridge",
            kind = InteractableKind.COMMUNITY_LANDMARK,
            x = 5.5f,
            y = 12.5f,
            targetId = "mission_sunbridge"
        ),
        WorldInteractable(
            id = "bush_lagoon_tonic",
            title = "Sunberry Herb Clump",
            actionVerb = "Forage Tonic",
            kind = InteractableKind.HARVEST_TONIC,
            x = 2.5f,
            y = 14.5f
        ),
        WorldInteractable(
            id = "puzzle_vine_weaver",
            title = "Moonflower Stream Sluice",
            actionVerb = "Play Vine Weaver",
            kind = InteractableKind.PUZZLE_PEDESTAL,
            x = 8.5f,
            y = 15.5f,
            targetId = PuzzleType.VINE_WEAVER.id
        ),
        WorldInteractable(
            id = "puzzle_canopy_spotter",
            title = "Briar Lookout Spyglass",
            actionVerb = "Play Canopy Spotter",
            kind = InteractableKind.PUZZLE_PEDESTAL,
            x = 9.5f,
            y = 18.5f,
            targetId = PuzzleType.CANOPY_SPOTTER.id
        ),
        WorldInteractable(
            id = "chapter_dew_lagoon",
            title = "Ch. 2: Dew Lagoon Shrine",
            actionVerb = "Chapter 2 Story",
            kind = InteractableKind.CHAPTER_BEACON,
            x = 4.5f,
            y = 18.5f,
            targetId = "dew_lagoon"
        ),

        // Mirror Glade & Aqueducts (Center-South)
        WorldInteractable(
            id = "landmark_mirror_glade",
            title = "Solar Clinic Mirrors",
            actionVerb = "Inspect Clinic",
            kind = InteractableKind.COMMUNITY_LANDMARK,
            x = 13.5f,
            y = 13.5f,
            targetId = "mission_glade_sanctum"
        ),
        WorldInteractable(
            id = "puzzle_grove_grid",
            title = "Four-Rune Quartz Plinth",
            actionVerb = "Play Grove Grid",
            kind = InteractableKind.PUZZLE_PEDESTAL,
            x = 15.5f,
            y = 11.5f,
            targetId = PuzzleType.GROVE_GRID.id
        ),
        WorldInteractable(
            id = "chapter_orchid_canopy",
            title = "Ch. 3: Orchid Observatory",
            actionVerb = "Chapter 3 Story",
            kind = InteractableKind.CHAPTER_BEACON,
            x = 17.5f,
            y = 11.5f,
            targetId = "orchid_canopy"
        ),
        WorldInteractable(
            id = "landmark_aqueduct",
            title = "Sunken Aqueduct Valves",
            actionVerb = "Inspect Aqueduct",
            kind = InteractableKind.COMMUNITY_LANDMARK,
            x = 14.5f,
            y = 17.5f,
            targetId = "mission_aqueduct_gate"
        ),
        WorldInteractable(
            id = "chapter_crystal_cavern",
            title = "Ch. 4: Crystal Root Arch",
            actionVerb = "Chapter 4 Story",
            kind = InteractableKind.CHAPTER_BEACON,
            x = 17.5f,
            y = 18.5f,
            targetId = "crystal_cavern"
        ),
        WorldInteractable(
            id = "lantern_glade_south",
            title = "Aqueduct Beacon Lantern",
            actionVerb = "Light Lantern",
            kind = InteractableKind.SPORE_LANTERN,
            x = 12.5f,
            y = 16.5f
        ),

        // Moonflower Orchard & Sky-Peak & Heartwood Shrine (East)
        WorldInteractable(
            id = "puzzle_fern_mosaic",
            title = "Starbloom Tapestry Loom",
            actionVerb = "Play Fern Mosaic",
            kind = InteractableKind.PUZZLE_PEDESTAL,
            x = 22.5f,
            y = 5.5f,
            targetId = PuzzleType.FERN_MOSAIC.id
        ),
        WorldInteractable(
            id = "landmark_orchard",
            title = "Moonflower Orchard Rows",
            actionVerb = "Inspect Orchard",
            kind = InteractableKind.COMMUNITY_LANDMARK,
            x = 24.5f,
            y = 7.5f,
            targetId = "mission_orchard_grove"
        ),
        WorldInteractable(
            id = "bush_orchard_nectar",
            title = "Star-Petal Blossom Patch",
            actionVerb = "Harvest Nectar",
            kind = InteractableKind.HARVEST_NECTAR,
            x = 21.5f,
            y = 3.5f
        ),
        WorldInteractable(
            id = "chapter_starbloom",
            title = "Ch. 5: Starbloom Loom",
            actionVerb = "Chapter 5 Story",
            kind = InteractableKind.CHAPTER_BEACON,
            x = 25.5f,
            y = 4.5f,
            targetId = "starbloom_treehouse"
        ),
        WorldInteractable(
            id = "landmark_weather_tower",
            title = "Sky-Bell Weather Spire",
            actionVerb = "Inspect Spire",
            kind = InteractableKind.COMMUNITY_LANDMARK,
            x = 24.5f,
            y = 12.5f,
            targetId = "mission_weather_spires"
        ),
        WorldInteractable(
            id = "puzzle_spirit_lock",
            title = "Heartwood Cipher Seal",
            actionVerb = "Play Spirit Lock",
            kind = InteractableKind.PUZZLE_PEDESTAL,
            x = 21.5f,
            y = 16.5f,
            targetId = PuzzleType.SPIRIT_LOCK.id
        ),
        WorldInteractable(
            id = "chapter_heartwood",
            title = "Ch. 6: Heartwood Shrine",
            actionVerb = "Chapter 6 Story",
            kind = InteractableKind.CHAPTER_BEACON,
            x = 21.5f,
            y = 19.5f,
            targetId = "heartwood_shrine"
        )
    )

    val puzzleSpawnPoints: List<PuzzleSpawnPoint> by lazy {
        val activeShrines = worldInteractables
            .filter { it.kind == InteractableKind.PUZZLE_PEDESTAL }
            .map { obj ->
                PuzzleSpawnPoint(
                    id = "spawn_${obj.id}",
                    title = obj.title,
                    x = obj.x,
                    y = obj.y,
                    suggestedPuzzle = PuzzleType.fromId(obj.targetId),
                    isActiveShrine = true
                )
            }
        val potentialClearings = listOf(
            PuzzleSpawnPoint(
                id = "potential_solari_ring",
                title = "Solari Brass Rune Ring",
                x = 8.5f,
                y = 5.5f,
                suggestedPuzzle = PuzzleType.FIREFLY_ECHO,
                isActiveShrine = false
            ),
            PuzzleSpawnPoint(
                id = "potential_hollow_clearing",
                title = "Moss Hollow Spore Circle",
                x = 14.5f,
                y = 8.5f,
                suggestedPuzzle = PuzzleType.MUSHROOM_MELODY,
                isActiveShrine = false
            ),
            PuzzleSpawnPoint(
                id = "potential_lagoon_mist",
                title = "Dew Lagoon Mist Clearing",
                x = 4.5f,
                y = 15.5f,
                suggestedPuzzle = PuzzleType.VINE_WEAVER,
                isActiveShrine = false
            ),
            PuzzleSpawnPoint(
                id = "potential_briar_ridge",
                title = "Briar Ridge Lookout Ring",
                x = 7.5f,
                y = 19.5f,
                suggestedPuzzle = PuzzleType.CANOPY_SPOTTER,
                isActiveShrine = false
            ),
            PuzzleSpawnPoint(
                id = "potential_glade_quartz",
                title = "Mirror Glade Quartz Verge",
                x = 19.5f,
                y = 14.5f,
                suggestedPuzzle = PuzzleType.GROVE_GRID,
                isActiveShrine = false
            ),
            PuzzleSpawnPoint(
                id = "potential_orchard_petal",
                title = "Starbloom Terrace Clearing",
                x = 23.5f,
                y = 8.5f,
                suggestedPuzzle = PuzzleType.FERN_MOSAIC,
                isActiveShrine = false
            ),
            PuzzleSpawnPoint(
                id = "potential_heartwood_seal",
                title = "Heartwood Resonant Floor",
                x = 23.5f,
                y = 19.5f,
                suggestedPuzzle = PuzzleType.SPIRIT_LOCK,
                isActiveShrine = false
            )
        )
        activeShrines + potentialClearings
    }

    fun computeMinimapRadar(
        state: RpgWorldState,
        completedMissions: Set<String> = emptySet(),
        puzzleSpawns: List<PuzzleSpawnPoint> = puzzleSpawnPoints,
        radius: Float = MINIMAP_NEARBY_RADIUS
    ): MinimapRadarSnapshot {
        val nearbyInteractables = state.interactables.filter { obj ->
            val harvested = obj.id in state.harvestedIds
            !harvested && hypot(state.heroX - obj.x, state.heroY - obj.y) <= radius
        }
        val nearbyPuzzleSpawns = puzzleSpawns.filter { spawn ->
            hypot(state.heroX - spawn.x, state.heroY - spawn.y) <= radius
        }
        val nearbyEnemies = state.creatures.filter { actor ->
            val cleared = actor.missionId != null && actor.missionId in completedMissions
            actor.isWildEnemy && !cleared && hypot(state.heroX - actor.x, state.heroY - actor.y) <= radius
        }
        return MinimapRadarSnapshot(
            nearbyInteractables = nearbyInteractables,
            nearbyPuzzleSpawns = nearbyPuzzleSpawns,
            nearbyEnemies = nearbyEnemies
        )
    }

    fun initialCreatures(): List<WorldCreatureActor> = listOf(
        // Friendly Sanctuary Mythical Creatures wandering their home groves
        WorldCreatureActor(
            id = "friendly_lumafern",
            name = "Lumafern",
            title = "Lantern-Antler Fox Spirit",
            x = 14.5f,
            y = 5.0f,
            homeX = 14.5f,
            homeY = 5.0f,
            patrolRadius = 1.2f,
            patrolAngle = 0.2f,
            speed = 0.7f,
            isWildEnemy = false,
            creatureId = "lumafern"
        ),
        WorldCreatureActor(
            id = "friendly_dewdrake",
            name = "Dewdrake",
            title = "Lagoon Hatchling",
            x = 6.5f,
            y = 17.5f,
            homeX = 6.5f,
            homeY = 17.5f,
            patrolRadius = 1.0f,
            patrolAngle = 1.4f,
            speed = 0.65f,
            isWildEnemy = false,
            creatureId = "dewdrake"
        ),
        WorldCreatureActor(
            id = "friendly_aureowl",
            name = "Aureowl",
            title = "Moon-Plumed Archivist",
            x = 16.5f,
            y = 13.5f,
            homeX = 16.5f,
            homeY = 13.5f,
            patrolRadius = 0.9f,
            patrolAngle = 2.6f,
            speed = 0.55f,
            isWildEnemy = false,
            creatureId = "aureowl"
        ),
        WorldCreatureActor(
            id = "friendly_brambleback",
            name = "Brambleback",
            title = "Heartwood Guardian",
            x = 20.5f,
            y = 18.5f,
            homeX = 20.5f,
            homeY = 18.5f,
            patrolRadius = 0.6f,
            patrolAngle = 0.9f,
            speed = 0.4f,
            isWildEnemy = false,
            creatureId = "brambleback"
        ),

        // Roaming Wild Battle Creatures (Menacing & Beautiful)
        WorldCreatureActor(
            id = "wild_thornscale",
            name = "Thornscale Stalker",
            title = "Briar-Armored Prowler",
            x = 5.5f,
            y = 14.5f,
            homeX = 5.5f,
            homeY = 14.5f,
            patrolRadius = 1.5f,
            patrolAngle = 0.0f,
            speed = 1.1f,
            isWildEnemy = true,
            isBeautiful = false,
            missionId = "mission_sunbridge",
            recommendedLevel = 1
        ),
        WorldCreatureActor(
            id = "wild_briar_patrol",
            name = "Briar Stalker (Patrol)",
            title = "Roaming Trail Prowler",
            x = 8.5f,
            y = 17.5f,
            homeX = 8.5f,
            homeY = 17.5f,
            patrolRadius = 1.3f,
            patrolAngle = 2.1f,
            speed = 1.15f,
            isWildEnemy = true,
            isBeautiful = false,
            missionId = "patrol_briar_trail",
            recommendedLevel = 1
        ),
        WorldCreatureActor(
            id = "wild_prism_stag",
            name = "Prism-Winged Stag",
            title = "Iridescent Glade Sovereign",
            x = 14.5f,
            y = 12.5f,
            homeX = 14.5f,
            homeY = 12.5f,
            patrolRadius = 1.4f,
            patrolAngle = 1.1f,
            speed = 0.95f,
            isWildEnemy = true,
            isBeautiful = true,
            missionId = "mission_glade_sanctum",
            recommendedLevel = 1
        ),
        WorldCreatureActor(
            id = "wild_mirefang",
            name = "Mirefang Serpent",
            title = "Deep-Spring Fangweaver",
            x = 14.5f,
            y = 18.5f,
            homeX = 14.5f,
            homeY = 18.5f,
            patrolRadius = 1.4f,
            patrolAngle = 3.2f,
            speed = 1.05f,
            isWildEnemy = true,
            isBeautiful = false,
            missionId = "mission_aqueduct_gate",
            recommendedLevel = 2
        ),
        WorldCreatureActor(
            id = "wild_orchid_lynx",
            name = "Celestial Orchid Lynx",
            title = "Star-Petal Spirit Cat",
            x = 23.5f,
            y = 6.5f,
            homeX = 23.5f,
            homeY = 6.5f,
            patrolRadius = 1.5f,
            patrolAngle = 0.7f,
            speed = 1.2f,
            isWildEnemy = true,
            isBeautiful = true,
            missionId = "mission_orchard_grove",
            recommendedLevel = 3
        ),
        WorldCreatureActor(
            id = "wild_storm_wyvern",
            name = "Obsidian Storm-Wyvern",
            title = "Thunder-Plume Ravager",
            x = 23.5f,
            y = 12.5f,
            homeX = 23.5f,
            homeY = 12.5f,
            patrolRadius = 1.5f,
            patrolAngle = 2.8f,
            speed = 1.25f,
            isWildEnemy = true,
            isBeautiful = false,
            missionId = "mission_weather_spires",
            recommendedLevel = 4
        ),
        WorldCreatureActor(
            id = "wild_aurora_phoenix",
            name = "Aurora-Crested Phoenix",
            title = "Living Sunfire Bird",
            x = 25.5f,
            y = 15.5f,
            homeX = 25.5f,
            homeY = 15.5f,
            patrolRadius = 1.1f,
            patrolAngle = 4.0f,
            speed = 1.0f,
            isWildEnemy = true,
            isBeautiful = true,
            missionId = "patrol_aurora_peak",
            recommendedLevel = 3
        ),
        WorldCreatureActor(
            id = "wild_shadowroot",
            name = "Shadowroot Sovereign",
            title = "Ancient Bramble Leviathan",
            x = 24.5f,
            y = 18.5f,
            homeX = 24.5f,
            homeY = 18.5f,
            patrolRadius = 0.8f,
            patrolAngle = 1.6f,
            speed = 0.85f,
            isWildEnemy = true,
            isBeautiful = false,
            missionId = "mission_final_crown",
            recommendedLevel = 5
        )
    )

    fun tileAt(tx: Int, ty: Int): WorldTile {
        if (tx !in 0 until MAP_WIDTH || ty !in 0 until MAP_HEIGHT) return WorldTile.CLIFF_TREE
        return terrain[ty][tx]
    }

    fun isWalkableTile(tile: WorldTile): Boolean = when (tile) {
        WorldTile.CLIFF_TREE, WorldTile.WATER -> false
        WorldTile.GRASS,
        WorldTile.MOSS_PATH,
        WorldTile.SOLAR_WALKWAY,
        WorldTile.SUN_BRIDGE_SITE,
        WorldTile.AQUEDUCT_CHANNEL,
        WorldTile.SHRINE_STONE -> true
    }

    fun isPointWalkable(x: Float, y: Float, radius: Float = HERO_COLLISION_RADIUS): Boolean {
        val minTx = (x - radius).toInt()
        val maxTx = (x + radius).toInt()
        val minTy = (y - radius).toInt()
        val maxTy = (y + radius).toInt()
        if (x - radius < 0.1f || x + radius > MAP_WIDTH - 0.1f || y - radius < 0.1f || y + radius > MAP_HEIGHT - 0.1f) {
            return false
        }
        for (ty in minTy..maxTy) {
            for (tx in minTx..maxTx) {
                if (!isWalkableTile(tileAt(tx, ty))) return false
            }
        }
        return true
    }

    fun zoneAt(x: Float, y: Float): WorldZone = when {
        x < 11f && y < 10f -> WorldZone.SOLARI_TERRACE
        x in 11f..19.5f && y < 10f -> WorldZone.MOSS_HOLLOW
        x < 11f && y >= 10f -> WorldZone.DEW_LAGOON
        x in 11f..19.5f && y >= 10f -> WorldZone.MIRROR_GLADE
        x >= 19.5f && y < 10f -> WorldZone.MOONFLOWER_ORCHARD
        else -> WorldZone.HEARTWOOD_PEAKS
    }

    fun stepWorld(
        state: RpgWorldState,
        joyX: Float,
        joyY: Float,
        deltaSec: Float,
        communityProsperity: Int,
        completedMissions: Set<String>
    ): Pair<RpgWorldState, WorldCreatureActor?> {
        val dt = deltaSec.coerceIn(0f, 0.05f)
        var vx = joyX
        var vy = joyY
        var nextTapTarget = state.tapTarget

        val joyMag = hypot(vx, vy)
        if (joyMag > 0.12f) {
            // Manual joystick overrides tap-to-walk target
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
            val stepX = vx * HERO_SPEED * dt
            val stepY = vy * HERO_SPEED * dt
            val candidateX = nextX + stepX
            if (isPointWalkable(candidateX, nextY)) {
                nextX = candidateX
            }
            val candidateY = nextY + stepY
            if (isPointWalkable(nextX, candidateY)) {
                nextY = candidateY
            }
            if (nextTapTarget != null && nextX == state.heroX && nextY == state.heroY) {
                // Blocked by obstacle toward tap target; clear tap target cleanly
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
        val updatedCreatures = state.creatures.map { actor ->
            val cooldown = (actor.battleCooldownSec - dt).coerceAtLeast(0f)
            val distToHero = hypot(nextX - actor.x, nextY - actor.y)
            val mission = actor.missionId?.let { RpgMissionCatalog.missionById(it) }
            val missionUnlocked = mission == null ||
                mission.isWildPatrol ||
                communityProsperity >= mission.requiredProsperity
            val alreadyCleared = actor.missionId != null && actor.missionId in completedMissions

            val chasing = actor.isWildEnemy &&
                missionUnlocked &&
                !alreadyCleared &&
                cooldown <= 0f &&
                distToHero <= CREATURE_AGGRO_RADIUS

            var targetX: Float
            var targetY: Float
            var nextAngle = actor.patrolAngle
            if (chasing) {
                targetX = nextX
                targetY = nextY
            } else {
                nextAngle = (actor.patrolAngle + dt * actor.speed * 0.7f) % (2f * Math.PI.toFloat())
                targetX = actor.homeX + cos(nextAngle) * actor.patrolRadius
                targetY = actor.homeY + sin(nextAngle) * (actor.patrolRadius * 0.72f)
            }

            val dx = targetX - actor.x
            val dy = targetY - actor.y
            val distToTarget = hypot(dx, dy)
            var cx = actor.x
            var cy = actor.y
            var cFacing = actor.facing
            if (distToTarget > 0.04f) {
                val moveStep = (if (chasing) actor.speed * 1.15f else actor.speed) * dt
                val ratio = (moveStep / distToTarget).coerceAtMost(1f)
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
                missionUnlocked &&
                !alreadyCleared &&
                cooldown <= 0f &&
                postDist <= CREATURE_MELEE_ENGAGE_RADIUS
            ) {
                engagedCreature = actor
            }

            actor.copy(
                x = cx,
                y = cy,
                patrolAngle = nextAngle,
                facing = cFacing,
                alertActive = chasing,
                walkPhase = (actor.walkPhase + dt * 5.5f) % (2f * Math.PI.toFloat()),
                battleCooldownSec = if (engagedCreature?.id == actor.id) 4.0f else cooldown
            )
        }

        val nextToast = state.worldToast?.let { toast ->
            val remaining = toast.ttlSec - dt
            if (remaining > 0f) toast.copy(ttlSec = remaining) else null
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

    fun applyBattleCooldownAll(state: RpgWorldState, cooldownSec: Float = 4.5f): RpgWorldState =
        state.copy(
            tapTarget = null,
            creatures = state.creatures.map { actor ->
                if (actor.isWildEnemy) actor.copy(battleCooldownSec = cooldownSec, alertActive = false) else actor
            }
        )

    fun findNearestTarget(
        state: RpgWorldState,
        nectar: Int,
        communityProsperity: Int,
        completedMissions: Set<String>
    ): NearestWorldTarget {
        var bestEnemy: WorldCreatureActor? = null
        var bestEnemyDist = Float.MAX_VALUE
        for (actor in state.creatures) {
            if (!actor.isWildEnemy) continue
            val d = hypot(state.heroX - actor.x, state.heroY - actor.y)
            if (d <= CREATURE_INTERACT_RADIUS && d < bestEnemyDist) {
                bestEnemy = actor
                bestEnemyDist = d
            }
        }

        var bestObj: WorldInteractable? = null
        var bestObjDist = Float.MAX_VALUE
        for (obj in state.interactables) {
            val d = hypot(state.heroX - obj.x, state.heroY - obj.y)
            if (d <= obj.triggerRadius && d < bestObjDist) {
                bestObj = obj
                bestObjDist = d
            }
        }

        // Prioritize whichever is closer; wild enemies within 1.45 tiles take priority for snappy combat
        if (bestEnemy != null && (bestObj == null || bestEnemyDist <= bestObjDist || bestEnemyDist <= 1.45f)) {
            val mission = bestEnemy.missionId?.let { RpgMissionCatalog.missionById(it) }
            val unlocked = mission == null || mission.isWildPatrol || communityProsperity >= mission.requiredProsperity
            val cleared = bestEnemy.missionId != null && bestEnemy.missionId in completedMissions
            val verb = when {
                !unlocked -> "Inspect (${mission.requiredProsperity}% Town)"
                cleared -> "Rematch ${bestEnemy.name}"
                else -> "Battle ${bestEnemy.name}"
            }
            return NearestWorldTarget(
                actionType = WorldActionType.BATTLE_CREATURE,
                label = verb,
                creature = bestEnemy
            )
        }

        if (bestObj != null) {
            val label = when (bestObj.kind) {
                InteractableKind.SPORE_LANTERN ->
                    if (bestObj.id in state.litLanternIds) "Chime Lantern" else "Light Lantern"
                InteractableKind.HARVEST_NECTAR, InteractableKind.HARVEST_TONIC ->
                    if (bestObj.id in state.harvestedIds) "Already Foraged" else bestObj.actionVerb
                InteractableKind.CHAPTER_BEACON -> {
                    val biome = JungleLoreCatalog.biomes.firstOrNull { it.id == bestObj.targetId }
                    if (biome != null && nectar < biome.unlockNectar) {
                        "Need ${biome.unlockNectar} Nectar"
                    } else {
                        bestObj.actionVerb
                    }
                }
                else -> bestObj.actionVerb
            }
            return NearestWorldTarget(
                actionType = WorldActionType.INTERACT_OBJECT,
                label = label,
                interactable = bestObj
            )
        }

        // Friendly creature talk check
        val friendly = state.creatures.firstOrNull {
            !it.isWildEnemy && hypot(state.heroX - it.x, state.heroY - it.y) <= 1.75f
        }
        if (friendly != null) {
            return NearestWorldTarget(
                actionType = WorldActionType.INTERACT_OBJECT,
                label = "Greet ${friendly.name}",
                creature = friendly
            )
        }

        return NearestWorldTarget()
    }

    fun isPointReachableFromSpawn(targetX: Float, targetY: Float): Boolean {
        val startTile = 5 to 5
        val goalTile = targetX.toInt() to targetY.toInt()
        if (!isWalkableTile(tileAt(goalTile.first, goalTile.second))) return false
        val visited = mutableSetOf(startTile)
        val queue = ArrayDeque<Pair<Int, Int>>().apply { add(startTile) }
        val dirs = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)
        while (queue.isNotEmpty()) {
            val cur = queue.removeFirst()
            if (cur == goalTile) return true
            for ((dx, dy) in dirs) {
                val nx = cur.first + dx
                val ny = cur.second + dy
                val next = nx to ny
                if (nx in 0 until MAP_WIDTH &&
                    ny in 0 until MAP_HEIGHT &&
                    next !in visited &&
                    isWalkableTile(tileAt(nx, ny))
                ) {
                    visited.add(next)
                    queue.addLast(next)
                }
            }
        }
        return false
    }

    private fun buildTerrainGrid(): Array<Array<WorldTile>> {
        // 28 wide x 22 tall lush, interconnected Solari Canopy & Wilds map
        // Legend:
        // T = CLIFF_TREE, G = GRASS, P = MOSS_PATH, W = SOLAR_WALKWAY,
        // ~ = WATER, B = SUN_BRIDGE_SITE, A = AQUEDUCT_CHANNEL, S = SHRINE_STONE
        val rows = listOf(
            "TTTTTTTTTTTTTTTTTTTTTTTTTTTT", // 0
            "TWWWWWWWWWTTGGGGGGGGTTGGGGGT", // 1
            "TWWWWWWWWWPPGGGGGGGGPPGGGGGT", // 2
            "TWWWWWWWWWTGGGGGGGGGGGGGGGGT", // 3
            "TWWWWWWWWWPPGGGGGGGGPPGGGGGT", // 4
            "TWWWWWWWWWGGGGGGGGGGGGGGGGGT", // 5
            "TWWWWWWWWWGGGGGGGGGGGGGGGGGT", // 6
            "TWWWWWWWWWPPGGGGGGGGPPGGGGGT", // 7
            "TGGGGPPGGGTTGGGGGGGGTTGGGGGT", // 8
            "TTTTTPPTTTTTTTTPPTTTTTTTTPPT", // 9  (passes at x=5..6, x=15..16, x=25..26)
            "~~~~~~PP~~~~TTGGGGGGGGTTGGGT", // 10 (stepping ford x=6..7)
            "~~~~BBPP~~~~GGGGGGGGGGGGGGGT", // 11 (Sun-Bridge site at x=4..5 + ford at 6..7)
            "TTTTBBPPTTTTTTGGGGGGTTGGGGGT", // 12
            "TGGGGPPPPGGGGGGSSSSGGGGGGGGT", // 13
            "TGGGGGGGGGGGGGGSSSSPPGGGGGGT", // 14
            "TGGGGGGGGGGGTTAAAAAAGGPPGGGT", // 15 (Aqueduct channel)
            "TGGGGGGGGGGGPPAAAAAAGGGGGGGT", // 16
            "TGGGGGGGGGGGGGGGGGGGGSSSSGGT", // 17 (Heartwood Shrine stone)
            "TGGGGGGGGGGGGGGGGGGGGSSSSGGT", // 18
            "TGGGGGGGGGGGGGPPGGGGGGSSSSGT", // 19
            "TGGGGGGGGGGGGGGGGGGGGSSSSGGT", // 20
            "TTTTTTTTTTTTTTTTTTTTTTTTTTTT"  // 21
        )
        return Array(MAP_HEIGHT) { y ->
            val row = rows[y]
            Array(MAP_WIDTH) { x ->
                when (row[x]) {
                    'T' -> WorldTile.CLIFF_TREE
                    'P' -> WorldTile.MOSS_PATH
                    'W' -> WorldTile.SOLAR_WALKWAY
                    '~' -> WorldTile.WATER
                    'B' -> WorldTile.SUN_BRIDGE_SITE
                    'A' -> WorldTile.AQUEDUCT_CHANNEL
                    'S' -> WorldTile.SHRINE_STONE
                    else -> WorldTile.GRASS
                }
            }
        }
    }
}
