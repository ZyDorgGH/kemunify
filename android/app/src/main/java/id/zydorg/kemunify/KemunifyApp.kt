package id.zydorg.kemunify

import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ImageSearch
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import id.zydorg.kemunify.ui.navigation.Screen
import id.zydorg.kemunify.ui.screen.camera.CameraScreen
import id.zydorg.kemunify.ui.screen.detail.DetailScreen
import id.zydorg.kemunify.ui.screen.detection.ImageDetectionScreen
import id.zydorg.kemunify.ui.screen.home.HomeScreen
import id.zydorg.kemunify.ui.screen.profile.ProfileScreen
import id.zydorg.kemunify.ui.screen.waste.AddWasteScreen
import id.zydorg.kemunify.ui.theme.DarkGreen
import id.zydorg.kemunify.ui.theme.LightGreen40
import id.zydorg.kemunify.ui.theme.LightGreen80

@RequiresApi(Build.VERSION_CODES.Q)

@Composable
fun KemunifyApp (
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()

) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route


    Scaffold (
        modifier = modifier,
        bottomBar = {
            if (currentRoute in listOf(
                    Screen.Home.route,
                    Screen.Detect.route,
                    Screen.Profile.route
                )
            )
                BottomBar(navController = navController, currentRoute = currentRoute)
        }

    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            enterTransition = {
                slideInHorizontally(initialOffsetX = { 1000 }) + fadeIn()
            },
            exitTransition = {
                slideOutHorizontally(targetOffsetX = { -1000 }) + fadeOut()
            },
            popEnterTransition = {
                slideInHorizontally(initialOffsetX = { -1000 }) + fadeIn()
            },
            popExitTransition = {
                slideOutHorizontally(targetOffsetX = { 1000 }) + fadeOut()
            },
            modifier = Modifier.padding(innerPadding)
        ){
            composable(
                route = Screen.Camera.route
            ){
                CameraScreen(
                    navigateToDetail = {photo ->
                        navController.navigate(Screen.Detect.takePhoto(photo))
                    })
            }
            composable(
                route = Screen.Detect.route,
                arguments = listOf(navArgument("photo")
                { type = NavType.StringType
                    nullable = true
                }),
            ){
                val photoUri = it.arguments?.getString("photo")?.let { Uri.parse(it) }
                ImageDetectionScreen(modifier = modifier,
                    photoTaken = photoUri.toString(),
                    navigateToCamera = {navController.navigate(Screen.Camera.route)}
                )
            }
            composable(
                route = Screen.Home.route
            ){
                HomeScreen(
                    navigateToDetail = { customer ->
                    navController.navigate(Screen.DetailCustomer.createRoute(customer))
                },
                    navigateToAddWaste = {navController.navigate(Screen.AddWaste.route)}
                )
            }

            composable(
                route = Screen.DetailCustomer.route,
                arguments = listOf(navArgument("customer") { type = NavType.StringType }),
            ){
                val customerId = it.arguments?.getString("customer") ?: ""
                DetailScreen(
                    customerId = customerId,
                    navigateBack = {navController.popBackStack()}
                    )
            }
            composable(
                route = Screen.AddWaste.route
            ){
                AddWasteScreen(onNavigateUp = { navController.navigate(Screen.Home.route) },)
            }

            composable(
                route = Screen.Profile.route
            ){
                ProfileScreen()
            }
        }
    }
}

data class NavigationItem (
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val screen: Screen
)


@Composable
private fun BottomBar(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    currentRoute: String?
) {
    NavigationBar(
        modifier = modifier,
        containerColor = LightGreen40.copy(alpha = 0.2f)
    ) {
        val navigationItems = listOf(
            NavigationItem(
                title = "Home",
                selectedIcon = Icons.Default.Home,
                unselectedIcon = Icons.Outlined.Home,
                screen = Screen.Home
            ),
            NavigationItem(
                title = "Detect Image",
                selectedIcon = Icons.Default.ImageSearch,
                unselectedIcon = Icons.Outlined.ImageSearch,
                screen = Screen.Detect
            ),
            NavigationItem(
                title = "Profile",
                selectedIcon = Icons.Default.AccountCircle,
                unselectedIcon = Icons.Outlined.AccountCircle,
                screen = Screen.Profile
            ),
        )

        navigationItems.map { item ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = if (currentRoute == item.screen.route) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.title
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = DarkGreen,
                    unselectedIconColor = DarkGreen.copy(alpha = 0.7f),
                    selectedTextColor = DarkGreen,
                    unselectedTextColor = DarkGreen.copy(alpha = 0.7f),
                    indicatorColor = LightGreen80
                ),
                label = { Text(item.title) },
                selected = currentRoute == item.screen.route,
                onClick = {
                    navController.navigate(item.screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        restoreState = true
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}