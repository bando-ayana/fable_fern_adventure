package com.example.model

import androidx.annotation.DrawableRes

enum class BrainSkill(
    val displayName: String,
    val subtitle: String
) {
    MEMORY("Memory", "Working recall & rhythm"),
    LOGIC("Logic", "Pattern & sequence rules"),
    SPATIAL("Spatial", "Mental rotation & paths"),
    DEDUCTION("Deduction", "Clue synthesis & patterns"),
    OBSERVATION("Observation", "Visual scanning & detail")
}

enum class PuzzleType(
    val id: String,
    val title: String,
    val tagline: String,
    val howToPlay: String,
    val skill: BrainSkill,
    val baseNectarReward: Int,
    val discoveryHint: String
) {
    FIREFLY_ECHO(
        id = "firefly_echo",
        title = "Firefly Echo",
        tagline = "Follow the glowing lantern blossoms",
        howToPlay = "Watch the enchanted blossoms glow in sequence, then tap them back in the exact same order. Use Replay Whisper anytime in Cozy Assist.",
        skill = BrainSkill.MEMORY,
        baseNectarReward = 18,
        discoveryHint = "Access in Ch. 1: Whispering Moss Hollow or Sky-Bell Weather Mission"
    ),
    MUSHROOM_MELODY(
        id = "mushroom_melody",
        title = "Mushroom Melody",
        tagline = "Arrange glowing mushrooms by fairy clues",
        howToPlay = "Tap any two glowing mushrooms on the mossy log to swap their spots until every fairy logic clue turns bright green!",
        skill = BrainSkill.LOGIC,
        baseNectarReward = 20,
        discoveryHint = "Access in Ch. 1 (Moss Hollow), Ch. 4 (Crystal Cavern), or Aurora Peak Trial"
    ),
    FERN_MOSAIC(
        id = "fern_mosaic",
        title = "Fern Mosaic",
        tagline = "Complete the woven jungle leaf pattern",
        howToPlay = "Study the repeating shapes, colors, and tilts along the leafy branch, then pick the botanical tile that completes the missing spot!",
        skill = BrainSkill.DEDUCTION,
        baseNectarReward = 20,
        discoveryHint = "Access in Ch. 2 (Dew Lagoon), Ch. 5 (Starbloom), or Moonflower Orchard Mission"
    ),
    CANOPY_SPOTTER(
        id = "canopy_spotter",
        title = "Canopy Spotter",
        tagline = "Find twin charms or the playful odd one out",
        howToPlay = "Look closely at the petals, spots, and colors of each jungle charm. Tap the two identical twins—or the single unique charm!",
        skill = BrainSkill.OBSERVATION,
        baseNectarReward = 19,
        discoveryHint = "Access in Wild Patrol: Briar Crossing or Ch. 3: Lantern Orchid Canopy"
    ),
    GROVE_GRID(
        id = "grove_grid",
        title = "Grove Grid",
        tagline = "Balance runes across every mossy grove",
        howToPlay = "Place Sun, Moon, Fern, and Dewdrop runes so each row, column, and 2x2 grove holds all four symbols without repeating.",
        skill = BrainSkill.LOGIC,
        baseNectarReward = 24,
        discoveryHint = "Access in Bless the Sunlight Mirror Glade Mission or Ch. 3 (Orchid Canopy)"
    ),
    VINE_WEAVER(
        id = "vine_weaver",
        title = "Vine Weaver",
        tagline = "Guide starlight water to the Moonflower",
        howToPlay = "Tap mossy stream tiles to rotate them until glowing water flows smoothly from the Spring Source to the sleeping Moonflower.",
        skill = BrainSkill.SPATIAL,
        baseNectarReward = 22,
        discoveryHint = "Access in Rebuild the Lower Sun-Bridges Mission or Ch. 2 (Dew Lagoon)"
    ),
    SPIRIT_LOCK(
        id = "spirit_lock",
        title = "Spirit Lock",
        tagline = "Crack the ancient treehouse rune lock",
        howToPlay = "Choose a rune sequence and test it! Gold pips mean right rune & spot; Silver pips mean right rune in another spot.",
        skill = BrainSkill.DEDUCTION,
        baseNectarReward = 25,
        discoveryHint = "Access in Clear the Sunken Aqueducts Mission or Ch. 6: Heartwood Shrine"
    );

    companion object {
        fun fromId(id: String): PuzzleType = entries.firstOrNull { it.id == id } ?: FIREFLY_ECHO
    }
}

enum class DifficultyTier(
    val label: String,
    val rewardMultiplier: Float,
    val xpBonus: Int
) {
    SPROUT("Sprout", 1.0f, 15),
    EXPLORER("Explorer", 1.35f, 25),
    ELDER("Elder", 1.75f, 40)
}

data class PlayableHero(
    val id: String,
    val name: String,
    val title: String,
    val heritage: String,
    val specialty: String,
    val bio: String,
    val unlockNectar: Int,
    val baseMagic: Int = 4,
    val baseTech: Int = 4,
    val baseHeart: Int = 4,
    val bonusNectarPerPuzzle: Int = 0,
    val bonusSkillXp: Int = 0,
    val bonusBondXp: Int = 0
)

data class RpgRelic(
    val id: String,
    val name: String,
    val description: String,
    val shardCost: Int,
    val magicBonus: Int = 0,
    val techBonus: Int = 0,
    val heartBonus: Int = 0,
    val bonusNectar: Int = 0
)

data class StoryChoice(
    val id: String,
    val label: String,
    val responseText: String,
    val rewardNectar: Int,
    val bondGain: Int,
    val rewardExp: Int = 35,
    val opensPuzzle: PuzzleType? = null,
    val startsRpg: Boolean = false
)

data class BiomeNode(
    val id: String,
    val index: Int,
    val name: String,
    val subtitle: String,
    val questTitle: String,
    val questObjective: String,
    val creatureId: String,
    val unlockNectar: Int,
    val storyIntro: String,
    val storyChoices: List<StoryChoice>,
    val signaturePuzzle: PuzzleType
)

data class MythicalCreature(
    val id: String,
    val name: String,
    val title: String,
    val habitat: String,
    val favoriteTreat: String,
    val lore: String,
    @param:DrawableRes val drawableRes: Int?,
    val signatureSkill: BrainSkill,
    val signaturePuzzle: PuzzleType,
    val biomeIndex: Int
)

data class BadgeDefinition(
    val id: String,
    val title: String,
    val description: String,
    val skill: BrainSkill? = null,
    val thresholdLabel: String
)

data class PuzzleCompletionResult(
    val puzzleType: PuzzleType,
    val difficulty: DifficultyTier,
    val completedLevel: Int,
    val nextLevelUnlocked: Int,
    val starsEarned: Int,
    val score: Int,
    val nectarEarned: Int,
    val moonstonesEarned: Int,
    val skillXpEarned: Int,
    val newBestScore: Boolean
)

enum class LoreCategory(val label: String) {
    SOLARI_SOCIETY("Solari Society"),
    ALLIED_REALMS("Allied Peoples"),
    JUNGLE_BIOMES("Mythical Groves"),
    CREATURE_MYTHS("Creature Myths")
}

data class LoreEntry(
    val id: String,
    val title: String,
    val category: LoreCategory,
    val chronicle: String,
    val unlockHint: String,
    val requiredNectar: Int = 0,
    val requiredBiomeQuestId: String? = null,
    val requiredCreatureId: String? = null,
    val requiredPuzzlesSolved: Int = 0,
    val starterUnlocked: Boolean = false
)

enum class LootItemType(
    val id: String,
    val displayName: String,
    val description: String,
    val shortEffect: String
) {
    SUNBERRY_TONIC(
        id = "sunberry_tonic",
        displayName = "Sunberry Tonic",
        description = "Warm amber nectar brew pressed by Solari botanists.",
        shortEffect = "+38 HP in battle"
    ),
    MANA_VIAL(
        id = "mana_vial",
        displayName = "Starlight Mana Vial",
        description = "Crystallized canopy dew charged with lunar resonance.",
        shortEffect = "+32 Mana in battle"
    ),
    SUNFIRE_CHARM(
        id = "sunfire_charm",
        displayName = "Sunfire Relic Charm",
        description = "Single-use solar prism that unleashes a radiant burst.",
        shortEffect = "34 radiant DMG"
    ),
    CANOPY_CREST(
        id = "canopy_crest",
        displayName = "Living Canopy Crest",
        description = "Sacred seal earned from wild guardians to awaken the Final Crown.",
        shortEffect = "Key Trophy Loot"
    );

    companion object {
        fun fromId(id: String): LootItemType? = entries.firstOrNull { it.id == id }
    }
}

enum class WildCreatureTemperament(val label: String) {
    MENACING("Menacing Wild Beast"),
    BEAUTIFUL("Beautiful Mythic Spirit")
}

data class WildEncounterOpponent(
    val id: String,
    val name: String,
    val title: String,
    val temperament: WildCreatureTemperament,
    val habitat: String,
    val description: String,
    val maxHp: Int,
    val minAttack: Int,
    val maxAttack: Int,
    val specialMoveName: String,
    val rewardExp: Int,
    val rewardMagic: Int,
    val rewardMana: Int,
    val rewardNectar: Int,
    val rewardMoonstones: Int,
    val guaranteedLoot: LootItemType
)

data class CommunityMission(
    val id: String,
    val orderIndex: Int,
    val title: String,
    val communityProject: String,
    val communityBenefit: String,
    val summary: String,
    val communityActionLabel: String,
    val communityActionOutcome: String,
    val recommendedLevel: Int,
    val prosperityReward: Int,
    val requiredProsperity: Int,
    val opponent: WildEncounterOpponent,
    val puzzleChallenge: PuzzleType? = null,
    val alwaysTriggersBattle: Boolean = true,
    val isFinalExpedition: Boolean = false,
    val isWildPatrol: Boolean = false
)

data class MissionCompletionSummary(
    val mission: CommunityMission,
    val victory: Boolean,
    val calmedSpirit: Boolean,
    val expGained: Int,
    val leveledUpTo: Int?,
    val magicGained: Int,
    val manaGained: Int,
    val nectarGained: Int,
    val moonstonesGained: Int,
    val prosperityGained: Int,
    val lootEarned: List<LootItemType>,
    val gameBeatenJustNow: Boolean
)