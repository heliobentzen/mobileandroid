# Prática: Consumindo APIs com Retrofit para Iniciantes

Este guia apresenta exercícios práticos para buscar dados da internet usando o **Retrofit**, a biblioteca mais popular para chamadas de rede no Android.

---

## O que é o Retrofit?

O Retrofit transforma sua API REST em uma interface Kotlin. Você descreve os endpoints como funções e o Retrofit cuida de toda a comunicação HTTP por baixo dos panos.

---

## Configuração

Adicione ao `app/build.gradle.kts`:

```kotlin
dependencies {
    // Retrofit + conversor de JSON
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // ViewModel e Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
}
```

Adicione a permissão de internet ao `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

---

## Prática 1: Buscando Piadas da Internet

### Objetivo
Fazer uma chamada simples a uma API pública e exibir o resultado na tela.

**API usada**: [Official Joke API](https://official-joke-api.appspot.com/random_joke) (sem chave)

### Passo a Passo

**1. Modelo de dados** (`Piada.kt`):

```kotlin
import com.google.gson.annotations.SerializedName

data class Piada(
    @SerializedName("id") val id: Int,
    @SerializedName("type") val tipo: String,
    @SerializedName("setup") val pergunta: String,
    @SerializedName("punchline") val resposta: String
)
```

> **O que é `@SerializedName`?** É a anotação que mapeia o campo do JSON para a propriedade da data class. Por exemplo, `"setup"` no JSON vira `pergunta` no Kotlin.

**2. Interface de serviço** (`PiadaService.kt`):

```kotlin
import retrofit2.http.GET

interface PiadaService {
    // Define o endpoint: GET https://official-joke-api.appspot.com/random_joke
    @GET("random_joke")
    suspend fun buscarPiadaAleatoria(): Piada

    @GET("jokes/ten")
    suspend fun buscarDezPiadas(): List<Piada>
}
```

**3. Cliente Retrofit** (`RetrofitClient.kt`):

```kotlin
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "https://official-joke-api.appspot.com/"

    // Lazy: o Retrofit só é criado na primeira vez que for acessado
    val piadaService: PiadaService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PiadaService::class.java)
    }
}
```

**4. Estado da UI** (`PiadaUiState.kt`):

```kotlin
sealed interface PiadaUiState {
    data object Carregando : PiadaUiState
    data class Sucesso(val piada: Piada) : PiadaUiState
    data class Erro(val mensagem: String) : PiadaUiState
}
```

**5. ViewModel** (`PiadaViewModel.kt`):

```kotlin
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PiadaViewModel(
    private val service: PiadaService = RetrofitClient.piadaService
) : ViewModel() {

    private val _uiState = MutableStateFlow<PiadaUiState>(PiadaUiState.Carregando)
    val uiState: StateFlow<PiadaUiState> = _uiState.asStateFlow()

    init { buscarPiada() }

    fun buscarPiada() {
        viewModelScope.launch {
            _uiState.value = PiadaUiState.Carregando
            try {
                val piada = service.buscarPiadaAleatoria()
                _uiState.value = PiadaUiState.Sucesso(piada)
            } catch (e: Exception) {
                _uiState.value = PiadaUiState.Erro("Falha ao carregar: ${e.message}")
            }
        }
    }
}
```

**6. Tela Compose** (`PiadaScreen.kt`):

```kotlin
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun PiadaScreen(viewModel: PiadaViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var revelarResposta by remember { mutableStateOf(false) }

    // Quando a piada muda, esconde a resposta automaticamente
    LaunchedEffect(uiState) { revelarResposta = false }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("😂 Piada do Momento", style = MaterialTheme.typography.headlineMedium)

        Spacer(Modifier.height(32.dp))

        when (val state = uiState) {
            is PiadaUiState.Carregando -> CircularProgressIndicator()

            is PiadaUiState.Erro -> {
                Text(
                    text = state.mensagem,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = { viewModel.buscarPiada() }) { Text("Tentar novamente") }
            }

            is PiadaUiState.Sucesso -> {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Pergunta da piada
                        Text(
                            text = state.piada.pergunta,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )

                        // Resposta — revelada ao clicar
                        AnimatedVisibility(visible = revelarResposta) {
                            Text(
                                text = state.piada.resposta,
                                style = MaterialTheme.typography.bodyLarge,
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )
                        }

                        if (!revelarResposta) {
                            OutlinedButton(onClick = { revelarResposta = true }) {
                                Text("Revelar resposta 🎭")
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.buscarPiada() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Próxima piada ➡")
                }
            }
        }
    }
}
```

### Exercícios

1. Adicione um histórico das últimas 5 piadas buscadas, exibido como uma lista abaixo do card principal.
2. Adicione um botão "Compartilhar" que abre o seletor de compartilhamento do Android com o texto da piada.
3. Modifique o ViewModel para buscar as dez piadas de uma vez (`service.buscarDezPiadas()`) e navegar entre elas localmente (sem nova chamada de rede a cada piada).

---

## Prática 2: Lista de Posts com JSONPlaceholder

### Objetivo
Buscar e exibir uma lista de dados da API pública JSONPlaceholder.

**API usada**: [JSONPlaceholder](https://jsonplaceholder.typicode.com/) (sem chave)

### Passo a Passo

**1. Modelos** (`Post.kt`):

```kotlin
import com.google.gson.annotations.SerializedName

data class PostDto(
    @SerializedName("id") val id: Int,
    @SerializedName("userId") val userId: Int,
    @SerializedName("title") val titulo: String,
    @SerializedName("body") val corpo: String
)

// Modelo de domínio: o que a UI usa (pode ser diferente do DTO)
data class Post(
    val id: Int,
    val titulo: String,
    val corpo: String
)

// Converte DTO em modelo de domínio
fun PostDto.toDomain() = Post(id = id, titulo = titulo, corpo = corpo)
```

**2. Service** (`PostService.kt`):

```kotlin
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface PostService {
    @GET("posts")
    suspend fun buscarPosts(): List<PostDto>

    @GET("posts/{id}")
    suspend fun buscarPost(@Path("id") id: Int): PostDto

    @GET("posts")
    suspend fun buscarPostsDoUsuario(@Query("userId") userId: Int): List<PostDto>
}
```

**3. Repositório** (`PostRepository.kt`):

```kotlin
class PostRepository(private val service: PostService) {
    suspend fun buscarPosts(): List<Post> = service.buscarPosts().map { it.toDomain() }
    suspend fun buscarPost(id: Int): Post = service.buscarPost(id).toDomain()
}
```

**4. ViewModel** (`PostListViewModel.kt`):

```kotlin
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface PostListUiState {
    data object Carregando : PostListUiState
    data class Sucesso(val posts: List<Post>) : PostListUiState
    data class Erro(val mensagem: String) : PostListUiState
}

class PostListViewModel(
    private val repository: PostRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PostListUiState>(PostListUiState.Carregando)
    val uiState: StateFlow<PostListUiState> = _uiState.asStateFlow()

    init { carregarPosts() }

    fun carregarPosts() {
        viewModelScope.launch {
            _uiState.value = PostListUiState.Carregando
            try {
                val posts = repository.buscarPosts()
                _uiState.value = PostListUiState.Sucesso(posts)
            } catch (e: Exception) {
                _uiState.value = PostListUiState.Erro(e.message ?: "Erro ao carregar posts")
            }
        }
    }
}
```

**5. Tela com lista** (`PostListScreen.kt`):

```kotlin
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun PostListScreen(
    viewModel: PostListViewModel,
    onPostClick: (Post) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Posts") }) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = uiState) {
                is PostListUiState.Carregando -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is PostListUiState.Erro -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("❌ ${state.mensagem}", color = MaterialTheme.colorScheme.error)
                        Button(onClick = { viewModel.carregarPosts() }) { Text("Tentar novamente") }
                    }
                }

                is PostListUiState.Sucesso -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.posts, key = { it.id }) { post ->
                            ElevatedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onPostClick(post) }
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "${post.id}. ${post.titulo}",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = post.corpo,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 2
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
```

### Exercícios

1. Adicione uma barra de pesquisa que filtra os posts pelo título em tempo real (sem nova chamada de rede).
2. Implemente uma tela de detalhe: ao clicar em um post, navegue para uma nova tela que exibe o título e o corpo completo.
3. Adicione paginação manual: exiba os 10 primeiros posts e um botão "Carregar mais" que adiciona os próximos 10.

---

## Prática 3: Tratamento de Erros

### Objetivo
Tratar os diferentes tipos de erro que podem acontecer em chamadas de rede.

```kotlin
import retrofit2.HttpException
import java.io.IOException

fun main() {
    // Tipos comuns de exceção em chamadas de rede:
}

// No ViewModel ou Repository, trate erros específicos:
suspend fun buscarComTratamento(service: PiadaService): Result<Piada> {
    return try {
        val piada = service.buscarPiadaAleatoria()
        Result.success(piada)
    } catch (e: IOException) {
        // Sem internet, DNS falhou, timeout, etc.
        Result.failure(Exception("Sem conexão com a internet. Verifique sua rede."))
    } catch (e: HttpException) {
        // O servidor respondeu com erro HTTP (4xx, 5xx)
        when (e.code()) {
            404 -> Result.failure(Exception("Recurso não encontrado."))
            401 -> Result.failure(Exception("Não autorizado. Verifique suas credenciais."))
            500 -> Result.failure(Exception("Erro interno do servidor. Tente mais tarde."))
            else -> Result.failure(Exception("Erro HTTP ${e.code()}: ${e.message()}"))
        }
    } catch (e: Exception) {
        // Outros erros inesperados
        Result.failure(Exception("Erro inesperado: ${e.message}"))
    }
}
```

### Exercícios

1. Modifique o `PiadaViewModel` para usar a função `buscarComTratamento` e exibir mensagens de erro específicas na tela.
2. Adicione um indicador visual diferente para cada tipo de erro (sem internet vs. erro do servidor).
3. Implemente um mecanismo de retry com backoff: na primeira falha, tente novamente após 1s; na segunda, após 2s; na terceira, desista e exiba o erro. Use `delay` e um loop `repeat`.

---

## Resumo

```
Interface (Service)  →  define endpoints
RetrofitClient       →  cria a instância do Retrofit
Repository       →  faz a chamada e transforma o DTO em modelo de domínio
ViewModel        →  chama o Repository no viewModelScope; expõe StateFlow
UI (Compose)     →  observa o StateFlow e exibe Loading/Success/Error
```

| Anotação Retrofit | O que faz |
|-------------------|-----------|
| `@GET("path")` | Requisição GET |
| `@POST("path")` | Requisição POST |
| `@Path("nome")` | Substitui `{nome}` na URL |
| `@Query("nome")` | Adiciona `?nome=valor` à URL |
| `@Body` | Envia objeto no corpo da requisição |
| `@SerializedName` | Mapeia campo do JSON para propriedade Kotlin |

---

## Próximos Passos

- Estude o módulo `02_retrofit.md` para ver a estrutura em camadas completa.
- Combine com `07_room_persistencia.md` para cache local dos dados da API.
- Explore o módulo `04_repository.md` para organizar o código com o padrão Repository.
