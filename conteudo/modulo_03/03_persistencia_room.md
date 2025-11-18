# Room + ViewModel + Coroutines + Compose (essencial)

Objetivo: configurar o Room e montar o fluxo mínimo de dados em camadas (Entity → DAO → Database → Repository → ViewModel → UI Compose) com corotinas e Flow, de forma simples.

## 1) Dependências (module build.gradle)
Use as versões estáveis mais recentes.

```kotlin
plugins {
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

dependencies {
    // Room
    implementation("androidx.room:room-ktx:<versão>")
    ksp("androidx.room:room-compiler:<versão>")

    // Lifecycle + ViewModel + coroutines
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:<versão>")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:<versão>")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:<versão>")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:<versão>")

}
```

Dica: prefira KSP ao KAPT para Room.

## 2) Camadas mínimas

### Entity
```kotlin
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val done: Boolean = false
)
```

### DAO
- Leituras com Flow para observar mudanças.
- Escritas como funções `suspend`.

```kotlin
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY id DESC")
    fun getAll(): Flow<List<Task>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: Task)

    @Delete
    suspend fun delete(task: Task)

    @Query("UPDATE tasks SET done = :done WHERE id = :id")
    suspend fun setDone(id: Long, done: Boolean)
}
```

### Database
```kotlin
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Task::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app.db"
                )
                .fallbackToDestructiveMigration() // simples para dev
                .build()
                .also { INSTANCE = it }
            }
    }
}
```
### Repository (API pública + cache Room)

Usa a API pública DummyJSON (https://dummyjson.com/todos) para CRUD e o Room como cache e fonte de verdade para a UI.

Observação (build.gradle): adicione Retrofit
```kotlin
dependencies {
    implementation("com.squareup.retrofit2:retrofit:<versão>")
    implementation("com.squareup.retrofit2:converter-gson:<versão>")
}
```

API + DTOs
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

Repository (sincroniza remoto ↔ local e expõe Flow do Room)
```kotlin
import kotlinx.coroutines.flow.Flow
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

private fun RemoteTodo.toTask() = Task(
    id = id,
    title = todo,
    done = completed
)

class TaskRepository(private val dao: TaskDao) {
    // Retrofit pronto para uso
    private val api: TaskApi = Retrofit.Builder()
        .baseUrl("https://dummyjson.com")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(TaskApi::class.java)

    // Fonte de verdade para a UI
    val tasks: Flow<List<Task>> = dao.getAll()

    // Sincroniza a lista inicial do servidor para o Room
    suspend fun syncFromRemote(limit: Int = 100) {
        runCatching {
            val remote = api.getTodos(limit).todos.map { it.toTask() }
            // Upsert simples item a item (mantém compatibilidade com o DAO atual)
            remote.forEach { dao.upsert(it) }
        }
    }

    // Cria no servidor e reflete no cache local
    suspend fun add(title: String) {
        runCatching {
            val created = api.add(AddTodoBody(todo = title)).toTask()
            dao.upsert(created)
        }.onFailure {
            // fallback local caso a API falhe
            dao.upsert(Task(title = title))
        }
    }

    // Atualiza no servidor e reflete no cache local
    suspend fun toggle(id: Long, done: Boolean) {
        runCatching {
            api.update(id, UpdateTodoBody(completed = done))
            dao.setDone(id, done)
        }.onFailure {
            // tentativa local para manter UX
            dao.setDone(id, done)
        }
    }

    // Exclui no servidor e no cache local
    suspend fun delete(task: Task) {
        runCatching {
            api.delete(task.id)
        }
        dao.delete(task)
    }
}
```

Dica: chame uma sincronização inicial no ViewModel.
```kotlin
// dentro de TaskViewModel
init {
    viewModelScope.launch { repo.syncFromRemote() }
}
```
- Centraliza regras simples e expõe Flow para a UI.

```kotlin
import kotlinx.coroutines.flow.Flow

class TaskRepository(private val dao: TaskDao) {
    val tasks: Flow<List<Task>> = dao.getAll()

    suspend fun add(title: String) = dao.upsert(Task(title = title))
    suspend fun toggle(id: Long, done: Boolean) = dao.setDone(id, done)
    suspend fun delete(task: Task) = dao.delete(task)
}
```

### ViewModel (corotinas + Flow)
```kotlin
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaskViewModel(private val repo: TaskRepository) : ViewModel() {
    val tasks: StateFlow<List<Task>> = repo.tasks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun add(title: String) = viewModelScope.launch { repo.add(title) }
    fun toggle(task: Task) = viewModelScope.launch { repo.toggle(task.id, !task.done) }
    fun delete(task: Task) = viewModelScope.launch { repo.delete(task) }
}

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
    val tasks by vm.tasks.collectAsStateWithLifecycle()
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
                Divider()
            }
        }
    }
}
```

## 4) Corotinas e Flow (essência)
- Query reativa com `Flow` no DAO.
- Escritas `suspend` para I/O.
- `viewModelScope.launch` para chamadas ao repositório.
- Na UI, `collectAsStateWithLifecycle()` para observar com segurança.

## 5) Possibilidades (além do essencial)
- DI com Hilt para fornecer `AppDatabase`, `TaskDao` e `TaskRepository`.
- Migrations em produção (substituir `fallbackToDestructiveMigration`).
- `@TypeConverters` para tipos complexos (LocalDateTime, listas).
- Relacionamentos (`@Relation`) e consultas avançadas.
- Pre-popular base com `RoomDatabase.Callback`.

Checklist mínimo:
- KSP ligado e compila sem erros.
- Consultas retornam `Flow`.
- ViewModel não expõe funções suspensas à UI: usa `viewModelScope`.
- UI apenas observa estado e envia intenções.
- Sem lógica de dados dentro do Composable.