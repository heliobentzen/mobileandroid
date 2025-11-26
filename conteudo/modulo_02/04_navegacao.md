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

## Estrutura de Projeto (Exemplo)
```
app/
  build.gradle.kts
  src/
    main/
      AndroidManifest.xml
      java/
        com/example/app/
          MainActivity.kt              // setContent { AppNavHost() }
          navigation/
            AppRoute.kt                // sealed class rotas principais
            AppNavHost.kt              // NavHost principal
            login/
              LoginRoute.kt            // sealed class rotas login
              LoginNavHost.kt          // NavHost de login (opcional separado)
          ui/
            HomeScreen.kt
            DetailScreen.kt
            LoginScreen.kt
            WelcomeScreen.kt
      res/
        values/
          strings.xml
    androidTest/
      java/
        com/example/app/
          AppNavTest.kt                // teste instrumentado de navegação
    test/
      java/
        com/example/app/
          AppNavUnitTest.kt            // testes unitários (se usar helpers)
```
Observação: Separar navigation/ e ui/ mantém rotas centralizadas e telas desacopladas.

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

# Navegação com Bottom Navigation e Jetpack Compose

Neste exemplo, vamos implementar uma navegação com `BottomNavigation` associada a três telas diferentes usando o Jetpack Compose.

---

## Passo 1: Configurar as Rotas

Defina as rotas para as telas no aplicativo:

```kotlin
sealed class Screen(val route: String, val title: String, val icon: Int) {
    object Home : Screen("home", "Home", R.drawable.ic_home)
    object Search : Screen("search", "Search", R.drawable.ic_search)
    object Profile : Screen("profile", "Profile", R.drawable.ic_profile)
}
```

---

## Passo 2: Criar as Telas

Implemente os composables para cada tela:

```kotlin
@Composable
fun HomeScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Tela Home")
    }
}

@Composable
fun SearchScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Tela Search")
    }
}

@Composable
fun ProfileScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Tela Profile")
    }
}
```

---

## Passo 3: Configurar o NavHost

Use o `NavHost` para gerenciar as rotas:

```kotlin
@Composable
fun NavigationGraph(navController: NavHostController) {
    NavHost(navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) { HomeScreen() }
        composable(Screen.Search.route) { SearchScreen() }
        composable(Screen.Profile.route) { ProfileScreen() }
    }
}
```

---

## Passo 4: Implementar o BottomNavigation

Crie o componente de navegação inferior:

```kotlin
@Composable
fun BottomNavigationBar(navController: NavController) {
    val items = listOf(
        Screen.Home,
        Screen.Search,
        Screen.Profile
    )

    BottomNavigation {
        val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

        items.forEach { screen ->
            BottomNavigationItem(
                icon = { Icon(painterResource(id = screen.icon), contentDescription = screen.title) },
                label = { Text(screen.title) },
                selected = currentRoute == screen.route,
                onClick = {
                    if (currentRoute != screen.route) {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}
```

---

## Passo 5: Integrar Tudo na MainActivity

Configure a `MainActivity` para usar o `BottomNavigation` e o `NavHost`:

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()

            Scaffold(
                bottomBar = { BottomNavigationBar(navController) }
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    NavigationGraph(navController)
                }
            }
        }
    }
}
```

---

Com isso, você terá um aplicativo com navegação inferior e três telas associadas. Cada botão na barra de navegação leva o usuário para a tela correspondente.