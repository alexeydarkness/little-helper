package ru.littlehelper.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ru.littlehelper.app.data.AppRepository
import ru.littlehelper.app.ui.auth.LoginScreen
import ru.littlehelper.app.ui.auth.RegisterScreen
import ru.littlehelper.app.ui.home.HomeScreen
import ru.littlehelper.app.ui.profile.ProfileScreen

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val PROFILE = "profile"
}

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val repository = remember { AppRepository.get(context) }
    val startDestination = remember { if (repository.hasSession()) Routes.HOME else Routes.LOGIN }

    val goHome = {
        navController.navigate(Routes.HOME) {
            popUpTo(Routes.LOGIN) { inclusive = true }
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoggedIn = goHome,
                onRegisterClick = { navController.navigate(Routes.REGISTER) },
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegistered = goHome,
                onLoginClick = { navController.popBackStack() },
            )
        }
        composable(Routes.HOME) {
            HomeScreen(
                onProfile = { navController.navigate(Routes.PROFILE) },
                onLogout = {
                    repository.logout()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.PROFILE) {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onSessionExpired = {
                    repository.logout()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                },
            )
        }
    }
}
