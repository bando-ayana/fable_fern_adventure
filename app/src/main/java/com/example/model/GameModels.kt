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
    val baseNectarReward: Int
) {
    FIREFLY_ECHO(
        id = "firefly_echo",
        title = "Firefly Echo",
        tagline = "Follow the glowing lantern blossoms",
        howToPlay = "Watch the enchanted blossoms glow in sequence, then tap them back in the exact same order. Use Replay Whisper anytime in Cozy Assist.",
        skill = BrainSkill.MEMORY,
        baseNectarReward = 18
    ),
    MUSHROOM_MELODY(
        id = "mushroom_melody",
        title = "Mushroom Melody",
        tagline = "Arrange glowing mushrooms by fairy clues",
        howToPlay = "Tap any two glowing mushrooms on the mossy log to swap their spots until every fairy logic clue turns bright green!",
        skill = BrainSkill.LOGIC,
        baseNectarReward = 20
    ),
    FERN_MOSAIC(
        id = "fern_mosaic",
        title = "Fern Mosaic",
        tagline = "Complete the woven jungle leaf pattern",
        howToPlay = "Study the repeating shapes, colors, and tilts along the leafy branch, then pick the botanical tile that completes the missing spot!",
        skill = BrainSkill.DEDUCTION,
        baseNectarReward = 20
    ),
    CANOPY_SPOTTER(
        id = "canopy_spotter",
        title = "Canopy Spotter",
        tagline = "Find twin charms or the playful odd one out",
        howToPlay = "Look closely at the petals, spots, and colors of each jungle charm. Tap the two identical twins—or the single unique charm!",
        skill = BrainSkill.OBSERVATION,
        baseNectarReward = 19
    ),
    GROVE_GRID(
        id = "grove_grid",
        title = "Grove Grid",
        tagline = "Balance runes across every mossy grove",
        howToPlay = "Place Sun, Moon, Fern, and Dewdrop runes so each row, column, and 2x2 grove holds all four symbols without repeating.",
        skill = BrainSkill.LOGIC,
        baseNectarReward = 24
    ),
    VINE_WEAVER(
        id = "vine_weaver",
        title = "Vine Weaver",
        tagline = "Guide starlight water to the Moonflower",
        howToPlay = "Tap mossy stream tiles to rotate them until glowing water flows smoothly from the Spring Source to the sleeping Moonflower.",
        skill = BrainSkill.SPATIAL,
        baseNectarReward = 22
    ),
    SPIRIT_LOCK(
        id = "spirit_lock",
        title = "Spirit Lock",
        tagline = "Crack the ancient treehouse rune lock",
        howToPlay = "Choose a rune sequence and test it! Gold pips mean right rune & spot; Silver pips mean right rune in another spot.",
        skill = BrainSkill.DEDUCTION,
        baseNectarReward = 25
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
    val opensPuzzle: PuzzleType? = null
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

