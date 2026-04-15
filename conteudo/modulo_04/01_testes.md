# Módulo 4: Testes Essenciais no Android

Objetivo: Garantir que as camadas principais do app (ViewModel, UseCases, Repository) funcionem corretamente por meio de testes automatizados. Neste módulo focamos em testes de unidade e um exemplo de teste de UI com Compose.

---

## Por que testar?

- **Confiança**: saber que mudanças futuras não quebram o que já funciona.
- **Documentação viva**: testes mostram como cada classe deve se comportar.
- **Refatoração segura**: altere a implementação interna sem medo, desde que os testes continuem passando.

---

## 1. Tipos de Testes

| Tipo | Onde roda | O que valida | Velocidade |
|------|-----------|--------------|------------|
| **Unitário** | JVM local (`test/`) | Lógica pura (ViewModel, UseCase, Repository) | Muito rápido |
| **Instrumentado** | Emulador/dispositivo (`androidTest/`) | Integração com framework Android, UI | Mais lento |

Para a maioria dos projetos, comece com testes unitários. Adicione testes de UI apenas para fluxos críticos.

---

## 2. Dependências de Teste

Adicione ao `app/build.gradle.kts`:

```kotlin
dependencies {
    // Testes unitários
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")

    // MockK (mock de dependências)
    testImplementation("io.mockk:mockk:1.13.13")

    // Turbine (testar StateFlow / Flow)
    testImplementation("app.cash.turbine:turbine:1.2.0")

    // Testes de UI com Compose (instrumentados)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
```

---

## 3. Testando o ViewModel

O ViewModel é a camada mais importante para testar, pois contém a lógica de apresentação.

### Exemplo: ViewModel de lista de tarefas

```kotlin
// TasksViewModel.kt (produção)
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

### Teste do ViewModel

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

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: TasksRepository
    private lateinit var viewModel: TasksViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
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
            // Estado inicial
            assertEquals(UiState.Loading, awaitItem())

            // Disparar a ação
            viewModel.loadTasks()
            testDispatcher.scheduler.advanceUntilIdle()

            // Estado final
            val result = awaitItem()
            assertTrue(result is UiState.Success)
            assertEquals(2, (result as UiState.Success).tasks.size)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loadTasks emite Error quando repositorio lanca excecao`() = runTest {
        // Arrange
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
| `Dispatchers.setMain(testDispatcher)` | Substitui o `Dispatchers.Main` (inexistente na JVM) por um dispatcher de teste |
| `coEvery { ... } returns ...` | Configura o mock para retornar um valor quando uma função `suspend` for chamada |
| `runTest { }` | Executa o bloco em um ambiente de coroutines de teste |
| `turbine.test { }` | Observa as emissões do `StateFlow` de forma estruturada |
| `advanceUntilIdle()` | Avança o tempo virtual até que todas as coroutines pendentes terminem |

---

## 4. Testando o Repository

```kotlin
// TasksRepositoryTest.kt
@OptIn(ExperimentalCoroutinesApi::class)
class TasksRepositoryTest {

    @Test
    fun `getPosts retorna lista mapeada corretamente`() = runTest {
        // Arrange: API fake que retorna DTOs
        val fakeApi = object : PostService {
            override suspend fun getPosts() = listOf(
                PostDto(userId = 1, id = 1, title = "Título", body = "Corpo")
            )
            override suspend fun getPost(id: Int) = PostDto(1, id, "T", "B")
            override suspend fun getPostsByUser(userId: Int) = emptyList<PostDto>()
        }

        val repository = PostRepositoryImpl(fakeApi)

        // Act
        val result = repository.getPosts()

        // Assert
        assertEquals(1, result.size)
        assertEquals("Título", result[0].title)
        assertEquals(1, result[0].authorId)
    }
}
```

---

## 5. Teste de UI com Compose

Testes instrumentados verificam que a interface se comporta como esperado.

```kotlin
// TasksScreenTest.kt (em src/androidTest/)
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class TasksScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun exibeListaDeTarefas() {
        val tarefas = listOf(
            Task(1, "Ler documentação", true),
            Task(2, "Implementar ViewModel", false)
        )

        composeRule.setContent {
            TasksList(tasks = tarefas)
        }

        // Verifica que os textos das tarefas aparecem na tela
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

---

## 6. Boas Práticas

1. **Teste o quê, não como**: verifique o estado final, não a implementação interna.
2. **Um assert por cenário**: cada teste deve validar um comportamento específico.
3. **Nomes descritivos**: use nomes que descrevam o cenário e o resultado esperado.
4. **Isole dependências**: use mocks ou fakes para não depender de rede ou banco real.
5. **Priorize ViewModel**: é onde está a lógica de apresentação e tem o melhor custo-benefício.

---

## 7. Exercícios Práticos

1. **Teste de ViewModel**: Escreva testes para o `PostViewModel` do Módulo 3 (Retrofit). Verifique que o estado é `Loading` inicialmente e `Success` após o carregamento.

2. **Teste de Repository**: Crie um fake do `PostService` e teste que o `PostRepositoryImpl` mapeia corretamente os DTOs para modelos de domínio.

3. **Teste de UI**: Escreva um teste instrumentado que verifique que a tela de lista de posts exibe o título do primeiro post.

4. **Desafio**: Teste o cenário de erro — simule uma falha de rede no mock e verifique que o ViewModel emite o estado `Error` com a mensagem correta.

---

## Checklist de Testes Mínimos

Antes de considerar o projeto pronto para release:

- [ ] ViewModel principal tem pelo menos 2 testes (sucesso e erro)
- [ ] Repository tem teste com fake de API
- [ ] 1–2 testes de UI para fluxos críticos (lista principal, tela de erro)
- [ ] Testes passam no CI (GitHub Actions ou similar)

---
