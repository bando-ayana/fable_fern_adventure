package com.example.data

import com.example.model.CommunityMission
import com.example.model.LootItemType
import com.example.model.PuzzleType
import com.example.model.WildCreatureTemperament
import com.example.model.WildEncounterOpponent

object RpgMissionCatalog {

    val wildOpponents: List<WildEncounterOpponent> = listOf(
        WildEncounterOpponent(
            id = "mossback_sproutling",
            name = "Mossback Sproutling",
            title = "Prickly Training Cub",
            temperament = WildCreatureTemperament.MENACING,
            habitat = "Elder Nia's Training Glade",
            description = "A feisty young bramble-cub that likes testing new Solari explorers in Elder Nia's sunlit practice clearing.",
            maxHp = 32,
            minAttack = 3,
            maxAttack = 5,
            specialMoveName = "Leaf-Spore Nudge",
            rewardExp = 35,
            rewardMagic = 10,
            rewardMana = 12,
            rewardNectar = 10,
            rewardMoonstones = 1,
            guaranteedLoot = LootItemType.SUNBERRY_TONIC
        ),
        WildEncounterOpponent(
            id = "thornscale_stalker",
            name = "Thornscale Stalker",
            title = "Briar-Armored Prowler",
            temperament = WildCreatureTemperament.MENACING,
            habitat = "Lower Canopy Crossings",
            description = "A snarling, vine-plated jungle predator territorial over fallen sun-bridges until driven back or outmatched.",
            maxHp = 64,
            minAttack = 9,
            maxAttack = 14,
            specialMoveName = "Razor Bramble Pounce",
            rewardExp = 55,
            rewardMagic = 14,
            rewardMana = 12,
            rewardNectar = 14,
            rewardMoonstones = 1,
            guaranteedLoot = LootItemType.SUNBERRY_TONIC
        ),
        WildEncounterOpponent(
            id = "prism_stag",
            name = "Prism-Winged Stag",
            title = "Iridescent Glade Sovereign",
            temperament = WildCreatureTemperament.BEAUTIFUL,
            habitat = "Sunlight Mirror Glade",
            description = "A breathtaking crystal-antlered stag whose translucent wings refract dawn light; challenges worthy guardians in ritual combat.",
            maxHp = 78,
            minAttack = 11,
            maxAttack = 16,
            specialMoveName = "Aurora Antler Surge",
            rewardExp = 70,
            rewardMagic = 20,
            rewardMana = 22,
            rewardNectar = 18,
            rewardMoonstones = 2,
            guaranteedLoot = LootItemType.MANA_VIAL
        ),
        WildEncounterOpponent(
            id = "mirefang_serpent",
            name = "Mirefang Serpent",
            title = "Deep-Spring Fangweaver",
            temperament = WildCreatureTemperament.MENACING,
            habitat = "Sunken Aqueduct Basin",
            description = "A hulking mossy python with glowing amber eyes coiled around the Solari cistern intake valves.",
            maxHp = 92,
            minAttack = 13,
            maxAttack = 19,
            specialMoveName = "Tidal Coil Crush",
            rewardExp = 85,
            rewardMagic = 22,
            rewardMana = 16,
            rewardNectar = 20,
            rewardMoonstones = 2,
            guaranteedLoot = LootItemType.SUNFIRE_CHARM
        ),
        WildEncounterOpponent(
            id = "orchid_lynx",
            name = "Celestial Orchid Lynx",
            title = "Star-Petal Spirit Cat",
            temperament = WildCreatureTemperament.BEAUTIFUL,
            habitat = "Moonflower Orchard Terraces",
            description = "A luminous silver-furred lynx draped in blooming night-orchids that tests pollinators with dazzling leaps.",
            maxHp = 104,
            minAttack = 14,
            maxAttack = 21,
            specialMoveName = "Nebula Petal Flurry",
            rewardExp = 95,
            rewardMagic = 28,
            rewardMana = 26,
            rewardNectar = 22,
            rewardMoonstones = 2,
            guaranteedLoot = LootItemType.MANA_VIAL
        ),
        WildEncounterOpponent(
            id = "storm_wyvern",
            name = "Obsidian Storm-Wyvern",
            title = "Thunder-Plume Ravager",
            temperament = WildCreatureTemperament.MENACING,
            habitat = "High Canopy Lightning Spires",
            description = "A fierce obsidian-scaled wyvern roosting atop Solari's weather chimes, hurling crackling gale gusts.",
            maxHp = 122,
            minAttack = 16,
            maxAttack = 24,
            specialMoveName = "Tempest Wing Rend",
            rewardExp = 115,
            rewardMagic = 34,
            rewardMana = 24,
            rewardNectar = 26,
            rewardMoonstones = 3,
            guaranteedLoot = LootItemType.SUNFIRE_CHARM
        ),
        WildEncounterOpponent(
            id = "aurora_phoenix",
            name = "Aurora-Crested Phoenix",
            title = "Living Sunfire Bird",
            temperament = WildCreatureTemperament.BEAUTIFUL,
            habitat = "Crown of Solari Sky-Peak",
            description = "A radiant songbird of emerald and gold flame whose trial awakens Solari's highest solar beacons.",
            maxHp = 132,
            minAttack = 17,
            maxAttack = 25,
            specialMoveName = "Solar Plume Resonance",
            rewardExp = 125,
            rewardMagic = 40,
            rewardMana = 32,
            rewardNectar = 28,
            rewardMoonstones = 3,
            guaranteedLoot = LootItemType.CANOPY_CREST
        ),
        WildEncounterOpponent(
            id = "shadowroot_sovereign",
            name = "Shadowroot Sovereign",
            title = "Ancient Bramble Leviathan",
            temperament = WildCreatureTemperament.MENACING,
            habitat = "Heartwood Abyssal Shrine",
            description = "A towering ancient colossus of twisted iron-vines and storm crystals guarding the sealed Crown of the Canopy.",
            maxHp = 165,
            minAttack = 19,
            maxAttack = 28,
            specialMoveName = "Cataclysm Root Quake",
            rewardExp = 180,
            rewardMagic = 60,
            rewardMana = 40,
            rewardNectar = 45,
            rewardMoonstones = 5,
            guaranteedLoot = LootItemType.CANOPY_CREST
        )
    )

    val communityMissions: List<CommunityMission> = listOf(
        CommunityMission(
            id = "mission_sunbridge",
            orderIndex = 0,
            title = "Rebuild the Lower Sun-Bridges",
            communityProject = "Living Sun-Bridge Network",
            communityBenefit = "Connects Solari artisans safely across the lower canopy (+20% Prosperity).",
            summary = "Repair snapped mahogany sky-cables with Solari engineers and face a menacing Thornscale Stalker prowling the timber.",
            communityActionLabel = "Splice Solar Vine Cables",
            communityActionOutcome = "You weave glowing copper-vine threads along the chasm! Suddenly, thorny armor rustles in the ferns ahead.",
            recommendedLevel = 1,
            prosperityReward = 20,
            requiredProsperity = 0,
            opponent = opponentById("thornscale_stalker"),
            puzzleChallenge = PuzzleType.VINE_WEAVER
        ),
        CommunityMission(
            id = "mission_glade_sanctum",
            orderIndex = 1,
            title = "Bless the Sunlight Mirror Glade",
            communityProject = "Solar Beacon & Clinic",
            communityBenefit = "Brings radiant healing light to Solari families (+20% Prosperity).",
            summary = "Raise a community crystal clinic and prove your worth to the breathtaking Prism-Winged Stag guarding the glade's spring.",
            communityActionLabel = "Align Healing Quartz Mirrors",
            communityActionOutcome = "The mirrors ring with golden warmth! From the mist steps a magnificent winged stag testing your resolve.",
            recommendedLevel = 1,
            prosperityReward = 20,
            requiredProsperity = 20,
            opponent = opponentById("prism_stag"),
            puzzleChallenge = PuzzleType.GROVE_GRID
        ),
        CommunityMission(
            id = "mission_aqueduct_gate",
            orderIndex = 2,
            title = "Clear the Sunken Aqueducts",
            communityProject = "Pure Canopy Cisterns",
            communityBenefit = "Restores fresh waterfall irrigation to every Solari terrace (+20% Prosperity).",
            summary = "Unblock the ancient hydro-valves so fresh water flows home—and drive off a menacing Mirefang Serpent!",
            communityActionLabel = "Open Hydro-Sluice Valves",
            communityActionOutcome = "Fresh spring water surges through the aqueduct, disturbing a colossal serpent coiled in the basin!",
            recommendedLevel = 2,
            prosperityReward = 20,
            requiredProsperity = 40,
            opponent = opponentById("mirefang_serpent"),
            puzzleChallenge = PuzzleType.SPIRIT_LOCK
        ),
        CommunityMission(
            id = "mission_orchard_grove",
            orderIndex = 3,
            title = "Plant the Moonflower Orchard",
            communityProject = "Night-Bloom Food & Nectar Grove",
            communityBenefit = "Provides year-round glowberries and nectar for every allied race (+20% Prosperity).",
            summary = "Sow luminous saplings with Solari botanists and duel the graceful Celestial Orchid Lynx in a starlit trial.",
            communityActionLabel = "Sow Bioluminescent Saplings",
            communityActionOutcome = "Rows of star-petals unfurl instantly! A radiant silver lynx bounds down to test your magic.",
            recommendedLevel = 3,
            prosperityReward = 20,
            requiredProsperity = 60,
            opponent = opponentById("orchid_lynx"),
            puzzleChallenge = PuzzleType.FERN_MOSAIC
        ),
        CommunityMission(
            id = "mission_weather_spires",
            orderIndex = 4,
            title = "Raise the Sky-Bell Weather Tower",
            communityProject = "Storm-Shield Observatory",
            communityBenefit = "Shields the united canopy from tempests (+20% Prosperity).",
            summary = "Hoist Solari's brass storm-chimes high above the treetops and clash with a fierce Obsidian Storm-Wyvern.",
            communityActionLabel = "Anchor Resonant Storm-Bells",
            communityActionOutcome = "Thunder rolls across the ridge as the weather tower chimes—an Obsidian Storm-Wyvern swoops into battle!",
            recommendedLevel = 4,
            prosperityReward = 20,
            requiredProsperity = 80,
            opponent = opponentById("storm_wyvern"),
            puzzleChallenge = PuzzleType.FIREFLY_ECHO
        ),
        CommunityMission(
            id = "mission_final_crown",
            orderIndex = 5,
            title = "Awaken the United Canopy Crown",
            communityProject = "Grand Heartwood Citadel",
            communityBenefit = "Unites all races & beats the Shadowroot threat to restore eternal peace!",
            summary = "With the community built, descend to the Abyssal Shrine and overcome the Shadowroot Sovereign to win the game!",
            communityActionLabel = "Unite All Heroes at the Shrine",
            communityActionOutcome = "Solari scholars and allied warriors raise their lanterns together as the mighty Shadowroot Sovereign awakens!",
            recommendedLevel = 5,
            prosperityReward = 0,
            requiredProsperity = 100,
            opponent = opponentById("shadowroot_sovereign"),
            puzzleChallenge = PuzzleType.SPIRIT_LOCK,
            isFinalExpedition = true
        )
    )

    val wildPatrols: List<CommunityMission> = listOf(
        CommunityMission(
            id = "patrol_briar_trail",
            orderIndex = 10,
            title = "Wild Patrol: Briar Crossing",
            communityProject = "Trail Safety & Foraging",
            communityBenefit = "Repeatable wild skirmish for XP, Magic, Mana & Sunberry Tonics.",
            summary = "Scout the misty brambles for menacing predators threatening supply caravans.",
            communityActionLabel = "Patrol the Perimeter Vines",
            communityActionOutcome = "Fresh claw marks score the bark—a wild Thornscale Stalker leaps from the brush!",
            recommendedLevel = 1,
            prosperityReward = 5,
            requiredProsperity = 0,
            opponent = opponentById("thornscale_stalker"),
            puzzleChallenge = PuzzleType.CANOPY_SPOTTER,
            isWildPatrol = true
        ),
        CommunityMission(
            id = "patrol_aurora_peak",
            orderIndex = 11,
            title = "Wild Trial: Aurora Sky-Peak",
            communityProject = "Highland Mana Harvest",
            communityBenefit = "Repeatable duel with a beautiful Phoenix for heavy Magic, Mana & Canopy Crests.",
            summary = "Ascend to the golden peaks where the Aurora-Crested Phoenix tests wandering mages.",
            communityActionLabel = "Sound the Sun-Chime Challenge",
            communityActionOutcome = "Golden embers swirl around your shoulders as the radiant Phoenix descends!",
            recommendedLevel = 3,
            prosperityReward = 5,
            requiredProsperity = 20,
            opponent = opponentById("aurora_phoenix"),
            puzzleChallenge = PuzzleType.MUSHROOM_MELODY,
            isWildPatrol = true
        )
    )

    val introMission: CommunityMission = CommunityMission(
        id = "mission_intro_sparring",
        orderIndex = -1,
        title = "Training Clearing: Mossback Sproutling",
        communityProject = "Explorer Readiness Trial",
        communityBenefit = "Certifies your canopy survival skills & awards starter XP, Magic, Mana & Loot.",
        summary = "Practice combat & creature soothing! Try Solar Strike, Arcane Pulse, or Soothe once its HP drops below 45%.",
        communityActionLabel = "Step Into the Practice Ring",
        communityActionOutcome = "Elder Nia cheers from the boardwalk as a prickly Mossback Sproutling bounds onto the moss!",
        recommendedLevel = 1,
        prosperityReward = 0,
        requiredProsperity = 0,
        opponent = opponentById("mossback_sproutling"),
        puzzleChallenge = PuzzleType.FIREFLY_ECHO,
        isWildPatrol = true
    )

    fun opponentById(id: String): WildEncounterOpponent =
        wildOpponents.firstOrNull { it.id == id } ?: wildOpponents.first()

    fun missionById(id: String): CommunityMission? =
        (listOf(introMission) + communityMissions + wildPatrols).firstOrNull { it.id == id }

    fun parseLootBag(csv: String): Map<LootItemType, Int> {
        if (csv.isBlank()) return emptyMap()
        val result = mutableMapOf<LootItemType, Int>()
        csv.split(",").forEach { entry ->
            val parts = entry.split(":")
            if (parts.size == 2) {
                val item = LootItemType.fromId(parts[0].trim())
                val count = parts[1].trim().toIntOrNull() ?: 0
                if (item != null && count > 0) {
                    result[item] = count
                }
            }
        }
        return result
    }

    fun serializeLootBag(map: Map<LootItemType, Int>): String =
        LootItemType.entries
            .mapNotNull { item ->
                val count = map[item] ?: 0
                if (count > 0) "${item.id}:$count" else null
            }
            .joinToString(",")

    fun addLoot(csv: String, items: List<LootItemType>): String {
        val mutable = parseLootBag(csv).toMutableMap()
        items.forEach { item ->
            mutable[item] = (mutable[item] ?: 0) + 1
        }
        return serializeLootBag(mutable)
    }

    fun consumeLoot(csv: String, item: LootItemType, amount: Int = 1): String? {
        val mutable = parseLootBag(csv).toMutableMap()
        val current = mutable[item] ?: 0
        if (current < amount) return null
        val remaining = current - amount
        if (remaining <= 0) mutable.remove(item) else mutable[item] = remaining
        return serializeLootBag(mutable)
    }
}
