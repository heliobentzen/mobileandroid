# Repository Pattern: combinando fonte local e remota (NetworkBoundResource simplificado)
Exemplos usando a API pública JSONPlaceholder (https://jsonplaceholder.typicode.com)

Objetivo:
- Ler rápido do cache local (ex.: Room).
- Atualizar com dados mais recentes da API pública.
- Expor um único fluxo de dados para a UI.

Quando usar:
- Listas ou detalhes que devem funcionar offline e sincronizar quando online.
- Quando é necessário cache simples com atualização automática.

Fluxo em 4 passos:
1) Ler do banco local.
2) Decidir se deve buscar remoto (ex.: cache vazio ou antigo).
3) Se buscar, salvar o resultado no banco.
4) Emitir o dado do banco para a UI.

Pré-requisitos e dependências (não nativas):
- Room (persistência local)
- Retrofit + Converter (Moshi ou Gson) + OkHttp (rede)
- Kotlin Coroutines (Flow)
- ViewModel (AndroidX)

build.gradle.kts (módulo app):
```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android { /* ... config padrão ... */ }

dependencies {
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Lifecycle/ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.6")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Retrofit + Moshi (pode trocar por Gson se preferir)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.11.0")

    // OkHttp (log)
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
}
```

AndroidManifest.xml (permite rede):
```xml
<uses-permission android:name="android.permission.INTERNET" />
```

Estrutura de pastas (sugestão):
```
app/src/main/java/com/seuapp/
  data/
    local/
      AppDatabase.kt
      PostDao.kt
    remote/
      PostApi.kt
      PostDto.kt
    repository/
      PostRepository.kt
      NetworkBoundResource.kt
  domain/
    model/
      Post.kt
  ui/
    PostViewModel.kt
```

Função base:
```kotlin
import kotlinx.coroutines.flow.*

fun <T> networkBoundResource(
    query: () -> Flow<T>,
    fetch: suspend () -> T,
    saveFetchResult: suspend (T) -> Unit,
    shouldFetch: (T) -> Boolean = { true }
): Flow<T> = flow {
    val cached = query().firstOrNull()
    if (cached == null || shouldFetch(cached)) {
        try {
            val remote = fetch()
            saveFetchResult(remote)
        } catch (_: Exception) {
            // simples: ignora erro e segue emitindo o cache
        }
    }
    emitAll(query())
}
```

Exemplo (Kotlin + Room + Retrofit) com JSONPlaceholder:

Modelo/Entity (domain/model):
```kotlin
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "post")
data class Post(
    @PrimaryKey val id: Int,
    val title: String,
    val body: String
)
```

DAO (data/local):
```kotlin
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {
    @Query("SELECT * FROM post ORDER BY id")
    fun observeAll(): Flow<List<Post>>

    @Query("SELECT * FROM post WHERE id = :id")
    fun observeById(id: Int): Flow<Post?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(posts: List<Post>)
}
```

Database (data/local):
```kotlin
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Post::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun postDao(): PostDao
}
```

API + mapeamento (data/remote), usando JSONPlaceholder:
```kotlin
import retrofit2.http.GET
import retrofit2.http.Path

data class PostDto(
    val userId: Int,
    val id: Int,
    val title: String,
    val body: String
)

interface PostApi {
    @GET("posts")
    suspend fun getPosts(): List<PostDto>

    @GET("posts/{id}")
    suspend fun getPost(@Path("id") id: Int): PostDto
}

fun PostDto.toEntity() = Post(id = id, title = title, body = body)
```

Configuração rápida de Retrofit e Room (ex.: em um módulo de DI ou inicialização):
```kotlin
import android.content.Context
import androidx.room.Room
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

fun provideDatabase(context: Context) =
    Room.databaseBuilder(context, AppDatabase::class.java, "app.db").build()

fun providePostApi(): PostApi {
    val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    val client = OkHttpClient.Builder().addInterceptor(logging).build()

    return Retrofit.Builder()
        .baseUrl("https://jsonplaceholder.typicode.com/")
        .addConverterFactory(MoshiConverterFactory.create())
        .client(client)
        .build()
        .create(PostApi::class.java)
}
```

Repository (data/repository):
```kotlin
import kotlinx.coroutines.flow.Flow

class PostRepository(
    private val dao: PostDao,
    private val api: PostApi
) {
    fun posts(): Flow<List<Post>> = networkBoundResource(
        query = { dao.observeAll() },
        fetch = { api.getPosts().map { it.toEntity() } },
        saveFetchResult = { dao.upsertAll(it) },
        shouldFetch = { it.isEmpty() } // busca se cache vazio
    )

    fun post(id: Int): Flow<Post?> = networkBoundResource(
        query = { dao.observeById(id) },
        fetch = { api.getPost(id).toEntity() },
        saveFetchResult = { entity -> dao.upsertAll(listOf(entity)) },
        shouldFetch = { it == null } // baixa se não existir no cache
    )
}
```

Uso na ViewModel (ui):
```kotlin
import androidx.lifecycle.ViewModel

class PostViewModel(repo: PostRepository) : ViewModel() {
    val posts = repo.posts()        // Flow<List<Post>>
    fun post(id: Int) = repo.post(id) // Flow<Post?>
}
```

Dicas:
- shouldFetch pode considerar tempo (TTL) salvo em Preferences.
- Sempre salvar primeiro no DB e expor o Flow do DAO (fonte única).
- Em caso de erro de rede, mantenha o cache (offline first).
- JSONPlaceholder não requer autenticação, tem dados estáticos e limites razoáveis para testes.

Exercício (prática rápida):
- Objetivo: adaptar para buscar “detalhe do Post” (um item por id) — já mostrado em PostRepository.post(id).
- Teste: primeiro carregamento deve vir do cache se existir, senão buscar da API e salvar; chamadas seguintes devem ler do DB.