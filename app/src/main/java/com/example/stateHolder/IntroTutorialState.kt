package com.example.stateHolder

import com.example.rpg.IntroStage
import com.example.rpg.IntroTutorialEngine
import com.example.rpg.RpgWorldState

data class IntroTutorialState(
    val stage: IntroStage = IntroStage.CUTSCENE,
    val cutscenePage: Int = 0,
    val worldState: RpgWorldState = IntroTutorialEngine.initialWorldState(),
    val lanternLit: Boolean = false,
    val shrinePuzzleSolved: Boolean = false,
    val sproutlingDefeated: Boolean = false
)