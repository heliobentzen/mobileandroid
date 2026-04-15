/**
 * Exemplo de navegação com Jetpack Compose usando NavHost.
 *
 * O NavHost é o contêiner que gerencia a troca de telas (composables) com base
 * nas rotas definidas. Cada rota é uma string que identifica uma tela, e o
 * NavController cuida da pilha de navegação (back stack) automaticamente.
 *
 * Dependências necessárias:
 *   - androidx.navigation:navigation-compose
 */

import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

// navController — objeto que controla a navegação entre telas.
// Ele mantém a pilha de telas (back stack) e oferece métodos como
// navigate() para ir a uma rota e popBackStack() para voltar.

// startDestination — rota da tela inicial que será exibida quando o
// NavHost for composto pela primeira vez. Funciona como a "home" do app.
NavHost(
    navController = navController,
    startDestination = "listaPrincipal"
) {
    // Lista principal — tela inicial do aplicativo
    composable("listaPrincipal") {
        ListaPrincipalScreen(
            onItemClick = { id ->
                // Navega para a tela de detalhes passando o id como parte da rota
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

    // Detalhes da lista principal — recebe um argumento "id" pela rota
    composable(
        route = "detalhesListaPrincipal/{id}",
        // NavType.IntType garante que o argumento "id" será interpretado como Int.
        // Sem isso, todos os argumentos de rota são tratados como String por padrão.
        arguments = listOf(navArgument("id") { type = NavType.IntType })
    ) { backStackEntry ->
        // backStackEntry representa a entrada atual na pilha de navegação.
        // Através dele acessamos os argumentos passados na rota (ex.: o "id").
        val id = backStackEntry.arguments?.getInt("id")
        DetalhesListaPrincipalScreen(id = id)
    }

    // Configuração de perfil — rota simples sem argumentos
    composable("configPerfil") {
        ConfigPerfilScreen()
    }

    // Configurações do app — rota simples sem argumentos
    composable("configApp") {
        ConfigAppScreen()
    }

    // Outra lista — demonstra navegação encadeada (lista → detalhes)
    composable("outraLista") {
        OutraListaScreen(
            onItemClick = { id ->
                navController.navigate("detalhesOutraLista/$id")
            }
        )
    }

    // Detalhes da outra lista — também recebe "id" como argumento inteiro
    composable(
        route = "detalhesOutraLista/{id}",
        arguments = listOf(navArgument("id") { type = NavType.IntType })
    ) { backStackEntry ->
        val id = backStackEntry.arguments?.getInt("id")
        DetalhesOutraListaScreen(id = id)
    }
}
