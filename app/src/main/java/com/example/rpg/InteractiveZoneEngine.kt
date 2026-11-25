package com.example.rpg

import com.example.data.JungleLoreCatalog
import com.example.data.RpgMissionCatalog
import com.example.model.BiomeNode
import com.example.model.CommunityMission
import com.example.model.LootItemType
import com.example.model.StoryChoice
import kotlin.math.atan2
import kotlin.math.hypot

enum class ZoneKind {
    CHAPTER_ACTION,
    COMMUNITY_MISSION
}

enum class ZoneTile {
    GRASS,
    PATH,
    WOOD_DECK,
    BRIDGE_PLANK,
    WATER,
    CHANNEL_WATER,
    TREE,
    STONE_WALL
}

enum class WorkstationVisual {
    VALVE_WHEEL,
    CABLE_WINCH,
    SOLAR_MIRROR,
    MIST_CALIBRATOR,
    BOTANICAL_POT,
    STORM_BELL,
    RESONANCE_CHIME,
    SPIRIT_BRAZIER
}

data class ZoneWorkstation(
    val id: String,
    val title: String,
    val actionVerb: String,
    val completedLabel: String,
    val workSummary: String,
    val visual: WorkstationVisual,
    val x: Float,
    val y: Float,
    val rewardNectar: Int,
    val rewardExp: Int,
    val rewardMagic: Int,
    val rewardLoot: LootItemType? = null,
    val lowersBridgePlanks: Boolean = false,
    val fillsWaterChannel: Boolean = false
)

data class InteractiveZoneState(
    val zoneId: String,
    val title: String,
    val subtitle: String,
    val objectiveHint: String,
    val kind: ZoneKind,
    val companionCreatureId: String,
    val companionName: String,
    val companionX: Float,
    val companionY: Float,
    val terrainRows: List<String>,
    val heroX: Float,
    val heroY: Float,
    val heroFacing: FacingDirection = FacingDirection.DOWN,
    val isMoving: Boolean = false,
    val walkCycle: Float = 0f,
    val tapTarget: Pair<Float, Float>? = null,
    val workstations: List<ZoneWorkstation>,
    val completedStationIds: Set<String> = emptySet(),
    val bridgeFillProgress: Float = 0f,
    val waterFillProgress: Float = 0f,
    val stationNectarEarned: Int = 0,
    val stationExpEarned: Int = 0,
    val stationMagicEarned: Int = 0,
    val stationLootEarned: List<LootItemType> = emptyList(),
    val completionBonusNectar: Int = 0,
    val completionBonusExp: Int = 0,
    val completionBonusMagic: Int = 0,
    val completionBonusMoonstones: Int = 0,
    val completionBonusLoot: LootItemType? = null,
    val zoneCleared: Boolean = false,
    val chapterBiome: BiomeNode? = null,
    val chapterChoice: StoryChoice? = null,
    val mission: CommunityMission? = null,
    val toastText: String? = null
) {
    val totalStations: Int get() = workstations.size
    val completedCount: Int get() = completedStationIds.size
    val totalNectarEarned: Int get() = stationNectarEarned + if (zoneCleared) completionBonusNectar else 0
    val totalExpEarned: Int get() = stationExpEarned + if (zoneCleared) completionBonusExp else 0
    val totalMagicEarned: Int get() = stationMagicEarned + if (zoneCleared) completionBonusMagic else 0
    val totalLootEarned: List<LootItemType>
        get() = if (zoneCleared && completionBonusLoot != null) {
            stationLootEarned + completionBonusLoot
        } else {
            stationLootEarned
        }
}

data class WorkstationActionOutcome(
    val updatedState: InteractiveZoneState,
    val station: ZoneWorkstation,
    val justClearedZone: Boolean,
    val awardedNectar: Int,
    val awardedExp: Int,
    val awardedMagic: Int,
    val awardedLoot: LootItemType?
)

object InteractiveZoneEngine {
    const val ZONE_WIDTH = 14
    const val ZONE_HEIGHT = 10
    const val HERO_SPEED = 4.25f
    const val HERO_RADIUS = 0.28f
    const val INTERACT_RADIUS = 1.65f

    fun parseTile(ch: Char): ZoneTile = when (ch) {
        'P' -> ZoneTile.PATH
        'W' -> ZoneTile.WOOD_DECK
        'B' -> ZoneTile.BRIDGE_PLANK
        '~' -> ZoneTile.WATER
        'A' -> ZoneTile.CHANNEL_WATER
        'T' -> ZoneTile.TREE
        'S' -> ZoneTile.STONE_WALL
        else -> ZoneTile.GRASS
    }

    fun tileAt(state: InteractiveZoneState, tx: Int, ty: Int): ZoneTile {
        if (tx !in 0 until ZONE_WIDTH || ty !in 0 until ZONE_HEIGHT) return ZoneTile.TREE
        val row = state.terrainRows.getOrNull(ty) ?: return ZoneTile.TREE
        return parseTile(row.getOrElse(tx) { 'T' })
    }

    fun isTileWalkable(tile: ZoneTile, bridgeLowered: Boolean): Boolean = when (tile) {
        ZoneTile.GRASS, ZoneTile.PATH, ZoneTile.WOOD_DECK -> true
        ZoneTile.BRIDGE_PLANK -> bridgeLowered
        ZoneTile.WATER, ZoneTile.CHANNEL_WATER, ZoneTile.TREE, ZoneTile.STONE_WALL -> false
    }

    fun isPointWalkable(
        state: InteractiveZoneState,
        x: Float,
        y: Float,
        radius: Float = HERO_RADIUS,
        bridgeLoweredOverride: Boolean? = null
    ): Boolean {
        val bridgeLowered = bridgeLoweredOverride ?: (state.bridgeFillProgress >= 0.99f)
        val checks = listOf(
            x - radius to y - radius,
            x + radius to y - radius,
            x - radius to y + radius,
            x + radius to y + radius
        )
        return checks.all { (cx, cy) ->
            if (cx < 0.35f || cy < 0.35f || cx > ZONE_WIDTH - 0.35f || cy > ZONE_HEIGHT - 0.35f) {
                false
            } else {
                isTileWalkable(tileAt(state, cx.toInt(), cy.toInt()), bridgeLowered)
            }
        }
    }

    fun isPointReachable(
        state: InteractiveZoneState,
        targetX: Float,
        targetY: Float,
        bridgeLowered: Boolean
    ): Boolean {
        val startTx = state.heroX.toInt().coerceIn(0, ZONE_WIDTH - 1)
        val startTy = state.heroY.toInt().coerceIn(0, ZONE_HEIGHT - 1)
        val goalTx = targetX.toInt().coerceIn(0, ZONE_WIDTH - 1)
        val goalTy = targetY.toInt().coerceIn(0, ZONE_HEIGHT - 1)
        if (!isTileWalkable(tileAt(state, startTx, startTy), bridgeLowered)) return false
        if (!isTileWalkable(tileAt(state, goalTx, goalTy), bridgeLowered)) return false

        val visited = Array(ZONE_HEIGHT) { BooleanArray(ZONE_WIDTH) }
        val queue = ArrayDeque<Pair<Int, Int>>()
        queue.add(startTx to startTy)
        visited[startTy][startTx] = true
        val dirs = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)

        while (queue.isNotEmpty()) {
            val (x, y) = queue.removeFirst()
            if (x == goalTx && y == goalTy) return true
            for ((dx, dy) in dirs) {
                val nx = x + dx
                val ny = y + dy
                if (nx in 0 until ZONE_WIDTH && ny in 0 until ZONE_HEIGHT && !visited[ny][nx]) {
                    if (isTileWalkable(tileAt(state, nx, ny), bridgeLowered)) {
                        visited[ny][nx] = true
                        queue.add(nx to ny)
                    }
                }
            }
        }
        return false
    }

    fun stepZone(
        state: InteractiveZoneState,
        joyX: Float,
        joyY: Float,
        deltaSec: Float
    ): InteractiveZoneState {
        val dt = deltaSec.coerceIn(0f, 0.08f)
        var vx = joyX
        var vy = joyY
        var nextTapTarget = state.tapTarget

        if (hypot(vx, vy) < 0.14f && nextTapTarget != null) {
            val dx = nextTapTarget.first - state.heroX
            val dy = nextTapTarget.second - state.heroY
            val dist = hypot(dx, dy)
            if (dist <= 0.18f) {
                nextTapTarget = null
                vx = 0f
                vy = 0f
            } else {
                vx = dx / dist
                vy = dy / dist
            }
        } else if (hypot(vx, vy) >= 0.14f) {
            nextTapTarget = null
        }

        val mag = hypot(vx, vy)
        val isMoving = mag >= 0.14f
        var nx = state.heroX
        var ny = state.heroY
        var nextFacing = state.heroFacing
        var nextCycle = state.walkCycle
        var bridgeBlockMsg: String? = null

        if (isMoving) {
            val normX = vx / mag.coerceAtLeast(1f)
            val normY = vy / mag.coerceAtLeast(1f)
            val speed = HERO_SPEED * mag.coerceAtMost(1f)
            val candX = nx + normX * speed * dt
            if (isPointWalkable(state, candX, ny)) {
                nx = candX
            } else if (tileAt(state, candX.toInt(), ny.toInt()) == ZoneTile.BRIDGE_PLANK &&
                state.bridgeFillProgress < 0.99f
            ) {
                bridgeBlockMsg = "Complete a winch station to lower this bridge span first!"
            }

            val candY = ny + normY * speed * dt
            if (isPointWalkable(state, nx, candY)) {
                ny = candY
            } else if (tileAt(state, nx.toInt(), candY.toInt()) == ZoneTile.BRIDGE_PLANK &&
                state.bridgeFillProgress < 0.99f
            ) {
                bridgeBlockMsg = "Complete a winch station to lower this bridge span first!"
            }

            nextFacing = facingFromVector(normX, normY, state.heroFacing)
            nextCycle = state.walkCycle + dt * 11f
        }

        return state.copy(
            heroX = nx,
            heroY = ny,
            heroFacing = nextFacing,
            isMoving = isMoving,
            walkCycle = nextCycle,
            tapTarget = nextTapTarget,
            toastText = bridgeBlockMsg ?: state.toastText
        )
    }

    fun findNearestWorkstation(state: InteractiveZoneState): ZoneWorkstation? {
        return state.workstations
            .filter { it.id !in state.completedStationIds }
            .map { ws -> ws to hypot(ws.x - state.heroX, ws.y - state.heroY) }
            .filter { (_, d) -> d <= INTERACT_RADIUS }
            .minByOrNull { (_, d) -> d }
            ?.first
    }

    fun completeWorkstation(
        state: InteractiveZoneState,
        stationId: String
    ): WorkstationActionOutcome? {
        val station = state.workstations.firstOrNull { it.id == stationId } ?: return null
        if (stationId in state.completedStationIds) return null

        val newCompleted = state.completedStationIds + stationId
        val nextBridgeProgress = if (station.lowersBridgePlanks || state.bridgeFillProgress >= 0.99f) {
            1f
        } else {
            state.bridgeFillProgress
        }
        val nextWaterProgress = if (station.fillsWaterChannel || state.waterFillProgress >= 0.99f) {
            1f
        } else {
            state.waterFillProgress
        }
        val clearedNow = newCompleted.size >= state.workstations.size
        val nextLoot = if (station.rewardLoot != null) {
            state.stationLootEarned + station.rewardLoot
        } else {
            state.stationLootEarned
        }

        val updated = state.copy(
            completedStationIds = newCompleted,
            bridgeFillProgress = nextBridgeProgress,
            waterFillProgress = nextWaterProgress,
            stationNectarEarned = state.stationNectarEarned + station.rewardNectar,
            stationExpEarned = state.stationExpEarned + station.rewardExp,
            stationMagicEarned = state.stationMagicEarned + station.rewardMagic,
            stationLootEarned = nextLoot,
            zoneCleared = clearedNow,
            toastText = buildString {
                append("${station.title} completed! +${station.rewardExp} XP, +${station.rewardNectar} Nectar")
                if (station.rewardLoot != null) {
                    append(", +1 ${station.rewardLoot.displayName}")
                }
                if (station.lowersBridgePlanks) {
                    append(" — Bridge planks lowered!")
                }
                if (station.fillsWaterChannel) {
                    append(" — Spring water flowing!")
                }
            }
        )

        return WorkstationActionOutcome(
            updatedState = updated,
            station = station,
            justClearedZone = clearedNow && !state.zoneCleared,
            awardedNectar = station.rewardNectar + if (clearedNow) state.completionBonusNectar else 0,
            awardedExp = station.rewardExp + if (clearedNow) state.completionBonusExp else 0,
            awardedMagic = station.rewardMagic + if (clearedNow) state.completionBonusMagic else 0,
            awardedLoot = station.rewardLoot
        )
    }

    fun forChapterChoice(biome: BiomeNode, choice: StoryChoice): InteractiveZoneState {
        val creature = JungleLoreCatalog.creatureById(biome.creatureId)
        return when (choice.id) {
            "dew_splash" -> InteractiveZoneState(
                zoneId = "zone_${choice.id}",
                title = "Floating Hydro-Pad Valve Deck",
                subtitle = "${biome.name} · Tech Maintenance Wing",
                objectiveHint = "Walk across the floating lily pads and turn all 3 hydro-pad valves to restore water flow!",
                kind = ZoneKind.CHAPTER_ACTION,
                companionCreatureId = creature.id,
                companionName = creature.name,
                companionX = 2.5f,
                companionY = 7.2f,
                terrainRows = listOf(
                    "TTTTTTTTTTTTTT",
                    "T~AAA~~~~AAA~T",
                    "T~AWWPPPPWW~~T",
                    "T~~WW~PP~WW~~T",
                    "T~~PP~WW~PP~~T",
                    "T~~WWPPPPWW~~T",
                    "T~~WW~PP~WW~~T",
                    "T~PPP~WW~PPP~T",
                    "T~GGGPPPPGGG~T",
                    "TTTTTTTTTTTTTT"
                ),
                heroX = 7.0f,
                heroY = 8.2f,
                workstations = listOf(
                    ZoneWorkstation(
                        id = "dew_ws_reed",
                        title = "Reed Intake Strainer",
                        actionVerb = "Clear Reed Clog",
                        completedLabel = "Strainer Cleared",
                        workSummary = "Sweeps floating moss away from the west lagoon intake valve.",
                        visual = WorkstationVisual.CABLE_WINCH,
                        x = 3.5f,
                        y = 5.5f,
                        rewardNectar = 6,
                        rewardExp = 15,
                        rewardMagic = 5,
                        rewardLoot = LootItemType.SUNBERRY_TONIC
                    ),
                    ZoneWorkstation(
                        id = "dew_ws_valve",
                        title = "Floating Hydro-Pad Valve",
                        actionVerb = "Adjust Hydro-Pad Valve",
                        completedLabel = "Hydro-Valve Tuned",
                        workSummary = "Rotates the brass hydro-pad wheel so crystal water surges through all aqueduct channels!",
                        visual = WorkstationVisual.VALVE_WHEEL,
                        x = 7.0f,
                        y = 4.5f,
                        rewardNectar = 8,
                        rewardExp = 20,
                        rewardMagic = 8,
                        fillsWaterChannel = true
                    ),
                    ZoneWorkstation(
                        id = "dew_ws_mist",
                        title = "Mist Rainbow Nozzle",
                        actionVerb = "Calibrate Mist Jet",
                        completedLabel = "Mist Rainbow Active",
                        workSummary = "Aligns the eastern hydro-nozzle to shower Dewdrake in prism dew!",
                        visual = WorkstationVisual.MIST_CALIBRATOR,
                        x = 10.5f,
                        y = 5.5f,
                        rewardNectar = 6,
                        rewardExp = 15,
                        rewardMagic = 7,
                        rewardLoot = LootItemType.MANA_VIAL
                    )
                ),
                completionBonusNectar = choice.rewardNectar,
                completionBonusExp = choice.rewardExp,
                completionBonusMagic = 10,
                completionBonusMoonstones = 1,
                completionBonusLoot = LootItemType.MANA_VIAL,
                chapterBiome = biome,
                chapterChoice = choice
            )

            "luma_greet" -> InteractiveZoneState(
                zoneId = "zone_${choice.id}",
                title = "Hollow Lantern Relay Terrace",
                subtitle = "${biome.name} · Solari Beacon Deck",
                objectiveHint = "Walk along the mossy terrace to calibrate all 3 solar spore-lanterns with Lumafern.",
                kind = ZoneKind.CHAPTER_ACTION,
                companionCreatureId = creature.id,
                companionName = creature.name,
                companionX = 2.5f,
                companionY = 6.5f,
                terrainRows = listOf(
                    "TTTTTTTTTTTTTT",
                    "TGGPPPPPPPPGGT",
                    "TGPWWGGGGWWGPT",
                    "TGPGGPPPPGGGPT",
                    "T~~GGPWWPGG~~T",
                    "T~~PPPWWPPP~~T",
                    "TGGPPGGGGPPGGT",
                    "TGGGGPPPPGGGGT",
                    "TGGGGGPPGGGGGT",
                    "TTTTTTTTTTTTTT"
                ),
                heroX = 7.0f,
                heroY = 8.0f,
                workstations = listOf(
                    ZoneWorkstation(
                        id = "luma_ws_west",
                        title = "West Spore Relay",
                        actionVerb = "Polish Amber Lens",
                        completedLabel = "West Relay Lit",
                        workSummary = "Synchronizes warm spore light along the western mossy arch.",
                        visual = WorkstationVisual.SOLAR_MIRROR,
                        x = 3.5f,
                        y = 5.5f,
                        rewardNectar = 5,
                        rewardExp = 12,
                        rewardMagic = 5
                    ),
                    ZoneWorkstation(
                        id = "luma_ws_center",
                        title = "Antler Harmonic Coil",
                        actionVerb = "Tune Lantern Coil",
                        completedLabel = "Coil Resonating",
                        workSummary = "Matches Lumafern's golden hum across the central wooden deck.",
                        visual = WorkstationVisual.RESONANCE_CHIME,
                        x = 7.0f,
                        y = 4.5f,
                        rewardNectar = 6,
                        rewardExp = 15,
                        rewardMagic = 6,
                        rewardLoot = LootItemType.SUNBERRY_TONIC
                    ),
                    ZoneWorkstation(
                        id = "luma_ws_east",
                        title = "East Firefly Brazier",
                        actionVerb = "Ignite Solar Brazier",
                        completedLabel = "Brazier Aglow",
                        workSummary = "Releases welcoming firefly sparks toward Moss Hollow's canopy.",
                        visual = WorkstationVisual.SPIRIT_BRAZIER,
                        x = 10.5f,
                        y = 2.5f,
                        rewardNectar = 6,
                        rewardExp = 15,
                        rewardMagic = 6
                    )
                ),
                completionBonusNectar = choice.rewardNectar,
                completionBonusExp = choice.rewardExp,
                completionBonusMagic = 8,
                completionBonusMoonstones = 1,
                completionBonusLoot = LootItemType.SUNBERRY_TONIC,
                chapterBiome = biome,
                chapterChoice = choice
            )

            "owl_listen" -> InteractiveZoneState(
                zoneId = "zone_${choice.id}",
                title = "High Observatory Optic Deck",
                subtitle = "${biome.name} · Starlight Lens Wing",
                objectiveHint = "Calibrate the brass starlight lens, plumb counterweight, and prism dial with Aureowl.",
                kind = ZoneKind.CHAPTER_ACTION,
                companionCreatureId = creature.id,
                companionName = creature.name,
                companionX = 2.5f,
                companionY = 7.0f,
                terrainRows = listOf(
                    "TTTTTTTTTTTTTT",
                    "TSGGGGGGGGGGST",
                    "T~PPWWPPWWPP~T",
                    "T~PPGGGGGGPP~T",
                    "T~PPPPWWPPPP~T",
                    "T~~PPPWWPPP~~T",
                    "T~~PPGGGGPP~~T",
                    "TSGPPPPPPPPG~T",
                    "TGGGGPPPPGGGGT",
                    "TTTTTTTTTTTTTT"
                ),
                heroX = 7.0f,
                heroY = 8.0f,
                workstations = listOf(
                    ZoneWorkstation(
                        id = "owl_ws_gear",
                        title = "Brass Azimuth Gear",
                        actionVerb = "Oil Brass Gears",
                        completedLabel = "Gears Aligned",
                        workSummary = "Frees the canopy spyglass rotation ring to track Orion.",
                        visual = WorkstationVisual.VALVE_WHEEL,
                        x = 3.5f,
                        y = 4.5f,
                        rewardNectar = 7,
                        rewardExp = 18,
                        rewardMagic = 6
                    ),
                    ZoneWorkstation(
                        id = "owl_ws_lens",
                        title = "Starlight Prism Mirror",
                        actionVerb = "Focus Starlight Lens",
                        completedLabel = "Optics Focused",
                        workSummary = "Brings distant sky-bridges and twin orchid charms into razor clarity.",
                        visual = WorkstationVisual.SOLAR_MIRROR,
                        x = 7.0f,
                        y = 4.5f,
                        rewardNectar = 8,
                        rewardExp = 20,
                        rewardMagic = 8,
                        rewardLoot = LootItemType.MANA_VIAL
                    ),
                    ZoneWorkstation(
                        id = "owl_ws_chime",
                        title = "Wind-Vane Chime Mast",
                        actionVerb = "Steady Sight Mast",
                        completedLabel = "Sight Mast Steady",
                        workSummary = "Locks the observatory sightline over the eastern orchard peaks.",
                        visual = WorkstationVisual.STORM_BELL,
                        x = 10.5f,
                        y = 2.5f,
                        rewardNectar = 7,
                        rewardExp = 16,
                        rewardMagic = 8
                    )
                ),
                completionBonusNectar = choice.rewardNectar,
                completionBonusExp = choice.rewardExp,
                completionBonusMagic = 12,
                completionBonusMoonstones = 1,
                completionBonusLoot = LootItemType.SUNFIRE_CHARM,
                chapterBiome = biome,
                chapterChoice = choice
            )

            "toad_chime" -> InteractiveZoneState(
                zoneId = "zone_${choice.id}",
                title = "Crystal Root Resonance Vault",
                subtitle = "${biome.name} · Quartz Chime Alcove",
                objectiveHint = "Tap and tune all 3 bioluminescent quartz chime crystals across the cavern vault.",
                kind = ZoneKind.CHAPTER_ACTION,
                companionCreatureId = creature.id,
                companionName = creature.name,
                companionX = 2.5f,
                companionY = 7.2f,
                terrainRows = listOf(
                    "SSSSSSSSSSSSSS",
                    "SGGPPPPPPPPGGS",
                    "SGPWWGGGGWWGPS",
                    "SGP~~PPPP~~GPS",
                    "S~AAAPWWPAAA~S",
                    "SGPPPWWWWPPPGS",
                    "SGPGGPPPPGGGPS",
                    "SGGGGPPPPGGGGS",
                    "SGGGGGPPGGGGGS",
                    "SSSSSSSSSSSSSS"
                ),
                heroX = 7.0f,
                heroY = 8.0f,
                workstations = listOf(
                    ZoneWorkstation(
                        id = "toad_ws_left",
                        title = "Basso Quartz Crystal",
                        actionVerb = "Strike Low Quartz",
                        completedLabel = "Low Note Hummed",
                        workSummary = "Deep violet chimes rumble softly under the ancient banyan roots.",
                        visual = WorkstationVisual.RESONANCE_CHIME,
                        x = 3.5f,
                        y = 2.5f,
                        rewardNectar = 8,
                        rewardExp = 18,
                        rewardMagic = 8
                    ),
                    ZoneWorkstation(
                        id = "toad_ws_pool",
                        title = "Echo Pool Sluice",
                        actionVerb = "Harmonize Cavern Pool",
                        completedLabel = "Pool Rippling",
                        workSummary = "Releases mineral spring water past Glimmertoad's acoustic bowl!",
                        visual = WorkstationVisual.VALVE_WHEEL,
                        x = 7.0f,
                        y = 5.5f,
                        rewardNectar = 8,
                        rewardExp = 22,
                        rewardMagic = 10,
                        rewardLoot = LootItemType.MANA_VIAL,
                        fillsWaterChannel = true
                    ),
                    ZoneWorkstation(
                        id = "toad_ws_right",
                        title = "Treble Fungi Chime",
                        actionVerb = "Tune High Mushrooms",
                        completedLabel = "Harmonic Chord Set",
                        workSummary = "Completes the triad chord that bathes Crystal Cavern in turquoise light.",
                        visual = WorkstationVisual.RESONANCE_CHIME,
                        x = 10.5f,
                        y = 2.5f,
                        rewardNectar = 8,
                        rewardExp = 20,
                        rewardMagic = 9
                    )
                ),
                completionBonusNectar = choice.rewardNectar,
                completionBonusExp = choice.rewardExp,
                completionBonusMagic = 14,
                completionBonusMoonstones = 1,
                completionBonusLoot = LootItemType.MANA_VIAL,
                chapterBiome = biome,
                chapterChoice = choice
            )

            "moth_gift" -> InteractiveZoneState(
                zoneId = "zone_${choice.id}",
                title = "Starbloom Silk Loom Walk",
                subtitle = "${biome.name} · Canopy Weaver Loom",
                objectiveHint = "Thread pollen spools, align starlight prisms, and ignite the tapestry brazier!",
                kind = ZoneKind.CHAPTER_ACTION,
                companionCreatureId = creature.id,
                companionName = creature.name,
                companionX = 2.5f,
                companionY = 7.2f,
                terrainRows = listOf(
                    "TTTTTTTTTTTTTT",
                    "T~GGWWPPWWGG~T",
                    "T~PPWWPPWWPP~T",
                    "T~~PPPPPPPP~~T",
                    "T~~PWWPPWWP~~T",
                    "T~~PPPWWPPP~~T",
                    "T~GPPGGGGPPG~T",
                    "T~GGPPPPPPGG~T",
                    "TGGGGPPPPGGGGT",
                    "TTTTTTTTTTTTTT"
                ),
                heroX = 7.0f,
                heroY = 8.0f,
                workstations = listOf(
                    ZoneWorkstation(
                        id = "moth_ws_spool",
                        title = "Golden Silk Spool",
                        actionVerb = "Thread Pollen Warp",
                        completedLabel = "Warp Threaded",
                        workSummary = "Strings luminous moonflower threads across Velvetmoth's high loom.",
                        visual = WorkstationVisual.CABLE_WINCH,
                        x = 3.5f,
                        y = 2.5f,
                        rewardNectar = 9,
                        rewardExp = 20,
                        rewardMagic = 10
                    ),
                    ZoneWorkstation(
                        id = "moth_ws_prism",
                        title = "Botanical Prism Frame",
                        actionVerb = "Set Starbloom Mirror",
                        completedLabel = "Tapestry Lit",
                        workSummary = "Focuses silver moonlight onto the woven fern tapestry.",
                        visual = WorkstationVisual.SOLAR_MIRROR,
                        x = 7.0f,
                        y = 4.5f,
                        rewardNectar = 10,
                        rewardExp = 24,
                        rewardMagic = 10,
                        rewardLoot = LootItemType.SUNFIRE_CHARM
                    ),
                    ZoneWorkstation(
                        id = "moth_ws_nectar",
                        title = "Pollen Diffuser Vase",
                        actionVerb = "Mist Starry Pollen",
                        completedLabel = "Pollen Radiant",
                        workSummary = "Wakes sleeping blossom motifs along the entire Starbloom Treehouse.",
                        visual = WorkstationVisual.BOTANICAL_POT,
                        x = 10.5f,
                        y = 2.5f,
                        rewardNectar = 9,
                        rewardExp = 20,
                        rewardMagic = 10
                    )
                ),
                completionBonusNectar = choice.rewardNectar,
                completionBonusExp = choice.rewardExp,
                completionBonusMagic = 15,
                completionBonusMoonstones = 1,
                completionBonusLoot = LootItemType.SUNFIRE_CHARM,
                chapterBiome = biome,
                chapterChoice = choice
            )

            else -> InteractiveZoneState( // Ch. 6 "turtle_rest" & fallback
                zoneId = "zone_${choice.id}",
                title = "Heartwood Covenant Courtyard",
                subtitle = "${biome.name} · Gathering of All Realms",
                objectiveHint = "Prepare ceremonial sun-tea, tend the unity saplings, and kindle the covenant hearth!",
                kind = ZoneKind.CHAPTER_ACTION,
                companionCreatureId = creature.id,
                companionName = creature.name,
                companionX = 2.5f,
                companionY = 7.2f,
                terrainRows = listOf(
                    "TTTTTTTTTTTTTT",
                    "TSGGPPPPPPGGST",
                    "TGPPWWWWWWPPGT",
                    "TGP~PPWWPP~PGT",
                    "TGP~PPWWPP~PGT",
                    "TGPPWWWWWWPPGT",
                    "TGGPPPPPPPPGGT",
                    "TGGGGPPPPGGGGT",
                    "TGGGGGPPGGGGGT",
                    "TTTTTTTTTTTTTT"
                ),
                heroX = 7.0f,
                heroY = 8.0f,
                workstations = listOf(
                    ZoneWorkstation(
                        id = "turtle_ws_tea",
                        title = "Sunbeam Tea Kettle",
                        actionVerb = "Steep Unity Tea",
                        completedLabel = "Tea Shared",
                        workSummary = "Brews fragrant honeydew and mint infusions for travelers from every realm.",
                        visual = WorkstationVisual.BOTANICAL_POT,
                        x = 3.5f,
                        y = 5.5f,
                        rewardNectar = 10,
                        rewardExp = 25,
                        rewardMagic = 12,
                        rewardLoot = LootItemType.SUNBERRY_TONIC
                    ),
                    ZoneWorkstation(
                        id = "turtle_ws_hearth",
                        title = "Covenant Ember Hearth",
                        actionVerb = "Kindle Harmony Flame",
                        completedLabel = "Hearth Glowing",
                        workSummary = "Warms the Heartwood Courtyard under Brambleback's ancient shell.",
                        visual = WorkstationVisual.SPIRIT_BRAZIER,
                        x = 7.0f,
                        y = 3.5f,
                        rewardNectar = 12,
                        rewardExp = 28,
                        rewardMagic = 14
                    ),
                    ZoneWorkstation(
                        id = "turtle_ws_crest",
                        title = "Allied Banner Chime",
                        actionVerb = "Sound United Chime",
                        completedLabel = "Banners Raised",
                        workSummary = "Rings a clear tone linking Solari, Riverbend, Cloud-Terrace, and Elderwood.",
                        visual = WorkstationVisual.STORM_BELL,
                        x = 10.5f,
                        y = 5.5f,
                        rewardNectar = 10,
                        rewardExp = 25,
                        rewardMagic = 14,
                        rewardLoot = LootItemType.CANOPY_CREST
                    )
                ),
                completionBonusNectar = choice.rewardNectar,
                completionBonusExp = choice.rewardExp,
                completionBonusMagic = 18,
                completionBonusMoonstones = 2,
                completionBonusLoot = LootItemType.CANOPY_CREST,
                chapterBiome = biome,
                chapterChoice = choice
            )
        }
    }

    fun forMission(mission: CommunityMission): InteractiveZoneState {
        val companionCreature = mission.opponent
        return when (mission.id) {
            "mission_sunbridge" -> InteractiveZoneState(
                zoneId = "zone_${mission.id}",
                title = "Lower Sun-Bridge Chasm & Winch Pier",
                subtitle = "Community Mission 1 · ${mission.communityProject}",
                objectiveHint = "Anchor north pier winches & splice cables to drop the Sun-Bridge planks, then cross south to lock the pier!",
                kind = ZoneKind.COMMUNITY_MISSION,
                companionCreatureId = companionCreature.id,
                companionName = companionCreature.name,
                companionX = 11.2f,
                companionY = 7.8f,
                terrainRows = listOf(
                    "TTTTTTTTTTTTTT",
                    "TGGGGPPPPGGGGT",
                    "TGWWPPPPPPWWGT",
                    "TGWWPPPPPPWWGT",
                    "T~~~~PWWP~~~~T",
                    "T~~~~~BB~~~~~T",
                    "T~~~~~BB~~~~~T",
                    "TGPPPPWWPPPPGT",
                    "TGGGGPPPPGGGGT",
                    "TTTTTTTTTTTTTT"
                ),
                heroX = 7.0f,
                heroY = 2.5f,
                workstations = listOf(
                    ZoneWorkstation(
                        id = "sunbridge_ws_anchor",
                        title = "North Pier Anchor",
                        actionVerb = "Bolt Sky-Pier Timber",
                        completedLabel = "Pier Bolted",
                        workSummary = "Secures heavy mahogany pilings along the northern canyon lip.",
                        visual = WorkstationVisual.CABLE_WINCH,
                        x = 3.0f,
                        y = 3.0f,
                        rewardNectar = 8,
                        rewardExp = 20,
                        rewardMagic = 6,
                        rewardLoot = LootItemType.SUNBERRY_TONIC
                    ),
                    ZoneWorkstation(
                        id = "sunbridge_ws_winch",
                        title = "Solar Cable Capstan",
                        actionVerb = "Splice Cables & Drop Bridge",
                        completedLabel = "Bridge Lowered!",
                        workSummary = "Winds copper-vine cables tight — planks thunder down across the chasm!",
                        visual = WorkstationVisual.VALVE_WHEEL,
                        x = 11.0f,
                        y = 3.0f,
                        rewardNectar = 10,
                        rewardExp = 25,
                        rewardMagic = 8,
                        lowersBridgePlanks = true
                    ),
                    ZoneWorkstation(
                        id = "sunbridge_ws_south_lock",
                        title = "South Landing Tension Lock",
                        actionVerb = "Cross Span & Lock South Pier",
                        completedLabel = "Span Locked Solid",
                        workSummary = "Walked across the lowered Sun-Bridge to bolt the Dew Lagoon landing!",
                        visual = WorkstationVisual.SOLAR_MIRROR,
                        x = 7.0f,
                        y = 7.5f,
                        rewardNectar = 10,
                        rewardExp = 25,
                        rewardMagic = 8,
                        rewardLoot = LootItemType.MANA_VIAL
                    )
                ),
                completionBonusNectar = 10,
                completionBonusExp = 20,
                completionBonusMagic = 8,
                completionBonusMoonstones = 1,
                completionBonusLoot = LootItemType.SUNBERRY_TONIC,
                mission = mission
            )

            "mission_glade_sanctum" -> InteractiveZoneState(
                zoneId = "zone_${mission.id}",
                title = "Sunlight Mirror Glade Yard",
                subtitle = "Community Mission 2 · ${mission.communityProject}",
                objectiveHint = "Align the healing quartz mirrors and tend the clinic herb basin before meeting the Prism-Winged Stag!",
                kind = ZoneKind.COMMUNITY_MISSION,
                companionCreatureId = companionCreature.id,
                companionName = companionCreature.name,
                companionX = 7.0f,
                companionY = 2.0f,
                terrainRows = listOf(
                    "TTTTTTTTTTTTTT",
                    "TSGGPPPPPPGGST",
                    "TGPPWWPPWWPPGT",
                    "TGP~GGPPGG~PGT",
                    "TGP~PPPPPP~PGT",
                    "TGP~PPWWPP~PGT",
                    "TGPPPPWWPPPPGT",
                    "TGGGGPPPPGGGGT",
                    "TGGGGGPPGGGGGT",
                    "TTTTTTTTTTTTTT"
                ),
                heroX = 7.0f,
                heroY = 8.0f,
                workstations = listOf(
                    ZoneWorkstation(
                        id = "glade_ws_west_mirror",
                        title = "Dawn Quartz Mirror",
                        actionVerb = "Angle West Mirror",
                        completedLabel = "Dawn Light Caught",
                        workSummary = "Catches golden canopy beams across the healing ward floor.",
                        visual = WorkstationVisual.SOLAR_MIRROR,
                        x = 4.5f,
                        y = 2.5f,
                        rewardNectar = 9,
                        rewardExp = 22,
                        rewardMagic = 8
                    ),
                    ZoneWorkstation(
                        id = "glade_ws_herb_basin",
                        title = "Solar Clinic Herb Tray",
                        actionVerb = "Blend Healing Tonics",
                        completedLabel = "Clinic Stocked",
                        workSummary = "Prepares warm healing Poultices & Sunberry Tonics for Solari families.",
                        visual = WorkstationVisual.BOTANICAL_POT,
                        x = 7.0f,
                        y = 5.5f,
                        rewardNectar = 10,
                        rewardExp = 24,
                        rewardMagic = 8,
                        rewardLoot = LootItemType.SUNBERRY_TONIC
                    ),
                    ZoneWorkstation(
                        id = "glade_ws_east_mirror",
                        title = "Prism Beacon Lens",
                        actionVerb = "Calibrate Prism Focus",
                        completedLabel = "Beacon Radiant",
                        workSummary = "Bathes the Mirror Glade Sanctum in a dome of restorative starlight!",
                        visual = WorkstationVisual.SOLAR_MIRROR,
                        x = 9.5f,
                        y = 2.5f,
                        rewardNectar = 10,
                        rewardExp = 26,
                        rewardMagic = 10,
                        rewardLoot = LootItemType.MANA_VIAL
                    )
                ),
                completionBonusNectar = 12,
                completionBonusExp = 25,
                completionBonusMagic = 10,
                completionBonusMoonstones = 1,
                completionBonusLoot = LootItemType.MANA_VIAL,
                mission = mission
            )

            "mission_aqueduct_gate" -> InteractiveZoneState(
                zoneId = "zone_${mission.id}",
                title = "Sunken Aqueduct & East Canopy Span",
                subtitle = "Community Mission 3 · ${mission.communityProject}",
                objectiveHint = "Flush the western sluice, open the main cistern valve to fill water channels & lower the East Span, then cross east!",
                kind = ZoneKind.COMMUNITY_MISSION,
                companionCreatureId = companionCreature.id,
                companionName = companionCreature.name,
                companionX = 11.5f,
                companionY = 3.5f,
                terrainRows = listOf(
                    "TTTTTTTTTTTTTT",
                    "T~AAAA~AAAAA~T",
                    "TGPPPP~PPPPGGT",
                    "TGWWPP~PPWWGGT",
                    "TGPPPPBPPPPGGT",
                    "TGPPPPBPPPPWGT",
                    "TGWWPP~PPPPGGT",
                    "TGPGGP~PGGPPGT",
                    "TGGGGP~PGGGGGT",
                    "TTTTTTTTTTTTTT"
                ),
                heroX = 2.5f,
                heroY = 8.0f,
                workstations = listOf(
                    ZoneWorkstation(
                        id = "aq_ws_debris",
                        title = "Western Sluice Grate",
                        actionVerb = "Clear Root Debris",
                        completedLabel = "Grate Cleared",
                        workSummary = "Clears tangled iron-roots choking the Solari cistern intake.",
                        visual = WorkstationVisual.CABLE_WINCH,
                        x = 3.0f,
                        y = 3.5f,
                        rewardNectar = 10,
                        rewardExp = 24,
                        rewardMagic = 8,
                        rewardLoot = LootItemType.SUNBERRY_TONIC
                    ),
                    ZoneWorkstation(
                        id = "aq_ws_master_valve",
                        title = "East Span Hydro-Valve",
                        actionVerb = "Open Cistern & Lower East Bridge",
                        completedLabel = "Cistern Flowing & East Span Down!",
                        workSummary = "Clean water surges down the aqueducts, lowering the East Canopy Sun-Bridge!",
                        visual = WorkstationVisual.VALVE_WHEEL,
                        x = 5.0f,
                        y = 5.5f,
                        rewardNectar = 12,
                        rewardExp = 30,
                        rewardMagic = 12,
                        lowersBridgePlanks = true,
                        fillsWaterChannel = true
                    ),
                    ZoneWorkstation(
                        id = "aq_ws_east_gauge",
                        title = "Eastern Terrace Pressure Dial",
                        actionVerb = "Cross Bridge & Lock Pressure",
                        completedLabel = "East Terrace Irrigated",
                        workSummary = "Crossed the lowered East Sun-Bridge to balance water flow toward Moonflower Orchard!",
                        visual = WorkstationVisual.MIST_CALIBRATOR,
                        x = 10.5f,
                        y = 5.5f,
                        rewardNectar = 12,
                        rewardExp = 28,
                        rewardMagic = 10,
                        rewardLoot = LootItemType.SUNFIRE_CHARM
                    )
                ),
                completionBonusNectar = 14,
                completionBonusExp = 30,
                completionBonusMagic = 12,
                completionBonusMoonstones = 1,
                completionBonusLoot = LootItemType.MANA_VIAL,
                mission = mission
            )

            "mission_orchard_grove" -> InteractiveZoneState(
                zoneId = "zone_${mission.id}",
                title = "Moonflower Orchard Nursery Terraces",
                subtitle = "Community Mission 4 · ${mission.communityProject}",
                objectiveHint = "Irrigate the nursery bed, plant bioluminescent saplings, and pollinate the star-petals!",
                kind = ZoneKind.COMMUNITY_MISSION,
                companionCreatureId = companionCreature.id,
                companionName = companionCreature.name,
                companionX = 2.5f,
                companionY = 7.0f,
                terrainRows = listOf(
                    "TTTTTTTTTTTTTT",
                    "TGGAAA~~AAAGGT",
                    "TGPPPPPPPPPPGT",
                    "TGPPWWGGWWPPGT",
                    "TGPPGGGGGGPPGT",
                    "TGPPPPWWPPPPGT",
                    "TGPPGGPPGGPPGT",
                    "TGGGGPPPPGGGGT",
                    "TGGGGGPPGGGGGT",
                    "TTTTTTTTTTTTTT"
                ),
                heroX = 7.0f,
                heroY = 8.0f,
                workstations = listOf(
                    ZoneWorkstation(
                        id = "orchard_ws_water",
                        title = "Terrace Irrigation Gate",
                        actionVerb = "Open Orchard Troughs",
                        completedLabel = "Beds Irrigated",
                        workSummary = "Guides mineral mist across the golden nursery soil.",
                        visual = WorkstationVisual.VALVE_WHEEL,
                        x = 4.5f,
                        y = 3.5f,
                        rewardNectar = 12,
                        rewardExp = 28,
                        rewardMagic = 10,
                        fillsWaterChannel = true
                    ),
                    ZoneWorkstation(
                        id = "orchard_ws_plant",
                        title = "Moonflower Sapling Row",
                        actionVerb = "Sow Glowing Saplings",
                        completedLabel = "Saplings Blooming",
                        workSummary = "Sets star-petal shoots firmly into enriched Solari compost.",
                        visual = WorkstationVisual.BOTANICAL_POT,
                        x = 7.0f,
                        y = 5.5f,
                        rewardNectar = 14,
                        rewardExp = 32,
                        rewardMagic = 12,
                        rewardLoot = LootItemType.SUNBERRY_TONIC
                    ),
                    ZoneWorkstation(
                        id = "orchard_ws_trellis",
                        title = "Pollen Lattice Arch",
                        actionVerb = "Raise Star-Petal Trellis",
                        completedLabel = "Orchard Radiant",
                        workSummary = "Lifts glowing vine arbors so year-round berries flourish for every tribe.",
                        visual = WorkstationVisual.MIST_CALIBRATOR,
                        x = 9.5f,
                        y = 3.5f,
                        rewardNectar = 12,
                        rewardExp = 30,
                        rewardMagic = 12,
                        rewardLoot = LootItemType.MANA_VIAL
                    )
                ),
                completionBonusNectar = 16,
                completionBonusExp = 32,
                completionBonusMagic = 14,
                completionBonusMoonstones = 1,
                completionBonusLoot = LootItemType.SUNFIRE_CHARM,
                mission = mission
            )

            "mission_weather_spires" -> InteractiveZoneState(
                zoneId = "zone_${mission.id}",
                title = "Sky-Bell Tempest Ridge",
                subtitle = "Community Mission 5 · ${mission.communityProject}",
                objectiveHint = "Winch the brass weather tower upright, tune the storm-bells, and ground the lightning rod!",
                kind = ZoneKind.COMMUNITY_MISSION,
                companionCreatureId = companionCreature.id,
                companionName = companionCreature.name,
                companionX = 2.5f,
                companionY = 7.0f,
                terrainRows = listOf(
                    "SSSSSSSSSSSSSS",
                    "S~~GGGGGGGG~~S",
                    "S~PPWWPPWWPP~S",
                    "S~PP~~PP~~PP~S",
                    "S~PPPPWWPPPP~S",
                    "S~~PPPWWPPP~~S",
                    "S~~PPGGGGPP~~S",
                    "SGGGGPPPPGGGGS",
                    "SGGGGGPPGGGGGS",
                    "SSSSSSSSSSSSSS"
                ),
                heroX = 7.0f,
                heroY = 8.0f,
                workstations = listOf(
                    ZoneWorkstation(
                        id = "spire_ws_hoist",
                        title = "Mast Hoist Capstan",
                        actionVerb = "Hoist Copper Spire",
                        completedLabel = "Spire Erected",
                        workSummary = "Locks the lightning deflection tower atop the windy sky ridge.",
                        visual = WorkstationVisual.CABLE_WINCH,
                        x = 4.5f,
                        y = 2.5f,
                        rewardNectar = 14,
                        rewardExp = 34,
                        rewardMagic = 14,
                        rewardLoot = LootItemType.MANA_VIAL
                    ),
                    ZoneWorkstation(
                        id = "spire_ws_bells",
                        title = "Resonant Storm-Bells",
                        actionVerb = "Anchor Storm-Bells",
                        completedLabel = "Storm-Bells Chime",
                        workSummary = "Attaches brass resonance chimes that scatter violent thunderheads.",
                        visual = WorkstationVisual.STORM_BELL,
                        x = 7.0f,
                        y = 4.5f,
                        rewardNectar = 15,
                        rewardExp = 36,
                        rewardMagic = 16,
                        rewardLoot = LootItemType.SUNFIRE_CHARM
                    ),
                    ZoneWorkstation(
                        id = "spire_ws_ground",
                        title = "Solar Prism Capacitor",
                        actionVerb = "Ground Tempest Grid",
                        completedLabel = "Grid Shielded",
                        workSummary = "Converts ambient lightning into clean glow for every Solari home!",
                        visual = WorkstationVisual.SOLAR_MIRROR,
                        x = 9.5f,
                        y = 2.5f,
                        rewardNectar = 14,
                        rewardExp = 34,
                        rewardMagic = 16
                    )
                ),
                completionBonusNectar = 18,
                completionBonusExp = 36,
                completionBonusMagic = 16,
                completionBonusMoonstones = 2,
                completionBonusLoot = LootItemType.CANOPY_CREST,
                mission = mission
            )

            "mission_final_crown" -> InteractiveZoneState(
                zoneId = "zone_${mission.id}",
                title = "Heartwood Seal Sanctuary",
                subtitle = "Final Expedition · ${mission.communityProject}",
                objectiveHint = "Unite Solari and all allied banners by kindling the 3 Heartwood Seals before facing Shadowroot Sovereign!",
                kind = ZoneKind.COMMUNITY_MISSION,
                companionCreatureId = companionCreature.id,
                companionName = companionCreature.name,
                companionX = 7.0f,
                companionY = 2.0f,
                terrainRows = listOf(
                    "SSSSSSSSSSSSSS",
                    "SSGGPPPPPPGGSS",
                    "SGPPWWPPWWPPGS",
                    "SGPP~PPPP~PPGS",
                    "S~PP~PWWP~PP~S",
                    "S~PPPPWWPPPP~S",
                    "SGPPGGGGGGPPGS",
                    "SGGGGPPPPGGGGS",
                    "SGGGGGPPGGGGGS",
                    "SSSSSSSSSSSSSS"
                ),
                heroX = 7.0f,
                heroY = 8.0f,
                workstations = listOf(
                    ZoneWorkstation(
                        id = "crown_ws_west",
                        title = "Seal of Solari Magic",
                        actionVerb = "Kindle Solar Seal",
                        completedLabel = "Solar Seal Burning",
                        workSummary = "Ignites ancient amber runes along the left sanctuary colonnade.",
                        visual = WorkstationVisual.SPIRIT_BRAZIER,
                        x = 4.5f,
                        y = 2.5f,
                        rewardNectar = 16,
                        rewardExp = 40,
                        rewardMagic = 18,
                        rewardLoot = LootItemType.SUNBERRY_TONIC
                    ),
                    ZoneWorkstation(
                        id = "crown_ws_center",
                        title = "United Canopy Pedestal",
                        actionVerb = "Unite All Realms' Crests",
                        completedLabel = "Allied Banner Unified",
                        workSummary = "Golden threads bind Solari, Riverbend, Cloud-Terrace, Elderwood & Quartz seals!",
                        visual = WorkstationVisual.RESONANCE_CHIME,
                        x = 7.0f,
                        y = 4.5f,
                        rewardNectar = 18,
                        rewardExp = 45,
                        rewardMagic = 20,
                        rewardLoot = LootItemType.MANA_VIAL
                    ),
                    ZoneWorkstation(
                        id = "crown_ws_east",
                        title = "Seal of Living Roots",
                        actionVerb = "Purify Bramble Lock",
                        completedLabel = "Root Seal Freed",
                        workSummary = "Drives dark thorns back, exposing the Shadowroot Sovereign's sanctuary arena!",
                        visual = WorkstationVisual.SPIRIT_BRAZIER,
                        x = 9.5f,
                        y = 2.5f,
                        rewardNectar = 16,
                        rewardExp = 40,
                        rewardMagic = 18,
                        rewardLoot = LootItemType.SUNFIRE_CHARM
                    )
                ),
                completionBonusNectar = 22,
                completionBonusExp = 50,
                completionBonusMagic = 22,
                completionBonusMoonstones = 2,
                completionBonusLoot = LootItemType.CANOPY_CREST,
                mission = mission
            )

            "patrol_briar_trail" -> InteractiveZoneState(
                zoneId = "zone_${mission.id}",
                title = "Briar Crossing Perimeter",
                subtitle = "Wild Patrol · ${mission.communityProject}",
                objectiveHint = "Repair trail lanterns, clear bramble traps, and harvest emergency Sunberry Tonics!",
                kind = ZoneKind.COMMUNITY_MISSION,
                companionCreatureId = companionCreature.id,
                companionName = companionCreature.name,
                companionX = 2.5f,
                companionY = 6.5f,
                terrainRows = listOf(
                    "TTTTTTTTTTTTTT",
                    "TGGGGPPPPGGGGT",
                    "TGPPWWPPWWPPGT",
                    "TGPGGPPPPGGPGT",
                    "T~~PPPWWPPP~~T",
                    "T~~PPPWWPPP~~T",
                    "TGPGGPPPPGGPGT",
                    "TGGGGPPPPGGGGT",
                    "TGGGGGPPGGGGGT",
                    "TTTTTTTTTTTTTT"
                ),
                heroX = 7.0f,
                heroY = 8.0f,
                workstations = listOf(
                    ZoneWorkstation(
                        id = "briar_ws_snare",
                        title = "Bramble Snare Bush",
                        actionVerb = "Trim Razor Vines",
                        completedLabel = "Path Cleared",
                        workSummary = "Opens the supply trail and uncovers ripe Sunberries.",
                        visual = WorkstationVisual.CABLE_WINCH,
                        x = 4.5f,
                        y = 2.5f,
                        rewardNectar = 8,
                        rewardExp = 18,
                        rewardMagic = 6,
                        rewardLoot = LootItemType.SUNBERRY_TONIC
                    ),
                    ZoneWorkstation(
                        id = "briar_ws_lantern",
                        title = "Perimeter Way-Beacon",
                        actionVerb = "Relight Patrol Beacon",
                        completedLabel = "Beacon Glowing",
                        workSummary = "Warns wandering caravans away from Thornscale dens.",
                        visual = WorkstationVisual.SPIRIT_BRAZIER,
                        x = 7.0f,
                        y = 4.5f,
                        rewardNectar = 8,
                        rewardExp = 20,
                        rewardMagic = 8
                    ),
                    ZoneWorkstation(
                        id = "briar_ws_cache",
                        title = "Forager Herb Patch",
                        actionVerb = "Gather Sunberry Pack",
                        completedLabel = "Satchel Resupplied",
                        workSummary = "Packs healing herbs cleanly before the wild skirmish.",
                        visual = WorkstationVisual.BOTANICAL_POT,
                        x = 9.5f,
                        y = 2.5f,
                        rewardNectar = 9,
                        rewardExp = 20,
                        rewardMagic = 8,
                        rewardLoot = LootItemType.SUNBERRY_TONIC
                    )
                ),
                completionBonusNectar = 10,
                completionBonusExp = 18,
                completionBonusMagic = 8,
                completionBonusMoonstones = 1,
                completionBonusLoot = LootItemType.SUNBERRY_TONIC,
                mission = mission
            )

            else -> InteractiveZoneState( // "patrol_aurora_peak" & fallback
                zoneId = "zone_${mission.id}",
                title = "Aurora Sun-Chime Summit",
                subtitle = "Wild Trial · ${mission.communityProject}",
                objectiveHint = "Align dawn mirrors, sound the sun-chimes, and gather highland Mana crystals!",
                kind = ZoneKind.COMMUNITY_MISSION,
                companionCreatureId = companionCreature.id,
                companionName = companionCreature.name,
                companionX = 2.5f,
                companionY = 7.0f,
                terrainRows = listOf(
                    "SSSSSSSSSSSSSS",
                    "SGGPPPPPPPPGGS",
                    "SGPWWGGGGWWGPS",
                    "S~PPPPWWPPPP~S",
                    "S~PPGGWWGGPP~S",
                    "S~PPPPPPPPPP~S",
                    "SGGPPGGGGPPGGS",
                    "SGGGGPPPPGGGGS",
                    "SGGGGGPPGGGGGS",
                    "SSSSSSSSSSSSSS"
                ),
                heroX = 7.0f,
                heroY = 8.0f,
                workstations = listOf(
                    ZoneWorkstation(
                        id = "aurora_ws_mirror",
                        title = "Summit Dawn Lens",
                        actionVerb = "Polish Summit Mirror",
                        completedLabel = "Lens gleaming",
                        workSummary = "Reflects sunrise across the highest Solari spires.",
                        visual = WorkstationVisual.SOLAR_MIRROR,
                        x = 3.5f,
                        y = 2.5f,
                        rewardNectar = 12,
                        rewardExp = 28,
                        rewardMagic = 12,
                        rewardLoot = LootItemType.MANA_VIAL
                    ),
                    ZoneWorkstation(
                        id = "aurora_ws_chime",
                        title = "Phoenix Sun-Chime",
                        actionVerb = "Ring Sun-Chime Challenge",
                        completedLabel = "Chime Ringing",
                        workSummary = "Summons radiant phoenix plumes down to the marble summit.",
                        visual = WorkstationVisual.STORM_BELL,
                        x = 7.0f,
                        y = 3.5f,
                        rewardNectar = 12,
                        rewardExp = 32,
                        rewardMagic = 14
                    ),
                    ZoneWorkstation(
                        id = "aurora_ws_brazier",
                        title = "Skyfire Crest Basin",
                        actionVerb = "Gather Highland Mana",
                        completedLabel = "Mana Condensed",
                        workSummary = "Condenses pure summit light into battle magic & loot.",
                        visual = WorkstationVisual.SPIRIT_BRAZIER,
                        x = 10.5f,
                        y = 2.5f,
                        rewardNectar = 14,
                        rewardExp = 30,
                        rewardMagic = 14,
                        rewardLoot = LootItemType.SUNFIRE_CHARM
                    )
                ),
                completionBonusNectar = 16,
                completionBonusExp = 30,
                completionBonusMagic = 15,
                completionBonusMoonstones = 1,
                completionBonusLoot = LootItemType.CANOPY_CREST,
                mission = mission
            )
        }
    }

    private fun facingFromVector(vx: Float, vy: Float, fallback: FacingDirection): FacingDirection {
        val angle = atan2(vy, vx)
        return when {
            angle in (-Math.PI / 4)..(Math.PI / 4) -> FacingDirection.RIGHT
            angle in (Math.PI / 4)..(3 * Math.PI / 4) -> FacingDirection.DOWN
            angle in (-3 * Math.PI / 4)..(-Math.PI / 4) -> FacingDirection.UP
            vx != 0f || vy != 0f -> FacingDirection.LEFT
            else -> fallback
        }
    }
}
