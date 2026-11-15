package com.example.data

import com.example.R
import com.example.model.BadgeDefinition
import com.example.model.BiomeNode
import com.example.model.BrainSkill
import com.example.model.LoreCategory
import com.example.model.LoreEntry
import com.example.model.MythicalCreature
import com.example.model.PlayableHero
import com.example.model.PuzzleType
import com.example.model.RpgRelic
import com.example.model.StoryChoice

object JungleLoreCatalog {

    val relics: List<RpgRelic> = listOf(
        RpgRelic(
            id = "solar_visor",
            name = "Sun-Crystal Visor",
            description = "Standard Solari optics calibrated to spot hidden spores and rune harmonics.",
            shardCost = 0,
            magicBonus = 1,
            techBonus = 2,
            heartBonus = 1
        ),
        RpgRelic(
            id = "spore_staff",
            name = "Spore-Circuit Staff",
            description = "A living mahogany staff threaded with golden solar filaments. +3 Nectar per puzzle.",
            shardCost = 3,
            magicBonus = 4,
            techBonus = 2,
            bonusNectar = 3
        ),
        RpgRelic(
            id = "star_astrolabe",
            name = "Starlight Astrolabe",
            description = "Precision Solari brass-and-quartz dial that amplifies pattern insight.",
            shardCost = 5,
            magicBonus = 2,
            techBonus = 5,
            heartBonus = 1,
            bonusNectar = 2
        ),
        RpgRelic(
            id = "heartwood_crown",
            name = "Heartwood Circlet",
            description = "Woven from singing vines and amber light, deepening bonds across every race.",
            shardCost = 7,
            magicBonus = 3,
            techBonus = 2,
            heartBonus = 5,
            bonusNectar = 4
        )
    )

    val heroes: List<PlayableHero> = listOf(
        // Founding Vanguard: All-Black thriving magitech canopy civilization (playable from start)
        PlayableHero(
            id = "zuri",
            name = "Zuri Vance",
            title = "Aether-Botanist",
            heritage = "Solari Canopy (Founding Society)",
            specialty = "+3 Bonus Nectar per solved puzzle",
            bio = "Raised in the sunlit brass-and-vine spires of Solari, Zuri pairs solar-crystal optics with gentle botanical magic to care for wild jungle groves.",
            unlockNectar = 0,
            baseMagic = 6,
            baseTech = 5,
            baseHeart = 5,
            bonusNectarPerPuzzle = 3
        ),
        PlayableHero(
            id = "kofi",
            name = "Kofi Solis",
            title = "Harmonic Engineer",
            heritage = "Solari Canopy (Founding Society)",
            specialty = "+6 Bonus Brain Skill XP per puzzle",
            bio = "An inventor of resonant stone chimes and glowing spore-circuits, Kofi reads ancient jungle logic runes like sheet music.",
            unlockNectar = 0,
            baseMagic = 4,
            baseTech = 7,
            baseHeart = 5,
            bonusSkillXp = 6
        ),
        PlayableHero(
            id = "amara",
            name = "Amara Okafor",
            title = "Beast-Speaker Archivist",
            heritage = "Solari Canopy (Founding Society)",
            specialty = "+6 Bonus Bond XP on story quests",
            bio = "Carrying a holographic leaf-grimoire forged in Solari's academies, Amara translates the songs and riddles of every mythical creature.",
            unlockNectar = 0,
            baseMagic = 6,
            baseTech = 4,
            baseHeart = 7,
            bonusBondXp = 6
        ),
        // Allied Travelers of all races unlocking as the RPG unfolds
        PlayableHero(
            id = "meilin",
            name = "Mei-Lin Zhao",
            title = "Starlight Cartographer",
            heritage = "Cloud-Terrace Envoys (Human)",
            specialty = "+4 Bonus Skill XP & +2 Nectar",
            bio = "Arriving by solar-silk glider from the eastern mist peaks, Mei-Lin charts the shifting canopy trails alongside Solari's scholars.",
            unlockNectar = 40,
            baseMagic = 5,
            baseTech = 7,
            baseHeart = 5,
            bonusNectarPerPuzzle = 2,
            bonusSkillXp = 4
        ),
        PlayableHero(
            id = "mateo",
            name = "Mateo Solano",
            title = "Lagoon Hydro-Mage",
            heritage = "Riverbend Guild (Human)",
            specialty = "+4 Bonus Nectar per puzzle",
            bio = "A cheerful copper-gauntlet stream-weaver who helps Solari engineers channel pure spring water to thirsty Moonflowers.",
            unlockNectar = 80,
            baseMagic = 7,
            baseTech = 5,
            baseHeart = 6,
            bonusNectarPerPuzzle = 4
        ),
        PlayableHero(
            id = "aelindra",
            name = "Aelindra Moonbough",
            title = "Sylvan Blossom Singer",
            heritage = "Elderwood Kin (Elf)",
            specialty = "+8 Bonus Bond XP on story quests",
            bio = "Drawn by Solari's peaceful harmony between technology and living roots, Aelindra shares timeless melodies with the jungle fauna.",
            unlockNectar = 130,
            baseMagic = 8,
            baseTech = 4,
            baseHeart = 8,
            bonusBondXp = 8
        ),
        PlayableHero(
            id = "thorek",
            name = "Thorek Mossbeard",
            title = "Crystal-Craft Artisan",
            heritage = "Deep-Quartz Halls (Dwarf)",
            specialty = "+8 Bonus Brain Skill XP per puzzle",
            bio = "Fascinated by Solari's glowing quartz lenses, Thorek tunes underground cavern chimes with joyful precision.",
            unlockNectar = 180,
            baseMagic = 5,
            baseTech = 9,
            baseHeart = 6,
            bonusSkillXp = 8
        )
    )

    val creatures: List<MythicalCreature> = listOf(
        MythicalCreature(
            id = "lumafern",
            name = "Lumafern",
            title = "Lantern-Antler Fox Spirit",
            habitat = "Whispering Moss Hollow",
            favoriteTreat = "Golden Glowberries",
            lore = "When twilight settles over the mossy ferns, Lumafern's budding antlers hum alongside Solari's spore-lanterns to guide lost fireflies home.",
            drawableRes = R.drawable.img_creature_lumafern,
            signatureSkill = BrainSkill.MEMORY,
            signaturePuzzle = PuzzleType.FIREFLY_ECHO,
            biomeIndex = 0
        ),
        MythicalCreature(
            id = "dewdrake",
            name = "Dewdrake",
            title = "Iridescent Lagoon Hatchling",
            habitat = "Sunlit Dew Lagoon",
            favoriteTreat = "Lotus Honeydew",
            lore = "No bigger than a teacup, Dewdrake naps on Solari's floating hydro-pads and weaves mist rainbows whenever she sneezes.",
            drawableRes = R.drawable.img_creature_dewdrake,
            signatureSkill = BrainSkill.SPATIAL,
            signaturePuzzle = PuzzleType.VINE_WEAVER,
            biomeIndex = 1
        ),
        MythicalCreature(
            id = "aureowl",
            name = "Aureowl",
            title = "Moon-Plumed Archivist",
            habitat = "Lantern Orchid Canopy",
            favoriteTreat = "Starlight Pine Nuts",
            lore = "Aureowl perches beside Solari's brass observatories, trading forgotten jungle riddles for clever pattern solutions.",
            drawableRes = R.drawable.img_creature_aureowl,
            signatureSkill = BrainSkill.OBSERVATION,
            signaturePuzzle = PuzzleType.CANOPY_SPOTTER,
            biomeIndex = 2
        ),
        MythicalCreature(
            id = "glimmertoad",
            name = "Glimmertoad",
            title = "Crystal Chime Amphibian",
            habitat = "Crystal Root Cavern",
            favoriteTreat = "Echo Mint Leaves",
            lore = "Glimmertoad loves arranging glowing cavern mushrooms in harmonic rows so their bioluminescence chimes in tune.",
            drawableRes = null,
            signatureSkill = BrainSkill.LOGIC,
            signaturePuzzle = PuzzleType.MUSHROOM_MELODY,
            biomeIndex = 3
        ),
        MythicalCreature(
            id = "velvetmoth",
            name = "Velvetmoth",
            title = "Starbloom Silk Weaver",
            habitat = "Starbloom Treehouse",
            favoriteTreat = "Moonflower Pollen",
            lore = "Velvetmoth weaves shimmering leaf mosaics with Solari's weavers, creating vibrant botanical tapestries high above the mist.",
            drawableRes = null,
            signatureSkill = BrainSkill.DEDUCTION,
            signaturePuzzle = PuzzleType.FERN_MOSAIC,
            biomeIndex = 4
        ),
        MythicalCreature(
            id = "brambleback",
            name = "Brambleback",
            title = "Ancient Heartwood Guardian",
            habitat = "Ancient Heartwood Shrine",
            favoriteTreat = "Sun-Warmed Figs",
            lore = "A miniature solar garden blooms atop Brambleback's shell. Guardians of all races gather in his peaceful shade.",
            drawableRes = null,
            signatureSkill = BrainSkill.DEDUCTION,
            signaturePuzzle = PuzzleType.SPIRIT_LOCK,
            biomeIndex = 5
        )
    )

    val biomes: List<BiomeNode> = listOf(
        BiomeNode(
            id = "moss_hollow",
            index = 0,
            name = "Whispering Moss Hollow",
            subtitle = "Solari spore-lanterns & drifting firefly chimes",
            questTitle = "Ch. 1: Song of the Spore-Lanterns",
            questObjective = "Harmonize with Lumafern and awaken the hollow's lantern trail.",
            creatureId = "lumafern",
            unlockNectar = 0,
            storyIntro = "Just beyond Solari's solar-crystal archways, emerald ferns shimmer in the mist. Lumafern tilts her glowing antlers, inviting you to calibrate a lantern relay or sort her glowing mushrooms.",
            storyChoices = listOf(
                StoryChoice(
                    id = "luma_greet",
                    label = "[Magic] Tune solar lantern to Lumafern's hum",
                    responseText = "Lumafern's ears perk up joyfully! Warm golden sparks dance around your magitech staff, granting +35 Hero EXP.",
                    rewardNectar = 12,
                    bondGain = 20,
                    rewardExp = 35,
                    startsRpg = true,
                ),
                StoryChoice(
                    id = "luma_puzzle",
                    label = "[Quest Puzzle] Play Firefly Echo together",
                    responseText = "Lumafern taps her paw beside the glowing blossoms—echo the lantern relay to earn bonus EXP!",
                    rewardNectar = 8,
                    bondGain = 15,
                    rewardExp = 40,
                    startsRpg = true,
                    opensPuzzle = PuzzleType.FIREFLY_ECHO
                ),
                StoryChoice(
                    id = "luma_mushroom",
                    label = "[Quest Puzzle] Arrange glowing mushrooms on the log",
                    responseText = "Tiny bioluminescent caps pop awake as Lumafern watches eagerly!",
                    rewardNectar = 8,
                    bondGain = 15,
                    rewardExp = 40,
                    startsRpg = true,
                    opensPuzzle = PuzzleType.MUSHROOM_MELODY
                )
            ),
            signaturePuzzle = PuzzleType.FIREFLY_ECHO
        ),
        BiomeNode(
            id = "dew_lagoon",
            index = 1,
            name = "Sunlit Dew Lagoon",
            subtitle = "Hydro-crystal pools & woven lily pathways",
            questTitle = "Ch. 2: Restore the Lagoon Aqueduct",
            questObjective = "Guide starlight water to the Moonflower and welcome Dewdrake.",
            creatureId = "dewdrake",
            unlockNectar = 25,
            storyIntro = "Golden light dances across Solari's tranquil aqueduct pools. Curled on a broad lily pad, Dewdrake blinks awake beside dry mossy channels and drifting fern leaves.",
            storyChoices = listOf(
                StoryChoice(
                    id = "dew_splash",
                    label = "[Tech] Adjust the floating hydro-pad valve",
                    responseText = "Plip-plop-plip! Cool mist rainbows shimmer over the lagoon, granting +40 Hero EXP.",
                    rewardNectar = 14,
                    bondGain = 20,
                    rewardExp = 40,
                    startsRpg = true,
                ),
                StoryChoice(
                    id = "dew_puzzle",
                    label = "[Quest Puzzle] Weave the lagoon streams",
                    responseText = "Dewdrake flutters her misty wings excitedly as the Vine Weaver channels light up!",
                    rewardNectar = 10,
                    bondGain = 15,
                    rewardExp = 45,
                    startsRpg = true,
                    opensPuzzle = PuzzleType.VINE_WEAVER
                ),
                StoryChoice(
                    id = "dew_mosaic",
                    label = "[Quest Puzzle] Match the floating leaf patterns",
                    responseText = "Shimmering fern tiles drift onto the water, waiting for your pattern eye!",
                    rewardNectar = 10,
                    bondGain = 15,
                    rewardExp = 45,
                    startsRpg = true,
                    opensPuzzle = PuzzleType.FERN_MOSAIC
                )
            ),
            signaturePuzzle = PuzzleType.VINE_WEAVER
        ),
        BiomeNode(
            id = "orchid_canopy",
            index = 2,
            name = "Lantern Orchid Canopy",
            subtitle = "Sky-bridges, brass optics & twin blossoms",
            questTitle = "Ch. 3: Eyes of the High Observatory",
            questObjective = "Assist Aureowl in scanning twin charms and balancing the canopy runes.",
            creatureId = "aureowl",
            unlockNectar = 60,
            storyIntro = "High among Solari's suspended vine bridges, Aureowl adjusts his brass spyglass and invites you to spot twin canopy charms or balance four ancient runes.",
            storyChoices = listOf(
                StoryChoice(
                    id = "owl_listen",
                    label = "[Tech] Focus Solari's starlight spyglass",
                    responseText = "Aureowl hoots warmly and hands you a Moonstone Shard tucked inside a folded orchid petal.",
                    rewardNectar = 16,
                    bondGain = 22,
                    rewardExp = 45,
                    startsRpg = true,
                ),
                StoryChoice(
                    id = "owl_spotter",
                    label = "[Quest Puzzle] Play Canopy Spotter with Aureowl",
                    responseText = "Aureowl spreads his moon-plumed wings as glowing jungle charms appear in the foliage!",
                    rewardNectar = 10,
                    bondGain = 15,
                    rewardExp = 50,
                    startsRpg = true,
                    opensPuzzle = PuzzleType.CANOPY_SPOTTER
                ),
                StoryChoice(
                    id = "owl_puzzle",
                    label = "[Quest Puzzle] Balance Aureowl's Grove Grid",
                    responseText = "Aureowl nods approvingly as the four rune stones shimmer into place.",
                    rewardNectar = 10,
                    bondGain = 15,
                    rewardExp = 50,
                    startsRpg = true,
                    opensPuzzle = PuzzleType.GROVE_GRID
                )
            ),
            signaturePuzzle = PuzzleType.CANOPY_SPOTTER
        ),
        BiomeNode(
            id = "crystal_cavern",
            index = 3,
            name = "Crystal Root Cavern",
            subtitle = "Bioluminescent fungi & quartz chimes",
            questTitle = "Ch. 4: Symphony of the Root Vault",
            questObjective = "Order the glowing cavern mushrooms to awaken the quartz resonators.",
            creatureId = "glimmertoad",
            unlockNectar = 100,
            storyIntro = "Beneath intertwined tree roots, quartz resonators hum softly. Glimmertoad hops onto a glowing mushroom log, waiting for you to place each cap by its fairy clues.",
            storyChoices = listOf(
                StoryChoice(
                    id = "toad_chime",
                    label = "[Heart] Hum a cavern resonance chord",
                    responseText = "A warm chord ripples through the cavern! Glimmertoad croaks happily and shares sparkling nectar.",
                    rewardNectar = 18,
                    bondGain = 25,
                    rewardExp = 50,
                    startsRpg = true,
                ),
                StoryChoice(
                    id = "toad_puzzle",
                    label = "[Quest Puzzle] Solve Mushroom Melody",
                    responseText = "Glowing mushroom caps shimmer gently along the mossy log!",
                    rewardNectar = 12,
                    bondGain = 18,
                    rewardExp = 55,
                    startsRpg = true,
                    opensPuzzle = PuzzleType.MUSHROOM_MELODY
                )
            ),
            signaturePuzzle = PuzzleType.MUSHROOM_MELODY
        ),
        BiomeNode(
            id = "starbloom_treehouse",
            index = 4,
            name = "Starbloom Treehouse",
            subtitle = "Gossamer tapestries & botanical patterns",
            questTitle = "Ch. 5: Tapestry of the Starbloom Loom",
            questObjective = "Weave Velvetmoth's missing fern pattern across the gossamer bridge.",
            creatureId = "velvetmoth",
            unlockNectar = 150,
            storyIntro = "A lantern-lit observatory hangs woven in silver silk. Velvetmoth flutters beside a half-finished botanical mosaic, eager to see you complete its pattern.",
            storyChoices = listOf(
                StoryChoice(
                    id = "moth_gift",
                    label = "[Magic] Illuminate the starlight tapestry",
                    responseText = "Velvetmoth dusts your cloak with shimmering pollen, brightening the upper canopy.",
                    rewardNectar = 20,
                    bondGain = 25,
                    rewardExp = 55,
                    startsRpg = true,
                ),
                StoryChoice(
                    id = "moth_puzzle",
                    label = "[Quest Puzzle] Weave the Fern Mosaic",
                    responseText = "Velvetmoth's wings shimmer as the glowing leaves line up across the branch!",
                    rewardNectar = 12,
                    bondGain = 18,
                    rewardExp = 60,
                    startsRpg = true,
                    opensPuzzle = PuzzleType.FERN_MOSAIC
                )
            ),
            signaturePuzzle = PuzzleType.FERN_MOSAIC
        ),
        BiomeNode(
            id = "heartwood_shrine",
            index = 5,
            name = "Ancient Heartwood Shrine",
            subtitle = "The timeless sanctuary where all allies meet",
            questTitle = "Ch. 6: Crown of the United Canopy",
            questObjective = "Unseal the Heartwood Spirit Cipher alongside allies of every race.",
            creatureId = "brambleback",
            unlockNectar = 210,
            storyIntro = "At the heart of the oldest banyan, Solari's scholars and allied travelers from distant lands gather beneath Brambleback's mossy boughs to unlock the ancient Spirit Cipher.",
            storyChoices = listOf(
                StoryChoice(
                    id = "turtle_rest",
                    label = "[Heart] Share sanctuary tea with all allies",
                    responseText = "Peaceful golden warmth washes over the gathering. Brambleback rumbles a gentle blessing.",
                    rewardNectar = 25,
                    bondGain = 30,
                    rewardExp = 65,
                    startsRpg = true,
                ),
                StoryChoice(
                    id = "turtle_puzzle",
                    label = "[Quest Puzzle] Unlock the Spirit Cipher",
                    responseText = "Brambleback blinks slowly with pride as the ancient rune dials spin!",
                    rewardNectar = 15,
                    bondGain = 20,
                    rewardExp = 70,
                    startsRpg = true,
                    opensPuzzle = PuzzleType.SPIRIT_LOCK
                ),
                StoryChoice(
                    id = "turtle_spotter",
                    label = "[Quest Puzzle] Spot the shrine's hidden charms",
                    responseText = "Lanterns flicker across the shrine canopy—spot the matching treasures!",
                    rewardNectar = 15,
                    bondGain = 20,
                    rewardExp = 70,
                    startsRpg = true,
                    opensPuzzle = PuzzleType.CANOPY_SPOTTER
                )
            ),
            signaturePuzzle = PuzzleType.SPIRIT_LOCK
        )
    )

    val badges: List<BadgeDefinition> = listOf(
        BadgeDefinition(
            id = "first_spark",
            title = "First Firefly Spark",
            description = "Complete your very first brain-teaser mini puzzle.",
            thresholdLabel = "Solve 1 puzzle"
        ),
        BadgeDefinition(
            id = "kindred_spirit",
            title = "Kindred Spirit",
            description = "Befriend at least 3 mythical jungle creatures.",
            thresholdLabel = "Discover 3 creatures"
        ),
        BadgeDefinition(
            id = "echo_keeper",
            title = "Echo Keeper",
            description = "Reach 100 Memory XP in Firefly Echo.",
            skill = BrainSkill.MEMORY,
            thresholdLabel = "100 Memory XP"
        ),
        BadgeDefinition(
            id = "rune_scholar",
            title = "Rune Scholar",
            description = "Reach 100 Logic XP in Mushroom Melody or Grove Grid.",
            skill = BrainSkill.LOGIC,
            thresholdLabel = "100 Logic XP"
        ),
        BadgeDefinition(
            id = "stream_weaver",
            title = "Stream Weaver",
            description = "Reach 100 Spatial XP in Vine Weaver.",
            skill = BrainSkill.SPATIAL,
            thresholdLabel = "100 Spatial XP"
        ),
        BadgeDefinition(
            id = "cipher_whisperer",
            title = "Pattern & Cipher Sage",
            description = "Reach 100 Deduction XP in Fern Mosaic or Spirit Lock.",
            skill = BrainSkill.DEDUCTION,
            thresholdLabel = "100 Deduction XP"
        ),
        BadgeDefinition(
            id = "keen_eyed",
            title = "Keen Canopy Eye",
            description = "Reach 100 Observation XP in Canopy Spotter.",
            skill = BrainSkill.OBSERVATION,
            thresholdLabel = "100 Observation XP"
        ),
        BadgeDefinition(
            id = "canopy_polymath",
            title = "Canopy Polymath",
            description = "Win at least 1 puzzle in all 7 mini-puzzle types.",
            thresholdLabel = "All 7 puzzles won"
        ),
        BadgeDefinition(
            id = "canopy_guardian",
            title = "Canopy Guardian",
            description = "Solve 15 mind-teasing puzzles across the sanctuary.",
            thresholdLabel = "15 puzzles solved"
        )
    )

    val loreEntries: List<LoreEntry> = listOf(
        LoreEntry(
            id = "solari_spires",
            title = "The Sun-Weave Spires of Solari",
            category = LoreCategory.SOLARI_SOCIETY,
            chronicle = "High above the mist, the thriving all-Black scholars and artisans of Solari Canopy weave living mahogany, resonant brass, and solar-quartz into weightless sky-bridges that nourish the forest without leaving a scar.",
            unlockHint = "Known from the beginning of your journey.",
            starterUnlocked = true
        ),
        LoreEntry(
            id = "moss_hollow_chronicle",
            title = "Whispering Moss Hollow & Spore-Lanterns",
            category = LoreCategory.JUNGLE_BIOMES,
            chronicle = "Solari's youngest botanists calibrate floating spore-lanterns along the lower boughs, where Lumafern's golden antlers answer every hum with warm bioluminescence.",
            unlockHint = "Known from the beginning of your journey.",
            starterUnlocked = true
        ),
        LoreEntry(
            id = "solari_forge_craft",
            title = "Harmonic Quartz & Magitech Circuits",
            category = LoreCategory.SOLARI_SOCIETY,
            chronicle = "In Solari's academy workshops, engineers braid conductive golden filaments through fallen heartwood, creating visors, astrolabes, and staffs powered purely by morning sunlight and Moonstone resonance.",
            unlockHint = "Solve 1 mini-puzzle to uncover Solari's craft chronicle.",
            requiredPuzzlesSolved = 1
        ),
        LoreEntry(
            id = "dew_aqueduct_scroll",
            title = "The Living Aqueducts of Dew Lagoon",
            category = LoreCategory.JUNGLE_BIOMES,
            chronicle = "Carved bamboo sluices and Solari hydro-valves lift cool spring water toward sleeping Moonflowers while keeping Dewdrake's favorite lily pools crystal clear.",
            unlockHint = "Gather 25 Sunbeam Nectar or complete Ch. 2 to uncover.",
            requiredNectar = 25,
            requiredBiomeQuestId = "dew_lagoon"
        ),
        LoreEntry(
            id = "cloud_terrace_accord",
            title = "The Cloud-Terrace Glider Accord",
            category = LoreCategory.ALLIED_REALMS,
            chronicle = "Guided by Mei-Lin Zhao's silk-wing charts, envoys from the eastern mist terraces established friendly trade routes with Solari Canopy—exchanging starlight maps for living brass optics.",
            unlockHint = "Gather 40 Sunbeam Nectar to welcome Cloud-Terrace allies.",
            requiredNectar = 40
        ),
        LoreEntry(
            id = "orchid_observatory",
            title = "Brass Spyglasses of Orchid Canopy",
            category = LoreCategory.CREATURE_MYTHS,
            chronicle = "Aureowl sits beside Solari's high canopy telescopes, keeping meticulous sky-ledgers of twin blossoms, migrating fireflies, and ancient rune constellations.",
            unlockHint = "Gather 60 Sunbeam Nectar or befriend Aureowl to uncover.",
            requiredNectar = 60,
            requiredCreatureId = "aureowl"
        ),
        LoreEntry(
            id = "riverbend_hydro_pact",
            title = "Riverbend Hydro-Mages & Lagoon Songs",
            category = LoreCategory.ALLIED_REALMS,
            chronicle = "Riverbend Guild artisan Mateo Solano taught Solari's lagoon caretakers copper-gauntlet water shaping, turning seasonal rainstorms into gentle mist rainbows.",
            unlockHint = "Gather 80 Sunbeam Nectar to welcome Riverbend allies.",
            requiredNectar = 80
        ),
        LoreEntry(
            id = "quartz_root_symphony",
            title = "Bioluminescent Choirs of Crystal Root",
            category = LoreCategory.CREATURE_MYTHS,
            chronicle = "Deep beneath the banyan roots, Glimmertoad conducts nightly light-choruses by arranging glowing mushrooms in exact logic scales that echo through quartz caves.",
            unlockHint = "Gather 100 Sunbeam Nectar or complete Ch. 4 to uncover.",
            requiredNectar = 100,
            requiredBiomeQuestId = "crystal_cavern"
        ),
        LoreEntry(
            id = "elderwood_deepquartz_fellowship",
            title = "Fellowship of Canopy, Wood & Stone",
            category = LoreCategory.ALLIED_REALMS,
            chronicle = "When Elderwood Elf Aelindra Moonbough and Deep-Quartz Dwarf Thorek Mossbeard stepped onto Solari's sun-bridges, all peoples united around the Heartwood Shrine to safeguard the jungle's magic.",
            unlockHint = "Gather 130 Sunbeam Nectar to welcome Elderwood & Deep-Quartz allies.",
            requiredNectar = 130
        ),
        LoreEntry(
            id = "heartwood_sanctuary_oath",
            title = "The Timeless Heartwood Covenant",
            category = LoreCategory.JUNGLE_BIOMES,
            chronicle = "Beneath Brambleback's mossy shell rests the oldest cipher lock in the sanctuary—said to open only when memory, logic, spatial vision, deduction, and keen observation shine together.",
            unlockHint = "Solve 5 mini-puzzles or reach 210 Sunbeam Nectar to uncover.",
            requiredNectar = 210,
            requiredPuzzlesSolved = 5,
            requiredBiomeQuestId = "heartwood_shrine"
        )
    )

    fun isLoreDiscovered(
        entry: LoreEntry,
        nectar: Int,
        totalPuzzlesSolved: Int,
        completedQuests: Set<String>,
        discoveredCreatures: Set<String>,
        savedLoreIds: Set<String>
    ): Boolean {
        if (entry.starterUnlocked || entry.id in savedLoreIds) return true
        if (entry.requiredBiomeQuestId != null && entry.requiredBiomeQuestId in completedQuests) return true
        if (entry.requiredCreatureId != null && entry.requiredCreatureId in discoveredCreatures) return true
        if (entry.requiredPuzzlesSolved > 0 && totalPuzzlesSolved >= entry.requiredPuzzlesSolved) return true
        if (entry.requiredNectar > 0 && nectar >= entry.requiredNectar) return true
        return false
    }

    fun creatureById(id: String): MythicalCreature =
        creatures.firstOrNull { it.id == id } ?: creatures.first()

    fun heroById(id: String): PlayableHero =
        heroes.firstOrNull { it.id == id } ?: heroes.first()

    fun relicById(id: String): RpgRelic =
        relics.firstOrNull { it.id == id } ?: relics.first()
}
