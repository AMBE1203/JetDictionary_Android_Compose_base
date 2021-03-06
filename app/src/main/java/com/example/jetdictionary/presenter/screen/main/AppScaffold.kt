package com.example.jetdictionary.presenter.screen.main

import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.jetdictionary.presenter.navigation.BottomBarNavHost
import com.example.jetdictionary.presenter.navigation.BottomNavigation
import com.example.jetdictionary.presenter.navigation.Navigator

@Composable
fun AppScaffold(navigator: Navigator, navController: NavController) {
    val scaffoldState = rememberScaffoldState()

    Scaffold(
        bottomBar = {
            BottomBar(navController = navController)
        },
        scaffoldState = scaffoldState,
    ) {
        BottomBarNavHost(navigator = navigator, navController = navController)
    }
}