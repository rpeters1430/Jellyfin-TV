package com.example.jellyfintv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Surface
import com.example.jellyfintv.data.model.MediaItem
import com.example.jellyfintv.data.preferences.AndroidServerPreferences
import com.example.jellyfintv.data.repository.JellyfinRepository
import com.example.jellyfintv.ui.components.UpdateDialog
import com.example.jellyfintv.ui.screens.*
import com.example.jellyfintv.ui.theme.JellyfinTVTheme

sealed class Screen {
    object Connect : Screen()
    object Home : Screen()
    object Search : Screen()
    data class Library(
        val parentId: String? = null,
        val collectionType: String? = null,
        val mediaType: String? = null,
        val title: String
    ) : Screen()
    data class Detail(val itemId: String) : Screen()
    data class Player(val media: MediaItem) : Screen()
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = AndroidServerPreferences(applicationContext)
        val repository = JellyfinRepository(prefs)

        setContent {
            JellyfinTVTheme {
                var currentScreen by remember {
                    mutableStateOf<Screen>(if (prefs.isLoggedIn) Screen.Home else Screen.Connect)
                }

                BackHandler(enabled = currentScreen !is Screen.Connect) {
                    when (currentScreen) {
                        is Screen.Detail -> currentScreen = Screen.Home
                        is Screen.Search -> currentScreen = Screen.Home
                        is Screen.Library -> currentScreen = Screen.Home
                        is Screen.Player -> {
                            val media = (currentScreen as Screen.Player).media
                            val targetId = media.seriesId ?: media.id
                            currentScreen = Screen.Detail(targetId)
                        }
                        is Screen.Home -> {
                            finish()
                        }
                        else -> {}
                    }
                }

                val handleLogout = {
                    repository.logout()
                    currentScreen = Screen.Connect
                }

                val context = LocalContext.current
                val updateViewModel: UpdateViewModel = viewModel { UpdateViewModel(context.applicationContext) }
                val updateState by updateViewModel.uiState.collectAsStateWithLifecycle()

                // androidx.tv.material3.Surface, not androidx.compose.material3.Surface - the
                // latter reads from M3's own LocalColorScheme, which JellyfinTVTheme (built on
                // tv.material3.MaterialTheme) never populates, so its default color silently
                // ignored the app's DarkTVColorScheme.
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                    when (val screen = currentScreen) {
                        is Screen.Connect -> {
                            ConnectScreen(
                                repository = repository,
                                onConnected = { currentScreen = Screen.Home }
                            )
                        }
                        is Screen.Home -> {
                            HomeScreen(
                                repository = repository,
                                onMediaSelected = { media -> currentScreen = Screen.Detail(media.id) },
                                onPlayMedia = { media -> currentScreen = Screen.Player(media) },
                                onNavigateToSearch = { currentScreen = Screen.Search },
                                onNavigateToLibrary = { lib ->
                                    currentScreen = Screen.Library(
                                        parentId = lib.id,
                                        collectionType = lib.collectionType,
                                        title = lib.name
                                    )
                                },
                                onLogout = handleLogout
                            )
                        }
                        is Screen.Search -> {
                            SearchScreen(
                                repository = repository,
                                onMediaSelected = { media ->
                                    val targetId = if (media.type.equals("Episode", ignoreCase = true) && !media.seriesId.isNullOrEmpty()) {
                                        media.seriesId
                                    } else {
                                        media.id
                                    }
                                    currentScreen = Screen.Detail(targetId)
                                },
                                onBack = { currentScreen = Screen.Home },
                                onNavigateHome = { currentScreen = Screen.Home },
                                onNavigateLibrary = { lib ->
                                    currentScreen = Screen.Library(
                                        parentId = lib.id,
                                        collectionType = lib.collectionType,
                                        title = lib.name
                                    )
                                },
                                onLogout = handleLogout
                            )
                        }
                        is Screen.Library -> {
                            LibraryScreen(
                                parentId = screen.parentId,
                                collectionType = screen.collectionType,
                                mediaType = screen.mediaType,
                                title = screen.title,
                                repository = repository,
                                onMediaSelected = { media -> currentScreen = Screen.Detail(media.id) },
                                onPlayMedia = { media -> currentScreen = Screen.Player(media) },
                                onBack = { currentScreen = Screen.Home },
                                onNavigateHome = { currentScreen = Screen.Home },
                                onNavigateSearch = { currentScreen = Screen.Search },
                                onNavigateLibrary = { lib ->
                                    currentScreen = Screen.Library(
                                        parentId = lib.id,
                                        collectionType = lib.collectionType,
                                        title = lib.name
                                    )
                                },
                                onLogout = handleLogout
                            )
                        }
                        is Screen.Detail -> {
                            MediaDetailScreen(
                                itemId = screen.itemId,
                                repository = repository,
                                onPlay = { media -> currentScreen = Screen.Player(media) },
                                onBack = { currentScreen = Screen.Home },
                                onLogout = handleLogout
                            )
                        }
                        is Screen.Player -> {
                            PlayerScreen(
                                media = screen.media,
                                repository = repository,
                                onBack = {
                                    val targetId = screen.media.seriesId ?: screen.media.id
                                    currentScreen = Screen.Detail(targetId)
                                },
                                onPlayNext = { nextMedia ->
                                    currentScreen = Screen.Player(nextMedia)
                                }
                            )
                        }
                    }

                    UpdateDialog(
                        state = updateState,
                        onUpdateNow = { updateViewModel.downloadAndInstall() },
                        onLater = { updateViewModel.dismiss() },
                        onOpenInstallSettings = {
                            context.startActivity(updateViewModel.installPermissionSettingsIntent())
                            updateViewModel.onInstallPermissionHandled()
                        }
                    )
                    }
                }
            }
        }
    }
}

