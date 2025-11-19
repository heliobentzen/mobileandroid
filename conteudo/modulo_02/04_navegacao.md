# Navegação Segura e Testável com Navigation Component + Safe Args (Jetpack Compose)

Objetivo: Mostrar um fluxo simples com passagem segura de argumentos e fácil testabilidade usando Navigation Component. Observação: O plugin Safe Args gera classes para grafos XML (Fragment/Activity). Em projetos 100% Compose (navigation-compose) ele não gera código; nesse caso, oferecemos duas abordagens:

1. XML + Safe Args: Usa um grafo XML para gerar Directions e chama composables dentro de um Fragment/Activity.
2. Compose puro: Emula segurança de tipos com rotas estruturadas (sealed classes) ou wrappers de argumentos.

Abaixo: opção 2 (Compose puro) focada em testabilidade e segurança manual dos tipos.

## Benefícios Resumidos
- Segurança ao passar dados (evita ClassCastException).
- Navegação declarativa com rotas centralizadas.
- Facilidade de teste: NavHostController substituível em testes.
- Extensível para deep links.

---

## Dependências (build.gradle.kts - módulo)
```kotlin
plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "com.example.app"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
    }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.3" }
}

dependencies {
    val navVersion = "2.7.7"
    implementation("androidx.navigation:navigation-compose:$navVersion")
    implementation(platform("androidx.compose:compose-bom:2024.10.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
```

---

## Rotas Tipadas (Compose)
```kotlin
sealed class AppRoute(val path: String) {
    data object Home : AppRoute("home")
    data object Detail : AppRoute("detail/{itemId}") {
        fun build(itemId: String) = "detail/$itemId"
        const val ARG_ITEM_ID = "itemId"
    }
}
```

## NavHost
```kotlin
@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController, startDestination = AppRoute.Home.path) {
        composable(AppRoute.Home.path) {
            HomeScreen(
                onOpenDetail = { id -> navController.navigate(AppRoute.Detail.build(id)) }
            )
        }
        composable(
            route = AppRoute.Detail.path,
            arguments = listOf(navArgument(AppRoute.Detail.ARG_ITEM_ID) { type = NavType.StringType })
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString(AppRoute.Detail.ARG_ITEM_ID) ?: ""
            DetailScreen(itemId)
        }
    }
}
```

## Telas
```kotlin
@Composable
fun HomeScreen(onOpenDetail: (String) -> Unit) {
    Button(onClick = { onOpenDetail("123-ABC") }) {
        Text("Ir para Detalhes")
    }
}

@Composable
fun DetailScreen(itemId: String) {
    Text("Detalhes do item: $itemId")
}
```

---

## Testabilidade
- Use createAndroidComposeRule e um NavController fake ou real.
- Verifique rotas com navController.currentBackStackEntry?.
- Rotas seladas evitam strings mágicas dispersas.

Exemplo de teste (instrumentado):
```kotlin
@get:Rule
val composeRule = createAndroidComposeRule<ComponentActivity>()

@Test
fun navegaParaDetalhe() {
    composeRule.setContent { AppNavHost() }
    composeRule.onNodeWithText("Ir para Detalhes").performClick()
    composeRule.onNodeWithText("Detalhes do item: 123-ABC").assertExists()
}
```

---

## Exercício Prático (Login -> Welcome)
Fluxo:
1. LoginScreen (TextField + Button).
2. WelcomeScreen (exibe nome).

Rotas:
```kotlin
sealed class LoginRoute(val path: String) {
    data object Login : LoginRoute("login")
    data object Welcome : LoginRoute("welcome/{username}") {
        fun build(username: String) = "welcome/$username"
        const val ARG_USERNAME = "username"
    }
}
```

Host:
```kotlin
@Composable
fun LoginNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController, startDestination = LoginRoute.Login.path) {
        composable(LoginRoute.Login.path) {
            LoginScreen { name -> navController.navigate(LoginRoute.Welcome.build(name)) }
        }
        composable(
            route = LoginRoute.Welcome.path,
            arguments = listOf(navArgument(LoginRoute.Welcome.ARG_USERNAME) { type = NavType.StringType })
        ) { entry ->
            val user = entry.arguments?.getString(LoginRoute.Welcome.ARG_USERNAME).orEmpty()
            WelcomeScreen(user)
        }
    }
}
```

Login:
```kotlin
@Composable
fun LoginScreen(onLogin: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    Column {
        TextField(value = text, onValueChange = { text = it }, label = { Text("Nome") })
        Button(enabled = text.isNotBlank(), onClick = { onLogin(text) }) {
            Text("Entrar")
        }
    }
}
```

Welcome:
```kotlin
@Composable
fun WelcomeScreen(username: String) {
    Text("Bem-vindo, $username")
}
```

---

Resumo: Para navegação segura em Compose hoje, substitua Safe Args por rotas tipadas (sealed classes + navArgument). Mantém segurança, centralização e testabilidade. Se exigir geração automática (Directions), use grafo XML com Fragment + Compose dentro. Escolha depende do nível de integração 100% Compose versus necessidade estrita do plugin Safe Args.