# Módulo 2: Arquitetura MVVM e Fluxo de Dados Unidirecional

Neste módulo, vamos mergulhar na arquitetura MVVM (Model-View-ViewModel) e como implementá-la de forma moderna no Android, utilizando `ViewModel`, `StateFlow` para gerenciamento de estado e um fluxo de dados unidirecional.

## O que é MVVM?

MVVM é um padrão de arquitetura que facilita a separação de responsabilidades no desenvolvimento da UI.

-   **Model**: Representa os dados e a lógica de negócio. É responsável por buscar e manipular os dados (seja de uma API, banco de dados local, etc.).
-   **View**: A camada de UI (Activities, Fragments, ou Composable functions). Sua única responsabilidade é exibir os dados recebidos do ViewModel e enviar eventos do usuário para ele. A View não contém nenhuma lógica de negócio.
-   **ViewModel**: Atua como uma ponte entre o Model e a View. Ele expõe os dados do Model de uma forma que a View possa consumir facilmente e contém a lógica de apresentação. O ViewModel sobrevive a mudanças de configuração (como rotação de tela), mantendo o estado da UI.

## Gerenciamento de Estado com `StateFlow` e `UiState`

Para comunicar o estado da UI do `ViewModel` para a `View`, usamos o `StateFlow`, uma evolução do `LiveData` que se integra perfeitamente com Coroutines. Para representar os diferentes estados da UI (carregando, sucesso, erro), criamos uma `sealed class` chamada `UiState`.

```kotlin
sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}
```

-   `Loading`: Indica que os dados estão sendo carregados.
-   `Success`: Indica que os dados foram carregados com sucesso e os contém.
-   `Error`: Indica que ocorreu um erro e contém uma mensagem.

## Fluxo Unidirecional de Dados (UDF)

UDF é um padrão de design onde o estado flui em apenas uma direção. Isso torna o fluxo de dados previsível e mais fácil de depurar.

1.  **Estado (State)**: O `ViewModel` mantém e atualiza o estado. Ele expõe o estado para a `View` através de um `StateFlow`. A `View` observa esse fluxo e se atualiza sempre que o estado muda.
2.  **Eventos (Events)**: A `View` captura eventos do usuário (cliques, texto digitado) e os envia para o `ViewModel` como chamadas de função.
3.  O `ViewModel` processa os eventos, interage com o `Model` (se necessário) e atualiza seu estado. A mudança de estado faz com que a `View` se redesenhe.

**Fluxo:** `View` -> `ViewModel` -> `Model` -> `ViewModel` -> `View`

```
      Eventos (ex: clique)
View +---------------------> ViewModel
 ^                             |
 |                             | Lógica de Negócio / Busca de Dados
 |                             v
 +---------------------< Model
      Estado (UiState)
```

## Exemplo Prático

Vamos criar uma tela simples que busca uma lista de tarefas.

### 1. O `ViewModel`

O `ViewModel` buscará os dados (simularemos uma chamada de rede) e exporá o estado através de um `StateFlow<UiState<List<String>>>`.

```kotlin
// TasksViewModel.kt
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TasksViewModel : ViewModel() {

    // O StateFlow privado que pode ser modificado internamente.
    private val _uiState = MutableStateFlow<UiState<List<String>>>(UiState.Loading)
    
    // O StateFlow público e somente leitura que a UI irá observar.
    val uiState = _uiState.asStateFlow()

    init {
        // Inicia a busca de dados quando o ViewModel é criado.
        fetchTasks()
    }

    // Evento que a UI pode chamar para recarregar os dados.
    fun refreshTasks() {
        fetchTasks()
    }

    private fun fetchTasks() {
        viewModelScope.launch {
            // 1. Define o estado como Loading
            _uiState.value = UiState.Loading
            try {
                // Simula uma chamada de rede com um delay
                delay(2000) 

                // Simula um erro aleatório
                if (System.currentTimeMillis() % 2 == 0L) {
                    throw Exception("Falha ao conectar com o servidor.")
                }

                val tasks = listOf("Comprar pão", "Estudar MVVM", "Passear com o cachorro")
                
                // 2. Em caso de sucesso, atualiza o estado com os dados
                _uiState.value = UiState.Success(tasks)

            } catch (e: Exception) {
                // 3. Em caso de erro, atualiza o estado com a mensagem
                _uiState.value = UiState.Error(e.message ?: "Erro desconhecido")
            }
        }
    }
}
```

### 2. A `View` (Jetpack Compose)

A `View` observará o `uiState` do `ViewModel` e se renderizará de acordo com o estado atual.

```kotlin
// TasksScreen.kt
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun TasksScreen(tasksViewModel: TasksViewModel = viewModel()) {
    // Coleta o estado do ViewModel. O Composable será recomposto sempre que o estado mudar.
    val uiState by tasksViewModel.uiState.collectAsState()

    // O `when` garante que todos os estados da sealed class sejam tratados.
    when (val state = uiState) {
        is UiState.Loading -> {
            LoadingComponent()
        }
        is UiState.Success -> {
            TasksListComponent(tasks = state.data) {
                // Envia um evento para o ViewModel quando o botão é clicado
                tasksViewModel.refreshTasks()
            }
        }
        is UiState.Error -> {
            ErrorComponent(message = state.message) {
                // Envia um evento para o ViewModel quando o botão é clicado
                tasksViewModel.refreshTasks()
            }
        }
    }
}

@Composable
fun LoadingComponent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun TasksListComponent(tasks: List<String>, onRefreshClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Minhas Tarefas", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        tasks.forEach { task ->
            Text(task, style = MaterialTheme.typography.bodyLarge)
        }
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onRefreshClick) {
            Text("Recarregar")
        }
    }
}

@Composable
fun ErrorComponent(message: String, onRetryClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Ocorreu um erro:", color = MaterialTheme.colorScheme.error)
        Text(message, color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetryClick) {
            Text("Tentar Novamente")
        }
    }
}
```

### Resumo do Fluxo no Exemplo

1.  `TasksScreen` é exibida. `TasksViewModel` é criado.
2.  O `init` do `ViewModel` chama `fetchTasks()`.
3.  O `_uiState` é atualizado para `UiState.Loading`.
4.  `TasksScreen` coleta o novo estado e exibe o `LoadingComponent`.
5.  Após o `delay`, `fetchTasks()` termina.
    -   **Se sucesso**: `_uiState` vira `UiState.Success(...)`. `TasksScreen` exibe `TasksListComponent`.
    -   **Se erro**: `_uiState` vira `UiState.Error(...)`. `TasksScreen` exibe `ErrorComponent`.
6.  O usuário clica no botão "Recarregar" ou "Tentar Novamente".
7.  O evento `onClick` chama a função `refreshTasks()` no `ViewModel`.
8.  O ciclo recomeça a partir do passo 3.

## Atividade Prática Guiada: Construindo a Tela de Tarefas

Agora, vamos colocar a mão na massa e construir a tela de tarefas do zero, seguindo os conceitos que aprendemos.

### Passo 1: Criar o `UiState`

Primeiro, defina a classe selada que representará todos os possíveis estados da nossa UI.

1.  Crie um novo arquivo Kotlin chamado `UiState.kt`.
2.  Adicione o seguinte código:

```kotlin
// UiState.kt
sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}
```

### Passo 2: Implementar o `TasksViewModel`

Este `ViewModel` será o cérebro da nossa tela, gerenciando o estado e a lógica de busca de dados.

1.  Certifique-se de ter as dependências do `ViewModel` e `Lifecycle` no seu `build.gradle.kts` (nível do módulo):
    ```groovy
    // build.gradle.kts
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    ```
2.  Crie um novo arquivo Kotlin chamado `TasksViewModel.kt`.
3.  Implemente a classe `TasksViewModel` exatamente como mostrado na seção "Exemplo Prático" acima. Ela conterá a lógica para buscar as tarefas e expor o `UiState`.

### Passo 3: Construir a `View` com Jetpack Compose

Agora, vamos criar os Composables que irão reagir às mudanças de estado.

1.  Crie um novo arquivo Kotlin chamado `TasksScreen.kt`.
2.  Adicione os quatro Composables (`TasksScreen`, `LoadingComponent`, `TasksListComponent`, `ErrorComponent`) exatamente como mostrado na seção "Exemplo Prático".
    -   `TasksScreen` irá observar o `uiState` e decidir qual componente mostrar.
    -   Os outros componentes são responsáveis por desenhar cada estado específico.

### Passo 4: Integrar a Tela na Aplicação

Para ver sua nova tela em ação, chame o `TasksScreen` Composable de dentro do `setContent` da sua `MainActivity`.

```kotlin
// MainActivity.kt
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.seutema.ui.theme.SeuTemaTheme // Altere para o seu tema

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SeuTemaTheme { // Altere para o seu tema
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Chame sua tela aqui!
                    TasksScreen()
                }
            }
        }
    }
}
```

### Passo 5: Testar o Fluxo

1.  Execute o aplicativo no emulador ou em um dispositivo físico.
2.  **Observe o fluxo:** Você verá primeiro o indicador de progresso (`LoadingComponent`) por 2 segundos.
3.  **Resultado:** Em seguida, a tela mudará para a lista de tarefas (`TasksListComponent`) ou para a mensagem de erro (`ErrorComponent`), dependendo do resultado da simulação.
4.  **Interaja:** Clique no botão "Recarregar" ou "Tentar Novamente". Observe como a UI envia um evento para o `ViewModel`, que reinicia o fluxo de dados, começando novamente pelo estado de `Loading`.

Parabéns! Você implementou com sucesso uma tela reativa usando MVVM e um fluxo de dados unidirecional no Android com Jetpack Compose.