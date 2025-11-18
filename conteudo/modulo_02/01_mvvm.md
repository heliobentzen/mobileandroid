# Módulo 2: MVVM + Fluxo Unidirecional

Objetivo: Mostrar o padrão MVVM com `StateFlow` e Jetpack Compose de forma direta, usando uma lista de tarefas carregada de um repositório simulado.

## Conceitos

- Model: Fonte de dados (ex: Repository).
- ViewModel: Expõe estado imutável (`StateFlow<UiState>`) e processa eventos.
- View (Compose): Observa estado e envia eventos de usuário.
- Fluxo: View -> ViewModel -> Repository -> ViewModel -> View.

## Estado da UI

Use uma sealed interface enxuta. Cada estado é explícito.

```kotlin
// UiState.kt
sealed interface UiState {
    object Loading : UiState
    data class Success(val tasks: List<Task>) : UiState
    data class Error(val message: String) : UiState
}

data class Task(val id: Long, val title: String, val done: Boolean)
```

## Fonte de Dados (Repository)

```kotlin
// TasksRepository.kt
import kotlinx.coroutines.delay
import kotlin.random.Random

class TasksRepository {
    suspend fun fetchTasks(): List<Task> {
        delay(1200) // Simula rede
        // 25% de chance de erro
        if (Random.nextInt(0, 4) == 0) throw Exception("Servidor indisponível.")
        return listOf(
            Task(1, "Ler documentação Compose", true),
            Task(2, "Implementar ViewModel", false),
            Task(3, "Refatorar UiState", false)
        )
    }
}
```

## ViewModel (Somente Lógica de Apresentação)

```kotlin
// TasksViewModel.kt
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TasksViewModel(
    private val repository: TasksRepository = TasksRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState

    init { load() }

    fun onRefresh() = load()

    private fun load() {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            try {
                val tasks = repository.fetchTasks()
                _uiState.value = UiState.Success(tasks)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Falha inesperada.")
            }
        }
    }
}
```

## View (Compose) Reativa

```kotlin
// TasksScreen.kt
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun TasksScreen(vm: TasksViewModel = viewModel()) {
    val state by vm.uiState.collectAsStateWithLifecycle()

    when (state) {
        UiState.Loading -> Loading()
        is UiState.Success -> TasksList(
            tasks = (state as UiState.Success).tasks,
            onRefresh = vm::onRefresh
        )
        is UiState.Error -> ErrorMessage(
            message = (state as UiState.Error).message,
            onRetry = vm::onRefresh
        )
    }
}

@Composable
private fun Loading() = Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
) { CircularProgressIndicator() }

@Composable
private fun TasksList(tasks: List<Task>, onRefresh: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Tarefas", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        tasks.forEach { task ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    (if (task.done) "✅ " else "⬜ ") + task.title,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = onRefresh) { Text("Atualizar") }
    }
}

@Composable
private fun ErrorMessage(message: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Erro: $message", color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(12.dp))
        Button(onClick = onRetry) { Text("Tentar novamente") }
    }
}
```

## Activity

```kotlin
// MainActivity.kt
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface { TasksScreen() }
            }
        }
    }
}
```

## Dependências (build.gradle.kts)

```kotlin
implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
implementation("androidx.activity:activity-compose:1.9.2")
implementation("androidx.compose.material3:material3:1.2.1")
```

## Fluxo Resumido

1. ViewModel inicia em Loading.
2. Repository simula busca.
3. Sucesso -> UiState.Success exibido.
4. Erro -> UiState.Error exibido.
5. Usuário aciona refresh -> repete ciclo.

## Por que é moderno?

- `collectAsStateWithLifecycle` evita vazamentos.
- Estado imutável e simples.
- Repositório isolado facilita testes.
- Código curto foca no essencial.

Experimente adicionar: alternância de done, persistência local, paginação.

Pronto: MVVM claro, reativo e previsível.
