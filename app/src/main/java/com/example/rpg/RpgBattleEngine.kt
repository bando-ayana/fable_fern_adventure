package com.example.rpg

import com.example.data.JungleLoreCatalog
import com.example.data.local.PlayerProfileEntity
import com.example.model.CommunityMission
import com.example.model.LootItemType
import com.example.model.WildCreatureTemperament
import kotlin.random.Random

enum class BattlePhase {
    COMMUNITY_PREP,
    ACTIVE_COMBAT,
    VICTORY,
    RETREATED
}

enum class BattleActionType {
    SOLAR_STRIKE,
    ARCANE_SPELL,
    GUARDIAN_WARD,
    SOOTHE_SPIRIT
}

data class RpgBattleState(
    val mission: CommunityMission,
    val phase: BattlePhase = BattlePhase.COMMUNITY_PREP,
    val roundNumber: Int = 1,
    val heroName: String,
    val heroMaxHp: Int,
    val heroHp: Int,
    val heroMaxMana: Int,
    val heroMana: Int,
    val attackPower: Int,
    val magicPower: Int,
    val wardActive: Boolean = false,
    val enemyHp: Int,
    val enemyMaxHp: Int,
    val upcomingEnemyDamage: Int,
    val upcomingEnemyMove: String,
    val calmedSpirit: Boolean = false,
    val combatLog: List<String> = emptyList(),
    val lastHeroHitTick: Int = 0,
    val lastEnemyHitTick: Int = 0,
    val usedLootInBattle: Map<LootItemType, Int> = emptyMap(),
    val lootAvailable: Map<LootItemType, Int> = emptyMap()
) {
    val canCastArcaneSpell: Boolean get() = heroMana >= RpgBattleEngine.ARCANE_SPELL_COST
    val canCastWard: Boolean get() = heroMana >= RpgBattleEngine.WARD_SPELL_COST
    val canSootheSpirit: Boolean get() =
        enemyHp <= (enemyMaxHp * 0.45f).toInt().coerceAtLeast(1) &&
                heroMana >= RpgBattleEngine.SOOTHE_SPELL_COST
}

object RpgBattleEngine {
    const val ARCANE_SPELL_COST = 12
    const val WARD_SPELL_COST = 10
    const val SOOTHE_SPELL_COST = 8
    const val STRIKE_MANA_GAIN = 6

    fun createBattleState(
        mission: CommunityMission,
        profile: PlayerProfileEntity,
        lootAvailable: Map<LootItemType, Int>,
        random: Random = Random.Default
    ): RpgBattleState {
        val hero = JungleLoreCatalog.heroById(profile.selectedHeroId)
        val relic = JungleLoreCatalog.relicById(profile.equippedRelicId)

        val totalMagicStat = hero.baseMagic + relic.magicBonus + (profile.heroLevel - 1) + (profile.magicEssence / 20)
        val totalTechStat = hero.baseTech + relic.techBonus + (profile.heroLevel - 1)
        val totalHeartStat = hero.baseHeart + relic.heartBonus + (profile.heroLevel - 1)

        val maxHp = 75 + (totalHeartStat * 5) + (profile.heroLevel * 6)
        val maxMana = profile.maxMana
        val startingMana = profile.mana.coerceIn(10, maxMana)
        val attackPower = 11 + totalTechStat + (profile.heroLevel * 2)
        val magicPower = 18 + (totalMagicStat * 2) + (profile.heroLevel * 2)

        val enemy = mission.opponent
        val initialTelegraph = rollEnemyIntent(enemy.minAttack, enemy.maxAttack, enemy.specialMoveName, random)

        return RpgBattleState(
            mission = mission,
            phase = BattlePhase.COMMUNITY_PREP,
            heroName = hero.name,
            heroMaxHp = maxHp,
            heroHp = maxHp,
            heroMaxMana = maxMana,
            heroMana = startingMana,
            attackPower = attackPower,
            magicPower = magicPower,
            enemyHp = enemy.maxHp,
            enemyMaxHp = enemy.maxHp,
            upcomingEnemyDamage = initialTelegraph.first,
            upcomingEnemyMove = initialTelegraph.second,
            combatLog = listOf(mission.summary),
            lootAvailable = lootAvailable
        )
    }

    fun beginWildEncounter(state: RpgBattleState): RpgBattleState {
        val temperamentNote = if (state.mission.opponent.temperament == WildCreatureTemperament.BEAUTIFUL) {
            "A radiant ${state.mission.opponent.name} steps into view to test your spirit!"
        } else {
            "A menacing ${state.mission.opponent.name} blocks the path!"
        }
        return state.copy(
            phase = BattlePhase.ACTIVE_COMBAT,
            combatLog = listOf(state.mission.communityActionOutcome, temperamentNote)
        )
    }

    fun performAction(
        state: RpgBattleState,
        action: BattleActionType,
        random: Random = Random.Default
    ): RpgBattleState {
        if (state.phase != BattlePhase.ACTIVE_COMBAT) return state

        var nextHeroHp = state.heroHp
        var nextHeroMana = state.heroMana
        var nextEnemyHp = state.enemyHp
        var wardNext = state.wardActive
        var enemyHitTick = state.lastEnemyHitTick
        var heroHitTick = state.lastHeroHitTick
        val logs = mutableListOf<String>()

        when (action) {
            BattleActionType.SOLAR_STRIKE -> {
                val dmg = state.attackPower + random.nextInt(0, 5)
                nextEnemyHp = (nextEnemyHp - dmg).coerceAtLeast(0)
                nextHeroMana = (nextHeroMana + STRIKE_MANA_GAIN).coerceAtMost(state.heroMaxMana)
                enemyHitTick++
                logs += "${state.heroName} lands a Solar Strike for $dmg DMG and recovers +$STRIKE_MANA_GAIN Mana!"
            }

            BattleActionType.ARCANE_SPELL -> {
                if (!state.canCastArcaneSpell) return state
                nextHeroMana -= ARCANE_SPELL_COST
                val dmg = state.magicPower + random.nextInt(2, 8)
                nextEnemyHp = (nextEnemyHp - dmg).coerceAtLeast(0)
                enemyHitTick++
                logs += "${state.heroName} casts Canopy Arcane Pulse (-$ARCANE_SPELL_COST Mana) for $dmg radiant DMG!"
            }

            BattleActionType.GUARDIAN_WARD -> {
                if (!state.canCastWard) return state
                nextHeroMana -= WARD_SPELL_COST
                val heal = (14 + (state.magicPower / 3)).coerceAtMost(state.heroMaxHp - nextHeroHp)
                nextHeroHp += heal
                wardNext = true
                logs += "${state.heroName} raises a Living Ward (-$WARD_SPELL_COST Mana), restoring +$heal HP and halving incoming damage!"
            }

            BattleActionType.SOOTHE_SPIRIT -> {
                if (!state.canSootheSpirit) return state
                nextHeroMana -= SOOTHE_SPELL_COST
                logs += "${state.heroName} channels a calming harmony (-$SOOTHE_SPELL_COST Mana) — ${state.mission.opponent.name} bows in respect!"
                return state.copy(
                    phase = BattlePhase.VICTORY,
                    heroHp = nextHeroHp,
                    heroMana = nextHeroMana,
                    enemyHp = 0,
                    calmedSpirit = true,
                    combatLog = (logs + state.combatLog).take(6)
                )
            }
        }

        if (nextEnemyHp <= 0) {
            logs += "${state.mission.opponent.name} is overcome! You claim victory and wild treasures."
            return state.copy(
                phase = BattlePhase.VICTORY,
                heroHp = nextHeroHp,
                heroMana = nextHeroMana,
                enemyHp = 0,
                wardActive = false,
                lastEnemyHitTick = enemyHitTick,
                combatLog = (logs + state.combatLog).take(6)
            )
        }

        // Wild creature counter-turn
        val rawIncoming = state.upcomingEnemyDamage
        val actualIncoming = if (wardNext) (rawIncoming / 2).coerceAtLeast(1) else rawIncoming
        nextHeroHp = (nextHeroHp - actualIncoming).coerceAtLeast(0)
        heroHitTick++
        logs += if (wardNext) {
            "${state.mission.opponent.name} uses ${state.upcomingEnemyMove} for $actualIncoming DMG (Ward absorbed half!)."
        } else {
            "${state.mission.opponent.name} uses ${state.upcomingEnemyMove} for $actualIncoming DMG!"
        }

        if (nextHeroHp <= 0) {
            logs += "${state.heroName} regroups safely back to Solari Canopy to recover!"
            return state.copy(
                phase = BattlePhase.RETREATED,
                heroHp = 0,
                heroMana = nextHeroMana,
                enemyHp = nextEnemyHp,
                wardActive = false,
                lastHeroHitTick = heroHitTick,
                lastEnemyHitTick = enemyHitTick,
                combatLog = (logs + state.combatLog).take(6)
            )
        }

        val enemy = state.mission.opponent
        val nextIntent = rollEnemyIntent(enemy.minAttack, enemy.maxAttack, enemy.specialMoveName, random)
        return state.copy(
            roundNumber = state.roundNumber + 1,
            heroHp = nextHeroHp,
            heroMana = nextHeroMana,
            enemyHp = nextEnemyHp,
            wardActive = false,
            upcomingEnemyDamage = nextIntent.first,
            upcomingEnemyMove = nextIntent.second,
            lastHeroHitTick = heroHitTick,
            lastEnemyHitTick = enemyHitTick,
            combatLog = (logs + state.combatLog).take(6)
        )
    }

    fun useLootItem(
        state: RpgBattleState,
        item: LootItemType
    ): RpgBattleState {
        if (state.phase != BattlePhase.ACTIVE_COMBAT) return state
        val count = state.lootAvailable[item] ?: 0
        if (count <= 0) return state

        val updatedAvailable = state.lootAvailable.toMutableMap().apply {
            if (count <= 1) remove(item) else put(item, count - 1)
        }
        val updatedUsed = state.usedLootInBattle.toMutableMap().apply {
            put(item, (get(item) ?: 0) + 1)
        }

        return when (item) {
            LootItemType.SUNBERRY_TONIC -> {
                val healed = (state.heroHp + 38).coerceAtMost(state.heroMaxHp) - state.heroHp
                state.copy(
                    heroHp = state.heroHp + healed,
                    lootAvailable = updatedAvailable,
                    usedLootInBattle = updatedUsed,
                    combatLog = (listOf("Used Sunberry Tonic: restored +$healed HP!") + state.combatLog).take(6)
                )
            }

            LootItemType.MANA_VIAL -> {
                val restored = (state.heroMana + 32).coerceAtMost(state.heroMaxMana) - state.heroMana
                state.copy(
                    heroMana = state.heroMana + restored,
                    lootAvailable = updatedAvailable,
                    usedLootInBattle = updatedUsed,
                    combatLog = (listOf("Used Starlight Mana Vial: restored +$restored Mana!") + state.combatLog).take(6)
                )
            }

            LootItemType.SUNFIRE_CHARM -> {
                val newEnemyHp = (state.enemyHp - 34).coerceAtLeast(0)
                val logMsg = "Unleashed Sunfire Relic Charm for 34 radiant DMG!"
                if (newEnemyHp <= 0) {
                    state.copy(
                        phase = BattlePhase.VICTORY,
                        enemyHp = 0,
                        lastEnemyHitTick = state.lastEnemyHitTick + 1,
                        lootAvailable = updatedAvailable,
                        usedLootInBattle = updatedUsed,
                        combatLog = (listOf("$logMsg ${state.mission.opponent.name} is overcome!") + state.combatLog).take(6)
                    )
                } else {
                    state.copy(
                        enemyHp = newEnemyHp,
                        lastEnemyHitTick = state.lastEnemyHitTick + 1,
                        lootAvailable = updatedAvailable,
                        usedLootInBattle = updatedUsed,
                        combatLog = (listOf(logMsg) + state.combatLog).take(6)
                    )
                }
            }

            LootItemType.CANOPY_CREST -> state
        }
    }

    private fun rollEnemyIntent(
        minAttack: Int,
        maxAttack: Int,
        specialMove: String,
        random: Random
    ): Pair<Int, String> {
        val isSpecial = random.nextInt(100) < 35
        return if (isSpecial) {
            val dmg = (maxAttack + 2).coerceAtLeast(minAttack + 3)
            dmg to specialMove
        } else {
            val dmg = random.nextInt(minAttack, maxAttack + 1)
            dmg to "Wild Strike"
        }
    }
}