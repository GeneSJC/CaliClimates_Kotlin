package io.github.genesjc.climatefinder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import io.github.genesjc.climatefinder.ui.theme.AppBackground
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.CompositionLocalProvider
import io.github.genesjc.climatefinder.ui.theme.LocalStateUsa
import io.github.genesjc.climatefinder.ui.home.HomeViewModel
import io.github.genesjc.climatefinder.ui.root.AppRootViewModel
import io.github.genesjc.climatefinder.ui.theme.CaliClimatesTheme
import androidx.compose.ui.graphics.asImageBitmap
import io.github.genesjc.climatefinder.ui.navigation.AppNavigation
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // The look (dark or light, remembered between launches) and the backdrop's texture live in the ViewModel;
            // the ViewModels are created by the standard factory and take their helpers from the dependency container
            val appRoot: AppRootViewModel = viewModel()
            val homeViewModel: HomeViewModel = viewModel()
            val darkMode by appRoot.darkMode.collectAsStateWithLifecycle()
            val backdropBitmap by appRoot.backdropTexture.collectAsStateWithLifecycle()
            val backdropTexture = remember(backdropBitmap) { backdropBitmap?.asImageBitmap() }
            // The backdrop takes a soft tint of the selected region's colour
            val backdropRegionId by homeViewModel.selectedRegionId.collectAsStateWithLifecycle()
            // The opening view shows once per launch (and not again after a rotation)
            var showIntro by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(true) }
            CompositionLocalProvider(LocalStateUsa provides appRoot.stateUsa) {
                CaliClimatesTheme(darkTheme = darkMode) {
                    androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
                    AppBackground(darkTheme = darkMode, regionId = backdropRegionId, texture = backdropTexture) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = Color.Transparent,
                            // The default text colour for everything below, so text without its own colour is
                            // dark on light and light on dark
                            contentColor = MaterialTheme.colorScheme.onBackground
                        ) {
                            AppNavigation(
                                darkMode = darkMode,
                                onDarkModeChange = appRoot::setDarkMode,
                                homeViewModel = homeViewModel
                            )
                        }
                    }
                    if (showIntro) {
                        io.github.genesjc.climatefinder.ui.components.IntroOverlay(onDone = { showIntro = false })
                    }
                    }
                }
            }
        }
    }
}

