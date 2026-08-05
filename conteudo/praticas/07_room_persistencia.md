# Prática: Persistência Local com Room para Iniciantes

Este guia apresenta exercícios práticos para salvar e recuperar dados localmente no dispositivo Android usando o **Room**, a biblioteca oficial de banco de dados do Android.

---

## O que é o Room?

O Room é uma camada sobre o SQLite que torna o trabalho com banco de dados muito mais simples:

- Você descreve a **tabela** como uma `data class` com `@Entity`.
- Você define as **operações** (inserir, deletar, buscar) em uma interface `@Dao`.
- O Room **gera o código SQL** automaticamente.

---

## Configuração

Adicione ao `app/build.gradle.kts`:

```kotlin
plugins {
    // ... outros plugins
    id("com.google.devtools.ksp") version "2.1.0-1.0.29" // Verifique a versão compatível com seu Kotlin
}

dependencies {
    implementation("androidx.room:room-runtime:2.7.0")
    implementation("androidx.room:room-ktx:2.7.0")
    ksp("androidx.room:room-compiler:2.7.0")

    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
}
```

---

## Prática 1: App de Anotações (CRUD Completo)

### Objetivo
Criar um app simples de anotações que salva os dados mesmo quando o app é fechado.

Sem persistência local, todo dado do app desaparece assim que o usuário fecha o aplicativo ou o sistema o encerra para liberar memória — uma experiência frustrante. O Room resolve isso salvando dados diretamente no dispositivo, em um banco SQLite gerenciado para você. Esta prática cobre o fluxo CRUD completo (Create, Read, Update, Delete) — o conjunto de operações que praticamente qualquer app com dados próprios precisa implementar, seja uma lista de tarefas, um catálogo de produtos favoritos ou um diário pessoal.

### Passo a Passo

Vamos construir aos poucos: primeiro só ler e criar anotações, depois evoluir para deletar e editar.

**1. Entity — Representa a tabela** (`Anotacao.kt`):

```kotlin
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "anotacoes")
data class Anotacao(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,             // ID gerado automaticamente
    val titulo: String,
    val conteudo: String,
    val criadaEm: Long = System.currentTimeMillis() // Timestamp da criação
)
```

**O que cada anotação significa:**
- `@Entity`: marca a classe como uma tabela do banco de dados.
- `@PrimaryKey(autoGenerate = true)`: o banco gera o ID automaticamente.
- Os outros campos viram colunas da tabela.

**2. DAO — comece só com ler e inserir** (`AnotacaoDao.kt`):

```kotlin
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AnotacaoDao {

    // Flow: a UI é atualizada automaticamente quando os dados mudam
    @Query("SELECT * FROM anotacoes ORDER BY criadaEm DESC")
    fun buscarTodas(): Flow<List<Anotacao>>

    // REPLACE: se já existir um registro com o mesmo ID, substitui
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(anotacao: Anotacao)
}
```

Com só essas duas operações já dá para ver a lista e adicionar anotações — falta deletar e editar, o que resolvemos no passo 6.

**3. Database — Configuração do banco** (`AppDatabase.kt`):

```kotlin
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Anotacao::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun anotacaoDao(): AnotacaoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun obter(context: Context): AppDatabase {
            // Garante que apenas uma instância seja criada (padrão Singleton)
            return INSTANCE ?: synchronized(this) {
                val instancia = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "anotacoes.db"
                ).build()
                INSTANCE = instancia
                instancia
            }
        }
    }
}
```

**4. ViewModel — leitura e criação** (`AnotacaoViewModel.kt`):

```kotlin
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AnotacaoViewModel(private val dao: AnotacaoDao) : ViewModel() {

    // Converte o Flow do DAO em StateFlow para a UI observar
    val anotacoes: StateFlow<List<Anotacao>> = dao.buscarTodas().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun adicionar(titulo: String, conteudo: String) {
        if (titulo.isBlank()) return
        viewModelScope.launch {
            dao.inserir(Anotacao(titulo = titulo.trim(), conteudo = conteudo.trim()))
        }
    }
}
```

**5. Tela — lista e criação** (`AnotacoesScreen.kt`):

Nesta primeira versão a tela já funciona de ponta a ponta: você consegue criar uma anotação pelo diálogo e vê-la aparecer na lista, persistida no banco. Ainda não há como deletar — isso vem no passo 8.

```kotlin
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AnotacoesScreen(viewModel: AnotacaoViewModel) {
    val anotacoes by viewModel.anotacoes.collectAsStateWithLifecycle()
    var mostrarDialogo by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Minhas Anotações") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { mostrarDialogo = true }) {
                Icon(Icons.Default.Add, contentDescription = "Nova anotação")
            }
        }
    ) { padding ->
        if (anotacoes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Nenhuma anotação. Toque em + para adicionar! 📝")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(anotacoes, key = { it.id }) { anotacao ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(anotacao.titulo, style = MaterialTheme.typography.titleMedium)
                            if (anotacao.conteudo.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    anotacao.conteudo,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 3
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (mostrarDialogo) {
        DialogoNovaAnotacao(
            onConfirmar = { titulo, conteudo ->
                viewModel.adicionar(titulo, conteudo)
                mostrarDialogo = false
            },
            onDescartar = { mostrarDialogo = false }
        )
    }
}

@Composable
fun DialogoNovaAnotacao(
    onConfirmar: (titulo: String, conteudo: String) -> Unit,
    onDescartar: () -> Unit
) {
    var titulo by remember { mutableStateOf("") }
    var conteudo by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDescartar,
        title = { Text("Nova Anotação") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text("Título *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = conteudo,
                    onValueChange = { conteudo = it },
                    label = { Text("Conteúdo (opcional)") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmar(titulo, conteudo) },
                enabled = titulo.isNotBlank()
            ) { Text("Salvar") }
        },
        dismissButton = {
            TextButton(onClick = onDescartar) { Text("Cancelar") }
        }
    )
}
```

**6. Evolua o DAO — adicione deletar e atualizar:**

Agora que a criação funciona, complete o DAO com as duas operações que faltam:

```kotlin
    @Delete
    suspend fun deletar(anotacao: Anotacao)

    @Query("UPDATE anotacoes SET titulo = :titulo, conteudo = :conteudo WHERE id = :id")
    suspend fun atualizar(id: Long, titulo: String, conteudo: String)
```

**7. Evolua o ViewModel — exponha deletar e atualizar:**

```kotlin
    fun deletar(anotacao: Anotacao) {
        viewModelScope.launch {
            dao.deletar(anotacao)
        }
    }

    fun atualizar(id: Long, titulo: String, conteudo: String) {
        viewModelScope.launch {
            dao.atualizar(id, titulo.trim(), conteudo.trim())
        }
    }
```

**8. Evolua a tela — adicione o botão de deletar:**

No `Card` de cada anotação (dentro do `items { }`), troque a `Column` sozinha por uma `Row` com um `IconButton` ao lado:

```kotlin
import androidx.compose.material.icons.filled.Delete

// Dentro de items(anotacoes, key = { it.id }) { anotacao -> ... }
ElevatedCard(modifier = Modifier.fillMaxWidth()) {
    Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(anotacao.titulo, style = MaterialTheme.typography.titleMedium)
            if (anotacao.conteudo.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    anotacao.conteudo,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3
                )
            }
        }
        IconButton(onClick = { viewModel.deletar(anotacao) }) {
            Icon(Icons.Default.Delete, contentDescription = "Deletar")
        }
    }
}
```

**9. Conectando na MainActivity**:

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = AppDatabase.obter(this)
        val viewModel = AnotacaoViewModel(db.anotacaoDao())

        setContent {
            MaterialTheme {
                AnotacoesScreen(viewModel = viewModel)
            }
        }
    }
}
```

> **💡 Por trás dos panos**
> Repare que `anotacoes` no ViewModel vem de `dao.buscarTodas().stateIn(...)` — o `Flow` retornado pelo DAO é **reativo**: toda vez que uma anotação é inserida, atualizada ou removida no banco, o Room emite automaticamente uma nova lista atualizada, e a tela redesenha sozinha. Você nunca precisa chamar manualmente "recarregar a lista" depois de salvar algo — é esse fluxo automático (`banco muda → Flow emite → StateFlow atualiza → UI recompõe`) que torna o Room tão prático de usar junto com Compose.

### Exercícios

1. **Editar anotações**: Adicione a funcionalidade de editar uma anotação existente.
   - Primeiro, guarde a anotação selecionada para edição em um `remember { mutableStateOf<Anotacao?>(null) }`.
   - Depois, ao clicar no card, preencha esse estado e abra o mesmo `AlertDialog`, mas com os campos já preenchidos com os dados atuais.
   - Por fim, no botão de confirmação, chame `viewModel.atualizar(...)` em vez de `adicionar(...)`.
   - *Dica se travar*: reaproveite o `DialogoNovaAnotacao` existente, adicionando parâmetros opcionais de `tituloInicial` e `conteudoInicial`.
2. **Pesquisa**: Adicione um campo de busca na `TopAppBar`. Use a função `filter` sobre a lista de anotações para exibir apenas as que contêm o texto buscado no título ou no conteúdo.
3. **Ordenação**: Adicione um menu no canto superior direito com opções de ordenação: "Mais recentes primeiro" e "Mais antigas primeiro". Modifique a query do DAO conforme a opção selecionada.
   - *Dica se travar*: você pode criar duas funções no DAO (`buscarTodasRecentes()` e `buscarTodasAntigas()`, cada uma com `ORDER BY` diferente) e alternar entre elas no ViewModel.

---

## Prática 2: Favoritos com Room

### Objetivo
Entender um caso de uso comum: salvar e remover favoritos localmente.

"Favoritar" é um dos recursos mais pedidos em qualquer app com listas — filmes, produtos, artigos, receitas. Esta prática mostra um padrão bem específico e muito reutilizável: verificar se um item já existe no banco antes de decidir se ele deve ser inserido ou removido, alternando o estado com um único método (`alternarFavorito`).

### Passo a Passo

**1. Entity**:

```kotlin
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favoritos")
data class FilmeFavorito(
    @PrimaryKey val id: Int,        // Usamos o ID do filme (sem autoGenerate)
    val titulo: String,
    val nota: Float,
    val adicionadoEm: Long = System.currentTimeMillis()
)
```

**2. DAO**:

```kotlin
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoritoDao {

    @Query("SELECT * FROM favoritos ORDER BY adicionadoEm DESC")
    fun observarTodos(): Flow<List<FilmeFavorito>>

    @Query("SELECT COUNT(*) FROM favoritos WHERE id = :id")
    suspend fun estaFavoritado(id: Int): Int  // Retorna 0 (não favorito) ou 1 (favorito)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun adicionar(favorito: FilmeFavorito)

    @Delete
    suspend fun remover(favorito: FilmeFavorito)
}
```

**3. ViewModel**:

```kotlin
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FavoritoViewModel(private val dao: FavoritoDao) : ViewModel() {

    val favoritos: StateFlow<List<FilmeFavorito>> = dao.observarTodos().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun alternarFavorito(filme: FilmeFavorito) {
        viewModelScope.launch {
            val jaFavoritado = dao.estaFavoritado(filme.id) > 0
            if (jaFavoritado) {
                dao.remover(filme)
            } else {
                dao.adicionar(filme)
            }
        }
    }
}
```

> **💡 Por trás dos panos**
> Note que a `@PrimaryKey` de `FilmeFavorito` não usa `autoGenerate = true` como em `Anotacao` — aqui o ID é o próprio ID do filme (vindo de outra fonte, como uma API). Isso é proposital: queremos que cada filme apareça no máximo uma vez na tabela de favoritos, e usar o mesmo ID do filme como chave primária garante isso automaticamente (o Room rejeita — ou substitui, dependendo do `OnConflictStrategy` — uma inserção com uma chave primária repetida).

### Exercícios

1. Adicione ao ViewModel uma função `estaFavoritado(id: Int): Flow<Boolean>` que emite `true` ou `false` conforme o estado no banco. Use-a em cada item da lista para mostrar um ícone de coração preenchido ou vazio.
   - *Dica se travar*: crie no DAO uma query `@Query("SELECT COUNT(*) > 0 FROM favoritos WHERE id = :id") fun estaFavoritadoFlow(id: Int): Flow<Boolean>` — o Room converte o `COUNT(*) > 0` automaticamente em `Boolean`.
2. Adicione uma tela "Meus Favoritos" que lista todos os filmes salvos, com a possibilidade de remover cada um.
3. Adicione uma coluna `categoria: String` à entidade `FilmeFavorito`. Atualize a query do DAO para retornar favoritos filtrados por categoria.

---

## Conceitos Chave

```
Usuário age → ViewModel chama o DAO → DAO salva/lê no banco → Flow emite novo valor → UI atualiza automaticamente
```

| Anotação Room | Função |
|---------------|--------|
| `@Entity` | Define uma tabela no banco |
| `@PrimaryKey` | Define a chave primária da tabela |
| `@Dao` | Interface com operações do banco |
| `@Insert` | Insere um ou mais registros |
| `@Delete` | Remove um ou mais registros |
| `@Query` | Executa SQL personalizado |
| `@Database` | Configura o banco de dados |

---

## Próximos Passos

- Estude o módulo `03_persistencia_room.md` para ver o Room com API remota e sincronização.
- Avance para `08_retrofit_api.md` para combinar dados remotos com persistência local.
- Explore `@TypeConverters` quando precisar salvar tipos complexos (como listas ou datas) no Room.
