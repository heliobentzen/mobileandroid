# Aula Completa: Persistência de Dados no Android com Room em Kotlin

## Introdução

Room abstrai o SQLite e:
- Reduz boilerplate.
- Verifica SQL em compile-time.
- Integra-se com coroutines, Flow, LiveData.
- Facilita migrations e organização em camadas.

---

## 1. Conceitos Básicos

Componentes:
- Entity: tabela.
- DAO: consultas e operações.
- Database: ponto central que expõe DAOs.

Fluxo: Entity -> DAO -> Database -> Repository -> ViewModel -> UI (Flow / LiveData).

---

## 2. Configuração Inicial

### Dependências (Versões Centralizadas)

Arquivo: gradle/libs.versions.toml
```toml
[versions]
room = "2.6.1"

[libraries]
room-runtime  = { module = "androidx.room:room-runtime", version.ref = "room" }
room-ktx      = { module = "androidx.room:room-ktx", version.ref = "room" }
room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }
```

build.gradle (module Kotlin + KSP):
```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

dependencies {
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
```

Observações:
- Use ksp em vez de kapt para melhor performance.
- Ative Java 17 ou 11 conforme projeto.

---

## 3. Exemplo Básico

### 3.1. Criando uma Entity

Arquivo: model/User.kt
```kotlin
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user")
data class User(
    @PrimaryKey(autoGenerate = true)
    val uid: Int = 0,

    @ColumnInfo(name = "first_name")
    val firstName: String,

    @ColumnInfo(name = "last_name")
    val lastName: String
)
```

Boas práticas:
- Usar data class.
- Tipos simples; para tipos customizados usar TypeConverter.

---

### 3.2. Criando o DAO

Arquivo: data/UserDao.kt
```kotlin
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: User): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(vararg users: User): List<Long>

    @Update
    suspend fun update(user: User)

    @Delete
    suspend fun delete(user: User)

    @Query("SELECT * FROM user")
    suspend fun getAllOnce(): List<User>

    @Query("SELECT * FROM user")
    fun getAllFlow(): Flow<List<User>>

    @Query("SELECT * FROM user WHERE first_name LIKE :name LIMIT 1")
    suspend fun findByFirstName(name: String): User?
}
```

---

### 3.3. Criando o Database

Arquivo: data/AppDatabase.kt
```kotlin
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [User::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
}
```

---

### 3.4. Inicializando e Usando (Exemplo)

Evite criar a instância repetidamente. Use singleton.

Arquivo: data/DatabaseProvider.kt
```kotlin
import android.content.Context
import androidx.room.Room

object DatabaseProvider {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun get(context: Context): AppDatabase =
        INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "app_database.db"
            )
            .build()
            .also { INSTANCE = it }
        }
}
```

Uso rápido (ex: em ViewModel via repository):
```kotlin
class UserRepository(private val dao: UserDao) {
    val usersFlow = dao.getAllFlow()
    suspend fun add(first: String, last: String) = dao.insert(User(firstName = first, lastName = last))
}
```

ViewModel:
```kotlin
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class UserViewModel(repo: UserRepository) : ViewModel() {
    val users = repo.usersFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun addSample() = viewModelScope.launch {
        repo.add("Maria", "Silva")
    }
}
```

---

## 4. Recursos Intermediários

### 4.1. Consultas
```kotlin
@Query("SELECT * FROM user WHERE first_name LIKE :prefix || '%'")
fun searchByPrefix(prefix: String): Flow<List<User>>
```

### 4.2. Atualização
```kotlin
@Update
suspend fun update(user: User)
```

### 4.3. Observando com Flow
- Flow integrado a coroutines.
- UI coleta com repeatOnLifecycle.

Exemplo na Activity/Fragment:
```kotlin
lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.users.collect { list ->
            // Atualizar UI
        }
    }
}
```

---

## 5. Recursos Avançados

### 5.1. Relacionamentos (One-to-Many)

Arquivo: model/Book.kt
```kotlin
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Book(
    @PrimaryKey(autoGenerate = true) val bookId: Int = 0,
    val userOwnerId: Int,
    val title: String
)
```

Classe de relação:
```kotlin
import androidx.room.Embedded
import androidx.room.Relation

data class UserWithBooks(
    @Embedded val user: User,
    @Relation(
        parentColumn = "uid",
        entityColumn = "userOwnerId"
    )
    val books: List<Book>
)
```

DAO:
```kotlin
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface UserWithBooksDao {
    @Transaction
    @Query("SELECT * FROM user")
    fun getUsersWithBooksFlow(): Flow<List<UserWithBooks>>
}
```

### 5.2. Migrations

Adicionar campo exige versão nova.

```kotlin
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE user ADD COLUMN age INTEGER")
    }
}
```

Aplicar:
```kotlin
Room.databaseBuilder(context, AppDatabase::class.java, "app_database.db")
    .addMigrations(MIGRATION_1_2)
    .build()
```

### 5.3. TypeConverters

Arquivo: util/Converters.kt
```kotlin
import androidx.room.TypeConverter
import java.util.Date

object Converters {
    @TypeConverter
    @JvmStatic
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }

    @TypeConverter
    @JvmStatic
    fun dateToTimestamp(date: Date?): Long? = date?.time
}
```

Aplicar no Database (versão 2):
```kotlin
import androidx.room.TypeConverters

@Database(entities = [User::class, Book::class], version = 2, exportSchema = true)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun userWithBooksDao(): UserWithBooksDao
}
```

---

## 6. Boas Práticas

- Usar coroutines + Flow para reatividade.
- Evitar I/O na main thread.
- Planejar migrations (sem fallback destrutivo em produção).
- Paginar listas grandes (Paging 3).
- Adicionar índices para colunas filtradas.
- Separar camadas: model, data (dao), db, repository, ui.

Índice:
```kotlin
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "user",
    indices = [Index(value = ["first_name"])]
)
data class User(
    @PrimaryKey(autoGenerate = true) val uid: Int = 0,
    @ColumnInfo(name = "first_name") val firstName: String,
    @ColumnInfo(name = "last_name") val lastName: String
)
```

---

## 7. Referências

- Room: https://developer.android.com/training/data-storage/room
- Jetpack: https://developer.android.com/jetpack
- Migrations: https://developer.android.com/training/data-storage/room/migrating-db
- Kotlin Coroutines: https://developer.android.com/kotlin/coroutines

---

## 8. Exercício Prático

1. Implementar CRUD completo com Flow.
2. Adicionar relação User -> Books.
3. Exibir lista em RecyclerView observando Flow.
4. Campo Date com TypeConverter.
5. Criar MIGRATION adicionando email.
6. Busca por prefixo.
7. Paginação com Paging 3 (RemoteMediator opcional).

Desafio:
- Testes de DAO com in-memory database.
- Medir tempo de queries com Trace.
- Adicionar cache + Repository estruturado.

---

## Conclusão

Com Kotlin, Room integra-se de forma direta a coroutines e Flow, simplificando persistência reativa e segura. Evolua gradualmente: entidades simples, DAOs com suspend, Flow, relações, migrations e otimizações.

Fim.
