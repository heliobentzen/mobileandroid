# Componentes Android (Jetpack Compose)

Este arquivo é uma visão panorâmica de como os principais componentes do Android — Activity, Fragment, ciclo de vida, efeitos do Compose e Intents — se encaixam juntos. Se você já leu o arquivo 04 (Activities), vai reconhecer vários conceitos aqui; a diferença é que agora vamos ver como eles se conectam entre si e com o Jetpack Compose.

## 1. Visão Geral

**O que é.** Com o Jetpack Compose, a arquitetura moderna de um app Android tende a usar: **uma única Activity** + **Navigation Compose**, gerenciando os destinos (telas) sem precisar de múltiplos Fragments. Você já viu essa ideia no arquivo 04 como "Single-Activity Architecture" — aqui vamos detalhar como ela funciona na prática.

**Por que isso importa.** Mesmo com essa simplificação, entender Activity, Fragment, ciclo de vida e Intents continua essencial, porque: (1) você vai encontrar projetos existentes que usam Fragments; (2) integrações com APIs do sistema (câmera, notificações, compartilhamento) ainda passam por esses conceitos; (3) entender o "porquê" da arquitetura moderna exige entender o que ela está simplificando.

## 2. Activity vs Fragment no Contexto Compose

**O que é um Fragment.** Um **Fragment** é um componente introduzido para representar uma parte reutilizável de interface dentro de uma Activity — historicamente usado para dividir a tela em blocos independentes (por exemplo, uma lista de um lado e um detalhe do outro, em tablets) ou para navegar entre "sub-telas" sem criar uma nova Activity para cada uma.

Comparando os dois papéis:
- **Activity**: é o ponto de entrada (launcher) do app; integra com sistemas do Android (permissões, resultados de outras telas, Intents, widgets de tela inicial, notificações).
- **Fragment**: usado em apps existentes que já adotaram essa abordagem; em projetos Compose novos, geralmente é opcional ou desnecessário.
- **Em Compose**: a recomendação é preferir Single-Activity + Navigation Compose, como você viu no arquivo 04.
- **Interoperabilidade**: é possível inserir Composables dentro de um Fragment existente (usando `ComposeView`) ou dentro de uma Activity (usando `setContent`) — útil quando você está migrando um projeto antigo aos poucos, em vez de reescrever tudo de uma vez.

Exemplo mínimo de Activity com Compose:
```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppRoot() // Composable raiz — a partir daqui, tudo é Compose
        }
    }
}
```

### Erros comuns / Pegadinhas

- **Adicionar Fragments em um projeto novo "porque um tutorial antigo usa":** se o projeto é novo e usa Compose, prefira Navigation Compose. Fragments só fazem sentido para manter compatibilidade com código legado.
- **Achar que Activity e Fragment competem pelo mesmo papel:** eles resolvem problemas diferentes — Activity é o "contêiner raiz" do app; Fragment (quando usado) é uma peça dentro dele.

## 3. Ciclo de Vida da Activity (Estados Principais)

Você já estudou isso em detalhe no arquivo 04 — aqui está o resumo rápido para referência:

```
onCreate -> onStart -> onResume -> (foreground)
onPause -> onStop -> (background)
onDestroy (final)
onRestart (retorno após onStop)
```

Capturando logs (mesmo padrão do arquivo 04, útil para observar o ciclo de vida rodando de verdade):
```kotlin
override fun onStart() { super.onStart(); Log.d("Life", "onStart") }
override fun onResume() { super.onResume(); Log.d("Life", "onResume") }
override fun onPause() { super.onPause(); Log.d("Life", "onPause") }
override fun onStop() { super.onStop(); Log.d("Life", "onStop") }
override fun onDestroy() { super.onDestroy(); Log.d("Life", "onDestroy") }
```

## 4. Fragment (Referência Rápida — Interoperabilidade)

> **Em projetos novos com Jetpack Compose, Fragments não são necessários.** Use Single-Activity + Navigation Compose. Esta seção serve apenas como referência para interoperabilidade com código legado — ou seja, para quando você precisar trabalhar em um projeto Android mais antigo que já usa Fragments.

**O que é.** O ciclo de vida de um Fragment é parecido com o de uma Activity, mas com etapas a mais, porque um Fragment vive "dentro" de uma Activity (que tem seu próprio ciclo de vida):

Ciclo resumido: `onAttach → onCreate → onCreateView → onViewCreated → onStart → onResume`
(reverso: `onPause → onStop → onDestroyView → onDestroy → onDetach`)

- **onAttach**: o Fragment é conectado à Activity que o hospeda.
- **onCreateView**: infla (cria) a view do Fragment — é aqui que, em código legado, o layout XML era carregado.
- **onDestroyView**: a view é destruída, mas o Fragment em si ainda pode existir (por exemplo, se estiver na pilha de retrocesso).
- **onDetach**: o Fragment é desconectado da Activity.

Inserindo Compose em um Fragment existente (útil durante uma migração gradual):
```kotlin
class HomeFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        // ComposeView é uma "ponte": permite hospedar Composables
        // dentro do sistema de Views tradicional (usado por Fragments legados)
        setContent { HomeScreen() }
    }
}
```

## 5. Lifecycle x Compose (Efeitos)

**O que é.** No Compose, "efeitos" (effects) são funções especiais que permitem executar código que tem **efeitos colaterais** — ou seja, código que interage com o mundo fora da árvore de Composables (chamar uma API, registrar um listener, atualizar uma biblioteca externa) de forma segura em relação ao ciclo de vida da composição.

**Por que isso importa.** Composables podem ser recompostos (reexecutados) várias vezes, e a ordem/frequência disso não é totalmente previsível "na mão". Se você chamar uma função com efeito colateral direto no corpo de um Composable (fora de um efeito), ela pode rodar mais vezes do que você espera, causando bugs como chamadas de API duplicadas.

- **`LaunchedEffect(key)`**: executa uma coroutine quando o Composable entra na composição pela primeira vez, ou sempre que a `key` (chave) passada mudar.
- **`DisposableEffect(key)`**: registra um recurso (como um observador) e garante que ele será limpo (`dispose`) quando o Composable sair da composição.
- **`SideEffect`**: executa código de forma síncrona depois de cada recomposição bem-sucedida — útil para sincronizar com sistemas fora do Compose.
- **`rememberUpdatedState(value)`**: evita que um efeito de longa duração capture um valor "antigo" de uma variável ou callback que muda ao longo do tempo.

**Quando usar cada efeito?**

| Efeito | Use quando... | Exemplo |
|---|---|---|
| `LaunchedEffect(key)` | Precisar executar uma **suspend function** (uma função que pode pausar sem bloquear a thread, como você viu no arquivo 02) ao entrar na composição ou quando uma chave mudar. | Buscar dados de uma API ao abrir a tela; iniciar um timer. |
| `DisposableEffect(key)` | Precisar registrar um recurso que exige limpeza (dispose) ao sair da composição. | Adicionar/remover um `LifecycleObserver`; registrar um listener de sensor. |
| `SideEffect` | Precisar sincronizar estado do Compose com código externo (não-Compose) a cada recomposição bem-sucedida. | Atualizar uma biblioteca de analytics com um valor de estado atual. |
| `rememberUpdatedState(value)` | Tiver um callback ou valor que pode mudar, mas é usado dentro de um efeito de longa duração que não deve ser reiniciado. | Manter referência atualizada de um `onTick` lambda dentro de um `LaunchedEffect(Unit)`. |

### Exemplo comentado: `LaunchedEffect` + `rememberUpdatedState`

#### Passo 1 — a versão mais simples de um timer

```kotlin
@Composable
fun Timer(onTick: (Long) -> Unit) {
    LaunchedEffect(Unit) {
        // 'Unit' como key significa "rode só uma vez, ao entrar na composição"
        while (true) {
            delay(1000) // suspend function — pausa sem travar a thread
            onTick(System.currentTimeMillis())
        }
    }
}
```

Isso já funciona no primeiro momento. O problema aparece se o valor de `onTick` (a lambda passada pelo chamador) mudar entre recomposições: como a `key` do `LaunchedEffect` é `Unit` (nunca muda), o efeito nunca é relançado — então o loop `while` continua usando para sempre a **versão antiga** de `onTick`, capturada quando o efeito começou.

#### Passo 2 — corrigindo com `rememberUpdatedState`

```kotlin
@Composable
fun Timer(onTick: (Long) -> Unit) {
    val currentOnTick by rememberUpdatedState(onTick)
    // Sempre que 'onTick' mudar entre recomposições, 'currentOnTick' é
    // atualizado — sem precisar reiniciar o LaunchedEffect.

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentOnTick(System.currentTimeMillis()) // sempre usa a versão mais recente
        }
    }
}
```

### Exemplo comentado: `DisposableEffect`

Observando o Lifecycle da Activity (por exemplo, para enviar eventos de analytics) usando `LifecycleOwner`:

#### Passo 1 — registrando o observer (sem limpeza)

```kotlin
@Composable
fun LifecycleLogger(tag: String) {
    // LocalLifecycleOwner dá acesso ao "dono" do ciclo de vida atual
    // (geralmente a Activity ou o NavBackStackEntry da tela)
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            Log.d(tag, "Event: $event") // chamado a cada mudança de estado do ciclo de vida
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        // Faltando: e se este Composable sair da composição? O observer
        // continua registrado no lifecycleOwner para sempre — vazamento de memória.
    }
}
```

#### Passo 2 — adicionando `onDispose` para limpar o observer

Dentro da mesma função `LifecycleLogger`, só o corpo do `DisposableEffect` muda:

```kotlin
DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
        Log.d(tag, "Event: $event")
    }
    lifecycleOwner.lifecycle.addObserver(observer)

    onDispose {
        // onDispose roda quando o Composable sai da composição —
        // aqui removemos o observer para não vazar memória
        lifecycleOwner.lifecycle.removeObserver(observer)
    }
}
```

### Erros comuns / Pegadinhas

- **Chamar uma função com efeito colateral direto no corpo do Composable**, sem envolver em `LaunchedEffect`/`SideEffect`: isso pode rodar a cada recomposição, de forma imprevisível — por exemplo, disparando uma chamada de rede repetidas vezes.
- **Esquecer o `onDispose { }` em um `DisposableEffect`**: sem ele, o recurso registrado (como um observer) nunca é removido, causando vazamento de memória.
- **Usar a key errada em `LaunchedEffect`**: se a key nunca muda, o efeito só roda uma vez (o que às vezes é o que você quer, com `Unit`); se a key muda a cada recomposição sem necessidade, o efeito reinicia com frequência demais.

## 6. Intents

**O que é.** Você vai estudar Intents em profundidade no arquivo 06 — aqui vai a definição básica: Intents conectam componentes do Android (Activity, Service, Broadcast Receiver) entre si, dentro do mesmo app ou entre apps diferentes.

Tipos:
- **Explícito**: você informa o nome exato da classe alvo (usado para abrir uma tela específica dentro do próprio app).
- **Implícito**: você descreve uma ação e dados, e o sistema encontra o componente adequado para atendê-la (usado para pedir a outro app que faça algo, como abrir um link).

Explícito:
```kotlin
startActivity(Intent(this, DetailActivity::class.java).apply {
    putExtra("userId", 42) // anexa um dado extra que a tela de destino pode ler
})
```

Implícito (abrir URL):
```kotlin
val uri = Uri.parse("https://developer.android.com")
startActivity(Intent(Intent.ACTION_VIEW, uri))
// ACTION_VIEW + uma Uri de site pede ao sistema para abrir o navegador padrão
```

Enviar texto para outro app:
```kotlin
val sendIntent = Intent().apply {
    action = Intent.ACTION_SEND // ação padrão para "compartilhar conteúdo"
    putExtra(Intent.EXTRA_TEXT, "Olá")
    type = "text/plain" // tipo MIME — informa que tipo de dado está sendo enviado
}
startActivity(Intent.createChooser(sendIntent, "Compartilhar via"))
// createChooser força a exibição de uma lista de apps para o usuário escolher
```

## 7. Passagem de Dados

**O que é.** Quando você navega entre Activities usando Intents, muitas vezes precisa enviar dados junto (não só a ação). Existem duas formas principais:

- **Primitivos** (String, Int, Boolean, etc.) via `putExtra`/`getX` — o método usado depende do tipo do dado (ex: `getStringExtra`, `getIntExtra`).
- **Objetos completos**: usando `Parcelable`, uma interface do Android que permite "empacotar" um objeto para ser transportado entre componentes de forma eficiente.

```kotlin
@Parcelize
// @Parcelize é uma anotação do plugin do Kotlin que GERA automaticamente
// o código repetitivo (boilerplate) necessário para implementar Parcelable —
// sem ela, você precisaria escrever manualmente a lógica de serialização.
data class User(val id: Int, val name: String): Parcelable
```
Uso:
```kotlin
intent.putExtra("user", user)
val user = intent.getParcelableExtra<User>("user")
// getParcelableExtra desempacota o objeto de volta, usando a chave "user"
```

### Erros comuns / Pegadinhas

- **Esquecer `@Parcelize` (ou implementar Parcelable manualmente sem necessidade)**: sem a anotação, tentar usar a classe como Parcelable gera erro de compilação.
- **Usar chaves diferentes ao salvar (`putExtra`) e ao recuperar (`getX`)**: um erro de digitação na chave (string) faz o valor recuperado vir sempre `null`, e o compilador não consegue avisar sobre isso — veja a seção 3 do arquivo 06 sobre como evitar esse problema com constantes.

## 8. Activity Result API (Recomendado)

**O que é.** É a forma moderna de pedir um resultado a outra Activity ou a um componente do sistema (como a galeria de fotos), substituindo o antigo `onActivityResult` (que exigia lidar manualmente com códigos numéricos de requisição, propensos a erro).

Registrar:
```kotlin
private val pickImage =
    registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        // Esta lambda é chamada automaticamente quando o usuário escolhe
        // (ou cancela) a seleção de imagem. 'uri' é null se ele cancelar.
        // tratar uri
    }

fun openPicker() { pickImage.launch("image/*") } // dispara o seletor de imagens
```

Compose chamando um método da Activity: como Composables não têm acesso direto aos métodos da Activity, você normalmente passa uma lambda de callback via parâmetro (como visto no arquivo 04) ou via ViewModel.

## 9. Navegação Compose vs Intents

Para telas internas do seu próprio app, prefira **Navigation Compose** em vez de Intents:
```kotlin
@Composable
fun AppRoot() {
    val nav = rememberNavController()
    // rememberNavController cria e mantém o controlador de navegação
    // vivo entre recomposições

    NavHost(nav, startDestination = "home") {
        // NavHost define o "mapa" de telas possíveis (rotas) e qual é a inicial
        composable("home") {
            HomeScreen(onDetail = { id -> nav.navigate("detail/$id") })
        }
        composable(
            route = "detail/{id}",
            // "{id}" na rota é um placeholder — um valor variável na URL de navegação
            arguments = listOf(navArgument("id"){ type = NavType.IntType })
        ) { backStackEntry ->
            DetailScreen(id = backStackEntry.arguments?.getInt("id") ?: 0)
        }
    }
}
```

Misturando as duas abordagens: use Intent para sair do escopo do seu app (por exemplo, abrir as configurações do sistema) e Navigation Compose para telas internas — a tabela do arquivo 06 detalha essa distinção.

## 10. Boas Práticas

- Manter lógica de estado fora da Activity, dentro do `ViewModel` (você viu essa recomendação no arquivo 04).
- Evitar acessar diretamente o ciclo de vida dentro de composables; prefira usar os efeitos (`LaunchedEffect`, `DisposableEffect`) e observadores vistos na seção 5.
- Preferir uma **única fonte de verdade** (Single Source of Truth) — geralmente o `ViewModel` combinado com `StateFlow` ou `MutableState` — em vez de duplicar o mesmo dado em vários lugares.
- Usar sealed classes (você viu no arquivo 02) ou `Parcelable`/`Serializable` para modelar dados de navegação de forma legível e segura.
- Minimizar o uso de Fragments em projetos Compose novos.

## 11. Exemplo Integrado Simplificado

Este exemplo junta tudo que vimos até aqui: Activity, Navegação interna com Compose, e um Intent externo (para abrir as configurações de Wi-Fi do sistema).

```kotlin
class MainActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AppRoot(openSettings = {
            // Intent para SAIR do app e abrir uma tela do próprio sistema Android
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
                onNavigateDetail = { id -> nav.navigate("detail/$id") }, // navegação INTERNA
                onOpenSettings = openSettings // ação EXTERNA, delegada à Activity
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
        LifecycleLogger(tag = "HomeLifecycle") // reutilizando o composable da seção 5
    }
}
```

## 12. Checklist Rápido

- **Activity mínima com `setContent`?** — A Activity deve conter apenas a chamada `setContent { ... }` e configurações essenciais (ex: tema, permissões). Toda lógica de UI fica nos composables e toda lógica de negócio fica no ViewModel. Isso mantém a Activity leve e testável.
- **Usando Navigation Compose?** — Navegar entre telas com `NavHost` e `NavController` evita a complexidade de múltiplas Activities ou Fragments. Garante navegação declarativa, type-safe e integrada ao ciclo de vida do Compose.
- **Evitou lógica pesada na Activity?** — Colocar lógica de negócio ou chamadas de rede na Activity gera código difícil de testar e viola o princípio de separação de responsabilidades. Mova tudo para ViewModels e repositórios.
- **Intents apenas para funcionalidades externas?** — Intents devem ser usados para interagir com outros apps ou componentes do sistema (câmera, configurações, compartilhamento). Para navegação interna, prefira Navigation Compose — é mais seguro e previsível.
- **Lifecycle observado via efeitos?** — Em vez de sobrescrever callbacks de ciclo de vida diretamente, use `DisposableEffect` com `LifecycleObserver` dentro dos composables. Isso mantém o código reativo, desacoplado e com limpeza automática.

## Resumo

- Em Compose, a arquitetura recomendada é Single-Activity + Navigation Compose; Fragments existem principalmente para interoperabilidade com código legado.
- O ciclo de vida da Activity (visto no arquivo 04) é a base para entender os "efeitos" do Compose (`LaunchedEffect`, `DisposableEffect`, `SideEffect`, `rememberUpdatedState`), que existem justamente para lidar com efeitos colaterais de forma segura em relação a esse ciclo.
- Intents conectam componentes do Android — explícitos para destinos internos conhecidos, implícitos para pedir ao sistema que encontre um app capaz de atender a uma ação.
- Passagem de dados entre Activities usa `putExtra`/`getX` para primitivos e `Parcelable` (com `@Parcelize`) para objetos.
- Activity Result API é a forma moderna de obter resultados de outras telas ou apps, substituindo `onActivityResult`.

**Próximo passo:** no arquivo 06, você vai se aprofundar em Intents — tipos, boas práticas de segurança e quando preferir Navigation Compose.
