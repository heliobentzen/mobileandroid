# Prática: Coroutines para Iniciantes

Este guia apresenta exercícios práticos para entender e usar **Coroutines** no desenvolvimento Android. As coroutines permitem executar tarefas demoradas (como chamadas de rede) sem travar a tela do aplicativo.

---

## O que são Coroutines?

Pense em coroutines como **tarefas que podem ser pausadas e retomadas** sem bloquear o restante do programa. Quando seu app precisa baixar dados da internet (o que leva alguns segundos), a coroutine "pausa" esperando os dados chegarem, enquanto a tela continua respondendo ao usuário.

---

## Pré-requisitos

Adicione as dependências ao `app/build.gradle.kts`:

```kotlin
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
}
```

---

## Prática 1: Sua Primeira Coroutine

### Objetivo
Entender como uma coroutine funciona e a diferença entre código bloqueante e não bloqueante.

### Passo a Passo

Crie um arquivo Kotlin puro (`Coroutines101.kt`) para testar fora do Android:

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking {
    println("Início - Thread: ${Thread.currentThread().name}")

    // 'launch' inicia uma coroutine e NÃO bloqueia a thread
    val tarefa = launch {
        println("Coroutine iniciada - Thread: ${Thread.currentThread().name}")
        delay(2000) // Simula uma operação demorada (não bloqueia a thread!)
        println("Coroutine finalizada após 2 segundos")
    }

    println("Continuando enquanto a coroutine está em execução...")
    tarefa.join() // Aguarda a coroutine terminar
    println("Fim")
}
```

Saída esperada:
```
Início - Thread: main
Coroutine iniciada - Thread: main
Continuando enquanto a coroutine está em execução...
Coroutine finalizada após 2 segundos
Fim
```

> **Observe**: `"Continuando..."` aparece antes de `"Coroutine finalizada"` porque `launch` não bloqueia.

### Exercícios

1. Adicione uma segunda coroutine que aguarda 1 segundo e imprime uma mensagem diferente. Observe a ordem das mensagens.
2. Troque `launch` por `async` e use `.await()` para obter o valor de retorno da coroutine. Dica: `async { 42 }.await()` retorna `42`.
3. Remova o `tarefa.join()` e execute novamente. O que muda na saída? Por quê?

---

## Prática 2: Dispatchers — Escolhendo onde a Tarefa Roda

### Objetivo
Entender os principais dispatchers e quando usar cada um.

### Passo a Passo

```kotlin
import kotlinx.coroutines.*

suspend fun operacaoDeRede(): String {
    // Dispatchers.IO: ideal para chamadas de rede e banco de dados
    return withContext(Dispatchers.IO) {
        delay(1000) // Simula chamada de rede
        "Dados recebidos da rede!"
    }
}

suspend fun processarDados(dados: String): String {
    // Dispatchers.Default: ideal para processamento intensivo de CPU
    return withContext(Dispatchers.Default) {
        delay(500) // Simula processamento
        dados.uppercase()
    }
}

fun main() = runBlocking {
    println("Buscando dados...")
    val dados = operacaoDeRede()
    println("Dados: $dados")

    println("Processando...")
    val resultado = processarDados(dados)
    println("Resultado: $resultado")
}
```

### Exercícios

1. Crie uma função `salvarNoArquivo(conteudo: String)` que use `Dispatchers.IO` e simule uma escrita em arquivo com `delay(800)`.
2. Crie uma função `calcularPrimos(limite: Int): List<Int>` que use `Dispatchers.Default` para encontrar todos os números primos até o limite informado.
3. Modifique o exemplo para executar `operacaoDeRede()` e `processarDados()` em **paralelo** usando `async/await`. Meça a diferença de tempo total.

---

## Prática 3: Coroutines no ViewModel (uso real no Android)

### Objetivo
Usar coroutines no ViewModel para carregar dados sem bloquear a UI.

### Passo a Passo

**1. Simule um repositório** (`ClimaRepository.kt`):

```kotlin
import kotlinx.coroutines.delay

data class Clima(
    val cidade: String,
    val temperatura: Int,
    val condicao: String,
    val umidade: Int
)

class ClimaRepository {
    // Simula uma chamada de rede lenta
    suspend fun buscarClima(cidade: String): Clima {
        delay(2000) // 2 segundos de "latência"
        // Simulando dados diferentes por cidade
        return when (cidade.lowercase()) {
            "são paulo" -> Clima("São Paulo", 25, "Parcialmente nublado", 78)
            "rio de janeiro" -> Clima("Rio de Janeiro", 32, "Ensolarado", 65)
            "curitiba" -> Clima("Curitiba", 18, "Chuvoso", 90)
            else -> Clima(cidade, 22, "Tempo agradável", 70)
        }
    }
}
```

**2. ViewModel com coroutines** (`ClimaViewModel.kt`):

```kotlin
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ClimaUiState {
    data object Idle : ClimaUiState           // Tela inicial, sem busca
    data object Carregando : ClimaUiState     // Buscando dados
    data class Sucesso(val clima: Clima) : ClimaUiState
    data class Erro(val mensagem: String) : ClimaUiState
}

class ClimaViewModel(
    private val repository: ClimaRepository = ClimaRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<ClimaUiState>(ClimaUiState.Idle)
    val uiState: StateFlow<ClimaUiState> = _uiState.asStateFlow()

    fun buscarClima(cidade: String) {
        if (cidade.isBlank()) return

        // viewModelScope: a coroutine é cancelada automaticamente quando o ViewModel é destruído
        viewModelScope.launch {
            _uiState.value = ClimaUiState.Carregando

            try {
                val clima = repository.buscarClima(cidade)
                _uiState.value = ClimaUiState.Sucesso(clima)
            } catch (e: Exception) {
                _uiState.value = ClimaUiState.Erro("Não foi possível buscar o clima: ${e.message}")
            }
        }
    }
}
```

**3. Tela Compose** (`ClimaScreen.kt`):

```kotlin
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ClimaScreen(viewModel: ClimaViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var cidade by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("🌤 Previsão do Tempo", style = MaterialTheme.typography.headlineMedium)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = cidade,
                onValueChange = { cidade = it },
                label = { Text("Cidade") },
                placeholder = { Text("Ex: São Paulo") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(onClick = { viewModel.buscarClima(cidade) }) {
                Text("Buscar")
            }
        }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when (val state = uiState) {
                is ClimaUiState.Idle -> Text("Digite uma cidade para ver o clima.", color = MaterialTheme.colorScheme.outline)
                is ClimaUiState.Carregando -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(8.dp))
                        Text("Buscando clima para \"$cidade\"...")
                    }
                }
                is ClimaUiState.Sucesso -> {
                    val clima = state.clima
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(clima.cidade, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("${clima.temperatura}°C", fontSize = 64.sp, fontWeight = FontWeight.Light)
                            Text(clima.condicao, color = MaterialTheme.colorScheme.primary)
                            Text("Umidade: ${clima.umidade}%", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                is ClimaUiState.Erro -> {
                    Text(state.mensagem, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
```

### Exercícios

1. Adicione um histórico de últimas cidades buscadas. Exiba-as como chips clicáveis abaixo do campo de busca.
2. Implemente busca automática: após o usuário parar de digitar por 1 segundo, inicie a busca automaticamente. Dica: pesquise sobre `debounce` com coroutines e `Flow`.
3. Adicione a funcionalidade de buscar o clima de múltiplas cidades ao mesmo tempo usando `async/await` e exibir todas em uma lista.

---

## Prática 4: Cancelamento de Coroutines

### Objetivo
Entender como e quando cancelar coroutines para evitar desperdício de recursos.

### Passo a Passo

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking {
    println("Iniciando download...")

    val downloadJob = launch {
        repeat(10) { i ->
            // isActive verifica se a coroutine ainda está ativa
            if (!isActive) {
                println("Download cancelado pelo usuário.")
                return@launch
            }
            delay(500)
            println("Baixando parte ${i + 1}/10...")
        }
        println("Download completo!")
    }

    delay(1800) // Deixa executar por 1,8 segundos
    println("Usuário cancelou o download!")
    downloadJob.cancel() // Cancela a coroutine
    downloadJob.join()   // Aguarda a finalização do cancelamento
    println("Programa encerrado.")
}
```

### Exercícios

1. Modifique o exemplo para simular um upload que pode ser cancelado. Adicione tratamento do `CancellationException` para imprimir uma mensagem quando o upload for cancelado.
2. Crie um ViewModel com uma coroutine de "processamento longo". Adicione um botão na tela para cancelar o processamento. Use `Job` para manter referência da coroutine.
3. Explique com suas palavras: por que é importante que a coroutine de carregamento seja cancelada quando o usuário sai da tela? Como o `viewModelScope` ajuda com isso?

---

## Prática 5: Flow — Dados que Mudam ao Longo do Tempo

### Objetivo
Usar `Flow` para transmitir uma sequência de valores ao longo do tempo.

### Passo a Passo

```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

// Flow emite múltiplos valores ao longo do tempo
fun contadorFlow(ate: Int, intervaloMs: Long = 1000L): Flow<Int> = flow {
    for (i in 1..ate) {
        delay(intervaloMs)
        emit(i) // Emite o próximo valor
    }
}

fun main() = runBlocking {
    println("Iniciando contagem:")

    contadorFlow(ate = 5, intervaloMs = 500L)
        .onEach { valor -> println("  Valor: $valor") }
        .collect() // Coleta (consome) todos os valores emitidos

    println("Contagem finalizada.")
}
```

**No Android, com StateFlow e ViewModel:**

```kotlin
// ViewModel: expõe dados que mudam ao longo do tempo
class TemporizadorViewModel : ViewModel() {
    private val _segundos = MutableStateFlow(0)
    val segundos: StateFlow<Int> = _segundos.asStateFlow()

    private var job: kotlinx.coroutines.Job? = null

    fun iniciar() {
        job = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                _segundos.value++
            }
        }
    }

    fun pausar() { job?.cancel() }

    fun resetar() {
        job?.cancel()
        _segundos.value = 0
    }
}
```

### Exercícios

1. Crie um composable `Temporizador` que usa o `TemporizadorViewModel` acima e exibe os segundos formatados como `"00:00"` (minutos:segundos). Adicione botões para Iniciar, Pausar e Resetar.
2. Crie um `Flow` que emite o preço de uma ação simulada a cada 2 segundos (número aleatório entre R$10 e R$100). Exiba na tela o preço atual, o máximo e o mínimo já observados.
3. Use o operador `map` em um `Flow` para transformar uma lista de números inteiros em uma lista de seus quadrados antes de exibir na tela.

---

## Resumo dos Conceitos

| Conceito | O que faz |
|----------|-----------|
| `launch` | Inicia coroutine sem retorno; não bloqueia |
| `async` | Inicia coroutine com retorno; use `.await()` para obter o resultado |
| `delay` | Pausa a coroutine sem bloquear a thread |
| `withContext` | Muda o dispatcher dentro de uma coroutine |
| `Dispatchers.Main` | Para atualizar a UI |
| `Dispatchers.IO` | Para rede, banco de dados, arquivos |
| `Dispatchers.Default` | Para processamento pesado de CPU |
| `viewModelScope` | Escopo ligado ao ciclo de vida do ViewModel |
| `Flow` | Sequência de valores emitidos ao longo do tempo |
| `StateFlow` | Flow especial para representar estado da UI |

---

## Próximos Passos

- Estude o módulo `01_coroutines.md` para aprofundar cancelamento cooperativo e concorrência estruturada.
- Avance para `08_retrofit_api.md` para usar coroutines em chamadas de rede reais.
- Combine com `07_room_persistencia.md` para usar `Flow` no banco de dados local.
