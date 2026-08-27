# Aula 1 — Salvando dados no celular

**Objetivo:** fazer um app de lista de tarefas que **não perde os dados** quando você fecha o app.

Nas aulas anteriores, tudo que você digitava sumia ao fechar o app. Isso acontece porque os dados ficavam só na memória. Agora vamos guardar no **banco de dados do celular**, usando uma biblioteca chamada **Room**.

No fim desta aula você vai ter um app funcionando: digita a tarefa, aperta "Adicionar", fecha o app, abre de novo — e a tarefa continua lá.

---

## 1. Instalar o Room

Abra o arquivo `build.gradle.kts` (o do módulo `app`) e adicione:

```kotlin
plugins {
    // ...os plugins que já existem...
    id("com.google.devtools.ksp") version "2.1.0-1.0.29"
}

dependencies {
    // ...as dependências que já existem...
    implementation("androidx.room:room-runtime:2.7.0")
    implementation("androidx.room:room-ktx:2.7.0")
    ksp("androidx.room:room-compiler:2.7.0")
}
```

Clique em **Sync Now** (a barra amarela que aparece no topo).

> **O que é `ksp`?** É o programa que lê as suas anotações (`@Entity`, `@Dao`...) e escreve sozinho o código chato de banco de dados. Você não vê esse código, mas ele existe.

---

## 2. Três arquivos, três papéis

O Room precisa de três coisas. Só três. Cada uma cabe em poucas linhas.

| Arquivo | Papel | Analogia |
|---------|-------|----------|
| `Tarefa.kt` | Como é **uma linha** da tabela | A ficha de papel |
| `TarefaDao.kt` | As **ações** possíveis (listar, inserir, apagar) | O que você pode fazer com as fichas |
| `Banco.kt` | O **banco** em si | O arquivo/gaveta onde as fichas ficam |

### Arquivo 1 — `Tarefa.kt`

```kotlin
import androidx.room.Entity
import androidx.room.PrimaryKey

// @Entity avisa: "isto vira uma tabela no banco".
@Entity
data class Tarefa(
    // Cada tarefa precisa de um número único. O Room gera sozinho (autoGenerate).
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val texto: String
)
```

### Arquivo 2 — `TarefaDao.kt`

```kotlin
import androidx.room.*

// DAO = Data Access Object. É a lista de ações que o app pode fazer no banco.
@Dao
interface TarefaDao {

    @Query("SELECT * FROM Tarefa")
    suspend fun listar(): List<Tarefa>

    @Insert
    suspend fun inserir(tarefa: Tarefa)

    @Delete
    suspend fun apagar(tarefa: Tarefa)
}
```

Repare: você **não escreve o corpo** dessas funções. Só diz o que quer; o Room escreve o resto.

> **Por que `suspend`?** Mexer no banco demora alguns milissegundos. `suspend` é o aviso do Kotlin: "esta função pode demorar, então ela só pode ser chamada de dentro de um `launch`". Isso impede que o app trave. Você vai ver o `launch` no próximo passo.

### Arquivo 3 — `Banco.kt`

```kotlin
import android.content.Context
import androidx.room.*

@Database(entities = [Tarefa::class], version = 1)
abstract class Banco : RoomDatabase() {

    abstract fun tarefaDao(): TarefaDao

    companion object {
        private var instancia: Banco? = null

        // Cria o banco só na primeira vez. Depois, reaproveita o mesmo.
        fun pegar(context: Context): Banco {
            if (instancia == null) {
                instancia = Room.databaseBuilder(context, Banco::class.java, "meu-banco").build()
            }
            return instancia!!
        }
    }
}
```

> **Por que reaproveitar?** Abrir o banco é caro. Se você abrisse um banco novo a cada tela, o app ficaria lento e poderia dar erro. Por isso guardamos em `instancia`.

---

## 3. O ViewModel

O ViewModel é quem conversa com o banco e guarda o que a tela precisa mostrar.

```kotlin
import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class TarefaViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = Banco.pegar(app).tarefaDao()

    // A lista que a tela mostra. Quando ela muda, a tela se redesenha sozinha.
    var tarefas by mutableStateOf(listOf<Tarefa>())
        private set

    init {
        carregar()
    }

    private fun carregar() = viewModelScope.launch {
        tarefas = dao.listar()
    }

    fun adicionar(texto: String) = viewModelScope.launch {
        if (texto.isBlank()) return@launch   // não deixa adicionar tarefa vazia
        dao.inserir(Tarefa(texto = texto))
        carregar()
    }

    fun apagar(tarefa: Tarefa) = viewModelScope.launch {
        dao.apagar(tarefa)
        carregar()
    }
}
```

**Duas palavras novas:**

- `viewModelScope.launch { ... }` — "faça isso em segundo plano, sem travar a tela". É dentro dele que dá para chamar funções `suspend`.
- `by mutableStateOf(...)` — cria uma variável **observada** pelo Compose. Mudou o valor, a tela se redesenha. Sem isso, você mudaria a lista e a tela continuaria igual.

---

## 4. A tela

```kotlin
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun TelaTarefas(vm: TarefaViewModel = viewModel()) {

    var texto by remember { mutableStateOf("") }

    Column(Modifier.padding(16.dp)) {

        Row(verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = texto,
                onValueChange = { texto = it },
                label = { Text("Nova tarefa") },
                modifier = Modifier.weight(1f)
            )
            Button(onClick = {
                vm.adicionar(texto)
                texto = ""          // limpa o campo depois de adicionar
            }) {
                Text("Add")
            }
        }

        Spacer(Modifier.height(16.dp))

        LazyColumn {
            items(vm.tarefas) { tarefa ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(tarefa.texto, Modifier.weight(1f))
                    TextButton(onClick = { vm.apagar(tarefa) }) {
                        Text("Apagar")
                    }
                }
            }
        }
    }
}
```

E na `MainActivity`:

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                TelaTarefas()
            }
        }
    }
}
```

Pronto. Rode o app.

---

## 5. Teste se funcionou

- [ ] Digitei uma tarefa e ela apareceu na lista.
- [ ] **Fechei o app de verdade** (arrastei para fora dos apps recentes) e abri de novo: a tarefa continua lá.
- [ ] Apertei "Apagar" e a tarefa sumiu — e continua sumida depois de reabrir.

Se os três funcionaram, seu app salva dados de verdade. 🎉

---

## Erros comuns

| Erro na tela | O que está acontecendo | Como resolver |
|--------------|------------------------|---------------|
| `Cannot access database on the main thread` | Você chamou o DAO fora de um `launch` | Coloque a chamada dentro de `viewModelScope.launch { }` |
| A lista não atualiza | Esqueceu de chamar `carregar()` depois de inserir/apagar | Chame `carregar()` no fim de cada função |
| `Unresolved reference: ksp` | Faltou o plugin `com.google.devtools.ksp` | Adicione o plugin e clique em Sync Now |
| App fecha ao abrir depois de mudar a `Tarefa` | Você mudou a tabela sem mudar a `version` | Desinstale o app do celular e instale de novo |

> **Sobre a última linha:** mudou os campos da `data class Tarefa`? Desinstalar o app apaga o banco antigo e resolve. Em um app já publicado isso não pode ser feito (o usuário perderia os dados) — a solução profissional se chama *migration*, e está no [apêndice](../apendice/04_room_avancado.md).

---

## Resumo

- **Room** guarda dados no celular de forma permanente.
- Você precisa de três peças: `@Entity` (a tabela), `@Dao` (as ações) e `@Database` (o banco).
- Funções de banco são `suspend` e rodam dentro de `viewModelScope.launch { }`.
- `mutableStateOf` faz a tela se redesenhar sozinha quando os dados mudam.

👉 Próxima aula: [Buscando dados da internet](02_dados_da_internet.md)
