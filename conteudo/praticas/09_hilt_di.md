# Prática: Injeção de Dependência com Hilt

Este guia apresenta exercícios práticos para configurar e utilizar o **Hilt**, a biblioteca recomendada pelo Google para injeção de dependência no Android.

**Pré-requisito: Módulo 3, Aula 07 — Hilt**

---

## O que é o Hilt?

O Hilt é um framework de injeção de dependência construído sobre o Dagger. Ele elimina a necessidade de criar dependências manualmente, gerenciando o ciclo de vida dos objetos automaticamente. Em vez de instanciar classes com `val repo = Repository(api, dao)`, você simplesmente anota com `@Inject` e o Hilt cuida do resto.

---

## Configuração

Adicione ao `build.gradle.kts` do **projeto** (raiz):

```kotlin
plugins {
    // Plugin do Hilt no nível do projeto
    id("com.google.dagger.hilt.android") version "2.51.1" apply false
}
```

Adicione ao `build.gradle.kts` do **módulo** (`app`):

```kotlin
plugins {
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

dependencies {
    // Hilt — injeção de dependência
    implementation("com.google.dagger:hilt-android:2.51.1")
    ksp("com.google.dagger:hilt-compiler:2.51.1")

    // Integração do Hilt com ViewModel
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Retrofit + conversor JSON
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

    // Room — banco de dados local
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // ViewModel e Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
}
```

---

## Exercício 1: Configuração Básica do Hilt

### Objetivo

Configurar o Hilt no projeto e injetar um repositório simples no ViewModel.

Conforme um app cresce, criar objetos manualmente (`val repo = Repository(Api(), Database())`) vira um problema: cada tela precisa saber montar todas as dependências de tudo que usa, e trocar uma implementação (por exemplo, para testes) exige mexer em vários lugares. Injeção de dependência resolve isso invertendo o controle: você declara "eu preciso de um `Repository`" e o Hilt entrega uma instância pronta, sabendo montar toda a cadeia de dependências por trás dela. Esse é o padrão usado em praticamente todo app Android profissional de porte médio para cima.

### Passo a Passo

**1. Application com Hilt** (`MeuApp.kt`):

```kotlin
import android.app.Application
import dagger.hilt.android.HiltAndroidApp

// @HiltAndroidApp é obrigatório — ativa a geração de código do Hilt
@HiltAndroidApp
class MeuApp : Application()
```

> **Importante:** Registre a classe no `AndroidManifest.xml` com `android:name=".MeuApp"`.

**2. Repositório simples** (`SaudacaoRepository.kt`):

```kotlin
import javax.inject.Inject

// @Inject no construtor permite que o Hilt crie essa classe automaticamente
class SaudacaoRepository @Inject constructor() {

    // Lista de saudações para simular uma fonte de dados
    private val saudacoes = listOf(
        "Olá, mundo!",
        "Bem-vindo ao Hilt!",
        "Injeção de dependência é poderosa!",
        "Android com Kotlin é incrível!"
    )

    // Retorna uma saudação aleatória da lista
    fun buscarSaudacao(): String = saudacoes.random()
}
```

**3. ViewModel com injeção** (`SaudacaoViewModel.kt`):

```kotlin
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

// @HiltViewModel permite que o Hilt injete dependências no ViewModel
@HiltViewModel
class SaudacaoViewModel @Inject constructor(
    private val repository: SaudacaoRepository // Injetado automaticamente pelo Hilt
) : ViewModel() {

    // Estado observável pela UI
    private val _saudacao = MutableStateFlow(repository.buscarSaudacao())
    val saudacao: StateFlow<String> = _saudacao.asStateFlow()

    // Atualiza a saudação buscando uma nova do repositório
    fun novaSaudacao() {
        _saudacao.value = repository.buscarSaudacao()
    }
}
```

**4. Activity com Hilt** (`MainActivity.kt`):

```kotlin
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint

// @AndroidEntryPoint habilita a injeção de dependência nesta Activity
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // A tela já pode usar o ViewModel com Hilt
            SaudacaoScreen()
        }
    }
}
```

**5. Tela Compose** (`SaudacaoScreen.kt`):

```kotlin
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SaudacaoScreen(
    // hiltViewModel() cria o ViewModel com as dependências injetadas
    viewModel: SaudacaoViewModel = hiltViewModel()
) {
    val saudacao by viewModel.saudacao.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Exibe a saudação atual
        Text(
            text = saudacao,
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(16.dp))

        // Botão para buscar uma nova saudação
        Button(onClick = { viewModel.novaSaudacao() }) {
            Text("Nova saudação 🎲")
        }
    }
}
```

> **💡 Por trás dos panos**
> Quando você marca `SaudacaoRepository` com `@Inject constructor()`, está ensinando o Hilt a criar essa classe sozinho, sem precisar de um `@Module` explícito — o Hilt só olha para o construtor e resolve o que ele precisa recursivamente. Já o `@HiltViewModel` + `hiltViewModel()` conectam esse mecanismo ao ciclo de vida do Android: o Hilt cria o `SaudacaoViewModel` (e injeta o `SaudacaoRepository` dentro dele) automaticamente na primeira vez que a tela pede por ele, e reaproveita a mesma instância enquanto a tela estiver viva — sem você escrever nenhum código de "fábrica" manual.

### Exercícios

1. Adicione um contador que exiba quantas saudações já foram geradas desde que o app abriu.
   - *Dica se travar*: adicione uma variável mutável no ViewModel (ou outro `MutableStateFlow<Int>`) que incrementa a cada chamada de `novaSaudacao()`.
2. Crie um segundo repositório (`FraseMotivacionalRepository`) e injete ambos no ViewModel, alternando entre saudações e frases motivacionais.
   - *Dica se travar*: basta adicionar um segundo parâmetro ao construtor do ViewModel (`private val fraseRepository: FraseMotivacionalRepository`) — o Hilt já resolve as duas dependências automaticamente.

---

## Exercício 2: Módulos Hilt com Retrofit e Room

### Objetivo

Criar módulos Hilt que fornecem instâncias de Retrofit e Room usando `@Module`, `@Provides` e `@Singleton`.

O Hilt consegue criar sozinho classes que você mesmo escreveu (usando `@Inject constructor`), mas não sabe como montar classes de bibliotecas externas, como `Retrofit` ou `RoomDatabase` — elas não têm um construtor simples que o Hilt possa simplesmente chamar. Para esses casos, você usa um `@Module`: um "manual de instruções" que ensina o Hilt a construir esse tipo de objeto. Esse é um dos usos mais comuns do Hilt no dia a dia, porque toda vez que seu app usa Retrofit, Room ou qualquer outra biblioteca configurável, você vai precisar desse padrão.

### Passo a Passo

**1. Entidade Room** (`TarefaEntity.kt`):

```kotlin
import androidx.room.Entity
import androidx.room.PrimaryKey

// Entidade que representa a tabela "tarefas" no banco de dados
@Entity(tableName = "tarefas")
data class TarefaEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val titulo: String,
    val concluida: Boolean = false
)
```

**2. DAO** (`TarefaDao.kt`):

```kotlin
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// DAO define as operações de acesso ao banco de dados
@Dao
interface TarefaDao {
    // Retorna todas as tarefas como Flow (reativo)
    @Query("SELECT * FROM tarefas ORDER BY id DESC")
    fun observarTodas(): Flow<List<TarefaEntity>>

    // Insere uma tarefa no banco
    @Insert
    suspend fun inserir(tarefa: TarefaEntity)

    // Atualiza uma tarefa existente
    @Update
    suspend fun atualizar(tarefa: TarefaEntity)
}
```

**3. Banco de dados Room** (`AppDatabase.kt`):

```kotlin
import androidx.room.Database
import androidx.room.RoomDatabase

// Classe abstrata do banco — o Room gera a implementação
@Database(entities = [TarefaEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tarefaDao(): TarefaDao
}
```

**4. Interface de API** (`TarefaApi.kt`):

```kotlin
import retrofit2.http.GET

// Modelo que vem da API
data class TarefaRemota(
    val id: Int,
    val title: String,
    val completed: Boolean
)

// Interface Retrofit para buscar tarefas remotas
interface TarefaApi {
    @GET("todos?_limit=20")
    suspend fun buscarTarefas(): List<TarefaRemota>
}
```

**5. Módulo de Rede** (`NetworkModule.kt`):

```kotlin
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

// @Module indica que esta classe fornece dependências ao Hilt
// @InstallIn(SingletonComponent) define que as dependências vivem durante todo o ciclo do app
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // @Provides diz ao Hilt como criar a instância do Retrofit
    // @Singleton garante que apenas UMA instância seja criada
    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://jsonplaceholder.typicode.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // Usa o Retrofit fornecido acima para criar o serviço da API
    @Provides
    @Singleton
    fun provideTarefaApi(retrofit: Retrofit): TarefaApi {
        return retrofit.create(TarefaApi::class.java)
    }
}
```

**6. Módulo do Banco de Dados** (`DatabaseModule.kt`):

```kotlin
import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    // @ApplicationContext injeta o contexto do aplicativo automaticamente
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "tarefas_db" // Nome do arquivo do banco de dados
        ).build()
    }

    // Fornece o DAO a partir do banco de dados já criado
    @Provides
    fun provideTarefaDao(database: AppDatabase): TarefaDao {
        return database.tarefaDao()
    }
}
```

> **💡 Por trás dos panos**
> `@Provides` funciona como uma "receita": toda vez que alguém no app precisar de um `Retrofit`, o Hilt executa a função `provideRetrofit()` para gerar essa instância. O `@Singleton` altera esse comportamento — em vez de rodar a receita toda vez, o Hilt executa uma única vez e guarda o resultado, entregando a mesma instância para todo mundo que pedir depois. Isso é importante para objetos "caros" de criar (como uma conexão de banco de dados) ou que precisam manter estado compartilhado (como um cache de rede) — recriá-los a cada uso desperdiçaria recursos.

### Exercícios

1. Adicione um `OkHttpClient` com `HttpLoggingInterceptor` ao módulo de rede para exibir logs das requisições no Logcat.
   - *Dica se travar*: crie um método `@Provides @Singleton fun provideOkHttpClient(): OkHttpClient` que retorna o client com o interceptor, e passe-o para o `Retrofit.Builder().client(okHttpClient)`.
2. Crie um `@Qualifier` customizado para diferenciar duas URLs base diferentes (por exemplo, `@BaseUrl` e `@AuthUrl`), cada uma com sua própria instância de Retrofit.
3. Mude o escopo do `TarefaDao` para `@Singleton` e observe se o comportamento muda. Reflita sobre quando usar ou não `@Singleton`.
   - *Dica se travar*: como o `TarefaDao` já vem de um `AppDatabase` que é `@Singleton`, marcar o DAO também como `@Singleton` normalmente não muda nada na prática — pense em por que isso acontece.

---

## Exercício 3: App Completo — ViewModel + Repository + Room + Retrofit

### Objetivo

Construir um app funcional de lista de tarefas que busca dados da API, salva no banco local e exibe na tela usando Compose — tudo conectado pelo Hilt.

Este exercício junta tudo que você viu nos guias anteriores — Retrofit, Room, MVVM, coroutines — e mostra como o Hilt amarra as peças sem que você precise "encanar" manualmente cada dependência em cada tela. É um retrato bem próximo de como um app Android profissional real é estruturado: camadas bem definidas (API, banco, repositório, ViewModel, UI), cada uma dependendo apenas da anterior, e o Hilt cuidando de toda a fiação por trás das cenas.

### Passo a Passo

**1. Repositório unificado** (`TarefaRepository.kt`):

```kotlin
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

// O Hilt injeta tanto a API quanto o DAO automaticamente
class TarefaRepository @Inject constructor(
    private val api: TarefaApi,
    private val dao: TarefaDao
) {
    // Observa as tarefas do banco local (reativo com Flow)
    fun observarTarefas(): Flow<List<TarefaEntity>> = dao.observarTodas()

    // Busca tarefas da API e salva no banco local
    suspend fun sincronizar() {
        val remotas = api.buscarTarefas()
        // Converte cada tarefa remota para a entidade do Room
        remotas.forEach { remota ->
            val entidade = TarefaEntity(
                id = remota.id,
                titulo = remota.title,
                concluida = remota.completed
            )
            dao.inserir(entidade)
        }
    }

    // Alterna o estado de conclusão de uma tarefa
    suspend fun alternarConclusao(tarefa: TarefaEntity) {
        dao.atualizar(tarefa.copy(concluida = !tarefa.concluida))
    }
}
```

**2. Estado da UI** (`TarefaUiState.kt`):

```kotlin
// Representa os possíveis estados da tela de tarefas
sealed interface TarefaUiState {
    data object Carregando : TarefaUiState
    data class Sucesso(val tarefas: List<TarefaEntity>) : TarefaUiState
    data class Erro(val mensagem: String) : TarefaUiState
}
```

**3. ViewModel completo** (`TarefaViewModel.kt`):

```kotlin
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TarefaViewModel @Inject constructor(
    private val repository: TarefaRepository // Injetado pelo Hilt
) : ViewModel() {

    // Estado da tela exposto como StateFlow
    val uiState: StateFlow<TarefaUiState> = repository
        .observarTarefas()
        .map { tarefas ->
            // Converte a lista de entidades para estado de sucesso
            TarefaUiState.Sucesso(tarefas) as TarefaUiState
        }
        .catch { e ->
            // Em caso de erro, emite estado de erro
            emit(TarefaUiState.Erro(e.message ?: "Erro desconhecido"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TarefaUiState.Carregando
        )

    init {
        // Sincroniza com a API ao iniciar o ViewModel
        sincronizar()
    }

    // Busca dados da API e salva localmente
    fun sincronizar() {
        viewModelScope.launch {
            try {
                repository.sincronizar()
            } catch (e: Exception) {
                // Erro de rede não impede exibir dados locais
            }
        }
    }

    // Marca ou desmarca uma tarefa como concluída
    fun alternarConclusao(tarefa: TarefaEntity) {
        viewModelScope.launch {
            repository.alternarConclusao(tarefa)
        }
    }
}
```

**4. Tela Compose completa** (`TarefaScreen.kt`):

```kotlin
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TarefaScreen(
    // hiltViewModel() resolve todas as dependências automaticamente
    viewModel: TarefaViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("📋 Minhas Tarefas") })
        },
        // Botão flutuante para sincronizar com a API
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.sincronizar() }) {
                Text("🔄")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            when (val state = uiState) {
                is TarefaUiState.Carregando -> {
                    // Indicador de carregamento centralizado
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is TarefaUiState.Erro -> {
                    // Mensagem de erro com botão para tentar novamente
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "❌ ${state.mensagem}",
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { viewModel.sincronizar() }) {
                            Text("Tentar novamente")
                        }
                    }
                }

                is TarefaUiState.Sucesso -> {
                    if (state.tarefas.isEmpty()) {
                        // Mensagem quando não há tarefas
                        Text(
                            text = "Nenhuma tarefa encontrada.\nToque em 🔄 para sincronizar.",
                            modifier = Modifier.align(Alignment.Center),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    } else {
                        // Lista de tarefas com LazyColumn
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.tarefas, key = { it.id }) { tarefa ->
                                TarefaItem(
                                    tarefa = tarefa,
                                    onAlternar = { viewModel.alternarConclusao(tarefa) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TarefaItem(
    tarefa: TarefaEntity,
    onAlternar: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox para marcar como concluída
            Checkbox(
                checked = tarefa.concluida,
                onCheckedChange = { onAlternar() }
            )
            Spacer(Modifier.width(12.dp))
            // Título com risco se a tarefa foi concluída
            Text(
                text = tarefa.titulo,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (tarefa.concluida) {
                    TextDecoration.LineThrough
                } else {
                    TextDecoration.None
                }
            )
        }
    }
}
```

> **💡 Por trás dos panos**
> Repare na cadeia de dependências: `TarefaViewModel` depende de `TarefaRepository`, que depende de `TarefaApi` e `TarefaDao`, que por sua vez dependem de `Retrofit` e `AppDatabase`. Você nunca escreveu o código que monta essa cadeia inteira — só anotou cada peça com `@Inject` ou `@Provides`, e o Hilt resolveu a ordem certa de criação sozinho (chamado de "grafo de dependências"). Essa é a maior vantagem prática da injeção de dependência: trocar uma peça (por exemplo, usar um repositório falso em testes) não exige mexer em nenhuma das outras camadas.

### Exercícios

1. Adicione um campo de texto e um botão para criar novas tarefas localmente (sem enviar para a API).
   - *Dica se travar*: crie uma função `criarTarefaLocal(titulo: String)` no repositório que insere direto no `dao`, sem passar pela `api`.
2. Implemente a funcionalidade de excluir uma tarefa com gesto de deslizar (swipe-to-delete).
3. Adicione um filtro com chips (`FilterChip`) para exibir "Todas", "Pendentes" ou "Concluídas".
   - *Dica se travar*: reaproveite a lógica de filtro que você já praticou no guia `04_mvvm_stateflow.md` (Prática 2, exercício 1) — o padrão é o mesmo.

---

## Resumo

```
@HiltAndroidApp        →  ativa o Hilt no Application
@AndroidEntryPoint     →  habilita injeção em Activity/Fragment
@HiltViewModel         →  permite injeção no ViewModel
@Inject constructor    →  marca classe como injetável
@Module + @Provides    →  ensina o Hilt a criar dependências externas
@Singleton             →  garante instância única no ciclo do app
hiltViewModel()        →  cria ViewModel com Hilt no Compose
```

| Anotação | O que faz |
|----------|-----------|
| `@HiltAndroidApp` | Gera o componente raiz do Hilt na classe Application |
| `@AndroidEntryPoint` | Habilita injeção em Activity, Fragment ou Service |
| `@HiltViewModel` | Permite que o Hilt crie e injete dependências no ViewModel |
| `@Inject` | Marca um construtor ou campo para receber injeção |
| `@Module` | Classe que agrupa métodos `@Provides` ou `@Binds` |
| `@InstallIn` | Define em qual componente (escopo) o módulo será instalado |
| `@Provides` | Ensina o Hilt a criar uma instância de uma classe externa |
| `@Singleton` | Garante que apenas uma instância será criada durante o ciclo do app |

---

## Próximos Passos

- Revise o módulo `08_retrofit_api.md` para comparar a versão sem Hilt e com Hilt.
- Explore `@Binds` como alternativa a `@Provides` para interfaces com uma única implementação.
- Estude escopos avançados como `@ViewModelScoped` e `@ActivityScoped` para controlar o ciclo de vida das dependências.
