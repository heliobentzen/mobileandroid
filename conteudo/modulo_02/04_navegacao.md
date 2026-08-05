# Navegação com Jetpack Compose

Neste módulo você aprenderá a navegar entre telas em um app Android usando a biblioteca
**Navigation Compose**. Vamos construir exemplos passo a passo: primeiro um fluxo simples
(Home → Detalhe), depois um exercício de Login e, por fim, uma barra de navegação inferior.

## O que é "navegação" em um app?

Um app quase nunca tem uma tela só. "Navegar" significa trocar qual tela está visível — por exemplo, sair da lista de produtos e mostrar a tela de detalhes de um produto específico, ou voltar da tela de detalhes para a lista. O **Navigation Compose** é a biblioteca oficial do Android para gerenciar essas trocas de tela dentro de um app 100% Compose, incluindo o comportamento do botão "voltar" e a passagem de dados entre telas.

---

## Por que usar o Navigation Component?

Em apps com várias telas, gerenciar manualmente qual tela exibir se torna complexo e propenso
a erros — imagine controlar "na mão" uma variável dizendo qual tela mostrar, mais outra lista para lembrar por onde o usuário passou (para o botão voltar funcionar), mais a lógica de passar dados de uma tela para outra sem se perder. O **Navigation Component** resolve isso oferecendo:

| Benefício | O que significa na prática |
|---|---|
| **Rotas centralizadas** | Todas as telas ficam listadas em um único lugar, facilitando manutenção. |
| **Passagem segura de dados** | Argumentos são declarados com tipo definido, evitando erros em tempo de execução. |
| **Back stack automático** | O botão "voltar" funciona corretamente sem código adicional. |
| **Testabilidade** | O `NavHostController` pode ser substituído em testes para verificar a navegação. |
| **Deep links** | Suporte nativo para abrir telas específicas via links externos. |

**O que é o "back stack"?** É a pilha de telas que o usuário já visitou dentro do fluxo de navegação — como uma pilha de cartas: a última tela aberta fica no topo, e apertar "voltar" remove essa carta do topo, revelando a de baixo (a tela anterior). O `Navigation Component` gerencia essa pilha para você automaticamente.

---

## Quando usar Intents vs Navigation Compose?

- **Navigation Compose**: para navegar entre telas **dentro** do seu app (ex: lista → detalhes).
- **Intents**: mecanismo do Android para abrir **outros apps** ou funcionalidades do sistema (ex: navegador, câmera, compartilhamento).

> **Regra prática:** tela do seu app → Navigation Compose. Outro app ou sistema → Intent.

---

## Dependências

Adicione ao `build.gradle.kts` do módulo:

```kotlin
dependencies {
    // Biblioteca de navegação para Compose
    val navVersion = "2.8.9"
    implementation("androidx.navigation:navigation-compose:$navVersion")

    // BOM garante que todas as versões do Compose sejam compatíveis entre si
    implementation(platform("androidx.compose:compose-bom:2025.01.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
```

## Conceito principal: Rotas com Sealed Class

### O que é uma rota

Uma **rota** é uma string que identifica cada tela dentro do grafo de navegação — funciona como um "endereço" (parecido com uma URL). O `NavHost` (que veremos a seguir) usa essa string para saber qual composable mostrar.

Para centralizar os caminhos e evitar erros de digitação (como escrever `"detial/123"` em vez de `"detail/123"` em algum lugar do código), usamos uma **sealed class**: um tipo do Kotlin que restringe quais subtipos podem existir, todos declarados no mesmo arquivo — assim todas as rotas do app ficam visíveis em um único lugar.

```kotlin
// Cada objeto representa uma tela do app.
// O parâmetro "path" é a string usada internamente pelo NavHost.
sealed class AppRoute(val path: String) {
    // Tela inicial — rota simples, sem argumentos
    data object Home : AppRoute("home")
    // Tela de detalhe — recebe um argumento "itemId" na URL
    data object Detail : AppRoute("detail/{itemId}") {
        // Função auxiliar que monta a rota com o valor real do argumento
        fun build(itemId: String) = "detail/$itemId"
        // Constante para evitar digitar "itemId" em vários lugares
        const val ARG_ITEM_ID = "itemId"
    }
}
```

**Por que sealed class?** O compilador garante que todas as rotas estejam definidas em um
único arquivo. Se você esquecer de tratar uma rota, o IDE avisa.

---

## Passo 1: Configurar o NavHost

O **`NavHost`** é o composable que efetivamente troca as telas na tela do celular, de acordo com a rota atual. Ele funciona como um "container" que sempre mostra a tela correspondente à rota ativa no momento.

```kotlin
@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController() // Substituível em testes
) {
    NavHost(navController, startDestination = AppRoute.Home.path) {
        // Registra a tela Home na rota "home"
        composable(AppRoute.Home.path) {
            // Passa callback — a tela não conhece o NavController (boa prática)
            HomeScreen(
                onOpenDetail = { id -> navController.navigate(AppRoute.Detail.build(id)) }
            )
        }
        // Registra a tela Detail na rota "detail/{itemId}"
        composable(
            route = AppRoute.Detail.path,
            arguments = listOf( // Declara argumento do tipo String
                navArgument(AppRoute.Detail.ARG_ITEM_ID) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments // Extrai o argumento da rota
                ?.getString(AppRoute.Detail.ARG_ITEM_ID) ?: ""
            DetailScreen(itemId)
        }
    }
}
```

**O que é `NavHostController`?** É o objeto que efetivamente comanda a navegação: você chama `navController.navigate("rota")` para trocar de tela, e ele atualiza o `NavHost` e o back stack automaticamente. `rememberNavController()` cria (ou recupera, em recomposições) essa instância.

**Por que a `HomeScreen` recebe um callback (`onOpenDetail`) em vez do `navController` diretamente?** Se cada tela recebesse o `NavHostController` diretamente, ela ficaria "acoplada" à navegação e mais difícil de testar isoladamente (você precisaria simular um `NavController` completo só para testar o botão). Passando um callback simples (`(String) -> Unit`), a tela não precisa saber nada sobre como a navegação funciona — só avisa "o usuário quer abrir o item X" e quem decide como navegar é o `AppNavHost`.

## Passo 2: Criar as Telas

```kotlin
@Composable
fun HomeScreen(onOpenDetail: (String) -> Unit) {
    Button(onClick = { onOpenDetail("123-ABC") }) { // Navega passando um ID
        Text("Ir para Detalhes")
    }
}

@Composable
fun DetailScreen(itemId: String) {
    Text("Detalhes do item: $itemId") // Exibe o argumento recebido
}
```

---

## Passo 3: Chamar na MainActivity

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AppNavHost() } // Raiz da UI em Compose
    }
}
```

---

## Testando a navegação

Testes instrumentados verificam se clicar em um botão realmente muda de tela. Isso é possível porque `AppNavHost` recebe o `navController` como parâmetro (com valor padrão) — em produção usamos o padrão, mas em testes podemos renderizar o `NavHost` completo e simular interações reais do usuário.

```kotlin
@get:Rule
val composeRule = createAndroidComposeRule<ComponentActivity>()

@Test
fun navegaParaDetalhe() {
    // Renderiza o NavHost completo
    composeRule.setContent { AppNavHost() }
    // Simula o clique no botão da HomeScreen
    composeRule.onNodeWithText("Ir para Detalhes").performClick()
    // Verifica se a DetailScreen apareceu com o argumento correto
    composeRule.onNodeWithText("Detalhes do item: 123-ABC").assertExists()
}
```

---

## Exercício Prático: Fluxo de Login

**Objetivo:** criar um fluxo com duas telas — `LoginScreen` (campo de texto + botão) e
`WelcomeScreen` (exibe o nome digitado).

### Checkpoints (tente resolver antes de ver a solução)

1. Crie uma sealed class `LoginRoute` com duas rotas: `Login` (sem argumentos) e
   `Welcome` (com argumento `username`).
2. Crie o `LoginNavHost` registrando as duas rotas. Na rota `Welcome`, extraia o
   argumento `username` do `backStackEntry`.
3. Na `LoginScreen`, use `remember { mutableStateOf("") }` para guardar o texto digitado
   e passe-o via callback quando o botão for clicado.
4. Na `WelcomeScreen`, apenas exiba o nome recebido como parâmetro.

> **Dica:** se travar no passo 2, releia o "Passo 1" acima — o padrão é idêntico ao `AppNavHost`, só muda o nome das rotas e das telas.

### Solução comentada

**Rotas:**
```kotlin
sealed class LoginRoute(val path: String) {
    // Tela de login — ponto de entrada do fluxo
    data object Login : LoginRoute("login")
    // Tela de boas-vindas — recebe o nome do usuário como argumento
    data object Welcome : LoginRoute("welcome/{username}") {
        fun build(username: String) = "welcome/$username"
        const val ARG_USERNAME = "username"
    }
}
```

**NavHost do fluxo de login:**
```kotlin
@Composable
fun LoginNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController, startDestination = LoginRoute.Login.path) {
        composable(LoginRoute.Login.path) {
            LoginScreen { name -> // Ao fazer login, navega passando o nome digitado
                navController.navigate(LoginRoute.Welcome.build(name))
            }
        }
        composable(
            route = LoginRoute.Welcome.path,
            arguments = listOf(
                navArgument(LoginRoute.Welcome.ARG_USERNAME) { type = NavType.StringType }
            )
        ) { entry ->
            // Extrai o username da rota; orEmpty() evita nulo
            val user = entry.arguments
                ?.getString(LoginRoute.Welcome.ARG_USERNAME).orEmpty()
            WelcomeScreen(user)
        }
    }
}
```

**Tela de Login:**
```kotlin
@Composable
fun LoginScreen(onLogin: (String) -> Unit) {
    var text by remember { mutableStateOf("") } // Estado local do campo de texto
    Column {
        TextField( // Campo controlado pelo estado "text"
            value = text,
            onValueChange = { text = it },
            label = { Text("Nome") }
        )
        Button( // Desabilitado enquanto o campo estiver vazio
            enabled = text.isNotBlank(),
            onClick = { onLogin(text) }
        ) {
            Text("Entrar")
        }
    }
}
```

**Tela de Boas-vindas:**
```kotlin
@Composable
fun WelcomeScreen(username: String) {
    // Exibe mensagem personalizada com o nome recebido
    Text("Bem-vindo, $username!")
}
```

---

## Bottom Navigation (Barra de Navegação Inferior)

Apps com seções principais (Início, Busca, Perfil) costumam usar uma barra inferior fixa, onde cada aba corresponde a um destino de navegação diferente. Reutilizamos o padrão de sealed class para manter a consistência.

### Rotas com ícone e título
```kotlin
// Cada aba possui rota, título exibido na barra e ícone correspondente
sealed class TabRoute(val path: String, val titulo: String, val icone: Int) {
    data object Inicio  : TabRoute("inicio",  "Início",  R.drawable.ic_home)
    data object Busca   : TabRoute("busca",   "Busca",   R.drawable.ic_search)
    data object Perfil  : TabRoute("perfil",  "Perfil",  R.drawable.ic_profile)
}
```

### Barra de Navegação
```kotlin
@Composable
fun BarraNavegacao(navController: NavController) {
    val abas = listOf(TabRoute.Inicio, TabRoute.Busca, TabRoute.Perfil)

    NavigationBar { // Componente Material 3 para barra inferior
        val rotaAtual = navController
            .currentBackStackEntryAsState().value?.destination?.route
        abas.forEach { aba ->
            NavigationBarItem(
                icon = { Icon(painterResource(id = aba.icone), contentDescription = aba.titulo) },
                label = { Text(aba.titulo) },
                selected = rotaAtual == aba.path, // Destaca a aba ativa
                onClick = {
                    if (rotaAtual != aba.path) {
                        navController.navigate(aba.path) {
                            // Volta até a tela inicial ao trocar de aba (evita empilhar)
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true  // Evita duplicar a mesma tela
                            restoreState = true     // Restaura estado ao voltar
                        }
                    }
                }
            )
        }
    }
}
```

### Integrando com Scaffold
```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            Scaffold( // Organiza barra inferior + conteúdo
                bottomBar = { BarraNavegacao(navController) }
            ) { innerPadding ->
                // innerPadding evita que o conteúdo fique atrás da barra
                NavHost(
                    navController,
                    startDestination = TabRoute.Inicio.path,
                    modifier = Modifier.padding(innerPadding)
                ) {
                    composable(TabRoute.Inicio.path) { TelaInicio() }
                    composable(TabRoute.Busca.path)  { TelaBusca() }
                    composable(TabRoute.Perfil.path)  { TelaPerfil() }
                }
            }
        }
    }
}
```

## Nota: XML + Safe Args (projetos legados)

Se você trabalhar em um projeto que ainda usa Fragments e XML, o plugin **Safe Args** gera
classes de navegação a partir de um grafo XML (`nav_graph.xml`). Isso oferece segurança de
tipos automaticamente (o compilador avisa se você tentar passar um argumento do tipo errado). Porém, em projetos **100% Compose** (como neste curso), usamos
`navigation-compose` com sealed classes, como visto acima — o mesmo objetivo (rotas seguras e centralizadas), com uma abordagem 100% Kotlin.

---

## Erros comuns / Pegadinhas

- **Esquecer de declarar o tipo do argumento (`navArgument { type = ... }`)**: sem isso, o `Navigation Compose` pode não conseguir extrair o valor corretamente do `backStackEntry`, retornando `null`.
- **Passar o `NavController` inteiro para telas internas em vez de callbacks**: isso "vaza" a responsabilidade de navegação para todas as telas, dificultando testes e reuso (veja a explicação no Passo 1 acima).
- **Esquecer `launchSingleTop = true` na navegação por abas**: sem isso, cada clique na mesma aba empilha uma nova instância da tela no back stack, fazendo o botão "voltar" passar por várias cópias da mesma tela antes de sair do app.
- **Concatenar argumentos manualmente na URL sem usar a função `build()`**: isso facilita erros de digitação. Sempre centralize a montagem da rota (como fizemos com `AppRoute.Detail.build(itemId)`).

---

## Resumo

| Conceito | Para que serve |
|---|---|
| `NavHost` | Componente que exibe a tela correspondente à rota ativa. |
| `composable()` | Registra uma tela no grafo de navegação. |
| `navArgument()` | Declara argumentos tipados em uma rota. |
| `sealed class` | Centraliza todas as rotas, evitando strings soltas no código. |
| `NavigationBar` | Barra inferior do Material 3 para navegação entre seções. |
| `popUpTo` / `launchSingleTop` | Controlam o comportamento da pilha de telas ao navegar. |

**Próximo passo**: na próxima aula (`05_acessibilidade.md`) você vai aprender a tornar as telas que acabou de construir acessíveis para todos os usuários, incluindo quem usa leitores de tela como o TalkBack.
