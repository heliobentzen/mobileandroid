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
- Activity mínima com setContent? OK
- Usando Navigation Compose? OK
- Evitou lógica pesada na Activity? OK
- Intents apenas para funcionalidades externas? OK
- Lifecycle observado via efeitos? OK

Concluindo: compreender componentes clássicos (Activity/Fragment/Intent) permanece vital, mas em novos projetos Compose foque em uma Activity, navegação declarativa, efeitos lifecycle-aware e interoperabilidade controlada.
