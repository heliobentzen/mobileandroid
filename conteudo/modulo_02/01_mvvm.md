# Módulo 2: MVVM + Fluxo Unidirecional

Objetivo: Mostrar o padrão MVVM com `StateFlow` e Jetpack Compose de forma direta, usando uma lista de tarefas carregada de um repositório simulado.

---

## Conceitos

- **Model**: Fonte de dados (ex: Repository).
- **ViewModel**: Expõe estado imutável (`StateFlow<UiState>`) e processa eventos.
- **View (Compose)**: Observa estado e envia eventos de usuário.
- **Fluxo**: View -> ViewModel -> Repository -> ViewModel -> View.

### Diagrama do Fluxo MVVM

```plaintext
[View] -- eventos --> [ViewModel] -- solicitações --> [Repository]
   ^                                                      |
   |-------------------- estado atualizado ----------------|
```

---

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

---

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

---

## ViewModel (Somente Lógica de Apresentação)

```kotlin
// TasksViewModel.kt
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TasksViewModel(private val repository: TasksRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState

    fun loadTasks() {
        viewModelScope.launch {
            try {
                val tasks = repository.fetchTasks()
                _uiState.value = UiState.Success(tasks)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Erro desconhecido")
            }
        }
    }
}
```

---

## View (Compose)

A View no MVVM é responsável apenas por **observar** o estado e **renderizar** a UI. Ela não contém lógica de negócio. O `collectAsState()` converte o `StateFlow` do ViewModel em um estado observável pelo Compose, disparando recomposição sempre que o valor mudar.

```kotlin
// TasksScreen.kt
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun TasksScreen(viewModel: TasksViewModel = viewModel()) {
    // Coleta o StateFlow como estado do Compose.
    // Sempre que uiState mudar no ViewModel, esta função é recomposta.
    val uiState by viewModel.uiState.collectAsState()

    when (uiState) {
        is UiState.Loading -> LoadingScreen()
        is UiState.Success -> TasksList((uiState as UiState.Success).tasks)
        is UiState.Error -> ErrorScreen(
            message = (uiState as UiState.Error).message,
            onRetry = { viewModel.loadTasks() }
        )
    }
}

// Exibe um indicador de carregamento centralizado na tela
@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

// Exibe a lista de tarefas usando LazyColumn (equivalente ao RecyclerView)
@Composable
fun TasksList(tasks: List<Task>, onToggle: (Task) -> Unit = {}) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(tasks, key = { it.id }) { task ->
            TaskItem(task, onToggle)
        }
    }
}

// Composable para renderizar um único item da lista.
// Recebe o callback `onToggle` do pai, mantendo este composable stateless.
@Composable
fun TaskItem(task: Task, onToggle: (Task) -> Unit = {}) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Passamos onToggle para onCheckedChange — o composable é stateless:
            // ele delega a mudança de estado para quem o chamou (ViewModel).
            Checkbox(
                checked = task.done,
                onCheckedChange = { onToggle(task) }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

// Exibe a mensagem de erro com opção de tentar novamente
@Composable
fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Ocorreu um erro: $message",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Tentar novamente")
        }
    }
}
```

---

## Exercícios Práticos

1. **Estado da UI**:
   - Adicione um novo estado à sealed interface `UiState` para representar uma lista vazia.

2. **Repository**:
   - Modifique o `TasksRepository` para simular diferentes tempos de resposta e erros específicos.

3. **ViewModel**:
   - Adicione um método ao `TasksViewModel` para marcar uma tarefa como concluída e atualizar o estado.

4. **View**:
   - Implemente a função `TasksList` para exibir as tarefas em uma `LazyColumn`.

5. **Desafio**:
   - Crie um botão na tela de erro para tentar carregar as tarefas novamente.

---
