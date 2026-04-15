# Componentes Android (Jetpack Compose)

## 1. Visão Geral
Com Jetpack Compose a arquitetura moderna tende a: uma única Activity + Navigation (Compose) gerenciando destinos (screens) sem necessidade de múltiplas Fragments. Ainda assim, entender Activity, Fragment, ciclo de vida e Intents permanece essencial para interoperabilidade e integração com APIs antigas.

## 2. Activity vs Fragment no Contexto Compose
- Activity: ponto de entrada (launcher), integra sistemas (permissões, resultados, intents, AppWidgets, notificações).
- Fragment: usado em apps existentes; em Compose novo pode ser opcional.
- Em Compose: preferir Single-Activity + Navigation Compose.
- Interoperabilidade: você pode inserir composables em Fragment via ComposeView ou em Activity via setContent.

Exemplo mínimo Activity com Compose:
```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppRoot()
        }
    }
}
```

## 3. Ciclo de Vida da Activity (Estados Principais)
onCreate -> onStart -> onResume -> (foreground)
onPause -> onStop -> (background)
onDestroy (final)
onRestart (retorno após onStop)

Capturando logs:
```kotlin
override fun onStart() { super.onStart(); Log.d("Life", "onStart") }
override fun onResume() { super.onResume(); Log.d("Life", "onResume") }
override fun onPause() { super.onPause(); Log.d("Life", "onPause") }
override fun onStop() { super.onStop(); Log.d("Life", "onStop") }
override fun onDestroy() { super.onDestroy(); Log.d("Life", "onDestroy") }
```

## 4. Fragment (Resumo de Ciclo de Vida)
onAttach -> onCreate -> onCreateView -> onViewCreated -> onStart -> onResume  
(onPause -> onStop -> onDestroyView -> onDestroy -> onDetach)

**Principais callbacks explicados:**

- **onAttach**: chamado quando o Fragment é associado à Activity hospedeira. É o primeiro ponto em que você pode acessar o contexto da Activity. Útil para obter referências ou validar que a Activity implementa interfaces esperadas.

- **onCreateView**: responsável por inflar (criar) a hierarquia de views do Fragment. Em projetos Compose, é aqui que você retorna um `ComposeView` com o conteúdo declarativo. Retorne `null` se o Fragment não possui UI (ex: Fragment headless).

- **onViewCreated**: chamado logo após `onCreateView`, quando a view já está criada mas ainda não foi exibida. É o local ideal para configurar listeners, observers e bindings na view — garante que a view não é nula.

- **onDestroyView**: chamado quando a view do Fragment é removida da tela. Aqui você deve liberar referências à view para evitar vazamento de memória (memory leak). O Fragment em si ainda pode existir (ex: na back stack) e ser recriado depois.

Compose em Fragment:
```kotlin
class HomeFragment: Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setContent { HomeScreen() }
    }
}
```

## 5. Lifecycle x Compose (Efeitos)
- LaunchedEffect(key): executa coroutine quando key muda / entra na composição.
- DisposableEffect(key): registra recurso e limpa quando sai.
- SideEffect: código sincronizado pós composição.
- rememberUpdatedState(value): evita captura de valor antigo em efeitos lançados.

**Quando usar cada efeito?**

| Efeito | Use quando... | Exemplo |
|---|---|---|
| `LaunchedEffect(key)` | Precisar executar uma suspending function (coroutine) ao entrar na composição ou quando uma chave mudar. | Buscar dados de uma API ao abrir a tela; iniciar um timer. |
| `DisposableEffect(key)` | Precisar registrar um recurso que exige limpeza (dispose) ao sair da composição. | Adicionar/remover um `LifecycleObserver`; registrar um listener de sensor. |
| `SideEffect` | Precisar sincronizar estado do Compose com código externo (não-Compose) a cada recomposição bem-sucedida. | Atualizar uma biblioteca de analytics com um valor de estado atual. |
| `rememberUpdatedState(value)` | Tiver um callback ou valor que pode mudar, mas é usado dentro de um efeito de longa duração que não deve ser reiniciado. | Manter referência atualizada de um `onTick` lambda dentro de um `LaunchedEffect(Unit)`. |

Exemplo:
```kotlin
@Composable
fun Timer(onTick: (Long) -> Unit) {
    val currentOnTick by rememberUpdatedState(onTick)
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentOnTick(System.currentTimeMillis())
        }
    }
}
```

Observando Lifecycle (ex: para analytics) usando LifecycleOwner:
```kotlin
@Composable
fun LifecycleLogger(tag: String) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            Log.d(tag, "Event: $event")
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}
```

## 6. Intents
Intents conectam componentes (Activity, Service, Broadcast).

Tipos:
- Explícito: nome da classe alvo.
- Implícito: ação + dados (resolvido pelo sistema).

Explícito:
```kotlin
startActivity(Intent(this, DetailActivity::class.java).apply {
    putExtra("userId", 42)
})
```

Implícito (abrir URL):
```kotlin
val uri = Uri.parse("https://developer.android.com")
startActivity(Intent(Intent.ACTION_VIEW, uri))
```

Enviar texto para outro app:
```kotlin
val sendIntent = Intent().apply {
    action = Intent.ACTION_SEND
    putExtra(Intent.EXTRA_TEXT, "Olá")
    type = "text/plain"
}
startActivity(Intent.createChooser(sendIntent, "Compartilhar via"))
```

## 7. Passagem de Dados
- Primitivos via putExtra/getX
- Objetos: Parcelable
```kotlin
@Parcelize
data class User(val id: Int, val name: String): Parcelable
```
Uso:
```kotlin
intent.putExtra("user", user)
val user = intent.getParcelableExtra<User>("user")
```

## 8. Activity Result API (Recomendado)
Registrar:
```kotlin
private val pickImage =
    registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        // tratar uri
    }

fun openPicker() { pickImage.launch("image/*") }
```

Compose chamando método da Activity (levantar callback via ambient ou viewModel).

## 9. Navegação Compose vs Intents
Para telas internas preferir Navigation Compose:
```kotlin
@Composable
fun AppRoot() {
    val nav = rememberNavController()
    NavHost(nav, startDestination = "home") {
        composable("home") {
            HomeScreen(onDetail = { id -> nav.navigate("detail/$id") })
        }
        composable(
            route = "detail/{id}",
            arguments = listOf(navArgument("id"){ type = NavType.IntType })
        ) { backStackEntry ->
            DetailScreen(id = backStackEntry.arguments?.getInt("id") ?: 0)
        }
    }
}
```

Misturando: usar Intent para sair do escopo (ex: abrir configurações do sistema) e Navigation Compose para telas internas.

## 10. Boas Práticas
- Manter lógica de estado fora da Activity (ViewModel).
- Evitar acessar diretamente ciclo de vida em composables; usar efeitos e observers.
- Preferir Single Source of Truth (ViewModel + StateFlow/MutableState).
- Usar sealed classes ou Parcelable/Serializable para dados de navegação legíveis.
- Minimizar Fragments em novos projetos Compose.

## 11. Exemplo Integrado Simplificado
Activity + Navegação + Intent externo:
```kotlin
class MainActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AppRoot(openSettings = {
            startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
        }) }
    }
}

@Composable
fun AppRoot(openSettings: () -> Unit) {
    val nav = rememberNavController()
    NavHost(nav, "home") {
        composable("home") {
            HomeScreen(
                onNavigateDetail = { id -> nav.navigate("detail/$id") },
                onOpenSettings = openSettings
            )
        }
        composable("detail/{id}",
            arguments = listOf(navArgument("id"){ type = NavType.IntType })
        ) {
            DetailScreen(it.arguments?.getInt("id") ?: 0)
        }
    }
}

@Composable
fun HomeScreen(onNavigateDetail: (Int) -> Unit, onOpenSettings: () -> Unit) {
    Column {
        Button(onClick = { onNavigateDetail(7) }) { Text("Detalhe 7") }
        Button(onClick = onOpenSettings) { Text("Wi-Fi") }
        LifecycleLogger(tag = "HomeLifecycle")
    }
}
```

## 12. Checklist Rápido
- **Activity mínima com `setContent`?** — A Activity deve conter apenas a chamada `setContent { ... }` e configurações essenciais (ex: tema, permissões). Toda lógica de UI fica nos composables e toda lógica de negócio fica no ViewModel. Isso mantém a Activity leve e testável.
- **Usando Navigation Compose?** — Navegar entre telas com `NavHost` e `NavController` evita a complexidade de múltiplas Activities ou Fragments. Garante navegação declarativa, type-safe e integrada ao ciclo de vida do Compose.
- **Evitou lógica pesada na Activity?** — Colocar lógica de negócio ou chamadas de rede na Activity gera código difícil de testar e viola o princípio de separação de responsabilidades. Mova tudo para ViewModels e repositórios.
- **Intents apenas para funcionalidades externas?** — Intents devem ser usados para interagir com outros apps ou componentes do sistema (câmera, configurações, compartilhamento). Para navegação interna, prefira Navigation Compose — é mais seguro e previsível.
- **Lifecycle observado via efeitos?** — Em vez de sobrescrever callbacks de ciclo de vida diretamente, use `DisposableEffect` com `LifecycleObserver` dentro dos composables. Isso mantém o código reativo, desacoplado e com limpeza automática.

Concluindo: compreender componentes clássicos (Activity/Fragment/Intent) permanece vital, mas em novos projetos Compose foque em uma Activity, navegação declarativa, efeitos lifecycle-aware e interoperabilidade controlada.
