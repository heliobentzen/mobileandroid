# Activities no Android Moderno

## 1. Por que existem e o que são
Uma Activity é um dos blocos fundamentais de um app Android: é a porta de entrada de uma UI com a qual o usuário interage diretamente. Historicamente cada tela era uma Activity. Hoje, adotamos (quando possível) uma arquitetura de Single-Activity + Navigation (Fragments ou, mais moderno, Jetpack Compose + Navigation), deixando a Activity como host do fluxo. Ainda assim, entender Activities continua essencial porque:
- O sistema (Android OS) gerencia seu ciclo de vida agressivamente para liberar recursos.
- Interações externas (intents, deep links, compartilhamento, permissões, resultado de outras telas) passam por Activities.
- Muitos componentes (câmera, notificações, App Links, Shortcuts, Wear, Auto) envolvem callbacks ligados à Activity.

## 2. Conceitos-chave modernos
- Single-Activity Architecture: 1 Activity (geralmente MainActivity) + Navigation Component para destinos (destinations) em Compose.
- ViewModel para desacoplar lógica de ciclo de vida (sobrevive a recriações).
- State hoisting + Compose para UI declarativa reativa.
- remember / rememberSaveable para estado efêmero vs persistente à recriação.
- Activity Result APIs (registerForActivityResult) substituem onActivityResult legado.
- Lifecycle-aware components (LifecycleOwner, repeatOnLifecycle) evitam vazamentos.
- Back dispatch unificado (OnBackPressedDispatcher + Compose BackHandler).

## 3. Ciclo de vida (essência prática)
Estados principais (callbacks):
1. onCreate: inicialização (injeção, composição de UI, registrar observers).
2. onStart: UI visível (não ainda interativa necessariamente).
3. onResume: foreground interativo.
4. onPause: perder foco parcial (salvar estado transitório leve).
5. onStop: não visível (liberar sensores, câmera).
6. onDestroy: limpeza final (exceto quando rotação: ViewModel permanece).
Extra:
- onRestart após voltar de Stopped.
- onSaveInstanceState para pequenos dados (Bundles) usados na recriação (ex: scroll, id selecionado).
Em Compose, muita lógica de estado vai para ViewModel; evita setContent recrear lógica pesada.

Resumo mental moderno:
Inicializar pesado: DI + ViewModel (onCreate)
Iniciar recursos visuais/animar: onStart
Assinar streams (Flow) com repeatOnLifecycle(Lifecycle.State.STARTED) dentro de lifecycleScope
Liberar recursos (camera, location): onStop
Salvar coisas essenciais: onSaveInstanceState + persistência (Room, DataStore)

## 4. Usos comuns valorizados no mercado
- Deep links / App Links: abrir rotas específicas (marketing, push).
- Integração com Share / Send (intents implícitas).
- Permissões sensíveis (Activity Result API).
- Captura de mídia (TakePicture, PickVisualMedia).
- Navegação segura (Navigation + Safe Args ou typed destinations).
- Autenticação (biometria, browser custom tab -> voltar via intent).
- Multimódulo: Activity como boundary do app (entry) carregando features dinâmicas (Play Feature Delivery).
- Gerenciamento de processos em segundo plano: reconhecimento de que Activity pode morrer; usar WorkManager para tarefas confiáveis.
- Suporte a múltiplas janelas (tablets, foldables) via intent flags e manifest (resizeableActivity, activity embedding).

## 5. Exemplos

### 5.1 Activity básica com Jetpack Compose + ViewModel

```kotlin
@AndroidEntryPoint // se usar Hilt
class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Side effects pesados fora do compose
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.events.collect { /* handle one-off events */ }
            }
        }
        setContent {
            AppTheme {
                val uiState by vm.uiState.collectAsStateWithLifecycle()
                MainScreen(
                    state = uiState,
                    onAction = vm::onAction
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // iniciar algo visível (ex: analytics screen start)
    }

    override fun onStop() {
        super.onStop()
        // liberar camera/location se usados
    }
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repo: UserRepository
): ViewModel() {
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    val events = MutableSharedFlow<OneOffEvent>()

    fun onAction(action: Action) { /* update state */ }
}

data class UiState(val counter: Int = 0)
```

### 5.2 Activity Result API (selecionar imagem)

```kotlin
class ProfileActivity : ComponentActivity() {

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) { /* upload / update state */ }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Button(onClick = {
                pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }) { Text("Selecionar foto") }
        }
    }
}
```

### 5.3 Deep Link (Manifest + Navigation Compose)

Manifest:
```xml
<activity
    android:name=".MainActivity"
    android:exported="true">
    <intent-filter android:autoVerify="true">
        <action android:name="android.intent.action.VIEW"/>
        <category android:name="android.intent.category.DEFAULT"/>
        <category android:name="android.intent.category.BROWSABLE"/>
        <data
            android:scheme="https"
            android:host="example.com"
            android:pathPrefix="/produto"/>
    </intent-filter>
</activity>
```

Handle dentro do setContent:
```kotlin
val startDestination = remember {
    val data = intent?.data
    if (data?.path?.startsWith("/produto") == true) "produto/${data.lastPathSegment}" else "home"
}
NavHost(navController, startDestination = startDestination) { /* ... */ }
```

### 5.4 Salvando estado em recriação (rotação)

```kotlin
@Composable
fun CounterScreen(vm: CounterViewModel = hiltViewModel()) {
    val count by vm.count.collectAsStateWithLifecycle()
    Column {
        Text("Count = $count")
        Button(onClick = vm::inc) { Text("+") }
    }
}

@HiltViewModel
class CounterViewModel @Inject constructor(): ViewModel() {
    private val _count = MutableStateFlow(0)
    val count: StateFlow<Int> = _count
    fun inc() { _count.update { it + 1 } }
}
```
ViewModel preserva estado em rotação, diferente de um remember simples.

### 5.5 BackHandler em Compose

```kotlin
@Composable
fun DetailsScreen(onLeave: () -> Unit, unsavedChanges: Boolean) {
    var showDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = unsavedChanges) {
        showDialog = true
    }

    if (showDialog) ConfirmLeaveDialog(
        onConfirm = onLeave,
        onDismiss = { showDialog = false }
    )
}
```

## 6. Boas práticas atuais
- Minimizar lógica em Activity; delegar a ViewModel / Use Cases.
- Evitar Singletons manuais; usar DI (Hilt/Koin).
- Usar DataStore (preferências) e Room (persistência) fora da Activity.
- Processar fluxos com collectAsStateWithLifecycle para evitar leaks.
- Expor estado imutável (StateFlow/Immutable data classes).
- Usar sealed classes para eventos de navegação e OneOff events (snackbar).
- Não bloquear main thread; usar coroutines.
- Testar Activity com Robolectric ou Espresso; lógica testável isolada em ViewModels.

## 7. Exercício prático

Ciclo de vida + Logs
Objetivo: Criar uma MainActivity com Compose que:
- Loga cada callback (onCreate, onStart, onResume, onPause, onStop, onDestroy)
- Exibe um contador persistido em ViewModel.
- Mostra em tela o último callback disparado reativo.
Desafio extra: adicionar um Flow que emite tempo a cada segundo apenas quando Activity está STARTED (repeatOnLifecycle).


## 8. Checklist mental rápida
- Necessito realmente de outra Activity? Se não, destino de navegação.
- Estado sobrevivente? ViewModel.
- Estado efêmero UI? remember / rememberSaveable.
- Interação externa? Activity Result API.
- Observando Flow? collectAsStateWithLifecycle.
- Navegação/back? Navigation + BackHandler declarativo.

Pronto: domínio sólido de Activity permite focar em entregar valor e manter app robusto perante o sistema operacional.
