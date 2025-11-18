# Retrofit + Gson + Coroutines + Jetpack Compose (guia progressivo)

Objetivo: consumir uma API pública com Retrofit, organizar em camadas (instância, model, ViewModel, UI com Compose), usando `suspend fun` e evoluindo o conteúdo de forma progressiva.

API usada: JSONPlaceholder (sem chave)
- Base URL: https://jsonplaceholder.typicode.com/
- Endpoints: /posts e /posts/{id}


Dependências (build.gradle.kts do módulo app)
```kotlin
dependencies {
    // Retrofit + Gson
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

    // OkHttp + logging
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Lifecycle / ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.10.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.2")
}
```

Estrutura de pastas (sugestão)
- data
  - remote (Retrofit, services, DTOs)
  - repository
- domain
  - model (modelos de domínio)
- presentation
  - post (ViewModel e UI/Compose)

Parte 1: Instância do Retrofit (camada data/remote)
```kotlin
// data/remote/ApiClient.kt
package com.example.retrofitdemo.data.remote

import com.example.retrofitdemo.data.remote.service.PostService
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val BASE_URL = "https://jsonplaceholder.typicode.com/"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val okHttp = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val gson = GsonBuilder()
        .setLenient()
        .create()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttp)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    val postService: PostService by lazy {
        retrofit.create(PostService::class.java)
    }
}
```

Parte 2: Modelos (DTO e Domain)
```kotlin
// data/remote/dto/PostDto.kt
package com.example.retrofitdemo.data.remote.dto

import com.google.gson.annotations.SerializedName

data class PostDto(
    @SerializedName("userId") val userId: Int,
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String,
    @SerializedName("body") val body: String
)
```

```kotlin
// domain/model/Post.kt
package com.example.retrofitdemo.domain.model

data class Post(
    val id: Int,
    val title: String,
    val body: String,
    val authorId: Int
)
```

Parte 3: Service (endpoints com suspend fun)
```kotlin
// data/remote/service/PostService.kt
package com.example.retrofitdemo.data.remote.service

import com.example.retrofitdemo.data.remote.dto.PostDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface PostService {
    @GET("posts")
    suspend fun getPosts(): List<PostDto>

    @GET("posts/{id}")
    suspend fun getPost(@Path("id") id: Int): PostDto

    @GET("posts")
    suspend fun getPostsByUser(@Query("userId") userId: Int): List<PostDto>
}
```

Parte 4: Repository (camada data/repository)
```kotlin
// data/repository/PostRepository.kt
package com.example.retrofitdemo.data.repository

import com.example.retrofitdemo.domain.model.Post

interface PostRepository {
    suspend fun getPosts(): List<Post>
    suspend fun getPost(id: Int): Post
    suspend fun getPostsByUser(userId: Int): List<Post>
}
```

```kotlin
// data/repository/PostRepositoryImpl.kt
package com.example.retrofitdemo.data.repository

import com.example.retrofitdemo.data.remote.service.PostService
import com.example.retrofitdemo.domain.model.Post

class PostRepositoryImpl(
    private val service: PostService
) : PostRepository {

    override suspend fun getPosts(): List<Post> {
        return service.getPosts().map {
            Post(
                id = it.id,
                title = it.title,
                body = it.body,
                authorId = it.userId
            )
        }
    }

    override suspend fun getPost(id: Int): Post {
        val dto = service.getPost(id)
        return Post(
            id = dto.id,
            title = dto.title,
            body = dto.body,
            authorId = dto.userId
        )
    }

    override suspend fun getPostsByUser(userId: Int): List<Post> {
        return service.getPostsByUser(userId).map {
            Post(
                id = it.id,
                title = it.title,
                body = it.body,
                authorId = it.userId
            )
        }
    }
}
```

Parte 5: ViewModel (StateFlow + coroutines)
```kotlin
// presentation/post/PostUiState.kt
package com.example.retrofitdemo.presentation.post

import com.example.retrofitdemo.domain.model.Post

sealed interface PostUiState {
    data object Loading : PostUiState
    data class Success(val posts: List<Post>) : PostUiState
    data class Error(val message: String) : PostUiState
}
```

```kotlin
// presentation/post/PostViewModel.kt
package com.example.retrofitdemo.presentation.post

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.retrofitdemo.data.repository.PostRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PostViewModel(
    private val repository: PostRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PostUiState>(PostUiState.Loading)
    val uiState: StateFlow<PostUiState> = _uiState.asStateFlow()

    init { loadPosts() }

    fun loadPosts() {
        viewModelScope.launch {
            _uiState.value = PostUiState.Loading
            runCatching { repository.getPosts() }
                .onSuccess { _uiState.value = PostUiState.Success(it) }
                .onFailure { _uiState.value = PostUiState.Error(it.message ?: "Erro inesperado") }
        }
    }
}
```

Parte 6: UI com Jetpack Compose
```kotlin
// presentation/post/PostScreen.kt
package com.example.retrofitdemo.presentation.post

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.retrofitdemo.domain.model.Post

@Composable
fun PostScreen(
    viewModel: PostViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    when (val s = state) {
        is PostUiState.Loading -> Box(
            modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) { CircularProgressIndicator() }

        is PostUiState.Error -> Box(
            modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Erro: ${s.message}")
                Spacer(Modifier.height(8.dp))
                Button(onClick = { viewModel.loadPosts() }) {
                    Text("Tentar novamente")
                }
            }
        }

        is PostUiState.Success -> PostList(posts = s.posts, modifier)
    }
}

@Composable
private fun PostList(posts: List<Post>, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(posts, key = { it.id }) { post ->
            PostItem(post)
        }
    }
}

@Composable
private fun PostItem(post: Post) {
    ElevatedCard {
        Column(Modifier.padding(16.dp)) {
            Text(text = post.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(text = post.body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
```

Factory extraída
```kotlin
// presentation/post/PostViewModelFactory.kt
package com.example.retrofitdemo.presentation.post

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.retrofitdemo.data.repository.PostRepository

class PostViewModelFactory(
    private val repository: PostRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PostViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PostViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
```

Activity
```kotlin
// MainActivity.kt
package com.example.retrofitdemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import androidx.compose.material3.MaterialTheme
import com.example.retrofitdemo.data.remote.ApiClient
import com.example.retrofitdemo.data.repository.PostRepositoryImpl
import com.example.retrofitdemo.presentation.post.PostScreen
import com.example.retrofitdemo.presentation.post.PostViewModel
import com.example.retrofitdemo.presentation.post.PostViewModelFactory

class MainActivity : ComponentActivity() {
    private val viewModel by lazy {
        ViewModelProvider(
            this,
            PostViewModelFactory(PostRepositoryImpl(ApiClient.postService))
        )[PostViewModel::class.java]
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repo = PostRepositoryImpl(ApiClient.postService)
        val viewModel = ViewModelProvider(
            this,
            PostViewModelFactory(repo)
        )[PostViewModel::class.java]

        setContent {
            MaterialTheme {
                PostScreen(viewModel = viewModel)
            }
        }
    }
}
```

Parte 7: Erros, logging e boas práticas
- Logging: HttpLoggingInterceptor (BASIC). Em debug, pode usar BODY.
- Tratamento de exceções: HttpException, IOException, mapeadas em ViewModel.
- Separar DTO de Domain: mantém isolamento sem precisar de arquivo mapper quando transformação é simples.
- Evitar trabalho pesado na UI thread: usar withContext(Dispatchers.Default) se necessário.

Exemplo de uso com Response (mapeando inline):
```kotlin
// Service
@GET("posts")
suspend fun getPostsResponse(): retrofit2.Response<List<PostDto>>

// Repository
val response = service.getPostsResponse()
if (response.isSuccessful) {
    val list = response.body().orEmpty().map {
        Post(
            id = it.id,
            title = it.title,
            body = it.body,
            authorId = it.userId
        )
    }
} else {
    throw IllegalStateException("HTTP ${response.code()}: ${response.message()}")
}
```

Parte 8: Evoluindo o exemplo
- Filtro por usuário (query):
```kotlin
// ViewModel
fun loadPostsByUser(userId: Int) = viewModelScope.launch {
    runCatching { repository.getPostsByUser(userId) }
        .onSuccess { /* atualizar estado */ }
        .onFailure { /* erro */ }
}
```

- Detalhe de um Post:
  - Service: getPost(id)
  - Novo UiState de detalhe ou outra ViewModel.

- DI com Hilt (opcional):
  - Módulos para Retrofit, Service e Repository.

Dicas finais
- baseUrl com barra final.
- Funções de rede como suspend.
- StateFlow para reatividade.
- Testes com MockWebServer.
- Ajustar timeouts e mensagens de erro para usuário.

Com isso, a app lista posts usando Retrofit+Gson com coroutines e Compose, sem camada de mapper dedicada (conversão inline no repository).