package com.rajiv.minimilitia.clone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.rajiv.minimilitia.clone.assets.SpriteRepository
import com.rajiv.minimilitia.clone.audio.GameSoundManager
import com.rajiv.minimilitia.clone.engine.GameLoopState
import com.rajiv.minimilitia.clone.ui.AppScreen
import com.rajiv.minimilitia.clone.ui.ArmoryInspectorScreen
import com.rajiv.minimilitia.clone.ui.GameArenaScreen
import com.rajiv.minimilitia.clone.ui.MainMenuScreen

class MainActivity : ComponentActivity() {

    private lateinit var soundManager: GameSoundManager
    private lateinit var spriteRepo: SpriteRepository
    private lateinit var gameState: GameLoopState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Force full-screen immersive mode
        hideSystemUi()

        // Initialize Audio & Sprites
        soundManager = GameSoundManager(this)
        spriteRepo = SpriteRepository(this)
        spriteRepo.loadAll()

        gameState = GameLoopState(this, soundManager, spriteRepo)

        setContent {
            var currentScreen by remember { mutableStateOf(AppScreen.MAIN_MENU) }

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black
            ) {
                when (currentScreen) {
                    AppScreen.MAIN_MENU -> {
                        MainMenuScreen(
                            spriteRepo = spriteRepo,
                            soundManager = soundManager,
                            onNavigate = { screen ->
                                if (screen == AppScreen.SURVIVAL_ARENA || screen == AppScreen.CATACOMBS_ARENA) {
                                    gameState.restartGame()
                                } else if (screen == AppScreen.TRAINING_RANGE) {
                                    gameState.restartGame()
                                    // Remove bots in training mode for solo range practice
                                    gameState.botController.bots.clear()
                                }
                                currentScreen = screen
                            }
                        )
                    }

                    AppScreen.SURVIVAL_ARENA, AppScreen.TRAINING_RANGE, AppScreen.CATACOMBS_ARENA -> {
                        GameArenaScreen(
                            gameState = gameState,
                            onExitToMenu = {
                                soundManager.stopJetpackThrust()
                                currentScreen = AppScreen.MAIN_MENU
                            }
                        )
                    }

                    AppScreen.ARMORY_INSPECTOR -> {
                        ArmoryInspectorScreen(
                            spriteRepo = spriteRepo,
                            soundManager = soundManager,
                            onBack = { currentScreen = AppScreen.MAIN_MENU }
                        )
                    }
                }
            }
        }
    }

    private fun hideSystemUi() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    override fun onResume() {
        super.onResume()
        hideSystemUi()
    }

    override fun onPause() {
        super.onPause()
        soundManager.stopJetpackThrust()
        soundManager.stopThemeMusic()
    }

    override fun onDestroy() {
        super.onDestroy()
        soundManager.release()
        spriteRepo.clear()
    }
}
