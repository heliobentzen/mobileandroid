# Room + ViewModel + Coroutines + Compose (essencial)

## O que é o Room e por que ele existe?

Toda variável na memória do seu app (uma lista, um objeto, um `State` do Compose) desaparece assim que o processo do app é encerrado — seja porque o usuário fechou o app, o sistema precisou liberar memória, ou o celular reiniciou. Se você guarda uma lista de tarefas só em memória, ela some. O usuário abre o app de novo e a lista está vazia. Isso é uma péssima experiência.

O **Room** é a biblioteca oficial do Android para persistência local de dados: ele guarda informações em um banco de dados **SQLite** (um banco de dados relacional que já vem embutido no sistema operacional Android) de forma estruturada, tipada e segura. Pense no Room como um arquivista extremamente organizado: você entrega os "documentos" (seus dados) em formato Kotlin, ele cuida de arquivá-los em gavetas (tabelas) e sabe exatamente onde encontrar cada um quando você pedir de volta — mesmo que o app tenha sido fechado e reaberto dias depois.

## Por que isso importa?

Sem persistência local, qualquer app que dependa de dados teria dois problemas sérios:

1. **Perda de dados ao fechar o app.** Uma lista de tarefas, notas ou favoritos precisa sobreviver ao fechamento do app — é isso que o usuário espera de qualquer aplicativo sério.
2. **Dependência total de internet.** Sem um cache local, toda tela precisaria buscar dados da rede toda vez que fosse aberta — lento, caro em dados móveis, e o app simplesmente não funciona offline.

O Room resolve os dois problemas: ele guarda os dados localmente e permite consultar (ou até observar mudanças) sem depender da rede.

**Objetivo deste arquivo:** configurar o Room e montar o fluxo mínimo de dados em camadas (Entity → DAO → Database → Repository → ViewModel → UI Compose) com coroutines e Flow, de forma simples.

---

## 1) Dependências (module build.gradle)

Use as versões estáveis mais recentes.

```kotlin
plugins {
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose") // obrigatório a partir do Kotlin 2.0
    id("com.google.devtools.ksp") // KSP: mais rápido que KAPT para processamento de anotações
}

dependencies {
    // Room (prefira KSP ao KAPT: mais rápido e sem warnings de depreciação)
    implementation("androidx.room:room-runtime:2.7.0")
    implementation("androidx.room:room-ktx:2.7.0")         // suporte a Coroutines e Flow
    ksp("androidx.room:room-compiler:2.7.0")               // gerador de código

    // Lifecycle + ViewModel + coroutines
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

}
```

> **O que é KSP?** É um processador de anotações (Kotlin Symbol Processing) que gera código Kotlin automaticamente em tempo de compilação a partir das suas anotações (`@Entity`, `@Dao`, etc.). O Room usa isso para escrever, por trás dos panos, todo o código SQL e a implementação das interfaces `@Dao` — você nunca vê esse código gerado, mas ele existe e é compilado junto com o seu app.

## 2) Camadas mínimas

O Room organiza a persistência em três peças que trabalham juntas: a **Entity** (o que vai ser guardado), o **DAO** (como acessar os dados) e a **Database** (o banco que junta tudo).

### Entity

Uma `@Entity` é uma classe Kotlin que representa **uma tabela** do banco de dados. Cada instância da classe vira **uma linha** dessa tabela, e cada propriedade vira **uma coluna**.

```kotlin
import androidx.room.Entity
import androidx.room.PrimaryKey

// @Entity marca esta classe como uma tabela do banco. tableName define o nome da tabela.
@Entity(tableName = "tasks")
data class Task(
    // @PrimaryKey identifica unicamente cada linha. autoGenerate = true faz o
    // Room gerar o próximo ID automaticamente (1, 2, 3...) a cada inserção.
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val done: Boolean = false
)
```

### DAO (Data Access Object)

O **DAO** ("Objeto de Acesso a Dados") é uma interface onde você declara *o que* quer fazer com o banco (buscar, inserir, atualizar, apagar), usando anotações e SQL. O Room gera a implementação real por trás dos panos — você nunca escreve o código que efetivamente conversa com o SQLite.

Duas convenções importantes:
- **Leituras** costumam retornar `Flow`, para que a UI seja notificada automaticamente sempre que os dados mudarem no banco (sem precisar buscar de novo manualmente).
- **Escritas** (inserir, atualizar, apagar) são funções `suspend`, porque acessar o disco é uma operação de I/O que pode demorar e não pode travar a UI thread (veja Módulo 3.01).

```kotlin
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// @Dao marca esta interface como um DAO. O Room gera a implementação automaticamente.
@Dao
interface TaskDao {
    // @Query executa SQL puro. Como retorna Flow, o Room reemite a lista
    // automaticamente sempre que a tabela "tasks" for alterada — sem polling.
    @Query("SELECT * FROM tasks ORDER BY id DESC")
    fun getAll(): Flow<List<Task>>

    // @Insert insere a entidade. OnConflictStrategy.REPLACE sobrescreve se já
    // existir uma linha com o mesmo id (útil para "upsert": inserir ou atualizar).
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: Task)

    // @Delete remove a linha correspondente à entidade passada.
    @Delete
    suspend fun delete(task: Task)

    // @Query também aceita comandos UPDATE com parâmetros (:done, :id são
    // preenchidos pelos argumentos da função, evitando SQL injection).
    @Query("UPDATE tasks SET done = :done WHERE id = :id")
    suspend fun setDone(id: Long, done: Boolean)
}
```

### Database

A classe `@Database` é o ponto de entrada do Room: ela declara quais entidades existem e fornece acesso aos DAOs. Ela deve ser um **singleton** — ou seja, deve existir só uma instância dela em todo o app, porque abrir várias conexões com o mesmo arquivo de banco pode causar problemas de concorrência e desperdício de memória.

```kotlin
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// @Database lista todas as entidades (tabelas) e a versão do schema (estrutura do banco).
@Database(entities = [Task::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    // O Room implementa este método automaticamente, retornando um TaskDao funcional.
    abstract fun taskDao(): TaskDao

    companion object {
        // @Volatile garante que mudanças nesta variável sejam visíveis para todas
        // as threads imediatamente — importante para o padrão singleton com threads.
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            // "synchronized" evita que duas threads criem duas instâncias ao mesmo tempo
            // (uma corrida rara, mas possível, na primeira chamada).
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app.db" // Nome do arquivo do banco no armazenamento do dispositivo.
                )
                .fallbackToDestructiveMigration() // simples para dev — CUIDADO em produção, veja "Erros Comuns"
                .build()
                .also { INSTANCE = it }
            }
    }
}
```

### Repository (API pública + cache Room)

Usa a API pública DummyJSON (https://dummyjson.com/todos) para CRUD (Create, Read, Update, Delete — as quatro operações básicas de persistência) e o Room como cache e fonte de verdade para a UI. A ideia: a tela sempre lê do Room (rápido, funciona offline); o Repository sincroniza o Room com o servidor em segundo plano.

Observação (build.gradle): adicione Retrofit (veja `02_retrofit.md` para entender cada peça em detalhe)
```kotlin
dependencies {
    implementation("com.squareup.retrofit2:retrofit:<versão>")
    implementation("com.squareup.retrofit2:converter-gson:<versão>")
}
```

API + DTOs — classes que espelham exatamente o formato JSON da API remota (veja a explicação de DTO no Módulo 3.02):
```kotlin
import retrofit2.http.*

data class RemoteTodo(
    val id: Long,
    val todo: String,
    val completed: Boolean
)

data class RemoteTodoList(
    val todos: List<RemoteTodo>
)

data class AddTodoBody(
    val todo: String,
    val completed: Boolean = false,
    val userId: Int = 1 // requerido pela API
)

data class UpdateTodoBody(
    val todo: String? = null,
    val completed: Boolean? = null
)

interface TaskApi {
    @GET("/todos")
    suspend fun getTodos(@Query("limit") limit: Int = 100): RemoteTodoList

    @POST("/todos/add")
    suspend fun add(@Body body: AddTodoBody): RemoteTodo

    @PUT("/todos/{id}")
    suspend fun update(@Path("id") id: Long, @Body body: UpdateTodoBody): RemoteTodo

    @DELETE("/todos/{id}")
    suspend fun delete(@Path("id") id: Long): RemoteTodo
}
```

Repository completo (sincroniza remoto ↔ local e expõe `Flow` do Room para a UI). Este é o exemplo "completo", com sincronização remota — mais abaixo mostramos uma versão simplificada, só com o Room, para quando você não precisa de rede.

```kotlin
import kotlinx.coroutines.flow.Flow
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// Função de extensão: converte o DTO remoto para a Entity do Room.
private fun RemoteTodo.toTask() = Task(
    id = id,
    title = todo,
    done = completed
)

class TaskRepository(private val dao: TaskDao) {
    // Retrofit pronto para uso (em um app real, isso viria injetado, veja Módulo 3.07)
    private val api: TaskApi = Retrofit.Builder()
        .baseUrl("https://dummyjson.com")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(TaskApi::class.java)

    // Fonte de verdade para a UI: a tela nunca fala com a rede diretamente,
    // ela só observa este Flow, que reflete o estado do banco local.
    val tasks: Flow<List<Task>> = dao.getAll()

    // Sincroniza a lista inicial do servidor para o Room.
    suspend fun syncFromRemote(limit: Int = 100) {
        // runCatching evita que uma falha de rede derrube o app; o erro é ignorado
        // silenciosamente aqui (em produção, você trataria/logaria isso).
        runCatching {
            val remote = api.getTodos(limit).todos.map { it.toTask() }
            // Upsert simples item a item (mantém compatibilidade com o DAO atual).
            remote.forEach { dao.upsert(it) }
        }
    }

    // Cria no servidor e reflete no cache local.
    suspend fun add(title: String) {
        runCatching {
            val created = api.add(AddTodoBody(todo = title)).toTask()
            dao.upsert(created)
        }.onFailure {
            // Fallback local caso a API falhe: cria a tarefa só localmente,
            // para o usuário não perder a ação mesmo sem internet.
            dao.upsert(Task(title = title))
        }
    }

    // Atualiza no servidor e reflete no cache local.
    suspend fun toggle(id: Long, done: Boolean) {
        runCatching {
            api.update(id, UpdateTodoBody(completed = done))
            dao.setDone(id, done)
        }.onFailure {
            // Tentativa local para manter a experiência do usuário mesmo offline.
            dao.setDone(id, done)
        }
    }

    // Exclui no servidor e no cache local.
    suspend fun delete(task: Task) {
        runCatching {
            api.delete(task.id)
        }
        dao.delete(task) // Remove localmente mesmo que a chamada remota falhe.
    }
}
```

Dica: chame uma sincronização inicial no ViewModel, assim que ele for criado:
```kotlin
// dentro de TaskViewModel
init {
    viewModelScope.launch { repo.syncFromRemote() }
}
```

> **Versão simplificada (só Room, sem rede):** se o seu app não precisa sincronizar com um servidor — por exemplo, uma lista de tarefas 100% local — o Repository pode ser bem mais enxuto, sem a parte de Retrofit. Esta é a versão "essencial" equivalente à de cima, útil como ponto de partida:
> ```kotlin
> import kotlinx.coroutines.flow.Flow
>
> class TaskRepository(private val dao: TaskDao) {
>     val tasks: Flow<List<Task>> = dao.getAll()
>
>     suspend fun add(title: String) = dao.upsert(Task(title = title))
>     suspend fun toggle(id: Long, done: Boolean) = dao.setDone(id, done)
>     suspend fun delete(task: Task) = dao.delete(task)
> }
> ```
> Use a versão completa (com Retrofit) quando precisar manter os dados sincronizados com um servidor; use esta versão simples quando o Room for a única fonte de dados.

### ViewModel (coroutines + Flow)

```kotlin
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaskViewModel(private val repo: TaskRepository) : ViewModel() {
    // Converte o Flow "frio" do Room em um StateFlow "quente", compartilhado
    // entre todos os coletores. Veja o Módulo 3.05 (Flow Avançado) para entender
    // a diferença entre Flow frio e quente, e o porquê de WhileSubscribed(5_000).
    val tasks: StateFlow<List<Task>> = repo.tasks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    // Cada ação da UI vira uma coroutine curta lançada no viewModelScope.
    fun add(title: String) = viewModelScope.launch { repo.add(title) }
    fun toggle(task: Task) = viewModelScope.launch { repo.toggle(task.id, !task.done) }
    fun delete(task: Task) = viewModelScope.launch { repo.delete(task) }
}

// Factory manual: necessária porque TaskViewModel recebe um TaskRepository
// no construtor, e o Android não sabe criar isso sozinho (sem Hilt — Módulo 3.07).
class TaskVMFactory(private val repo: TaskRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TaskViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel")
    }
}
```

## 3) UI com Compose (essencial)

```kotlin
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import android.os.Bundle

class MainActivity : ComponentActivity() {
    // "by viewModels { }" cria o ViewModel usando a Factory, e o Android
    // reaproveita a mesma instância entre rotações de tela.
    private val vm: TaskViewModel by viewModels {
        TaskVMFactory(TaskRepository(AppDatabase.get(this).taskDao()))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { App(vm) }
    }
}
```

```kotlin
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun App(vm: TaskViewModel) {
    MaterialTheme { TaskScreen(vm) }
}

@Composable
fun TaskScreen(vm: TaskViewModel) {
    // Observa o StateFlow com segurança de ciclo de vida (para de coletar em background).
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    // rememberSaveable preserva o texto digitado mesmo após rotação de tela.
    var text by rememberSaveable { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Nova tarefa") }
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    val t = text.trim()
                    if (t.isNotEmpty()) {
                        vm.add(t)
                        text = ""
                    }
                }
            ) { Text("Adicionar") }
        }

        Spacer(Modifier.height(16.dp))

        LazyColumn {
            items(items = tasks, key = { it.id }) { task ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = task.done, onCheckedChange = { vm.toggle(task) })
                    Text(task.title, Modifier.weight(1f).padding(start = 8.dp))
                    TextButton(onClick = { vm.delete(task) }) { Text("Excluir") }
                }
                HorizontalDivider() // Material 3: use HorizontalDivider em vez de Divider
            }
        }
    }
}
```

## 4) Coroutines e Flow (essência)

- Query reativa com `Flow` no DAO — a UI é atualizada automaticamente sempre que os dados mudam no banco.
- Escritas `suspend` para I/O — nunca bloqueiam a thread principal.
- `viewModelScope.launch` para chamadas ao repositório a partir da UI.
- Na UI, `collectAsStateWithLifecycle()` para observar com segurança, evitando coletar dados quando a tela está em segundo plano.

## 5) Possibilidades (além do essencial)

- DI com Hilt (Módulo 3.07) para fornecer `AppDatabase`, `TaskDao` e `TaskRepository` automaticamente, eliminando a `Factory` manual.
- **Migrations** em produção (substituir `fallbackToDestructiveMigration`) — veja a explicação detalhada abaixo.
- `@TypeConverters` para tipos complexos que o SQLite não entende nativamente (ex.: `LocalDateTime`, listas).
- Relacionamentos (`@Relation`) e consultas avançadas entre tabelas diferentes.
- Pré-popular a base com `RoomDatabase.Callback`.

### O que é uma "migration" e por que ela é perigosa se ignorada

Quando você muda a estrutura de uma `@Entity` (por exemplo, adiciona uma nova coluna `prioridade: Int` à tabela `tasks`), o Room percebe que o "desenho" do banco (o *schema*) mudou e precisa saber como atualizar o banco que já existe no celular do usuário, sem perder os dados que já estavam lá. Isso é uma **migration**: um conjunto de instruções SQL que descreve como transformar a versão antiga do banco na nova versão.

Neste guia usamos `fallbackToDestructiveMigration()`, que é **conveniente durante o desenvolvimento**: sempre que a versão do banco muda e não existe uma migration explícita, o Room simplesmente apaga o banco inteiro e recria do zero. Isso é ótimo enquanto você está testando e o schema muda toda hora — mas em um app publicado, isso significa **apagar todos os dados do usuário** na próxima atualização que mudar qualquer coisa na estrutura do banco. Antes de publicar um app de verdade, substitua isso por migrations explícitas.

## Erros Comuns / Pegadinhas

1. **Usar `fallbackToDestructiveMigration()` em produção.** Como explicado acima, isso apaga os dados do usuário sempre que o schema do banco mudar sem uma migration explícita. Use apenas durante o desenvolvimento local.

2. **Esquecer `suspend` em funções de escrita do DAO.** Se você tentar chamar `dao.upsert(task)` fora de uma coroutine, o código não compila (o Kotlin já protege você). Mas se você tentar rodar uma operação de banco diretamente na thread principal usando APIs de baixo nível (sem passar pelo Room), o app pode lançar `IllegalStateException: Cannot access database on the main thread`. O Room, por padrão, bloqueia acesso direto ao banco pela UI thread justamente para evitar travamentos.

3. **Esquecer que consultas `Flow` do Room "vivem para sempre" enquanto tiverem coletores.** Se você não usar `stateIn` com `WhileSubscribed` (ou não parar de coletar quando a tela some), o Flow continua rodando em segundo plano, consumindo recursos. Sempre observe Flows do Room a partir do ViewModel com `viewModelScope` e `SharingStarted.WhileSubscribed(5_000)`.

4. **Criar mais de uma instância de `AppDatabase`.** Abrir o mesmo arquivo `.db` a partir de instâncias diferentes do Room pode causar comportamento inconsistente e desperdiça memória. Sempre use o padrão singleton mostrado na classe `Database` acima.

---

## Resumo

| Conceito | O que é |
|---|---|
| `@Entity` | Classe que representa uma tabela do banco |
| `@PrimaryKey` | Identifica unicamente cada linha da tabela |
| `@Dao` | Interface com as operações de acesso ao banco |
| `@Query` | Executa SQL customizado (geralmente leituras) |
| `@Insert` / `@Delete` | Operações de escrita geradas automaticamente |
| `@Database` | Ponto de entrada do Room; declara entidades e versão |
| `Flow` no DAO | Notifica a UI automaticamente quando os dados mudam |
| `suspend fun` no DAO | Escreve no banco sem travar a UI thread |
| Migration | Instruções para atualizar o schema sem perder dados |
| `fallbackToDestructiveMigration` | Atalho de desenvolvimento que apaga o banco em vez de migrar |

Checklist mínimo antes de considerar essa parte pronta:
- KSP ligado e o projeto compila sem erros.
- Consultas de leitura retornam `Flow`.
- ViewModel não expõe funções `suspend` diretamente à UI — ele sempre usa `viewModelScope`.
- UI apenas observa estado e envia intenções (cliques viram chamadas ao ViewModel).
- Não há lógica de acesso a dados dentro de um Composable.

**Próximo passo:** no arquivo `04_repository.md` vamos aprofundar o Repository Pattern, combinando Room (cache local) e Retrofit (rede) com uma estratégia clara de "de onde vêm os dados".
