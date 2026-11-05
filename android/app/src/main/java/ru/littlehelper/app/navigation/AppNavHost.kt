package ru.littlehelper.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ru.littlehelper.app.ui.auth.LoginScreen
import ru.littlehelper.app.ui.auth.RegisterScreen
import ru.littlehelper.app.ui.home.HomeScreen

/** Адреса экранов. Новый экран = новая константа + новый composable(...) ниже. */
object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
}

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    // После входа/регистрации переходим на главный и убираем экраны входа из истории,
    // чтобы кнопка «Назад» не возвращала на форму входа
    val goHome = {
        navController.navigate(Routes.HOME) {
            popUpTo(Routes.LOGIN) { inclusive = true }
        }
    }

    NavHost(navController = navController, startDestination = Routes.LOGIN) {
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
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                },
            )
        }
    }
}
