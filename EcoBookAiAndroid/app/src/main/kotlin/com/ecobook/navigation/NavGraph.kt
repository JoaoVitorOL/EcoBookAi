package com.ecobook.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ecobook.auth.AuthScreen
import com.ecobook.auth.LogoutViewModel
import com.ecobook.discovery.DiscoveryScreen
import com.ecobook.fcm.NotificationNavigationManager
import com.ecobook.material.MaterialUploadScreen
import com.ecobook.model.SessionDestination
import com.ecobook.notifications.NotificationsScreen
import com.ecobook.notifications.NotificationsViewModel
import com.ecobook.onboarding.OnboardingScreen
import com.ecobook.request.DonorRequestsScreen
import com.ecobook.request.MyRequestsScreen
import com.ecobook.ui.EcoBookViewModel
import com.ecobook.ui.screens.DeleteAccountScreen
import com.ecobook.ui.screens.DonateScreen
import com.ecobook.ui.screens.DonateViewModel
import com.ecobook.ui.screens.ProfileScreen

@Composable
fun NavGraph(
    notificationNavigationManager: NotificationNavigationManager
) {
    val viewModel: EcoBookViewModel = hiltViewModel()
    val notificationsViewModel: NotificationsViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val notificationsUiState by notificationsViewModel.uiState.collectAsStateWithLifecycle()
    val pendingNotification by notificationNavigationManager.pendingDestination.collectAsStateWithLifecycle()
    val sessionDestination = uiState.session.destination
    val mainDestinations = listOf(
        AppDestination.MyRequests,
        AppDestination.Discovery,
        AppDestination.Donate,
        AppDestination.Profile
    )

    key(sessionDestination) {
        val navController = rememberNavController()
        val currentDestination = navController.currentBackStackEntryAsState().value?.destination
        val showBottomBar = sessionDestination == SessionDestination.MAIN

        LaunchedEffect(sessionDestination, pendingNotification) {
            val destination = pendingNotification ?: return@LaunchedEffect
            if (sessionDestination != SessionDestination.MAIN) {
                return@LaunchedEffect
            }

            navController.navigateToNotificationDestination(destination.route)
            notificationNavigationManager.consume(destination)
        }

        LaunchedEffect(sessionDestination) {
            if (sessionDestination == SessionDestination.MAIN) {
                notificationsViewModel.refresh()
            }
        }

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
                                    val selected = currentDestination?.hierarchy?.any { backStackDestination ->
                                        backStackDestination.route in destination.selectedRoutes
                                    } == true
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = {
                                            navController.navigateToTopLevelDestination(destination.route)
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
                        SessionDestination.MAIN -> AppDestination.MyRequests.route
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
                    composable(AppDestination.Discovery.route) {
                        DiscoveryScreen(
                            onOpenMyRequests = {
                                navController.navigateToTopLevelDestination(AppDestination.MyRequests.route)
                            },
                            unreadNotifications = notificationsUiState.unreadCount,
                            onOpenNotifications = {
                                navController.navigateToNotifications()
                            }
                        )
                    }
                    composable(AppDestination.Donate.route) {
                        DonateScreen(
                            onOpenDonorRequests = { navController.navigate(AppDestination.DonorRequests.route) },
                            onOpenPublishNew = { navController.navigate(AppDestination.DonatePublish.route) },
                            unreadNotifications = notificationsUiState.unreadCount,
                            onOpenNotifications = {
                                navController.navigateToNotifications()
                            }
                        )
                    }
                    composable(AppDestination.DonatePublish.route) { entry ->
                        val donateEntry = remember(entry) {
                            navController.getBackStackEntry(AppDestination.Donate.route)
                        }
                        val donateViewModel: DonateViewModel = hiltViewModel(donateEntry)

                        ChildDestinationScaffold(
                            title = "Publicar novo",
                            onNavigateUp = { navController.navigateUp() }
                        ) { topPadding ->
                            MaterialUploadScreen(
                                modifier = Modifier.padding(topPadding),
                                topContent = {
                                    Text(
                                        text = "Escolha as imagens do material, revise os dados sugeridos e finalize a publicação quando tudo estiver certo.",
                                        color = Color(0xFF4B635A)
                                    )
                                },
                                showSectionHeading = false,
                                unreadNotifications = notificationsUiState.unreadCount,
                                onOpenNotifications = {
                                    navController.navigateToNotifications()
                                },
                                onMaterialPublished = { material ->
                                    donateViewModel.onMaterialPublished(material)
                                    navController.navigateUp()
                                },
                                autoResetAfterPublish = true
                            )
                        }
                    }
                    composable(AppDestination.MyRequests.route) {
                        MyRequestsScreen(
                            unreadNotifications = notificationsUiState.unreadCount,
                            onOpenNotifications = {
                                navController.navigateToNotifications()
                            }
                        )
                    }
                    composable(AppDestination.Notifications.route) {
                        ChildDestinationScaffold(
                            title = "Notificações",
                            onNavigateUp = { navController.navigateUp() }
                        ) { topPadding ->
                            NotificationsScreen(
                                topPadding = topPadding,
                                onOpenNotification = { notification ->
                                    navController.navigateToNotificationDestination(notification.route)
                                }
                            )
                        }
                    }
                    composable(AppDestination.DonorRequests.route) {
                        ChildDestinationScaffold(
                            title = "Pedidos recebidos",
                            onNavigateUp = { navController.navigateUp() }
                        ) { topPadding ->
                            DonorRequestsScreen(
                                topPadding = topPadding
                            )
                        }
                    }
                    composable(AppDestination.Profile.route) {
                        val logoutViewModel: LogoutViewModel = hiltViewModel()
                        ProfileScreen(
                            uiState = uiState,
                            unreadNotifications = notificationsUiState.unreadCount,
                            onOpenNotifications = {
                                navController.navigateToNotifications()
                            },
                            onNameChange = viewModel::updateNome,
                            onEmailChange = viewModel::updateEmail,
                            onWhatsappChange = viewModel::updateWhatsapp,
                            onCityChange = viewModel::updateCidade,
                            onNeighborhoodChange = viewModel::updateBairro,
                            onInstitutionChange = viewModel::updateInstituicao,
                            onSaveProfile = viewModel::saveProfile,
                            onToggleAiConsent = viewModel::updateAiConsent,
                            onOpenDeleteAccount = {
                                navController.navigate(AppDestination.AccountDelete.route)
                            },
                            onLogout = logoutViewModel::logout
                        )
                    }
                    composable(AppDestination.AccountDelete.route) {
                        ChildDestinationScaffold(
                            title = "Excluir conta",
                            onNavigateUp = {
                                viewModel.clearAccountDeletionMessage()
                                navController.navigateUp()
                            }
                        ) { topPadding ->
                            DeleteAccountScreen(
                                topPadding = topPadding,
                                uiState = uiState,
                                onDeleteAccount = viewModel::deleteAccount,
                                onNavigateUp = {
                                    viewModel.clearAccountDeletionMessage()
                                    navController.navigateUp()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun NavHostController.navigateToNotificationDestination(route: String) {
    when (route) {
        AppRoutes.MY_REQUESTS,
        AppRoutes.DISCOVERY,
        AppRoutes.DONATE,
        AppRoutes.PROFILE -> navigateToTopLevelDestination(route)

        AppRoutes.NOTIFICATIONS -> navigateToNotifications()

        AppRoutes.DONOR_REQUESTS -> navigate(route) {
            launchSingleTop = true
        }
    }
}

private fun NavHostController.navigateToNotifications() {
    navigate(AppRoutes.NOTIFICATIONS) {
        launchSingleTop = true
    }
}

private fun NavHostController.navigateToTopLevelDestination(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

private sealed class AppDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedRoutes: Set<String> = setOf(route)
) {
    data object Auth : AppDestination(AppRoutes.AUTH, "Entrar", Icons.Rounded.AccountCircle)
    data object Onboarding : AppDestination(AppRoutes.ONBOARDING, "Onboarding", Icons.Rounded.AccountCircle)
    data object Discovery : AppDestination(AppRoutes.DISCOVERY, "Buscar", Icons.Rounded.Explore)
    data object Donate : AppDestination(
        route = AppRoutes.DONATE,
        label = "Doar",
        icon = Icons.Rounded.VolunteerActivism,
        selectedRoutes = setOf(AppRoutes.DONATE, AppRoutes.DONATE_PUBLISH, AppRoutes.DONOR_REQUESTS)
    )
    data object DonatePublish : AppDestination(AppRoutes.DONATE_PUBLISH, "Publicar", Icons.Rounded.VolunteerActivism)
    data object MyRequests : AppDestination(AppRoutes.MY_REQUESTS, "Solicitações", Icons.Rounded.MenuBook)
    data object Notifications : AppDestination(AppRoutes.NOTIFICATIONS, "Notificações", Icons.Rounded.MenuBook)
    data object DonorRequests : AppDestination(AppRoutes.DONOR_REQUESTS, "Pedidos", Icons.Rounded.VolunteerActivism)
    data object Profile : AppDestination(
        route = AppRoutes.PROFILE,
        label = "Perfil",
        icon = Icons.Rounded.AccountCircle,
        selectedRoutes = setOf(AppRoutes.PROFILE, AppRoutes.ACCOUNT_DELETE)
    )
    data object AccountDelete : AppDestination(AppRoutes.ACCOUNT_DELETE, "Excluir conta", Icons.Rounded.AccountCircle)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChildDestinationScaffold(
    title: String,
    onNavigateUp: () -> Unit,
    content: @Composable (topPadding: PaddingValues) -> Unit
) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        content(innerPadding)
    }
}
