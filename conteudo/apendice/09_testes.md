# Apêndice: Testes Essenciais no Android

**Objetivo**: aprender a escrever testes automatizados para as camadas principais do app (ViewModel, UseCases, Repository), além de um exemplo de teste de interface com Compose. Ao final deste módulo, você será capaz de garantir — com código, não com "achismo" — que seu app funciona como deveria.

Se você nunca escreveu um teste automatizado na vida, está no lugar certo. Vamos com calma, explicando cada termo novo antes de usá-lo.

---

## Por que testar?

### O que é um teste automatizado?

Um **teste automatizado** é um pequeno programa que executa outro pedaço do seu código e verifica se o resultado é o esperado — tudo isso sem você precisar abrir o app manualmente, clicar em botões e olhar a tela. É como ter um assistente que aperta os botões do seu app centenas de vezes por segundo e avisa imediatamente se algo deu errado.

### Por que isso importa?

Sem testes automatizados, a única forma de saber se o app funciona é testando manualmente — abrindo o app, navegando pelas telas, digitando dados de exemplo. Isso funciona no começo, mas o problema aparece conforme o projeto cresce:

- **Confiança**: com testes, você sabe que uma mudança nova não quebrou uma funcionalidade antiga (esse tipo de quebra silenciosa é chamado de **regressão**). Sem testes, você só descobre a regressão quando um usuário reclama — ou pior, quando o app já está publicado com o bug.
- **Documentação viva**: um teste bem escrito mostra, em código, como uma classe deve se comportar em cada situação. É uma documentação que nunca fica desatualizada, porque se o comportamento mudar e o teste não for ajustado, o teste falha.
- **Refatoração segura**: **refatorar** é reorganizar/reescrever a implementação interna de um código sem mudar o que ele faz por fora. Com testes, você pode refatorar com confiança: se os testes continuarem passando, o comportamento não mudou.

Imagine testar manualmente a tela de login toda vez que você altera uma linha de código em qualquer parte do app, só para garantir que não quebrou nada. Isso é lento, cansativo e você provavelmente vai esquecer de testar algum cenário (como "o que acontece se a senha estiver errada?"). Um teste automatizado faz essa checagem em segundos, sempre da mesma forma, sem cansaço e sem esquecimento.

### Erros comuns / Pegadinhas

- **"Vou testar depois, quando o app estiver pronto"**: na prática, "depois" quase nunca chega, e testar um app grande do zero é muito mais difícil do que testar aos poucos, enquanto você escreve o código.
- **Achar que testar manualmente é suficiente**: funciona para projetos pequenos e únicos, mas não escala — cada nova funcionalidade aumenta o número de cenários que você precisaria reconferir manualmente a cada mudança.
- **Testar tudo de uma vez**: comece pequeno. Um teste simples no ViewModel já traz muito valor. Não é preciso cobrir 100% do código para começar a colher benefícios.

---

## 1. Tipos de Testes

### O que é

No Android, existem dois tipos principais de teste, que diferem em **onde** rodam e **o que** conseguem verificar:

| Tipo | Onde roda | O que valida | Velocidade |
|------|-----------|--------------|------------|
| **Unitário** | JVM local (pasta `test/`) | Lógica pura (ViewModel, UseCase, Repository) — código Kotlin/Java que não depende do sistema Android | Muito rápido |
| **Instrumentado** | Emulador ou dispositivo físico (pasta `androidTest/`) | Integração com o framework Android de verdade — telas, componentes de UI, permissões | Mais lento |

**Teste unitário** roda direto na sua máquina, na JVM (a máquina virtual Java), sem precisar de emulador. Por isso é extremamente rápido — leva milissegundos. Ele testa "unidades" isoladas de lógica, como uma função ou uma classe, sem envolver a interface gráfica.

**Teste instrumentado** precisa de um emulador ou celular real rodando o Android de verdade, porque ele testa coisas que só existem no sistema operacional Android (como desenhar uma tela na tela ou simular um toque). Por isso é mais lento — pode levar segundos ou minutos para cada teste.

### Por que isso importa

Se você tentasse testar tudo com testes instrumentados, o pipeline de testes ficaria lento demais para rodar a cada mudança de código. Por isso a recomendação prática é:

Para a maioria dos projetos, comece com testes unitários — eles são rápidos, baratos de escrever e cobrem a lógica mais importante (a que decide "o que" o app faz). Adicione testes de UI (instrumentados) apenas para fluxos críticos, como o login ou o checkout de uma compra, onde vale o custo extra de tempo.

### Erros comuns / Pegadinhas

- Tentar testar tudo com testes instrumentados: além de lento, é mais difícil de manter porque depende de um emulador configurado corretamente.
- Colocar um teste de lógica pura (por exemplo, uma função de cálculo) na pasta `androidTest/`: isso o torna desnecessariamente lento. Se o código não depende de nada do Android, ele pertence a `test/`.

---

## 2. Dependências de Teste

### O que é

Antes de escrever qualquer teste, é preciso adicionar algumas bibliotecas ("dependências") ao projeto. Cada uma resolve um problema específico de testar código Android:

- **JUnit**: o framework mais usado para organizar e executar testes em Kotlin/Java. É ele que fornece anotações como `@Test` (marca uma função como um teste) e funções de verificação como `assertEquals` (compara um valor esperado com o valor obtido).
- **kotlinx-coroutines-test**: ferramentas para testar código que usa **coroutines** (a forma do Kotlin de rodar tarefas assíncronas, como buscar dados na internet, sem travar a tela). Sem essa biblioteca, testar coroutines seria muito mais trabalhoso.
- **MockK**: biblioteca para criar **mocks** — objetos "de mentira" que imitam o comportamento de uma dependência real (como um `Repository` que buscaria dados de um servidor). Em vez de fazer uma chamada de rede de verdade durante o teste (lenta e imprevisível), você cria um mock que devolve exatamente o dado que você quer testar.
- **Turbine**: biblioteca para testar `Flow` e `StateFlow` — os "fluxos" de dados reativos usados no Android para representar estados que mudam com o tempo (como o estado de carregamento de uma tela). Turbine facilita "escutar" essas emissões dentro de um teste.
- **androidx.compose.ui:ui-test**: ferramentas para escrever testes instrumentados de telas feitas com Jetpack Compose — simulam toques, verificam textos na tela, etc.

Adicione ao `app/build.gradle.kts`:

```kotlin
dependencies {
    // JUnit: framework base para escrever e rodar testes (fornece @Test, assertEquals, etc.)
    testImplementation("junit:junit:4.13.2")

    // Ferramentas para testar código que usa coroutines (ex.: viewModelScope.launch)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")

    // MockK: cria objetos "de mentira" (mocks) para simular dependências como Repository
    testImplementation("io.mockk:mockk:1.13.13")

    // Turbine: facilita testar emissões de StateFlow / Flow
    testImplementation("app.cash.turbine:turbine:1.2.0")

    // Testes de UI com Compose (rodam em emulador/dispositivo, pasta androidTest/)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
```

Repare que `testImplementation` é usado para dependências dos testes unitários (pasta `test/`), enquanto `androidTestImplementation` é usado para dependências dos testes instrumentados (pasta `androidTest/`). São "gavetas" diferentes — o Gradle só inclui cada biblioteca no tipo de teste correspondente.

### Erros comuns / Pegadinhas

- Adicionar uma dependência com `implementation` em vez de `testImplementation`: isso faz a biblioteca de teste ir parar dentro do app publicado, aumentando o tamanho do APK/AAB sem necessidade.
- Esquecer de sincronizar o Gradle depois de editar o `build.gradle.kts` (no Android Studio, clique em "Sync Now" quando o aviso aparecer). Sem isso, as novas dependências não ficam disponíveis para uso.

---

## 3. Testando o ViewModel

### O que é

O **ViewModel** é a classe responsável por manter o estado da tela e a lógica de apresentação (por exemplo, "carregar a lista de tarefas" e decidir se a tela deve mostrar "carregando", "sucesso" ou "erro"). Como ele normalmente não depende diretamente de componentes visuais do Android, é a camada mais fácil e mais valiosa de testar com testes unitários.

### Por que isso importa

O ViewModel concentra as decisões mais importantes da tela: o que mostrar, quando mostrar, e como reagir a erros. Um bug aqui afeta diretamente o que o usuário vê. Testar o ViewModel é onde você tem o melhor "custo-benefício": poucos testes cobrem os cenários mais críticos do app.

### Exemplo: ViewModel de lista de tarefas

```kotlin
// TasksViewModel.kt (produção)
class TasksViewModel(private val repository: TasksRepository) : ViewModel() {
    // MutableStateFlow guarda o estado atual da tela e pode ser alterado internamente
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    // Versão somente-leitura exposta para a tela observar (a tela não pode alterar o estado diretamente)
    val uiState: StateFlow<UiState> = _uiState

    fun loadTasks() {
        // viewModelScope.launch inicia uma coroutine ligada ao ciclo de vida do ViewModel
        viewModelScope.launch {
            try {
                val tasks = repository.fetchTasks() // chamada suspend: pode demorar (ex.: rede)
                _uiState.value = UiState.Success(tasks)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Erro desconhecido")
            }
        }
    }
}
```

### Exemplo comentado: teste do ViewModel

Antes de ler o código, vale entender três termos que aparecem o tempo todo em testes:

- **Arrange / Act / Assert (organizar / agir / verificar)**: um padrão comum para estruturar um teste em três partes — primeiro você prepara os dados e mocks necessários (*Arrange*), depois executa a ação que quer testar (*Act*), e por fim verifica se o resultado é o esperado (*Assert*).
- **`assert`**: uma função de verificação. Se a condição passada for falsa, o teste falha imediatamente e mostra o erro. `assertEquals(esperado, obtido)` compara dois valores; `assertTrue(condição)` verifica se algo é verdadeiro.
- **mock**: um objeto "de mentira" que substitui uma dependência real durante o teste. `coEvery { repository.fetchTasks() } returns tarefas` diz ao mock: "quando alguém chamar essa função suspend, devolva esta lista, sem de fato buscar nada de verdade".

```kotlin
// TasksViewModelTest.kt (em src/test/)
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TasksViewModelTest {

    // StandardTestDispatcher: um "dispatcher" de teste que controla o tempo virtual das coroutines,
    // permitindo avançar o relógio manualmente em vez de esperar tempo real passar
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: TasksRepository
    private lateinit var viewModel: TasksViewModel

    @Before // roda antes de cada teste, para preparar o ambiente
    fun setup() {
        // Substitui o Dispatchers.Main (que não existe na JVM local) pelo dispatcher de teste
        Dispatchers.setMain(testDispatcher)
        repository = mockk() // cria um mock vazio de TasksRepository
    }

    @After // roda depois de cada teste, para "limpar a bagunça"
    fun tearDown() {
        Dispatchers.resetMain() // devolve o Dispatchers.Main ao estado original
    }

    @Test
    fun `loadTasks emite Success quando repositorio retorna dados`() = runTest {
        // Arrange: configurar o mock para retornar tarefas
        val tarefas = listOf(
            Task(1, "Tarefa 1", false),
            Task(2, "Tarefa 2", true)
        )
        coEvery { repository.fetchTasks() } returns tarefas

        viewModel = TasksViewModel(repository)

        // Act & Assert: observar o StateFlow com Turbine
        viewModel.uiState.test {
            // Estado inicial (antes de chamar loadTasks, o ViewModel já começa em Loading)
            assertEquals(UiState.Loading, awaitItem()) // awaitItem() espera a próxima emissão do Flow

            // Disparar a ação que queremos testar
            viewModel.loadTasks()
            // Avança o tempo virtual até que todas as coroutines pendentes terminem
            testDispatcher.scheduler.advanceUntilIdle()

            // Estado final esperado
            val result = awaitItem()
            assertTrue(result is UiState.Success)
            assertEquals(2, (result as UiState.Success).tasks.size)

            cancelAndIgnoreRemainingEvents() // encerra a observação, ignorando emissões restantes
        }
    }

    @Test
    fun `loadTasks emite Error quando repositorio lanca excecao`() = runTest {
        // Arrange: desta vez, o mock lança uma exceção em vez de retornar dados
        coEvery { repository.fetchTasks() } throws Exception("Servidor indisponível")

        viewModel = TasksViewModel(repository)

        // Act & Assert
        viewModel.uiState.test {
            assertEquals(UiState.Loading, awaitItem())

            viewModel.loadTasks()
            testDispatcher.scheduler.advanceUntilIdle()

            val result = awaitItem()
            assertTrue(result is UiState.Error)
            assertEquals("Servidor indisponível", (result as UiState.Error).message)

            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

### Conceitos-chave do teste

| Conceito | Para que serve |
|----------|----------------|
| `Dispatchers.setMain(testDispatcher)` | Substitui o `Dispatchers.Main` (inexistente na JVM local) por um dispatcher de teste |
| `coEvery { ... } returns ...` | Configura o mock para retornar um valor quando uma função `suspend` for chamada |
| `runTest { }` | Executa o bloco em um ambiente de coroutines de teste, com tempo virtual controlável |
| `turbine.test { }` | Observa as emissões do `StateFlow`/`Flow` de forma estruturada, uma a uma |
| `advanceUntilIdle()` | Avança o tempo virtual até que todas as coroutines pendentes terminem de executar |

### Erros comuns / Pegadinhas

- **Esquecer `advanceUntilIdle()`**: sem avançar o tempo virtual, a coroutine disparada por `loadTasks()` pode não ter terminado ainda quando você chamar `awaitItem()`, e o teste trava esperando uma emissão que nunca chega (ou chega fora de ordem).
- **Não resetar o `Dispatchers.Main` no `@After`**: se um teste "vaza" a configuração para o próximo, pode causar falhas difíceis de entender em testes que rodam depois.
- **Confundir mock com fake**: um *mock* (como os do MockK) é configurado dinamicamente dentro do teste (`coEvery { ... } returns ...`); um *fake* é uma implementação alternativa e simplificada da interface, escrita à mão (você vai ver um exemplo na próxima seção). Os dois servem ao mesmo propósito — isolar o teste de dependências reais —, mas de formas diferentes.

---

## 4. Testando o Repository

### O que é

O **Repository** é a camada que decide de onde vêm os dados (rede, banco local, cache) e os transforma no formato que o resto do app usa. Testar o Repository garante que essa transformação — por exemplo, de um DTO vindo da API para um modelo de domínio do app — está correta.

### Por que isso importa

Erros de mapeamento (um campo trocado, um valor não convertido corretamente) são difíceis de perceber visualmente, mas causam bugs sutis mais na frente. Um teste de Repository pega esse tipo de erro imediatamente, sem precisar de uma chamada de rede real.

### Exemplo comentado

Aqui usamos um **fake**: em vez de usar MockK, criamos manualmente uma implementação de `PostService` que devolve dados fixos, sem se conectar à internet de verdade.

```kotlin
// TasksRepositoryTest.kt
@OptIn(ExperimentalCoroutinesApi::class)
class TasksRepositoryTest {

    @Test
    fun `getPosts retorna lista mapeada corretamente`() = runTest {
        // Arrange: API "fake" que devolve DTOs fixos, sem acessar a rede de verdade
        val fakeApi = object : PostService {
            override suspend fun getPosts() = listOf(
                PostDto(userId = 1, id = 1, title = "Título", body = "Corpo")
            )
            override suspend fun getPost(id: Int) = PostDto(1, id, "T", "B")
            override suspend fun getPostsByUser(userId: Int) = emptyList<PostDto>()
        }

        val repository = PostRepositoryImpl(fakeApi)

        // Act: chamar o método que queremos testar
        val result = repository.getPosts()

        // Assert: conferir que o DTO foi mapeado corretamente para o modelo de domínio
        assertEquals(1, result.size)
        assertEquals("Título", result[0].title)
        assertEquals(1, result[0].authorId)
    }
}
```

### Erros comuns / Pegadinhas

- Testar apenas o "caminho feliz" (quando tudo dá certo) e esquecer de testar o que acontece quando a API retorna uma lista vazia ou lança uma exceção.
- Criar um fake tão complexo que ele mesmo precisa ser testado — mantenha fakes simples, devolvendo apenas o que o teste precisa.

---

## 5. Teste de UI com Compose

### O que é

Testes de UI (interface) verificam se a tela realmente mostra o que deveria, na prática — não apenas se a lógica por trás está correta, mas se o usuário veria o texto certo, o botão certo, na posição certa. Como envolvem desenhar uma tela de verdade, esses são **testes instrumentados**: rodam em um emulador ou dispositivo físico, na pasta `androidTest/`.

### Por que isso importa

É possível que a lógica do ViewModel esteja perfeita (testada e aprovada), mas a tela ainda assim não exiba o dado corretamente por causa de um erro na composição da UI (por exemplo, o texto errado sendo passado para um componente). Testes de UI pegam esse tipo de problema, que os testes unitários não alcançam.

### Exemplo comentado

```kotlin
// TasksScreenTest.kt (em src/androidTest/)
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class TasksScreenTest {

    // @get:Rule prepara um ambiente Compose isolado para cada teste (como uma "tela em branco" de testes)
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun exibeListaDeTarefas() {
        val tarefas = listOf(
            Task(1, "Ler documentação", true),
            Task(2, "Implementar ViewModel", false)
        )

        // setContent renderiza o Composable como se estivesse em uma tela real
        composeRule.setContent {
            TasksList(tasks = tarefas)
        }

        // onNodeWithText procura um elemento na tela pelo texto exibido
        composeRule.onNodeWithText("Ler documentação").assertIsDisplayed()
        composeRule.onNodeWithText("Implementar ViewModel").assertIsDisplayed()
    }

    @Test
    fun exibeTelaDeErroComBotaoRetry() {
        composeRule.setContent {
            ErrorScreen(message = "Sem conexão", onRetry = {})
        }

        composeRule.onNodeWithText("Sem conexão").assertIsDisplayed()
        composeRule.onNodeWithText("Tentar novamente").assertIsDisplayed()
    }
}
```

### Erros comuns / Pegadinhas

- Escrever testes de UI para todas as telas do app: eles são lentos e mais frágeis (quebram com mudanças visuais pequenas). Reserve-os para os fluxos mais críticos.
- Buscar elementos pelo texto exibido quando o texto pode mudar com traduções (internacionalização). Em projetos maiores, prefira identificar elementos por uma "tag de teste" fixa, em vez do texto visível.

---

## 6. Boas Práticas

1. **Teste o quê, não como**: verifique o estado final (o resultado observável), não os detalhes de implementação interna — assim seus testes não quebram só porque você refatorou a forma de calcular algo.
2. **Um assert por cenário**: cada teste deve validar um comportamento específico. Se um teste testa "sucesso e erro ao mesmo tempo", divida em dois testes.
3. **Nomes descritivos**: use nomes que descrevam o cenário e o resultado esperado (ex.: `` `loadTasks emite Error quando repositorio lanca excecao` ``) — assim, quando um teste falhar, o nome já ajuda a entender o problema.
4. **Isole dependências**: use mocks ou fakes para não depender de rede ou banco de dados real — isso torna os testes rápidos e confiáveis, independente da internet estar disponível.
5. **Priorize o ViewModel**: é onde está a lógica de apresentação e tem o melhor custo-benefício para começar.

---

## Resumo

- **Testes automatizados** substituem a checagem manual repetitiva, dando confiança para mudar o código sem medo de quebrar algo (regressão).
- Existem **testes unitários** (rápidos, rodam na JVM, testam lógica pura) e **testes instrumentados** (mais lentos, rodam em emulador/dispositivo, testam integração com o Android e a UI).
- **MockK** cria mocks para simular dependências; **Turbine** ajuda a testar `StateFlow`/`Flow`; **JUnit** organiza e executa os testes.
- O **ViewModel** é o melhor ponto de partida para testar, porque concentra a lógica de apresentação com pouco acoplamento ao Android.
- O padrão **Arrange / Act / Assert** ajuda a estruturar qualquer teste de forma clara: preparar, agir, verificar.

**Próximo passo**: com o app testado, o próximo módulo (["Publicação no Google Play"](./11_publicacao_detalhada.md)) ensina como preparar, assinar e enviar seu app para os usuários reais.

---

## 7. Exercícios Práticos

Faça os exercícios na ordem — cada um usa o que você aprendeu no anterior.

1. **Teste de ViewModel**: escreva testes para o `PostViewModel` do Módulo 3 (Retrofit).
   - Checkpoint 1: crie o mock do `PostRepository` com `mockk()`.
   - Checkpoint 2: verifique que o estado inicial é `Loading`.
   - Checkpoint 3: dispare o carregamento e verifique que o estado final é `Success` com a lista correta.
   - Dica: siga o mesmo padrão do `TasksViewModelTest` mostrado acima — você pode praticamente copiar a estrutura e adaptar os nomes.

2. **Teste de Repository**: crie um fake do `PostService` e teste que o `PostRepositoryImpl` mapeia corretamente os DTOs para modelos de domínio.
   - Checkpoint: confira pelo menos um campo de cada DTO mapeado (ex.: `title`, `authorId`).

3. **Teste de UI**: escreva um teste instrumentado que verifique que a tela de lista de posts exibe o título do primeiro post.
   - Dica: use `composeRule.onNodeWithText(...)` como no exemplo de `TasksScreenTest`.

4. **Desafio**: teste o cenário de erro — simule uma falha de rede no mock (`coEvery { ... } throws Exception(...)`) e verifique que o ViewModel emite o estado `Error` com a mensagem correta.
   - Dica extra: pense em outro cenário de erro comum, como uma lista vazia retornada pela API. O ViewModel deveria tratar isso como erro ou como um estado "vazio" separado? Escreva um teste para o comportamento que você decidir.

---

## Checklist de Testes Mínimos

Antes de considerar o projeto pronto para release:

- [ ] ViewModel principal tem pelo menos 2 testes (sucesso e erro)
- [ ] Repository tem teste com fake de API
- [ ] 1–2 testes de UI para fluxos críticos (lista principal, tela de erro)
- [ ] Testes passam no CI (GitHub Actions ou similar — veja o [Apêndice — CI/CD](./10_ci_cd.md))

---
