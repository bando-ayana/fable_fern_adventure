package com.example.stateHolder

import com.example.model.CommunityMission
import com.example.model.LootItemType
import com.example.rpg.BattlePhase
import com.example.rpg.RpgBattleEngine

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
