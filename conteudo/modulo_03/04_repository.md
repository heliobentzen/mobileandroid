# Repository Pattern com Room + Retrofit (JSONPlaceholder)

Exemplo mínimo, focado em aprendizagem e clareza. A lógica: tentar ler do banco; se vazio, buscar remoto, salvar e devolver. Camadas: Entity (Room), DAO, Database, API (Retrofit), Repository, ViewModel, UI (Compose).

## Dependências (build.gradle app)

```kotlin
dependencies {
    implementation("androidx.room:room-runtime:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.google.code.gson:gson:2.10.1")

    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")

    implementation("androidx.compose.ui:ui:1.7.0")
    implementation("androidx.compose.material3:material3:1.2.1")
    implementation("androidx.activity:activity-compose:1.9.2")
}
```

## Entity + DAO + Database (Room)

```kotlin
import androidx.room.*

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey val id: Int,
    val userId: Int,
    val title: String,
    val body: String
)

@Dao
interface PostDao {
    @Query("SELECT * FROM posts")
    suspend fun getAll(): List<PostEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(posts: List<PostEntity>)
}

@Database(entities = [PostEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun postDao(): PostDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: android.content.Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app.db"
                ).build().also { INSTANCE = it }
            }
    }
}
```

## API Retrofit

```kotlin
import retrofit2.http.GET

data class PostDto(
    val userId: Int,
    val id: Int,
    val title: String,
    val body: String
)

interface JsonPlaceholderApi {
    @GET("posts")
    suspend fun getPosts(): List<PostDto>
}

object ApiFactory {
    fun create(): JsonPlaceholderApi {
        val gson = com.google.gson.GsonBuilder().create()
        val retrofit = retrofit2.Retrofit.Builder()
            .baseUrl("https://jsonplaceholder.typicode.com/")
            .addConverterFactory(
                retrofit2.converter.gson.GsonConverterFactory
                    .create(gson)
            )
            .build()
        return retrofit.create(JsonPlaceholderApi::class.java)
    }
}
```

## Model (opcional para camada de domínio)

```kotlin
data class Post(
    val id: Int,
    val userId: Int,
    val title: String,
    val body: String
)

fun PostEntity.toModel() = Post(id, userId, title, body)
fun PostDto.toEntity() = PostEntity(id, userId, title, body)
```

## Repository

```kotlin
class PostRepository(
    private val dao: PostDao,
    private val api: JsonPlaceholderApi
) {
    // Obtém posts: se banco vazio, busca remoto e preenche.
    suspend fun getPosts(): List<Post> {
        val local = dao.getAll()
        if (local.isNotEmpty()) {
            return local.map { it.toModel() }
        }
        val remote = api.getPosts()
        val entities = remote.map { it.toEntity() }
        dao.insertAll(entities)
        return entities.map { it.toModel() }
    }

    // Força atualização remota.
    suspend fun refresh(): List<Post> {
        val remote = api.getPosts()
        val entities = remote.map { it.toEntity() }
        dao.insertAll(entities)
        return entities.map { it.toModel() }
    }
}
```

## ViewModel

```kotlin
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PostViewModel(
    private val repository: PostRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    data class UiState(
        val loading: Boolean = false,
        val posts: List<Post> = emptyList(),
        val error: String? = null
    )

    init {
        loadInitial()
    }

    private fun loadInitial() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            try {
                val data = repository.getPosts()
                _uiState.update { it.copy(loading = false, posts = data) }
            } catch (e: Exception) {
                _uiState.update { it.copy(loading = false, error = e.message) }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            try {
                val data = repository.refresh()
                _uiState.update { it.copy(loading = false, posts = data) }
            } catch (e: Exception) {
                _uiState.update { it.copy(loading = false, error = e.message) }
            }
        }
    }
}
```

## Factory

```kotlin
class PostViewModelFactory(
    private val context: android.content.Context
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        val db = AppDatabase.get(context)
        val api = ApiFactory.create()
        val repo = PostRepository(db.postDao(), api)
        return PostViewModel(repo) as T
    }
}
```

## UI (Jetpack Compose)

```kotlin
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun PostScreen(
    vm: PostViewModel = viewModel(factory = PostViewModelFactory(LocalContext.current))
) {
    val state by vm.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Posts") },
                actions = {
                    IconButton(onClick = { vm.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Atualizar")
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.error != null -> Text(
                    "Erro: ${state.error}",
                    Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.error
                )
                else -> PostList(state.posts)
            }
        }
    }
}

@Composable
fun PostList(posts: List<Post>) {
    LazyColumn {
        items(posts) { post ->
            Card(
                Modifier
                    .padding(8.dp)
                    .fillMaxWidth()
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(post.title, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(post.body, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
```

## Activity

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                PostScreen()
            }
        }
    }
}
```

## Observações

- Erros tratados de forma simples (Exception genérica).
- Para produção usar DI (Hilt) e melhor estratégia de cache/atualização.
- Atualização manual via botão e automática quando vazio.
- Exemplo não cobre paginação nem testes.

