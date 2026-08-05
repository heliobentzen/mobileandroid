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

Se seu app trava a tela toda vez que busca dados da internet ou lê um arquivo grande, a experiência do usuário fica ruim — ele nem consegue tocar em outro botão enquanto espera. Coroutines resolvem exatamente esse problema: permitem que seu app espere por uma tarefa demorada sem congelar a interface. Esse é o primeiro passo de um dos assuntos mais usados no Android moderno — praticamente toda chamada de rede, leitura de banco de dados ou operação demorada no Android usa coroutines por baixo dos panos.

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

> **💡 Por trás dos panos**
> `delay(2000)` parece uma pausa, mas não é como `Thread.sleep()` — ela não trava a thread onde está rodando. Internamente, a coroutine "libera" a thread para fazer outras coisas e agenda para ser retomada depois de 2 segundos. É esse mecanismo (chamado de suspensão) que permite rodar milhares de coroutines "esperando" ao mesmo tempo, usando pouquíssimas threads reais — coisa que seria inviável com threads tradicionais.

### Exercícios

1. Adicione uma segunda coroutine que aguarda 1 segundo e imprime uma mensagem diferente. Observe a ordem das mensagens.
   - *Dica se travar*: use `launch { ... }` de novo, com `delay(1000)` dentro — as duas coroutines rodam de forma independente.
2. Troque `launch` por `async` e use `.await()` para obter o valor de retorno da coroutine. Dica: `async { 42 }.await()` retorna `42`.
3. Remova o `tarefa.join()` e execute novamente. O que muda na saída? Por quê?
   - *Dica se travar*: sem `.join()`, o programa principal (`runBlocking`) pode terminar antes da coroutine, então a mensagem final dela pode nunca aparecer.

---

## Prática 2: Dispatchers — Escolhendo onde a Tarefa Roda

### Objetivo
Entender os principais dispatchers e quando usar cada um.

Nem toda tarefa deve rodar no mesmo lugar. Atualizar a tela precisa acontecer na thread principal (Main), mas fazer uma chamada de rede na thread principal trava o app inteiro. Escolher o `Dispatcher` certo para cada tipo de tarefa é uma das decisões mais comuns — e mais importantes — ao trabalhar com coroutines no Android. Usar o dispatcher errado é uma causa frequente de apps que travam ("ANR — App Not Responding") ou que desperdiçam bateria e desempenho.

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

> **💡 Por trás dos panos**
> `withContext(Dispatchers.IO)` não cria uma coroutine nova — ele muda temporariamente a thread onde o bloco de código roda, e depois retorna automaticamente para o dispatcher de onde veio. `Dispatchers.IO` mantém um conjunto maior de threads reservadas para operações que ficam "esperando" (rede, disco), enquanto `Dispatchers.Default` usa um número de threads baseado nos núcleos do processador, ideal para cálculos pesados de CPU. Escolher o dispatcher errado não trava o código, mas desperdiça recursos: usar `Dispatchers.Default` para uma chamada de rede, por exemplo, ocupa uma thread pensada para processamento enquanto ela só fica esperando a resposta chegar.

### Exercícios

1. Crie uma função `salvarNoArquivo(conteudo: String)` que use `Dispatchers.IO` e simule uma escrita em arquivo com `delay(800)`.
2. Crie uma função `calcularPrimos(limite: Int): List<Int>` que use `Dispatchers.Default` para encontrar todos os números primos até o limite informado.
   - *Dica se travar*: um número é primo se não tiver divisores além de 1 e ele mesmo — teste com um loop simples de `2 until numero`.
3. Modifique o exemplo para executar `operacaoDeRede()` e `processarDados()` em **paralelo** usando `async/await`. Meça a diferença de tempo total.
   - *Dica se travar*: use `System.currentTimeMillis()` antes e depois do bloco para medir o tempo, e lembre que `processarDados()` precisa do resultado de `operacaoDeRede()` — pense se elas realmente podem rodar em paralelo neste caso específico.

---

## Prática 3: Coroutines no ViewModel (uso real no Android)

### Objetivo
Usar coroutines no ViewModel para carregar dados sem bloquear a UI.

Esta é a forma como coroutines realmente aparecem no dia a dia de um app Android: dentro de um `ViewModel`, buscando dados e atualizando o estado da tela. Diferente dos exemplos anteriores (que rodavam soltos em um `fun main()`), aqui você vai ver o `viewModelScope`, que amarra a coroutine ao ciclo de vida da tela — se o usuário sair da tela no meio da busca, a coroutine é cancelada automaticamente, evitando desperdício de recursos e crashes.

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

> **💡 Por trás dos panos**
> `viewModelScope` é um escopo de coroutine especial fornecido pelo Android Jetpack, atrelado ao ciclo de vida do `ViewModel`. Quando o `ViewModel` é destruído (por exemplo, quando o usuário sai definitivamente da tela), todas as coroutines lançadas com `viewModelScope.launch { ... }` são canceladas automaticamente — você não precisa lembrar de cancelar manualmente. Isso evita um problema clássico: uma busca de rede que termina *depois* que a tela já não existe mais, tentando atualizar um estado que ninguém está mais observando.

### Exercícios

1. Adicione um histórico de últimas cidades buscadas. Exiba-as como chips clicáveis abaixo do campo de busca.
   - *Dica se travar*: adicione um `historico: List<String>` ao lado do `uiState` (ou dentro dele) e atualize-o toda vez que `buscarClima` for chamado com sucesso.
2. Implemente busca automática: após o usuário parar de digitar por 1 segundo, inicie a busca automaticamente. Dica: pesquise sobre `debounce` com coroutines e `Flow`.
3. Adicione a funcionalidade de buscar o clima de múltiplas cidades ao mesmo tempo usando `async/await` e exibir todas em uma lista.
   - *Dica se travar*: dentro de `viewModelScope.launch { }`, crie uma lista de `async { repository.buscarClima(cidade) }` para cada cidade e depois use `.awaitAll()` para esperar todas terminarem juntas.

---

## Prática 4: Cancelamento de Coroutines

### Objetivo
Entender como e quando cancelar coroutines para evitar desperdício de recursos.

Imagine um usuário que inicia o upload de um vídeo e, no meio do processo, decide sair da tela ou cancelar. Se a coroutine continuar rodando escondida, ela consome dados, bateria e processamento à toa — e pode até tentar atualizar uma tela que já não existe mais. Saber cancelar coroutines corretamente é essencial para apps que respeitam os recursos do dispositivo do usuário.

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

> **💡 Por trás dos panos**
> Cancelamento em coroutines é **cooperativo**: chamar `.cancel()` não interrompe a coroutine à força no meio de qualquer linha — ele apenas sinaliza que ela deve parar assim que tiver a chance. É por isso que o exemplo verifica `isActive` dentro do `repeat`: funções suspensas como `delay` já verificam esse sinal automaticamente, mas se você tiver um loop de cálculo puro (sem `delay` ou outra função suspensa), precisa checar `isActive` você mesmo, ou a coroutine nunca vai perceber que foi cancelada.

### Exercícios

1. Modifique o exemplo para simular um upload que pode ser cancelado. Adicione tratamento do `CancellationException` para imprimir uma mensagem quando o upload for cancelado.
   - *Dica se travar*: envolva o corpo da coroutine em `try { ... } catch (e: CancellationException) { println("Upload cancelado") }`.
2. Crie um ViewModel com uma coroutine de "processamento longo". Adicione um botão na tela para cancelar o processamento. Use `Job` para manter referência da coroutine.
3. Explique com suas palavras: por que é importante que a coroutine de carregamento seja cancelada quando o usuário sai da tela? Como o `viewModelScope` ajuda com isso?

---

## Prática 5: Flow — Dados que Mudam ao Longo do Tempo

### Objetivo
Usar `Flow` para transmitir uma sequência de valores ao longo do tempo.

Uma função `suspend` normal retorna um único valor (como o resultado de uma chamada de rede). Mas muitas vezes você precisa de uma sequência de valores ao longo do tempo — um cronômetro que atualiza a cada segundo, uma lista do banco de dados que muda quando um item é inserido, uma cotação de moeda que atualiza periodicamente. `Flow` é a ferramenta do Kotlin para esse tipo de dado "vivo", e é a base de `StateFlow`, que você já usou no guia de MVVM.

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

> **💡 Por trás dos panos**
> Um `Flow` só começa a produzir valores quando alguém chama `.collect()` nele — antes disso, ele fica "adormecido". Cada nova chamada de `.collect()` dispara uma nova execução do bloco `flow { ... }` do zero. Já o `StateFlow` (usado no `TemporizadorViewModel`) é diferente: ele sempre guarda o **último valor emitido** e o entrega imediatamente para qualquer novo observador, mesmo que ele comece a observar depois que o valor já foi emitido — por isso é o tipo ideal para representar o estado atual de uma tela.

### Exercícios

1. Crie um composable `Temporizador` que usa o `TemporizadorViewModel` acima e exibe os segundos formatados como `"00:00"` (minutos:segundos). Adicione botões para Iniciar, Pausar e Resetar.
   - *Dica se travar*: para formatar, calcule `segundos / 60` (minutos) e `segundos % 60` (segundos restantes), e use `String.format("%02d:%02d", min, seg)`.
2. Crie um `Flow` que emite o preço de uma ação simulada a cada 2 segundos (número aleatório entre R$10 e R$100). Exiba na tela o preço atual, o máximo e o mínimo já observados.
   - *Dica se travar*: guarde o máximo e o mínimo como variáveis no ViewModel, atualizando-as toda vez que um novo preço chega via `.collect { ... }`.
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
