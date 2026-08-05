# Módulo 2: MVVM + Fluxo Unidirecional

Objetivo: mostrar o padrão **MVVM** (Model-View-ViewModel) com `StateFlow` e Jetpack Compose de forma direta, usando uma lista de tarefas carregada de um repositório simulado. Ao final desta aula você vai entender por que separar "o que a tela mostra" de "como os dados são buscados e processados" torna o código mais fácil de testar, manter e evoluir.

---

## O que é MVVM?

MVVM é uma forma de organizar o código de uma tela em três partes com responsabilidades bem definidas:

- **Model**: de onde os dados vêm (banco de dados, internet, arquivo). É a "fonte da verdade".
- **ViewModel**: a "ponte" entre o Model e a View. Busca dados, aplica regras de negócio e expõe um **estado** (o que a tela deve mostrar neste momento) pronto para ser exibido.
- **View**: a tela em si (aqui, uma função `@Composable`). Ela só sabe **mostrar** o que o ViewModel manda e **avisar** quando o usuário faz alguma ação (clicar, digitar).

Analogia: pense em um restaurante. O **Model** é a despensa e a geladeira (onde os ingredientes existem). O **ViewModel** é o cozinheiro, que pega os ingredientes crus e prepara o prato pronto para servir. A **View** é o garçom, que apenas leva o prato pronto até a mesa e avisa a cozinha quando o cliente pede algo novo. O garçom não cozinha, e o cozinheiro não atende a mesa — cada um faz uma coisa só, e bem feita.

## Por que isso importa?

Sem essa separação, é comum ver telas (Activities/Composables) que buscam dados da internet, decidem regras de negócio e desenham a UI tudo junto, na mesma função. Isso causa problemas reais:

- **Difícil de testar**: para testar a lógica de "marcar tarefa como concluída" você precisaria rodar a tela inteira em um emulador, em vez de rodar um teste simples de Kotlin puro em segundos.
- **Perda de estado na rotação de tela**: se os dados vivem só dentro do Composable, ao girar o celular a Activity é recriada e os dados podem se perder. Um `ViewModel` sobrevive a mudanças de configuração (como rotação), então o estado permanece.
- **Código difícil de entender e manter**: quando tudo está misturado, qualquer mudança pequena (ex: um novo campo na tela) obriga a mexer em lógica de rede, de banco e de desenho da UI ao mesmo tempo, aumentando o risco de introduzir bugs.

Com MVVM, cada peça pode ser entendida, testada e alterada isoladamente — sem medo de quebrar as outras.

### Diagrama do Fluxo MVVM

O fluxo é sempre em um único sentido (por isso "fluxo unidirecional", ou *unidirectional data flow*): a View nunca altera dados diretamente, ela só envia eventos (como "usuário clicou no botão X") para o ViewModel, que decide o que fazer com eles.

```plaintext
[View] -- eventos --> [ViewModel] -- solicitações --> [Repository]
   ^                                                      |
   |-------------------- estado atualizado ----------------|
```

Leia assim: a View dispara um evento (ex: "carregar tarefas"). O ViewModel recebe o evento e pede os dados ao Repository. Quando os dados chegam, o ViewModel atualiza o estado, e a View, que está "observando" esse estado, se redesenha automaticamente — sem que ninguém precise chamar manualmente "atualize a tela agora".

---

## Estado da UI

### O que é

O **estado da UI** (`UiState`) é uma representação de "tudo que a tela precisa saber para se desenhar corretamente neste momento". Em vez de várias variáveis soltas (`isLoading`, `hasError`, `tasks`, `errorMessage`...), agrupamos tudo em um único tipo que representa exatamente as situações possíveis da tela.

Usamos uma **`sealed interface`**: um tipo do Kotlin que restringe quais subtipos podem existir, todos declarados no mesmo arquivo. Isso é útil aqui porque a tela de tarefas só pode estar em um de três estados possíveis: carregando, com sucesso (mostrando a lista) ou com erro. Uma `sealed interface` "trava" essas opções, e o compilador Kotlin obriga você a tratar todos os casos sempre que usar um `when`.

### Por que isso importa

Se você usar variáveis booleanas soltas (`var isLoading = true`, `var hasError = false`...), é fácil criar estados **impossíveis** por engano — por exemplo, `isLoading = true` e `hasError = true` ao mesmo tempo, o que não faz sentido e pode gerar uma tela bugada (mostrando spinner de carregamento e mensagem de erro juntos, por exemplo). Com uma `sealed interface`, cada estado é **mutuamente exclusivo**: a tela está OU carregando, OU com sucesso, OU com erro — nunca duas coisas ao mesmo tempo.

### Exemplo comentado

```kotlin
// UiState.kt

// sealed interface: define um conjunto fechado de possíveis estados da tela.
// Só pode existir Loading, Success ou Error — nada além disso.
sealed interface UiState {
    object Loading : UiState                            // Tela carregando (ex: exibe um spinner)
    data class Success(val tasks: List<Task>) : UiState  // Sucesso — carrega a lista de tarefas
    data class Error(val message: String) : UiState      // Erro — guarda a mensagem para mostrar ao usuário
}

// Modelo simples que representa uma tarefa da lista
data class Task(val id: Long, val title: String, val done: Boolean)
```

### Erros comuns / Pegadinhas

- **Usar várias variáveis booleanas soltas em vez de um estado único**: isso permite combinações inválidas (ex: `loading` e `error` juntos) e é mais difícil de testar.
- **Esquecer o estado de "lista vazia"**: se a busca funciona mas retorna zero tarefas, a tela pode simplesmente não mostrar nada, deixando o usuário sem entender se é um bug ou se realmente não há tarefas. (Veja o Exercício 1 mais abaixo.)
- **Colocar lógica de UI (cores, strings formatadas) dentro do `UiState`**: o estado deve representar *dados*, não *como desenhar*. Deixe decisões visuais para a View.

---

## Fonte de Dados (Repository)

### O que é

O **Repository** é a classe responsável por buscar os dados brutos — de uma API, banco de dados local, cache, etc. Ele não sabe nada sobre a UI; sua única responsabilidade é "conseguir os dados e entregá-los".

### Por que isso importa

Separar o Repository do ViewModel permite trocar a fonte de dados (por exemplo, de uma API fake para uma API real, ou adicionar cache local) sem tocar em nenhuma linha da tela. Também facilita testes: você pode substituir o Repository real por uma versão falsa (fake) nos testes do ViewModel, sem precisar de internet.

### Exemplo comentado

```kotlin
// TasksRepository.kt
import kotlinx.coroutines.delay
import kotlin.random.Random

class TasksRepository {
    // "suspend" indica que esta função pode pausar sua execução sem travar a
    // thread principal — necessário para operações demoradas, como chamadas de rede.
    suspend fun fetchTasks(): List<Task> {
        delay(1200) // Simula o tempo de espera de uma chamada de rede real (1.2s)
        // Simula uma falha aleatória (25% de chance) — útil para testar o estado de erro
        if (Random.nextInt(0, 4) == 0) throw Exception("Servidor indisponível.")
        return listOf(
            Task(1, "Ler documentação Compose", true),
            Task(2, "Implementar ViewModel", false),
            Task(3, "Refatorar UiState", false)
        )
    }
}
```

### Erros comuns / Pegadinhas

- **Fazer chamadas de rede na thread principal (UI thread)**: isso trava a tela — o famoso "app não está respondendo". Sempre use funções `suspend` chamadas dentro de uma coroutine, como veremos no ViewModel a seguir.
- **Deixar o Repository conhecer o `UiState`**: o Repository deve devolver dados "crus" (`List<Task>`), não decidir se isso é `Loading`/`Success`/`Error` — essa é responsabilidade do ViewModel.

---

## ViewModel (Somente Lógica de Apresentação)

### O que é

`ViewModel` é uma classe do Jetpack Android (`androidx.lifecycle.ViewModel`) desenhada para guardar dados e lógica relacionados a uma tela **sobrevivendo a mudanças de configuração**, como a rotação do aparelho. Diferente de uma variável dentro do Composable, o ViewModel não é destruído quando a Activity é recriada.

`StateFlow` é um tipo de fluxo de dados observável — pense nele como uma "caixa" que sempre guarda o valor mais recente e avisa automaticamente todo mundo que está "olhando" para ela (observando) sempre que o valor muda. É a ferramenta que o ViewModel usa para expor o `UiState` atual para a tela.

### Por que isso importa

Se a lógica de "buscar tarefas e decidir se deu certo ou erro" ficasse dentro do Composable, ela seria refeita a cada recomposição e perdida a cada rotação de tela. O ViewModel resolve isso: ele guarda o estado atual e só busca os dados novamente quando você mandar explicitamente (`loadTasks()`).

### Construindo o ViewModel passo a passo

Em vez de olhar para a versão final de uma vez, vamos construir o `TasksViewModel` em duas etapas: primeiro o essencial (expor o estado e buscar os dados), depois o que falta para lidar com falhas de verdade.

#### Passo 1 — a versão mais simples (sem tratar erros)

```kotlin
// TasksViewModel.kt
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TasksViewModel(private val repository: TasksRepository) : ViewModel() {

    // _uiState é privado e mutável — só o próprio ViewModel pode alterá-lo.
    // Começa em UiState.Loading porque, ao abrir a tela, ainda não temos dados.
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)

    // uiState é público e IMUTÁVEL do ponto de vista de quem observa (a View).
    // A View só pode ler o valor, nunca alterá-lo diretamente — isso reforça o
    // fluxo unidirecional (a View não "empurra" estado, ela só reage a ele).
    val uiState: StateFlow<UiState> = _uiState

    fun loadTasks() {
        // viewModelScope: escopo de coroutine atrelado ao ciclo de vida do
        // ViewModel. Quando o ViewModel é destruído, qualquer coroutine
        // pendente aqui é cancelada automaticamente — evita vazamento de memória.
        viewModelScope.launch {
            val tasks = repository.fetchTasks()     // chamada suspend — não trava a UI
            _uiState.value = UiState.Success(tasks)  // atualiza o estado -> View recompõe
        }
    }
}
```

Essa versão já resolve o problema principal: o estado sobrevive à rotação de tela e a View só reage ao que o ViewModel publica. Mas ela tem uma limitação séria — lembra que `TasksRepository.fetchTasks()` pode lançar uma exceção (falha de "servidor")? Como nada aqui captura essa exceção, uma falha de rede derruba a coroutine sem aviso, e a tela nunca sai do estado `Loading` (ou, dependendo do caso, o app pode até fechar sozinho). O caso de `UiState.Error` que definimos nunca chega a ser usado.

#### Passo 2 — adicionando tratamento de erro (versão final)

A mudança é pequena: envolver a chamada em `try/catch` e, em caso de falha, publicar `UiState.Error` em vez de deixar a exceção escapar.

```kotlin
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
```

Agora os três estados de `UiState` (`Loading`, `Success`, `Error`) são realmente alcançáveis, e uma falha de rede vira uma tela de erro amigável em vez de travar o app.

### Erros comuns / Pegadinhas

- **Expor `MutableStateFlow` diretamente para a View**: isso permitiria que a View alterasse o estado por conta própria, quebrando o fluxo unidirecional. Sempre exponha a versão somente-leitura (`StateFlow`).
- **Chamar `loadTasks()` sem tratar exceções**: sem o `try/catch`, uma falha de rede derrubaria o app (crash) em vez de mostrar a tela de erro amigável.
- **Guardar referências de `Context`/`View` dentro do ViewModel**: isso causa vazamento de memória, porque o ViewModel vive mais tempo que a tela. O ViewModel deve depender só de dados, nunca de componentes de UI do Android.

---

## View (Compose)

A View no MVVM é responsável apenas por **observar** o estado e **renderizar** a UI. Ela não contém lógica de negócio. O `collectAsState()` converte o `StateFlow` do ViewModel em um estado observável pelo Compose, disparando **recomposição** — o processo pelo qual o Compose redesenha (executa novamente) uma função `@Composable` quando algum dado que ela lê muda — sempre que o valor mudar.

#### Passo 1 — conectar o estado à UI com `when`

Esta função concentra a parte que é específica de MVVM: coletar o `StateFlow` e decidir o que desenhar para cada estado possível. O resto (as telas de cada estado) é Compose "comum", sem nada de específico do padrão.

```kotlin
// TasksScreen.kt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun TasksScreen(viewModel: TasksViewModel = viewModel()) {
    // Coleta o StateFlow como estado do Compose.
    // Sempre que uiState mudar no ViewModel, esta função é recomposta.
    val uiState by viewModel.uiState.collectAsState()

    // "when" com sealed interface: o compilador Kotlin exige que todos os
    // casos (Loading, Success, Error) sejam tratados — evita esquecer algum.
    when (uiState) {
        is UiState.Loading -> LoadingScreen()
        is UiState.Success -> TasksList((uiState as UiState.Success).tasks)
        is UiState.Error -> ErrorScreen(
            message = (uiState as UiState.Error).message,
            onRetry = { viewModel.loadTasks() } // usuário pode tentar de novo
        )
    }
}
```

#### Passo 2 — as telas para cada estado

Agora só falta desenhar cada uma das três telas que `TasksScreen` já sabe escolher.

```kotlin
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Exibe um indicador de carregamento centralizado na tela
@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator() // spinner padrão do Material 3
    }
}

// Exibe a lista de tarefas usando LazyColumn (equivalente ao RecyclerView, mas
// sem precisar de Adapter ou ViewHolder — o Compose cuida disso por você)
@Composable
fun TasksList(tasks: List<Task>, onToggle: (Task) -> Unit = {}) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),          // espaçamento nas bordas
        verticalArrangement = Arrangement.spacedBy(8.dp) // espaço entre itens
    ) {
        // key = { it.id } dá uma identidade estável a cada item — importante
        // para o Compose não confundir itens quando a lista muda (mais sobre
        // isso na aula "Listas com Jetpack Compose")
        items(tasks, key = { it.id }) { task ->
            TaskItem(task, onToggle)
        }
    }
}

// Composable para renderizar um único item da lista.
// Recebe o callback `onToggle` do pai, mantendo este composable "stateless"
// (sem estado próprio) — ele só mostra dados e repassa eventos para cima.
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

### Erros comuns / Pegadinhas

- **Fazer cast repetido (`uiState as UiState.Success`) em vez de aproveitar o smart cast do Kotlin**: no exemplo acima usamos `when (uiState)` com casts manuais por clareza didática, mas em Kotlin idiomático você pode capturar a variável dentro do `is` (`is UiState.Success -> TasksList(uiState.tasks)`) e o compilador já entende o tipo automaticamente, sem precisar de `as`.
- **Esquecer `key` no `items()`**: sem chave estável, o Compose pode perder a posição de scroll ou misturar o estado interno dos itens ao reordenar a lista.
- **Colocar lógica de negócio dentro do Composable** (ex: decidir se um e-mail é válido, calcular totais): isso deveria estar no ViewModel, não na View.

---

## Resumo

- **MVVM** separa a tela em três papéis: Model (dados), ViewModel (lógica + estado) e View (desenho da UI).
- O fluxo é **unidirecional**: a View envia eventos, o ViewModel processa e atualiza o estado, a View observa e se redesenha.
- **`UiState`** como `sealed interface` evita estados impossíveis e obriga a tratar todos os casos (`Loading`, `Success`, `Error`).
- **`StateFlow`** é a "caixa observável" que guarda o estado atual e notifica a View quando ele muda.
- **`collectAsState()`** conecta o `StateFlow` do ViewModel ao mundo do Compose, disparando recomposição automaticamente.
- O `Repository` isola o acesso a dados (rede, banco) do resto do app, facilitando testes e manutenção.

**Próximo passo**: na próxima aula (`02_eventos_oneshot.md`) você vai aprender a lidar com eventos que devem acontecer **uma única vez** — como mostrar um Toast ou navegar de tela — usando `SharedFlow`, resolvendo um problema clássico que o `StateFlow` sozinho não resolve bem.

---

## Exercícios Práticos

Resolva na ordem — cada exercício constrói em cima do anterior.

1. **Estado da UI — adicionar estado vazio**
   - Checkpoint 1: adicione `object Empty : UiState` à sealed interface.
   - Checkpoint 2: no `TasksViewModel.loadTasks()`, se `tasks.isEmpty()`, emita `UiState.Empty` em vez de `Success`.
   - Checkpoint 3: no `when` da `TasksScreen`, trate `is UiState.Empty` mostrando um texto como "Nenhuma tarefa ainda".
   - Dica: sem esse tratamento, uma lista vazia renderiza uma `LazyColumn` vazia e o usuário não sabe se é um bug ou se realmente não há tarefas.

2. **Repository — simular cenários diferentes**
   - Modifique `TasksRepository` para receber um parâmetro (ex: `delayMs: Long`) e simular tempos de resposta variados.
   - Desafio extra: crie uma exceção customizada (`class ServidorIndisponivelException : Exception()`) em vez de usar `Exception` genérica — isso deixa o tratamento de erros mais preciso.

3. **ViewModel — marcar tarefa como concluída**
   - Checkpoint 1: crie `fun toggleTask(taskId: Long)` no `TasksViewModel`.
   - Checkpoint 2: dentro dela, pegue o estado atual (`_uiState.value`), verifique se é `UiState.Success`, e crie uma nova lista com o item alterado (lembre-se: `Task` é `data class`, use `.copy(done = !task.done)`).
   - Dica: nunca altere uma lista existente por dentro (ex: com `.toMutableList()` seguido de mudanças in-place mantendo a mesma referência); prefira criar uma nova lista, para o Compose detectar a mudança corretamente.

4. **View — conectar o `onToggle` de verdade**
   - Ligue o `onToggle` de `TasksList`/`TaskItem` ao `viewModel::toggleTask` criado no exercício 3 (hoje ele está com valor padrão vazio `{}`).

5. **Desafio**: já existe um botão "Tentar novamente" na tela de erro — teste-o forçando o `Repository` a sempre lançar erro e confirme que clicar no botão chama `loadTasks()` novamente e eventualmente mostra a lista (já que o erro é aleatório, tente algumas vezes).

---
