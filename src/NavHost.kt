NavHost(
    navController = navController,
    startDestination = "listaPrincipal"
) {
    // Lista principal
    composable("listaPrincipal") {
        ListaPrincipalScreen(
            onItemClick = { id ->
                navController.navigate("detalhesListaPrincipal/$id")
            },
            onNavigateToPerfil = {
                navController.navigate("configPerfil")
            },
            onNavigateToConfigApp = {
                navController.navigate("configApp")
            },
            onNavigateToOutraLista = {
                navController.navigate("outraLista")
            }
        )
    }

    // Detalhes da lista principal
    composable(
        route = "detalhesListaPrincipal/{id}",
        arguments = listOf(navArgument("id") { type = NavType.IntType })
    ) { backStackEntry ->
        val id = backStackEntry.arguments?.getInt("id")
        DetalhesListaPrincipalScreen(id = id)
    }

    // Configuração de perfil
    composable("configPerfil") {
        ConfigPerfilScreen()
    }

    // Configurações do app
    composable("configApp") {
        ConfigAppScreen()
    }

    // Outra lista
    composable("outraLista") {
        OutraListaScreen(
            onItemClick = { id ->
                navController.navigate("detalhesOutraLista/$id")
            }
        )
    }

    // Detalhes da outra lista
    composable(
        route = "detalhesOutraLista/{id}",
        arguments = listOf(navArgument("id") { type = NavType.IntType })
    ) { backStackEntry ->
        val id = backStackEntry.arguments?.getInt("id")
        DetalhesOutraListaScreen(id = id)
    }
}
