# Retrofit: do essencial a um projeto completo
Retrofit é um cliente HTTP de alto nível para Android/Kotlin que mapeia endpoints REST para interfaces Kotlin tipadas. As requisições são definidas por anotações (@GET, @POST, @Path, @Query), o transporte é feito pelo OkHttp e a conversão de JSON é delegada a um converter (neste guia, Kotlinx Serialization). Com coroutines, cada chamada vira uma função suspend, simples e segura.

Por que usar:
- Tipagem forte do contrato de rede (menos parsing manual e erros).
- Integração direta com OkHttp (timeouts, interceptors, logging).
- Converters pluggables (kotlinx-serialization, Moshi, Gson).
- Call adapters para coroutines/Flow e ergonomia moderna.
- Testabilidade com MockWebServer e fácil simulação de respostas.
- Controle de status/headers via Response<T> quando necessário.

O que está incluso:
- Cliente HTTP tipado, tolerante a mudanças de JSON.
- Repositório com Result<T> e mapeamento de erros.
- ViewModel com Flow/StateFlow e UI (Compose) mínima.
- Teste de rede com MockWebServer.
- Estrutura de pastas e dependências.

Base URL usada:
- https://jsonplaceholder.typicode.com/

Estrutura sugerida do módulo app:
- core/network/Network.kt
- feature/posts/data/remote/PostsApi.kt
- feature/posts/data/remote/Models.kt
- feature/posts/data/PostsRepository.kt
- feature/posts/ui/PostsViewModel.kt
- feature/posts/ui/PostsScreen.kt
- AppServiceLocator.kt

## Dependências (Kotlin DSL)

build.gradle.kts do módulo app:
```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.example.app"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
    }
}

dependencies {
    // Retrofit + converter Kotlinx Serialization
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-kotlinx-serialization:2.11.0")

    // OkHttp + logging apenas em debug
    implementation(platform("com.squareup.okhttp3:okhttp-bom:4.12.0"))
    implementation("com.squareup.okhttp3:okhttp")
    debugImplementation("com.squareup.okhttp3:logging-interceptor")

    // Kotlinx Serialization (JSON) — use a mais recente do Maven Central
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Lifecycle/ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")

    // Compose (opcional, apenas para a tela de exemplo)
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Testes
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
}
```

## 1) Core de rede: OkHttp, Retrofit e JSON

core/network/Network.kt
```kotlin
package com.example.app.core.network

import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

// JSON tolerante a mudanças de contrato
val json: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    coerceInputValues = true
}

fun provideOkHttp(
    isDebug: Boolean,
    tokenProvider: () -> String? = { null }
): OkHttpClient {
    val auth = Interceptor { chain ->
        val req = chain.request().newBuilder().apply {
            tokenProvider()?.let { addHeader("Authorization", "Bearer $it") }
            addHeader("Accept", "application/json")
        }.build()
        chain.proceed(req)
    }

    val logging = HttpLoggingInterceptor().apply {
        level = if (isDebug) HttpLoggingInterceptor.Level.BODY
        else HttpLoggingInterceptor.Level.NONE
    }

    return OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .addInterceptor(auth)
        .addInterceptor(logging)
        .build()
}

fun provideRetrofit(baseUrl: String, client: OkHttpClient): Retrofit {
    val contentType = "application/json".toMediaType()
    return Retrofit.Builder()
        .baseUrl(baseUrl) // sempre terminar com /
        .addConverterFactory(json.asConverterFactory(contentType))
        .client(client)
        .build()
}
```

## 2) API e modelos

feature/posts/data/remote/Models.kt
```kotlin
package com.example.app.feature.posts.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class Post(
    val id: Int? = null,
    val userId: Int,
    val title: String,
    val body: String
)
```

feature/posts/data/remote/PostsApi.kt
```kotlin
package com.example.app.feature.posts.data.remote

import retrofit2.Response
import retrofit2.http.*

interface PostsApi {
    @GET("posts")
    suspend fun getPosts(): List<Post> // corpo confiável -> T direto

    @GET("posts/{id}")
    suspend fun getPost(@Path("id") id: Int): Response<Post> // precisa de status

    @POST("posts")
    suspend fun create(@Body post: Post): Response<Post>
}
```

## 3) Erros e Result

Crie um mapeamento simples e reutilizável:

feature/posts/data/ResultErrors.kt
```kotlin
package com.example.app.feature.posts.data

import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException

sealed interface AppError {
    data class Network(val cause: IOException) : AppError
    data class Http(val code: Int, val message: String?) : AppError
    data class Parse(val cause: SerializationException) : AppError
    data class Unknown(val cause: Throwable) : AppError
}

fun Throwable.toAppError(): AppError = when (this) {
    is IOException -> AppError.Network(this)
    is HttpException -> AppError.Http(code(), message())
    is SerializationException -> AppError.Parse(this)
    else -> AppError.Unknown(this)
}
```

## 4) Repositório

feature/posts/data/PostsRepository.kt
```kotlin
package com.example.app.feature.posts.data

import com.example.app.feature.posts.data.remote.Post
import com.example.app.feature.posts.data.remote.PostsApi
import retrofit2.HttpException

class PostsRepository(private val api: PostsApi) {

    suspend fun list(): Result<List<Post>> = runCatching { api.getPosts() }

    suspend fun get(id: Int): Result<Post> = runCatching {
        val resp = api.getPost(id)
        if (resp.isSuccessful) resp.body() ?: error("Empty body")
        else throw HttpException(resp)
    }

    suspend fun create(post: Post): Result<Post> = runCatching {
        val resp = api.create(post)
        if (resp.isSuccessful) resp.body() ?: error("Empty body")
        else throw HttpException(resp)
    }
}
```

## 5) Service Locator (objeto único para wiring)

AppServiceLocator.kt
```kotlin
package com.example.app

import com.example.app.core.network.provideOkHttp
import com.example.app.core.network.provideRetrofit
import com.example.app.feature.posts.data.PostsRepository
import com.example.app.feature.posts.data.remote.PostsApi

object AppServiceLocator {
    private const val BASE_URL = "https://jsonplaceholder.typicode.com/"

    private val client by lazy { provideOkHttp(isDebug = BuildConfig.DEBUG) }
    private val retrofit by lazy { provideRetrofit(BASE_URL, client) }

    private val postsApi by lazy { retrofit.create(PostsApi::class.java) }

    val postsRepository by lazy { PostsRepository(postsApi) }
}
```

Opcional: se precisar de token, injete um tokenProvider no provideOkHttp.

## 6) ViewModel e estados

feature/posts/ui/PostsViewModel.kt
```kotlin
package com.example.app.feature.posts.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app.feature.posts.data.PostsRepository
import com.example.app.feature.posts.data.toAppError
import com.example.app.feature.posts.data.remote.Post
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface PostsUiState {
    object Idle : PostsUiState
    object Loading : PostsUiState
    data class Success(val items: List<Post>) : PostsUiState
    data class Error(val message: String) : PostsUiState
}

class PostsViewModel(private val repo: PostsRepository) : ViewModel() {
    private val _state = MutableStateFlow<PostsUiState>(PostsUiState.Idle)
    val state: StateFlow<PostsUiState> = _state

    fun load() {
        _state.value = PostsUiState.Loading
        viewModelScope.launch {
            repo.list()
                .onSuccess { _state.value = PostsUiState.Success(it) }
                .onFailure {
                    val msg = when (val e = it.toAppError()) {
                        is com.example.app.feature.posts.data.AppError.Network -> "Conexão indisponível."
                        is com.example.app.feature.posts.data.AppError.Http -> "Erro HTTP ${e.code}."
                        is com.example.app.feature.posts.data.AppError.Parse -> "Resposta inesperada."
                        is com.example.app.feature.posts.data.AppError.Unknown -> "Erro desconhecido."
                    }
                    _state.value = PostsUiState.Error(msg)
                }
        }
    }

    fun createSample() {
        viewModelScope.launch {
            repo.create(Post(userId = 1, title = "Novo", body = "Conteúdo"))
                .onSuccess { load() }
                .onFailure { /* tratar se necessário */ }
        }
    }
}
```

Se não usar DI, instancie a ViewModel com um factory simples, obtendo o repositório do AppServiceLocator.

## 7) UI (Compose) mínima

feature/posts/ui/PostsScreen.kt
```kotlin
package com.example.app.feature.posts.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.app.feature.posts.data.remote.Post

@Composable
fun PostsScreen(vm: PostsViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.load() }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Posts") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = vm::createSample) { Text("+") }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (val s = state) {
                is PostsUiState.Idle -> {}
                is PostsUiState.Loading -> CircularProgressIndicator(Modifier.padding(16.dp))
                is PostsUiState.Error -> Text(s.message, Modifier.padding(16.dp))
                is PostsUiState.Success -> PostsList(s.items)
            }
        }
    }
}

@Composable
private fun PostsList(items: List<Post>) {
    LazyColumn {
        items(items) { p ->
            ListItem(
                headlineContent = { Text(p.title) },
                supportingContent = { Text(p.body) }
            )
            Divider()
        }
    }
}
```

Factory simples (se necessário):
```kotlin
class PostsVmFactory(
    private val repo: com.example.app.feature.posts.data.PostsRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return com.example.app.feature.posts.ui.PostsViewModel(repo) as T
    }
}
```

## 8) Teste de rede com MockWebServer

feature/posts/data/PostsRepositoryTest.kt
```kotlin
package com.example.app.feature.posts.data

import com.example.app.core.network.provideOkHttp
import com.example.app.core.network.provideRetrofit
import com.example.app.feature.posts.data.remote.PostsApi
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertTrue
import org.junit.Test

class PostsRepositoryTest {
    @Test
    fun list_success() = runTest {
        val server = MockWebServer().apply {
            enqueue(MockResponse().setResponseCode(200).setBody(
                """[{"userId":1,"id":1,"title":"t","body":"b"}]"""
            ))
            start()
        }

        val client = provideOkHttp(isDebug = false)
        val retrofit = provideRetrofit(server.url("/").toString(), client)
        val repo = PostsRepository(retrofit.create(PostsApi::class.java))

        val result = repo.list()
        assertTrue(result.isSuccess)

        server.shutdown()
    }
}
```

O que o teste garante:
- Conversão JSON ↔ objeto funciona.
- Retrofit/OkHttp estão plugados corretamente.
- Sem dependência de internet para testar.

## Checklist essencial e moderno

- JSON: Json(ignoreUnknownKeys=true, explicitNulls=false).
- Retrofit converter: kotlinx-serialization com asConverterFactory.
- OkHttp: timeouts explícitos, Authorization opcional, logging só em debug.
- Result<T> + mapper de erros (IOException/HttpException/SerializationException).
- Response<T> apenas quando precisar ler status/headers; senão, use T direto.
- UI reativa (Flow/StateFlow) e cancelamento por lifecycle (viewModelScope).
- Testes com MockWebServer cobrindo 200/404/500 e timeouts.

## Exercício rápido

- Adicione paginação simples na lista (stateful no ViewModel, carregando páginas).
- Implemente cache em memória no Repository, com invalidação após 60s.
- Mostre mensagens diferentes para timeout (IOException), 4xx e 5xx.
- Cubra com testes no MockWebServer para sucesso e erros.

Pronto: um projeto enxuto, atual e pronto para evoluir com DI (Hilt/Koin) se necessário, mantendo simplicidade para aprender e manter.