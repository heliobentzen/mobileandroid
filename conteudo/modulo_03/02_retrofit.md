# Retrofit + Gson + Coroutines + Jetpack Compose (guia progressivo)

## O que é Retrofit?

Retrofit é uma biblioteca que transforma "conversar com uma API pela internet" em algo tão simples quanto chamar uma função Kotlin comum. Sem ela, você precisaria escrever manualmente todo o código de baixo nível para abrir uma conexão HTTP, montar a requisição, esperar a resposta, converter o texto JSON recebido em objetos Kotlin e tratar erros de rede — tudo isso "na mão".

Pense no Retrofit como um garçom multilíngue que sabe pedir exatamente o que você quer ao "restaurante remoto" (o servidor). Você só precisa dizer *o que* quer (ex.: "me dê a lista de posts") em uma interface Kotlin, e o Retrofit cuida de todo o processo de comunicação e tradução (JSON → objeto Kotlin) por trás dos panos.

Ela funciona declarando **interfaces Kotlin com anotações** que descrevem os endpoints (endereços da API), e é integrada com **coroutines** (veja `01_coroutines.md`) para que as chamadas de rede sejam assíncronas, ou seja, não travem a interface enquanto esperam a resposta do servidor.

## Por que isso importa?

Toda tela que mostra dados vindos da internet — uma lista de produtos, o perfil de um usuário, uma timeline — depende de buscar esses dados em algum servidor remoto. Sem uma biblioteca como o Retrofit, você teria que:

- Escrever manualmente as requisições HTTP (GET, POST, PUT, DELETE) usando classes de baixo nível.
- Fazer o parsing (conversão) do texto JSON recebido para objetos Kotlin na mão, tratando cada campo.
- Reimplementar tratamento de erro, timeout e cabeçalhos HTTP em todo lugar que fizer uma chamada.
- Correr o risco de esquecer de rodar a chamada fora da thread principal, travando o app (veja o Módulo 3.01 sobre a UI thread).

O Retrofit resolve tudo isso com poucas linhas declarativas, reduzindo drasticamente a chance de erro e o tempo gasto com código repetitivo ("boilerplate").

**Alguns termos que vamos usar neste arquivo:**

- **API REST**: um conjunto de endereços (endpoints) em um servidor que respondem a requisições HTTP e retornam dados, geralmente em formato JSON.
- **Endpoint**: um endereço específico da API, como `/posts` ou `/posts/1`.
- **JSON**: um formato de texto para representar dados estruturados (parecido com um mapa de chave-valor), o formato mais comum de resposta de APIs REST.
- **DTO (Data Transfer Object)**: uma classe Kotlin cujo único propósito é espelhar exatamente a estrutura do JSON recebido da API.

Neste guia, vamos consumir a [JSONPlaceholder](https://jsonplaceholder.typicode.com/), uma API pública gratuita ideal para aprendizado, organizando o código em camadas (data, domain, presentation) — a mesma organização em camadas que você vai reencontrar no Módulo 3.04 (Repository Pattern).

**API usada:** JSONPlaceholder (sem necessidade de chave de acesso)
- Base URL: `https://jsonplaceholder.typicode.com/`
- Endpoints utilizados: `/posts` e `/posts/{id}`

---

## Dependências (build.gradle.kts do módulo app)

Estas são as bibliotecas externas que o projeto precisa baixar para usar Retrofit, Gson (conversor JSON) e OkHttp (cliente HTTP por baixo do Retrofit).

```kotlin
dependencies {
    // Retrofit + Gson
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

    // OkHttp + logging (mesma versão do OkHttp embutido no Retrofit 2.11)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Lifecycle / ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // Compose (BOM gerencia versões automaticamente)
    implementation(platform("androidx.compose:compose-bom:2025.01.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.10.1")
}
```

## Estrutura de pastas (sugestão)

Organizar o projeto em camadas facilita testes, manutenção e clareza sobre onde cada tipo de código deve ficar. Cada pasta tem uma responsabilidade única:

- **data** — tudo relacionado a *como* os dados são obtidos:
  - `remote` (Retrofit, services, DTOs) — comunicação com a internet.
  - `repository` — decide de onde vêm os dados (veremos a fundo no Módulo 3.04).
- **domain** — o que a aplicação *entende* como seus conceitos de negócio:
  - `model` (modelos de domínio) — classes que representam "o que é um Post" para o app, sem depender de como os dados chegaram até ali.
- **presentation** — tudo relacionado a *mostrar* os dados na tela:
  - `post` (ViewModel e UI/Compose).

---

## Parte 1: Instância do Retrofit (camada data/remote)

O `ApiClient` é um objeto singleton (uma única instância compartilhada por todo o app) responsável por criar e fornecer a instância do Retrofit. Configuramos aqui o OkHttp (cliente HTTP que faz a conexão de fato), o Gson (conversor JSON → objeto Kotlin) e os timeouts (tempo máximo de espera antes de desistir de uma requisição).

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

// "object" cria um singleton: só existe UMA instância de ApiClient em todo o app.
object ApiClient {
    // Endereço base da API. Todos os endpoints (ex.: "posts") são somados a ele.
    private const val BASE_URL = "https://jsonplaceholder.typicode.com/"

    // Interceptor: um "espião" que registra no Logcat cada requisição/resposta HTTP.
    // Útil para depurar problemas de rede durante o desenvolvimento.
    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    // OkHttpClient é o motor HTTP de verdade por trás do Retrofit.
    private val okHttp = OkHttpClient.Builder()
        .addInterceptor(logging)                    // Anexa o "espião" de logs.
        .connectTimeout(15, TimeUnit.SECONDS)        // Tempo máx. para abrir a conexão.
        .readTimeout(20, TimeUnit.SECONDS)           // Tempo máx. esperando a resposta.
        .build()

    // Gson converte JSON <-> objetos Kotlin. setLenient() tolera JSON levemente malformado.
    private val gson = GsonBuilder()
        .setLenient()
        .create()

    // Monta o Retrofit: precisa de baseUrl, um cliente HTTP e um conversor de dados.
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttp)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    // "by lazy" cria o serviço só na primeira vez que for usado, e reaproveita depois.
    // retrofit.create() gera automaticamente a implementação da interface PostService.
    val postService: PostService by lazy {
        retrofit.create(PostService::class.java)
    }
}
```

---

## Parte 2: Modelos (DTO e Domain)

É uma boa prática separar o modelo de dados da rede (DTO — *Data Transfer Object*) do modelo de domínio. O DTO reflete exatamente o JSON recebido (mesmos nomes de campo que a API usa); o modelo de domínio representa o conceito de negócio do jeito que o resto do app prefere trabalhar, independente da fonte de dados. Essa separação evita que uma mudança na API (por exemplo, a API renomear um campo) quebre o app inteiro — você só ajusta o ponto de conversão.

```kotlin
// data/remote/dto/PostDto.kt
package com.example.retrofitdemo.data.remote.dto

import com.google.gson.annotations.SerializedName

// DTO: espelha exatamente os campos que a API retorna em JSON.
data class PostDto(
    // @SerializedName mapeia o nome do campo no JSON para a propriedade Kotlin.
    // Aqui os nomes já são iguais, mas a anotação deixa isso explícito e documentado.
    @SerializedName("userId") val userId: Int,
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String,
    @SerializedName("body") val body: String
)
```

```kotlin
// domain/model/Post.kt
package com.example.retrofitdemo.domain.model

// Modelo de domínio: o que o restante do app enxerga como "um Post".
// Note que "userId" virou "authorId" — um nome mais claro para quem usa esse modelo.
data class Post(
    val id: Int,
    val title: String,
    val body: String,
    val authorId: Int
)
```

---

## Parte 3: Service (endpoints com suspend fun)

O `PostService` é a interface que descreve os endpoints da API. Você não escreve a implementação — o Retrofit gera automaticamente o código real em tempo de execução, a partir das anotações. Usamos `suspend fun` (função suspensa, veja o Módulo 3.01) para que as chamadas de rede possam ser executadas de forma assíncrona com coroutines, sem travar a UI.

```kotlin
// data/remote/service/PostService.kt
package com.example.retrofitdemo.data.remote.service

import com.example.retrofitdemo.data.remote.dto.PostDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface PostService {
    // @GET define o verbo HTTP e o caminho do endpoint (some com a BASE_URL).
    // "suspend" permite chamar essa função de dentro de uma coroutine e aguardar o resultado.
    @GET("posts")
    suspend fun getPosts(): List<PostDto>

    // @Path substitui "{id}" na URL pelo valor passado como argumento.
    // Ex.: getPost(5) chama "posts/5".
    @GET("posts/{id}")
    suspend fun getPost(@Path("id") id: Int): PostDto

    // @Query adiciona um parâmetro de URL (?userId=valor), comum em filtros.
    @GET("posts")
    suspend fun getPostsByUser(@Query("userId") userId: Int): List<PostDto>
}
```

---

## Parte 4: Repository (camada data/repository)

O Repository é o mediador entre as fontes de dados (remota, local) e o ViewModel. Ele abstrai a origem dos dados, permitindo que o ViewModel apenas solicite "os posts" sem se preocupar se vieram da rede, do banco local, etc. Vamos aprofundar esse padrão no Módulo 3.04 — aqui usamos uma versão simples, só com a rede.

```kotlin
// data/repository/PostRepository.kt
package com.example.retrofitdemo.data.repository

import com.example.retrofitdemo.domain.model.Post

// A interface descreve O QUE o repositório faz, sem dizer COMO.
// Isso facilita testes: em testes automatizados, criamos uma implementação "fake".
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

// Implementação real: recebe o PostService (a interface do Retrofit) por construtor.
class PostRepositoryImpl(
    private val service: PostService
) : PostRepository {

    override suspend fun getPosts(): List<Post> {
        // service.getPosts() retorna List<PostDto>; .map converte cada DTO em Post (domínio).
        return service.getPosts().map {
            Post(
                id = it.id,
                title = it.title,
                body = it.body,
                authorId = it.userId // Aqui acontece a "tradução" userId -> authorId.
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

---

## Parte 5: ViewModel (StateFlow + coroutines)

O ViewModel expõe o estado da UI como `StateFlow` (um "container" observável de estado — veja o Módulo 3.05 para se aprofundar em Flow) e usa `viewModelScope` para lançar coroutines com ciclo de vida seguro. O uso de `runCatching` simplifica o tratamento de sucesso/falha sem blocos `try/catch` explícitos: ele executa o bloco e devolve um resultado que pode ser tratado com `.onSuccess { }` e `.onFailure { }`.

Primeiro, definimos os possíveis estados da tela com uma `sealed interface` — um tipo que só pode assumir um conjunto fechado e conhecido de formas (aqui: carregando, sucesso ou erro), o que obriga o código que lê o estado a tratar todos os casos:

```kotlin
// presentation/post/PostUiState.kt
package com.example.retrofitdemo.presentation.post

import com.example.retrofitdemo.domain.model.Post

// sealed interface: a tela só pode estar em UM destes três estados por vez.
sealed interface PostUiState {
    data object Loading : PostUiState                    // Carregando (sem dados ainda).
    data class Success(val posts: List<Post>) : PostUiState // Sucesso, com a lista de posts.
    data class Error(val message: String) : PostUiState     // Falhou, com mensagem de erro.
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

    // _uiState é mutável e privado: só o ViewModel pode alterá-lo.
    private val _uiState = MutableStateFlow<PostUiState>(PostUiState.Loading)
    // uiState é a versão pública, somente leitura, que a UI observa.
    val uiState: StateFlow<PostUiState> = _uiState.asStateFlow()

    init { loadPosts() } // Busca os posts assim que o ViewModel é criado.

    fun loadPosts() {
        // viewModelScope cancela automaticamente a coroutine se a tela for destruída.
        viewModelScope.launch {
            _uiState.value = PostUiState.Loading
            runCatching { repository.getPosts() }
                .onSuccess { _uiState.value = PostUiState.Success(it) }
                .onFailure { _uiState.value = PostUiState.Error(it.message ?: "Erro inesperado") }
        }
    }
}
```

---

## Parte 6: UI com Jetpack Compose

A tela observa o `StateFlow` do ViewModel e renderiza cada estado (carregando, erro ou sucesso) de forma declarativa — ou seja, você descreve *como a tela deve parecer para cada estado*, e o Compose cuida de atualizar a tela quando o estado muda.

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
    // collectAsStateWithLifecycle observa o StateFlow e para de coletar quando a
    // tela vai para segundo plano, evitando trabalho desnecessário e vazamentos.
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // "when" sobre a sealed interface: o compilador obriga a tratar todos os casos.
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
        // "key = { it.id }" ajuda o Compose a identificar cada item de forma
        // estável, mesmo que a lista seja reordenada — melhora performance.
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

---

## Factory extraída

Antes de existir o Hilt (Módulo 3.07), o Android não sabe automaticamente como criar um `PostViewModel` que precisa de um `PostRepository` no construtor. A `Factory` é uma "receita" que ensina o sistema a construir esse ViewModel manualmente.

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

---

## Activity

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

---

## Parte 7: Erros, logging e boas práticas

- **Logging**: o `HttpLoggingInterceptor` no nível `BASIC` mostra no Logcat a URL, o método e o código de status de cada requisição. Em modo debug, você pode trocar para `BODY` para ver o JSON completo — útil para depurar, mas nunca deixe isso ativo em builds de produção (pode vazar dados sensíveis nos logs).
- **Tratamento de exceções**: duas exceções aparecem com frequência ao trabalhar com Retrofit — `HttpException` (o servidor respondeu, mas com um código de erro, como 404 ou 500) e `IOException` (a requisição nem chegou a ter resposta, geralmente por falta de internet ou timeout). Trate as duas de forma diferenciada quando quiser mostrar mensagens de erro específicas ao usuário.
- **Separar DTO de Domain**: mantém isolamento sem precisar de um arquivo "mapper" dedicado quando a transformação é simples (como fizemos com `.map { }` direto no repository).
- **Evitar trabalho pesado na UI thread**: se você precisar processar uma resposta grande (ordenar, filtrar milhares de itens), use `withContext(Dispatchers.Default)` para não travar a interface (veja Módulo 3.01).

Exemplo de uso com `Response<T>` (quando você quer inspecionar o código HTTP manualmente em vez de deixar o Retrofit lançar exceção automaticamente):

```kotlin
// Service
@GET("posts")
suspend fun getPostsResponse(): retrofit2.Response<List<PostDto>>

// Repository
val response = service.getPostsResponse()
if (response.isSuccessful) {
    // response.body() pode ser nulo mesmo em sucesso (ex.: resposta 204) — orEmpty() protege disso.
    val list = response.body().orEmpty().map {
        Post(
            id = it.id,
            title = it.title,
            body = it.body,
            authorId = it.userId
        )
    }
} else {
    // isSuccessful é false para qualquer código 4xx ou 5xx.
    throw IllegalStateException("HTTP ${response.code()}: ${response.message()}")
}
```

## Parte 8: Evoluindo o exemplo

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
  - Service: `getPost(id)`.
  - Novo `UiState` de detalhe ou outra ViewModel dedicada à tela de detalhe.

- DI com Hilt (opcional, veja Módulo 3.07):
  - Módulos para Retrofit, Service e Repository, eliminando a `Factory` manual.

---

## Erros Comuns / Pegadinhas

1. **Esquecer a barra final na `BASE_URL`.** O Retrofit exige que a URL base termine com `/` (ex.: `"https://jsonplaceholder.typicode.com/"`, não `"...typicode.com"`). Sem a barra, o app quebra em tempo de execução com `IllegalArgumentException`, mesmo que o código compile normalmente.

2. **Achar que qualquer resposta HTTP vira exceção automaticamente.** Quando você declara `suspend fun getPosts(): List<PostDto>` (sem `Response<T>`), o Retrofit já lança `HttpException` para respostas de erro (4xx/5xx) — mas isso só acontece dentro de um `try/catch` ou de um `runCatching`. Se você não tratar isso em algum lugar da cadeia, o app pode crashar.

3. **Não diferenciar erro de rede (`IOException`) de erro de servidor (`HttpException`).** Mostrar "Erro desconhecido" para os dois casos deixa o usuário sem saber se o problema é a internet dele ou o servidor. Sempre que possível, trate os dois tipos separadamente com mensagens específicas.

4. **Chamar uma função `suspend` do Retrofit fora de uma coroutine.** Como vimos no Módulo 3.01, isso simplesmente não compila — o compilador Kotlin já evita esse erro antes de você rodar o app.

---

## Resumo

| Conceito | Para que serve |
|---|---|
| `interface` com `@GET`/`@POST`/... | Descreve os endpoints da API de forma declarativa |
| `suspend fun` nos endpoints | Permite chamar a API sem travar a UI |
| `Converter` (Gson) | Converte JSON em objetos Kotlin e vice-versa |
| DTO | Espelha o JSON exato da API |
| Modelo de domínio | Representa o conceito de negócio, isolado da rede |
| `Repository` | Abstrai de onde os dados vêm para o ViewModel |
| `StateFlow` no ViewModel | Expõe o estado da tela de forma observável |
| `HttpException` / `IOException` | Erros de servidor vs. erros de conexão |

**Dicas finais:**
- `baseUrl` sempre com barra final.
- Funções de rede sempre como `suspend`.
- `StateFlow` para reatividade da UI.
- Testes de rede com `MockWebServer` (biblioteca que simula um servidor HTTP local).
- Ajuste os timeouts e as mensagens de erro pensando na experiência do usuário.

**Próximo passo:** no arquivo `03_persistencia_room.md` você vai aprender a guardar esses dados localmente no dispositivo com o Room, para que o app funcione mesmo sem internet e não precise buscar tudo de novo a cada abertura.

Com isso, a app lista posts usando Retrofit + Gson com coroutines e Compose, sem camada de mapper dedicada (conversão inline no repository).
