package com.ecobook.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ecobook.auth.AuthScreen
import com.ecobook.discovery.DiscoveryScreen
import com.ecobook.auth.LogoutViewModel
import com.ecobook.model.SessionDestination
import com.ecobook.onboarding.OnboardingScreen
import com.ecobook.request.DonorRequestsScreen
import com.ecobook.request.MyRequestsScreen
import com.ecobook.ui.EcoBookViewModel
import com.ecobook.ui.screens.DonateScreen
import com.ecobook.ui.screens.HomeScreen
import com.ecobook.ui.screens.ProfileScreen

@Composable
fun NavGraph() {
    val viewModel: EcoBookViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sessionDestination = uiState.session.destination
    val mainDestinations = listOf(
        AppDestination.Home,
        AppDestination.Discovery,
        AppDestination.Donate,
        AppDestination.Profile
    )

    key(sessionDestination) {
        val navController = rememberNavController()
        val currentDestination = navController.currentBackStackEntryAsState().value?.destination
        val showBottomBar = sessionDestination == SessionDestination.MAIN

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFF6E7CF),
                            Color(0xFFF7F2E7),
                            Color(0xFFE2EFE6)
                        )
                    )
                )
        ) {
            Scaffold(
                containerColor = Color.Transparent,
                bottomBar = {
                    if (showBottomBar) {
                        Surface(
                            tonalElevation = 8.dp,
                            shadowElevation = 8.dp,
                            color = Color.White.copy(alpha = 0.94f)
                        ) {
                            NavigationBar(containerColor = Color.Transparent) {
                                mainDestinations.forEach { destination ->
                                    val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = {
                                            navController.navigate(destination.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = destination.icon,
                                                contentDescription = destination.label
                                            )
                                        },
                                        label = { Text(destination.label) },
                                        colors = NavigationBarItemDefaults.colors(
                                            indicatorColor = Color(0xFFDAEBDD),
                                            selectedIconColor = Color(0xFF205447),
                                            selectedTextColor = Color(0xFF205447),
                                            unselectedIconColor = Color(0xFF5F746B),
                                            unselectedTextColor = Color(0xFF5F746B)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = when (sessionDestination) {
                        SessionDestination.AUTH -> AppDestination.Auth.route
                        SessionDestination.ONBOARDING -> AppDestination.Onboarding.route
                        SessionDestination.MAIN -> AppDestination.Home.route
                    },
                    modifier = Modifier.padding(innerPadding)
                ) {
                    composable(AppDestination.Auth.route) {
                        AuthScreen(
                            sessionMessage = uiState.session.lastErrorMessage,
                            backendStatus = uiState.backendStatus,
                            onRefreshBackend = viewModel::refreshBackendStatus
                        )
                    }
                    composable(AppDestination.Onboarding.route) {
                        val logoutViewModel: LogoutViewModel = hiltViewModel()
                        OnboardingScreen(
                            onLogout = logoutViewModel::logout
                        )
                    }
                    composable(AppDestination.Home.route) {
                        HomeScreen(
                            uiState = uiState,
                            onRefreshBackend = viewModel::refreshBackendStatus,
                            onOpenDiscovery = { navController.navigate(AppDestination.Discovery.route) },
                            onOpenDonate = { navController.navigate(AppDestination.Donate.route) },
                            onOpenMyRequests = { navController.navigate(AppDestination.MyRequests.route) },
                            onOpenDonorRequests = { navController.navigate(AppDestination.DonorRequests.route) },
                            onOpenProfile = { navController.navigate(AppDestination.Profile.route) }
                        )
                    }
                    composable(AppDestination.Discovery.route) {
                        DiscoveryScreen(
                            onOpenMyRequests = { navController.navigate(AppDestination.MyRequests.route) }
                        )
                    }
                    composable(AppDestination.Donate.route) {
                        DonateScreen(
                            onOpenDonorRequests = { navController.navigate(AppDestination.DonorRequests.route) }
                        )
                    }
                    composable(AppDestination.MyRequests.route) {
                        MyRequestsScreen()
                    }
                    composable(AppDestination.DonorRequests.route) {
                        DonorRequestsScreen()
                    }
                    composable(AppDestination.Profile.route) {
                        val logoutViewModel: LogoutViewModel = hiltViewModel()
                        ProfileScreen(
                            uiState = uiState,
                            onToggleAiConsent = viewModel::updateAiConsent,
                            onLogout = logoutViewModel::logout
                        )
                    }
                }
            }
        }
    }
}

private sealed class AppDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    data object Auth : AppDestination("auth", "Entrar", Icons.Rounded.AccountCircle)
    data object Onboarding : AppDestination("onboarding", "Onboarding", Icons.Rounded.AccountCircle)
    data object Home : AppDestination("home", "Painel", Icons.Rounded.AutoAwesome)
    data object Discovery : AppDestination("discovery", "Buscar", Icons.Rounded.Explore)
    data object Donate : AppDestination("donate", "Doar", Icons.Rounded.VolunteerActivism)
    data object MyRequests : AppDestination("my-requests", "Solicitacoes", Icons.Rounded.MenuBook)
    data object DonorRequests : AppDestination("donor-requests", "Pedidos", Icons.Rounded.VolunteerActivism)
    data object Profile : AppDestination("profile", "Perfil", Icons.Rounded.AccountCircle)
}
