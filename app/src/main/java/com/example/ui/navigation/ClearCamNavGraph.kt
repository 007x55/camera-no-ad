package com.example.ui.navigation

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.ClearCamAppContainer
import com.example.R
import com.example.data.local.AppPreferences
import com.example.ui.screens.AppSettingsScreen
import com.example.ui.screens.CameraSettingsScreen
import com.example.ui.screens.CamerasScreen
import com.example.ui.screens.DeviceCameraScreen
import com.example.ui.screens.GalleryScreen
import com.example.ui.screens.IpCameraLiveScreen
import com.example.ui.screens.MediaPreviewScreen
import com.example.ui.viewmodel.AppSettingsViewModel
import com.example.ui.viewmodel.CameraListViewModel
import com.example.ui.viewmodel.CameraSettingsViewModel
import com.example.ui.viewmodel.DeviceCameraViewModel
import com.example.ui.viewmodel.GalleryViewModel
import com.example.ui.viewmodel.IpCameraLiveViewModel
import kotlinx.serialization.Serializable

@Serializable data object CamerasRoute

@Serializable data object DeviceCameraRoute

@Serializable data object GalleryRoute

@Serializable data object AppSettingsRoute

@Serializable data class IpCameraLiveRoute(val cameraId: Long)

@Serializable data class CameraSettingsRoute(val cameraId: Long)

@Serializable data class MediaPreviewRoute(val mediaId: Long)

private data class TopLevelTab(
  val route: Any,
  val labelRes: Int,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val tag: String,
)

@Composable
fun ClearCamNavRoot(
  appContainer: ClearCamAppContainer,
  preferences: AppPreferences,
  appSettingsViewModel: AppSettingsViewModel,
) {
  val navController = rememberNavController()
  val backStackEntry by navController.currentBackStackEntryAsState()
  val currentDest = backStackEntry?.destination

  val tabs =
    listOf(
      TopLevelTab(
        route = CamerasRoute,
        labelRes = R.string.nav_cameras,
        selectedIcon = Icons.Rounded.Videocam,
        unselectedIcon = Icons.Outlined.Videocam,
        tag = "tab_cameras",
      ),
      TopLevelTab(
        route = DeviceCameraRoute,
        labelRes = R.string.nav_device_cam,
        selectedIcon = Icons.Rounded.CameraAlt,
        unselectedIcon = Icons.Outlined.CameraAlt,
        tag = "tab_device_cam",
      ),
      TopLevelTab(
        route = GalleryRoute,
        labelRes = R.string.nav_gallery,
        selectedIcon = Icons.Rounded.PhotoLibrary,
        unselectedIcon = Icons.Outlined.PhotoLibrary,
        tag = "tab_gallery",
      ),
      TopLevelTab(
        route = AppSettingsRoute,
        labelRes = R.string.nav_settings,
        selectedIcon = Icons.Rounded.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        tag = "tab_settings",
      ),
    )

  val isTopLevel =
    currentDest == null ||
      currentDest.hasRoute<CamerasRoute>() ||
      currentDest.hasRoute<DeviceCameraRoute>() ||
      currentDest.hasRoute<GalleryRoute>() ||
      currentDest.hasRoute<AppSettingsRoute>()

  fun navigateTab(route: Any) {
    navController.navigate(route) {
      popUpTo(navController.graph.findStartDestination().id) { saveState = true }
      launchSingleTop = true
      restoreState = true
    }
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useNavRail = maxWidth >= 600.dp

    Scaffold(
      contentWindowInsets = WindowInsets(0, 0, 0, 0),
      bottomBar = {
        if (isTopLevel && !useNavRail) {
          NavigationBar(
            windowInsets = WindowInsets.navigationBars,
            containerColor = MaterialTheme.colorScheme.surface,
          ) {
            tabs.forEach { tab ->
              val selected =
                when (tab.route) {
                  is CamerasRoute -> currentDest?.hasRoute<CamerasRoute>() == true
                  is DeviceCameraRoute -> currentDest?.hasRoute<DeviceCameraRoute>() == true
                  is GalleryRoute -> currentDest?.hasRoute<GalleryRoute>() == true
                  is AppSettingsRoute -> currentDest?.hasRoute<AppSettingsRoute>() == true
                  else -> false
                }
              val label = stringResource(tab.labelRes)
              NavigationBarItem(
                selected = selected,
                onClick = { navigateTab(tab.route) },
                icon = {
                  Icon(
                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                    contentDescription = label,
                  )
                },
                label = { Text(label) },
                modifier = Modifier.testTag(tab.tag),
              )
            }
          }
        }
      },
    ) { innerPadding ->
      Row(
        modifier =
          Modifier.fillMaxSize()
            .let { mod ->
              if (isTopLevel && !useNavRail) {
                mod.padding(bottom = innerPadding.calculateBottomPadding())
              } else {
                mod
              }
            }
      ) {
        if (isTopLevel && useNavRail) {
          NavigationRail(
            modifier = Modifier.safeDrawingPadding(),
            containerColor = MaterialTheme.colorScheme.surface,
          ) {
            tabs.forEach { tab ->
              val selected =
                when (tab.route) {
                  is CamerasRoute -> currentDest?.hasRoute<CamerasRoute>() == true
                  is DeviceCameraRoute -> currentDest?.hasRoute<DeviceCameraRoute>() == true
                  is GalleryRoute -> currentDest?.hasRoute<GalleryRoute>() == true
                  is AppSettingsRoute -> currentDest?.hasRoute<AppSettingsRoute>() == true
                  else -> false
                }
              val label = stringResource(tab.labelRes)
              NavigationRailItem(
                selected = selected,
                onClick = { navigateTab(tab.route) },
                icon = {
                  Icon(
                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                    contentDescription = label,
                  )
                },
                label = { Text(label) },
                modifier = Modifier.testTag(tab.tag),
              )
            }
          }
        }

        NavHost(
          navController = navController,
          startDestination = CamerasRoute,
          modifier =
            Modifier.weight(1f)
              .fillMaxSize()
              .let { mod ->
                if (isTopLevel) {
                  if (useNavRail) mod.statusBarsPadding().navigationBarsPadding()
                  else mod.statusBarsPadding()
                } else {
                  mod
                }
              },
        ) {
          composable<CamerasRoute> {
            val vm: CameraListViewModel =
              viewModel(factory = CameraListViewModel.factory(appContainer.repository))
            CamerasScreen(
              viewModel = vm,
              defaultForceTcp = preferences.defaultForceTcp,
              onOpenDeviceCamera = { navigateTab(DeviceCameraRoute) },
              onOpenLiveCamera = { camId -> navController.navigate(IpCameraLiveRoute(camId)) },
              onOpenCameraSettings = { camId ->
                navController.navigate(CameraSettingsRoute(camId))
              },
            )
          }

          composable<DeviceCameraRoute> {
            val vm: DeviceCameraViewModel =
              viewModel(factory = DeviceCameraViewModel.factory(appContainer.repository))
            DeviceCameraScreen(
              viewModel = vm,
              repository = appContainer.repository,
              onOpenGalleryItem = { mediaId -> navController.navigate(MediaPreviewRoute(mediaId)) },
              onOpenGalleryTab = { navigateTab(GalleryRoute) },
            )
          }

          composable<GalleryRoute> {
            val vm: GalleryViewModel =
              viewModel(factory = GalleryViewModel.factory(appContainer.repository))
            GalleryScreen(
              viewModel = vm,
              onOpenMedia = { mediaId -> navController.navigate(MediaPreviewRoute(mediaId)) },
            )
          }

          composable<AppSettingsRoute> { AppSettingsScreen(viewModel = appSettingsViewModel) }

          composable<IpCameraLiveRoute> { entry ->
            val route: IpCameraLiveRoute = entry.toRoute()
            val vm: IpCameraLiveViewModel =
              viewModel(
                key = "ip_live_${route.cameraId}",
                factory = IpCameraLiveViewModel.factory(route.cameraId, appContainer.repository),
              )
            IpCameraLiveScreen(
              viewModel = vm,
              showHudOverlay = preferences.showHudOverlay,
              onBack = { navController.popBackStack() },
              onOpenSettings = { camId -> navController.navigate(CameraSettingsRoute(camId)) },
            )
          }

          composable<CameraSettingsRoute> { entry ->
            val route: CameraSettingsRoute = entry.toRoute()
            val vm: CameraSettingsViewModel =
              viewModel(
                key = "cam_settings_${route.cameraId}",
                factory = CameraSettingsViewModel.factory(route.cameraId, appContainer.repository),
              )
            CameraSettingsScreen(
              viewModel = vm,
              onBack = { navController.popBackStack() },
              onDeleted = {
                navController.popBackStack(CamerasRoute, inclusive = false)
              },
            )
          }

          composable<MediaPreviewRoute> { entry ->
            val route: MediaPreviewRoute = entry.toRoute()
            val vm: GalleryViewModel =
              viewModel(factory = GalleryViewModel.factory(appContainer.repository))
            MediaPreviewScreen(
              mediaId = route.mediaId,
              viewModel = vm,
              onBack = { navController.popBackStack() },
            )
          }
        }
      }
    }
  }
}
