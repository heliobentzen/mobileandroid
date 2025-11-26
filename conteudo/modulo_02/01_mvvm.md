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

```kotlin
// TasksScreen.kt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun TasksScreen(viewModel: TasksViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    when (uiState) {
        is UiState.Loading -> LoadingScreen()
        is UiState.Success -> TasksList((uiState as UiState.Success).tasks)
        is UiState.Error -> ErrorScreen((uiState as UiState.Error).message)
    }
}

@Composable
fun LoadingScreen() {
    // Exibe um indicador de carregamento
}

@Composable
fun TasksList(tasks: List<Task>) {
    // Exibe a lista de tarefas
}

@Composable
fun ErrorScreen(message: String) {
    // Exibe uma mensagem de erro
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
