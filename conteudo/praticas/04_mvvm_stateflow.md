# Prática: MVVM com StateFlow para Iniciantes

Este guia apresenta exercícios práticos do padrão **MVVM** (Model-View-ViewModel) usando `StateFlow` e Jetpack Compose. O objetivo é separar a lógica de negócio da interface gráfica, tornando o código mais organizado e fácil de manter.

---

## Por que usar MVVM?

Sem MVVM, é fácil colocar lógica de negócio diretamente nos composables. Isso funciona para exemplos simples, mas torna o código difícil de testar e manter. O MVVM resolve isso:

- **Model**: representa os dados (data classes, repositório).
- **ViewModel**: contém a lógica; expõe o estado para a UI.
- **View (Compose)**: apenas exibe o estado e envia eventos para o ViewModel.

---

## Pré-requisitos

Adicione as dependências ao `app/build.gradle.kts`:

```kotlin
dependencies {
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
}
```

---

## Prática 1: Contador com ViewModel

### Objetivo
Mover o estado de um contador para um `ViewModel` usando `StateFlow`.

Este é o exemplo mais simples possível de MVVM, mas nele já aparecem as duas peças centrais que você vai usar em praticamente todo app: um `ViewModel` guardando o estado, e a tela apenas observando e reagindo a esse estado. Dominar esse fluxo básico com um contador facilita muito entender casos mais complexos depois, como listas de tarefas ou dados vindos de uma API.

### Passo a Passo

**1. Crie o ViewModel** (`ContadorViewModel.kt`):

```kotlin
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ContadorViewModel : ViewModel() {

    // Estado privado (mutável internamente)
    private val _contador = MutableStateFlow(0)

    // Estado público (somente leitura para a UI)
    val contador: StateFlow<Int> = _contador.asStateFlow()

    fun incrementar() {
        _contador.value++
    }

    fun decrementar() {
        if (_contador.value > 0) _contador.value--
    }

    fun resetar() {
        _contador.value = 0
    }
}
```

**2. Crie a tela** (`ContadorScreen.kt`):

```kotlin
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ContadorScreen(viewModel: ContadorViewModel = viewModel()) {
    // Observa o StateFlow — a UI atualiza automaticamente quando o valor muda
    val contador by viewModel.contador.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Contador", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "$contador", fontSize = 64.sp)

        Spacer(modifier = Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = { viewModel.decrementar() }) { Text("−") }
            Button(onClick = { viewModel.incrementar() }) { Text("+") }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(onClick = { viewModel.resetar() }) {
            Text("Resetar")
        }
    }
}
```

**3. Conecte na `MainActivity`**:

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ContadorScreen()
            }
        }
    }
}
```

### Por que isso é melhor?

- Se o usuário girar a tela, o `ViewModel` sobrevive e o contador **não é perdido**.
- A lógica de negócio (incrementar, decrementar) está isolada da UI — fácil de testar.

> **💡 Por trás dos panos**
> O `ViewModel` sobrevive a mudanças de configuração (como girar a tela) porque o Android o mantém vivo separadamente da Activity/Composable que o usa — apenas a interface é recriada, não o `ViewModel`. É por isso que o contador não zera ao girar a tela: o estado mora no `ViewModel`, não na UI. Já o `MutableStateFlow` funciona como uma "caixa" que guarda o valor atual e avisa automaticamente todo mundo que está observando (`collectAsStateWithLifecycle`) sempre que o valor muda — não é preciso "empurrar" a atualização manualmente para a tela.

### Exercícios

1. Adicione um estado `historico: List<String>` ao ViewModel que registra cada operação (`"+1"`, `"-1"`, `"reset"`). Exiba o histórico abaixo do contador na tela.
   - *Dica se travar*: você vai precisar de dois `MutableStateFlow` no ViewModel (um para o contador, outro para o histórico) ou combinar os dois em uma única data class de estado.
2. Adicione um limite máximo configurável ao contador (ex.: não pode passar de 100). Mostre uma mensagem de aviso quando o limite for atingido.
3. Adicione um campo de texto na tela para que o usuário defina o valor inicial do contador.
   - *Dica se travar*: use `remember { mutableStateOf("") }` para o campo de texto local (isso não precisa ir no ViewModel) e só chame um método do ViewModel quando o usuário confirmar o valor.

---

## Prática 2: Lista de Tarefas (Todo List) com MVVM

### Objetivo
Criar um app de lista de tarefas usando o padrão MVVM completo.

Diferente do contador (que tinha um único valor), aqui o estado da tela é um objeto mais complexo — uma lista de tarefas, um texto sendo digitado, uma mensagem de erro. É exatamente esse tipo de cenário que você vai encontrar no dia a dia: quase toda tela real precisa representar vários pedaços de informação ao mesmo tempo. Essa prática ensina o padrão de agrupar tudo isso em uma única `data class` de estado (`TarefaUiState`), que é como profissionais estruturam telas Compose no mundo real.

### Passo a Passo

**1. Modelos de dados** (`Tarefa.kt`):

```kotlin
data class Tarefa(
    val id: Int,
    val titulo: String,
    val concluida: Boolean = false
)
```

**2. Estado da UI** (`TarefaUiState.kt`):

```kotlin
data class TarefaUiState(
    val tarefas: List<Tarefa> = emptyList(),
    val textoNovoItem: String = "",
    val mensagem: String? = null
)
```

**3. ViewModel** (`TarefaViewModel.kt`):

```kotlin
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class TarefaViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(TarefaUiState())
    val uiState: StateFlow<TarefaUiState> = _uiState.asStateFlow()

    private var proximoId = 1

    fun atualizarTexto(texto: String) {
        _uiState.update { it.copy(textoNovoItem = texto) }
    }

    fun adicionarTarefa() {
        val texto = _uiState.value.textoNovoItem.trim()
        if (texto.isBlank()) {
            _uiState.update { it.copy(mensagem = "Digite o título da tarefa.") }
            return
        }
        _uiState.update { estado ->
            estado.copy(
                tarefas = estado.tarefas + Tarefa(id = proximoId++, titulo = texto),
                textoNovoItem = "",
                mensagem = null
            )
        }
    }

    fun alternarConclusao(id: Int) {
        _uiState.update { estado ->
            estado.copy(
                tarefas = estado.tarefas.map { tarefa ->
                    if (tarefa.id == id) tarefa.copy(concluida = !tarefa.concluida) else tarefa
                }
            )
        }
    }

    fun removerTarefa(id: Int) {
        _uiState.update { estado ->
            estado.copy(tarefas = estado.tarefas.filter { it.id != id })
        }
    }

    fun limparMensagem() {
        _uiState.update { it.copy(mensagem = null) }
    }
}
```

**4. Tela** (`TarefaScreen.kt`):

```kotlin
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun TarefaScreen(viewModel: TarefaViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Exibe snackbar quando há mensagem
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.mensagem) {
        uiState.mensagem?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.limparMensagem()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("Minhas Tarefas", style = MaterialTheme.typography.headlineSmall)

            Spacer(Modifier.height(8.dp))

            // Campo de entrada
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.textoNovoItem,
                    onValueChange = viewModel::atualizarTexto,
                    label = { Text("Nova tarefa") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = viewModel::adicionarTarefa) {
                    Text("Adicionar")
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = "${uiState.tarefas.count { it.concluida }} de ${uiState.tarefas.size} concluídas",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(Modifier.height(8.dp))

            if (uiState.tarefas.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nenhuma tarefa ainda. Adicione uma acima! 😊")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(uiState.tarefas, key = { it.id }) { tarefa ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = tarefa.concluida,
                                onCheckedChange = { viewModel.alternarConclusao(tarefa.id) }
                            )
                            Text(
                                text = tarefa.titulo,
                                modifier = Modifier.weight(1f).padding(start = 4.dp)
                            )
                            IconButton(onClick = { viewModel.removerTarefa(tarefa.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remover")
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
```

> **💡 Por trás dos panos**
> Repare que `_uiState.update { it.copy(...) }` aparece em quase todas as funções do ViewModel. Esse é o padrão recomendado para atualizar `StateFlow`: em vez de modificar o estado atual diretamente, você pede uma **cópia** com os campos alterados (lembra do `copy()` das data classes?) e substitui o estado inteiro por essa cópia. Isso evita bugs de concorrência (duas coroutines tentando alterar o mesmo objeto ao mesmo tempo) e mantém o fluxo de dados previsível: sempre um novo objeto de estado "de cada vez", nunca uma mutação escondida no meio do código.

### Exercícios

1. Adicione um filtro com três abas: **Todas**, **Pendentes** e **Concluídas**. A aba selecionada deve filtrar a lista exibida.
   - Primeiro, adicione um campo `filtro: String` ao `TarefaUiState` (valor padrão `"Todas"`).
   - Depois, crie uma função `selecionarFiltro(filtro: String)` no ViewModel que atualiza esse campo.
   - Por fim, na tela, aplique `.filter { ... }` sobre `uiState.tarefas` de acordo com o filtro antes de passar para o `LazyColumn`.
   - *Dica se travar*: comece exibindo as três abas fixas na tela (sem lógica) e só depois conecte o clique de cada uma à função do ViewModel.
2. Adicione um campo de texto para pesquisa. Filtre a lista em tempo real conforme o usuário digita.
3. Adicione um botão `"Remover todas as concluídas"` que remove de uma vez todas as tarefas marcadas.
   - *Dica se travar*: `tarefas.filterNot { it.concluida }` gera a nova lista sem as tarefas concluídas — use dentro do `_uiState.update { ... }`.

---

## Prática 3: Carregamento Assíncrono com UiState

### Objetivo
Simular o carregamento de dados de uma "rede" e gerenciar os estados Loading, Success e Error.

Toda vez que seu app busca algo da internet, existe um período de espera — e as coisas podem dar errado (sem conexão, servidor fora do ar, etc.). Se a tela não trata esses três estados (carregando, sucesso, erro), o usuário fica olhando para uma tela em branco ou travada sem entender o que está acontecendo. Esse padrão de "estado selado" (`sealed interface`) com `Loading`/`Success`/`Error` é praticamente universal em apps Android profissionais — você vai reutilizá-lo o tempo todo, inclusive nos próximos guias de Retrofit e Room.

### Passo a Passo

**1. Defina o estado da UI** (`QuoteUiState.kt`):

```kotlin
sealed interface QuoteUiState {
    data object Loading : QuoteUiState
    data class Success(val frase: String, val autor: String) : QuoteUiState
    data class Error(val mensagem: String) : QuoteUiState
}
```

**2. Repositório simulado** (`QuoteRepository.kt`):

```kotlin
import kotlinx.coroutines.delay
import kotlin.random.Random

class QuoteRepository {
    private val frases = listOf(
        "A simplicidade é a sofisticação máxima." to "Leonardo da Vinci",
        "O sucesso é a soma de pequenos esforços repetidos dia após dia." to "Robert Collier",
        "Aprenda como se fosse viver para sempre." to "Mahatma Gandhi"
    )

    suspend fun buscarFraseAleatoria(): Pair<String, String> {
        delay(1500) // Simula latência de rede
        if (Random.nextFloat() < 0.2f) throw Exception("Falha ao conectar ao servidor.")
        return frases.random()
    }
}
```

**3. ViewModel** (`QuoteViewModel.kt`):

```kotlin
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class QuoteViewModel(
    private val repository: QuoteRepository = QuoteRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<QuoteUiState>(QuoteUiState.Loading)
    val uiState: StateFlow<QuoteUiState> = _uiState.asStateFlow()

    init { buscarFrase() }

    fun buscarFrase() {
        viewModelScope.launch {
            _uiState.value = QuoteUiState.Loading
            try {
                val (frase, autor) = repository.buscarFraseAleatoria()
                _uiState.value = QuoteUiState.Success(frase, autor)
            } catch (e: Exception) {
                _uiState.value = QuoteUiState.Error(e.message ?: "Erro desconhecido")
            }
        }
    }
}
```

**4. Tela** (`QuoteScreen.kt`):

```kotlin
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun QuoteScreen(viewModel: QuoteViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        when (val state = uiState) {
            is QuoteUiState.Loading -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("Buscando frase...")
                }
            }
            is QuoteUiState.Success -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "\"${state.frase}\"",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "— ${state.autor}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Button(onClick = { viewModel.buscarFrase() }) {
                        Text("Nova frase")
                    }
                }
            }
            is QuoteUiState.Error -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("❌ ${state.mensagem}", color = MaterialTheme.colorScheme.error)
                    Button(onClick = { viewModel.buscarFrase() }) {
                        Text("Tentar novamente")
                    }
                }
            }
        }
    }
}
```

> **💡 Por trás dos panos**
> `sealed interface QuoteUiState` garante que o `when (val state = uiState)` na tela **precise** tratar todos os casos possíveis (`Loading`, `Success`, `Error`) — se você esquecer um caso, o compilador avisa. Isso é diferente de usar variáveis booleanas soltas como `isLoading` e `hasError`, onde é fácil deixar combinações inválidas passarem despercebidas (por exemplo, `isLoading = true` e `hasError = true` ao mesmo tempo, o que não faz sentido). Com um estado selado, só existe um estado "verdadeiro" por vez — a tela nunca fica em uma combinação impossível.

### Exercícios

1. Adicione um estado `Empty` ao `QuoteUiState` para quando o repositório retornar uma lista vazia. Adapte o repositório para simular essa situação.
   - *Dica se travar*: `Empty` pode ser um `data object`, igual a `Loading` — não precisa carregar nenhum dado extra.
2. Adicione um botão `"Favoritar"` na tela de sucesso. Mantenha no ViewModel uma lista de frases favoritas e crie uma segunda tela que exibe essa lista.
   - Primeiro, adicione um `MutableStateFlow<List<Pair<String, String>>>` de favoritos no ViewModel.
   - Depois, crie a função `favoritar()` que adiciona a frase atual a essa lista.
   - Por fim, crie um novo composable `FavoritosScreen` que recebe a lista e a exibe em uma `Column` ou `LazyColumn`.
3. Adicione um contador de "frases buscadas" ao ViewModel e exiba no canto superior da tela.

---

## Resumo do Padrão MVVM

```
[Composable] -- chama --> [ViewModel.metodo()]
[ViewModel]  -- expõe --> [StateFlow<UiState>]
[Composable] -- observa --> [collectAsStateWithLifecycle()]
```

**Regra de ouro**: O composable **nunca** deve conter lógica de negócio — apenas observar estado e enviar eventos.

---

## Próximos Passos

- Estude o módulo `01_mvvm.md` para aprofundar o padrão.
- Avance para `05_listas_lazy_column.md` para aprender a exibir listas eficientemente.
- Em seguida, veja `07_room_persistencia.md` para persistir dados localmente.
