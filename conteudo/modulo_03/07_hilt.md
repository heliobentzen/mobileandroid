# Injeção de Dependências com Hilt

Objetivo: entender o conceito de Injeção de Dependências (DI) e aplicar o Hilt como framework recomendado pelo Google para gerenciar dependências em apps Android — integrando ViewModel, Repository, Room e Retrofit.

**Pré-requisito:** Módulo 3.04 (Repository Pattern com Room + Retrofit).

---

## 1. O que é Injeção de Dependências?

Sem DI, cada classe cria suas próprias dependências — gerando acoplamento forte:

```kotlin
// ❌ Problema: ViewModel conhece TODAS as camadas e cria tudo manualmente
class PostViewModel : ViewModel() {
    private val db = Room.databaseBuilder(/*...*/).build()
    private val api = Retrofit.Builder().build().create(JsonPlaceholderApi::class.java)
    private val repository = PostRepository(db.postDao(), api)
}
```

Isso torna o código impossível de testar, cheio de duplicação e frágil para mudanças. Com DI, as classes *recebem* o que precisam:

```kotlin
// ✅ Solução: a dependência é recebida pelo construtor — Hilt fornece automaticamente
class PostViewModel @Inject constructor(
    private val repository: PostRepository
) : ViewModel()
```

Analogia: em vez de o cozinheiro ir à fazenda buscar ingredientes, o restaurante entrega tudo pronto na bancada.

---

## 2. Por que Hilt?

| Framework | Características |
|---|---|
| **Dagger** | Poderoso, mas configuração complexa e verbosa |
| **Koin** | Simples, porém resolução em runtime (erros tardios) |
| **Hilt** | Construído sobre Dagger, integração nativa ao Android |

O Hilt é a recomendação oficial do Google: gera código em tempo de compilação, integra-se com `ViewModel`, `Activity` e `Fragment`, e reduz o boilerplate do Dagger puro.

---

## 3. Configuração

### Dependências Gradle

```kotlin
// build.gradle (projeto) — adiciona o plugin do Hilt
plugins {
    id("com.google.dagger.hilt.android") version "2.51.1" apply false
}
```

```kotlin
// build.gradle (app)
plugins {
    id("com.google.devtools.ksp")            // Processamento de anotações
    id("com.google.dagger.hilt.android")     // Plugin do Hilt
}
dependencies {
    implementation("com.google.dagger:hilt-android:2.51.1")       // Hilt core
    ksp("com.google.dagger:hilt-compiler:2.51.1")                 // Geração de código
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0") // hiltViewModel()
}
```

### @HiltAndroidApp e @AndroidEntryPoint

```kotlin
@HiltAndroidApp // Obrigatório — gera o componente raiz do Hilt
class MeuApp : Application()

@AndroidEntryPoint // Habilita injeção nesta Activity
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PostScreen() } // Composables usam hiltViewModel()
    }
}
```

> **Importante:** registre `MeuApp` no `AndroidManifest.xml` com `android:name=".MeuApp"`.

---

## 4. @Inject no Construtor

Quando o Hilt consegue criar a classe sozinho, basta usar `@Inject constructor`:

```kotlin
// Hilt cria PostRepository porque DAO e API serão fornecidos via @Module
class PostRepository @Inject constructor(
    private val dao: PostDao,           // Injetado automaticamente
    private val api: JsonPlaceholderApi  // Injetado automaticamente
) {
    // Busca posts com estratégia cache-first
    suspend fun getPosts(): List<Post> {
        val local = dao.getAll()
        if (local.isNotEmpty()) return local.map { it.toModel() }
        val remoto = api.getPosts()
        dao.insertAll(remoto.map { it.toEntity() })
        return remoto.map { it.toModel() }
    }
}
```

Para o ViewModel, usamos `@HiltViewModel`. Na UI, `hiltViewModel()` substitui `viewModel()`:

```kotlin
// @HiltViewModel permite que o Hilt injete dependências no ViewModel
@HiltViewModel
class PostViewModel @Inject constructor(
    private val repository: PostRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    data class UiState(val loading: Boolean = false, val posts: List<Post> = emptyList(), val error: String? = null)

    init { carregar() }

    private fun carregar() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            try {
                val dados = repository.getPosts()
                _uiState.update { it.copy(loading = false, posts = dados) }
            } catch (e: Exception) {
                _uiState.update { it.copy(loading = false, error = e.message) }
            }
        }
    }
}

// Na UI Compose — hiltViewModel() resolve toda a cadeia de dependências
@Composable
fun PostScreen(viewModel: PostViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // ... restante da UI
}
```

---

## 5. @Module e @Provides

O Hilt não cria automaticamente classes externas (Retrofit, Room). Usamos `@Module`:

```kotlin
@Module
@InstallIn(SingletonComponent::class) // Vive enquanto o app estiver ativo
object NetworkModule {
    @Provides
    @Singleton // Apenas uma instância em todo o app
    fun provideApi(): JsonPlaceholderApi {
        return retrofit2.Retrofit.Builder()
            .baseUrl("https://jsonplaceholder.typicode.com/")
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()
            .create(JsonPlaceholderApi::class.java) // Cria implementação da interface
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton // Singleton evita problemas de concorrência
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        androidx.room.Room.databaseBuilder(context, AppDatabase::class.java, "app.db").build()

    @Provides
    fun providePostDao(database: AppDatabase): PostDao = database.postDao() // Banco já é singleton
}
```

---

## 6. Escopos

| Escopo | Tempo de vida | Uso típico |
|---|---|---|
| `@Singleton` | Enquanto o app existir | Retrofit, Room, DataStore |
| `@ViewModelScoped` | Enquanto o ViewModel existir | Use Cases, estado compartilhado |
| `@ActivityScoped` | Enquanto a Activity existir | Dependências por tela |
| Sem escopo | Nova instância a cada injeção | Objetos leves e stateless |

```kotlin
@ViewModelScoped // Instância compartilhada apenas dentro do mesmo ViewModel
class FormatadorDePost @Inject constructor() {
    fun formatarTitulo(titulo: String): String =
        titulo.replaceFirstChar { it.uppercase() } // Capitaliza a primeira letra
}
```

> **Regra prática:** use `@Singleton` para objetos caros (banco, rede) e evite escopo para objetos simples.

---

## 7. Exemplo Completo

Estrutura do app com Hilt:

```
app/
├── MeuApp.kt              // @HiltAndroidApp
├── MainActivity.kt        // @AndroidEntryPoint
├── di/                    // NetworkModule, DatabaseModule
├── data/                  // local/ (Room), remote/ (Retrofit), PostRepository
├── model/Post.kt          // Modelo de domínio
└── ui/                    // PostViewModel, PostScreen
```

Fluxo de injeção:

```plaintext
NetworkModule ─► JsonPlaceholderApi ─┐
                                     ├─► PostRepository ─► PostViewModel ─► UI
DatabaseModule ─► AppDatabase ─► PostDao ─┘
```

A `PostViewModelFactory` manual do Módulo 3.04 é completamente eliminada — o Hilt resolve toda a cadeia automaticamente.

---

## 8. Boas Práticas

### Interfaces para testabilidade

```kotlin
// Define o contrato como interface — facilita substituição em testes
interface PostRepository {
    suspend fun getPosts(): List<Post>
}

class PostRepositoryImpl @Inject constructor(
    private val dao: PostDao,
    private val api: JsonPlaceholderApi
) : PostRepository {
    override suspend fun getPosts(): List<Post> { /* cache-first */ }
}

// @Binds é mais eficiente que @Provides para vincular interface → implementação
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds abstract fun bindPostRepository(impl: PostRepositoryImpl): PostRepository
}
```

### Módulos separados por camada

Organize por responsabilidade: `NetworkModule` (Retrofit, OkHttp), `DatabaseModule` (Room, DAOs), `RepositoryModule` (interface → implementação) e `DataStoreModule` (veja Módulo 3.06).

### Regras gerais

1. Use `@Binds` para interfaces; reserve `@Provides` para classes externas.
2. Prefira `@Singleton` apenas quando necessário — exagerar desperdiça memória.
3. Nunca injete `Context` diretamente — use `@ApplicationContext` ou `@ActivityContext`.
4. Extraia interfaces dos Repositories para facilitar substituição em testes.

---

## 9. Resumo

| Conceito | Para que serve |
|---|---|
| `@HiltAndroidApp` | Gera o componente raiz na classe Application |
| `@AndroidEntryPoint` | Habilita injeção em Activity, Fragment e Service |
| `@HiltViewModel` | Permite injeção de dependências no ViewModel |
| `@Inject constructor` | Marca classe para criação automática pelo Hilt |
| `@Module` + `@Provides` | Fornece instâncias que o Hilt não cria sozinho |
| `@Binds` | Mapeia interface para implementação concreta |
| `@Singleton` | Garante instância única durante toda a vida do app |
| `@ViewModelScoped` | Instância vive enquanto o ViewModel existir |
| `hiltViewModel()` | Obtém ViewModel injetado em Composables |

---

## Próximos Passos

- **Módulo 4.01 (Testes):** substituir Repositories por fakes usando interfaces e Hilt Testing (`@UninstallModules`, `@BindValue`).
- Explorar `@AssistedInject` para ViewModels que recebem parâmetros dinâmicos (ex.: ID de item via navegação).
- Integrar Hilt com `WorkManager` para injetar dependências em tarefas em segundo plano.
