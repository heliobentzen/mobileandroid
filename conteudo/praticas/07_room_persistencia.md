# Prática 07 — Room: salvando dados no celular

**Pré-requisito:** [Módulo 3 — Aula 1](../modulo_03/01_salvar_dados.md)

Na aula você fez uma lista de tarefas. Aqui você vai fazer um **app de anotações**, que é a mesma ideia com um campo a mais. Repetir o padrão com um app diferente é o que faz ele grudar.

No fim, você tem um app que salva anotações com título e texto, e que continua com tudo lá depois de fechar.

---

## Configuração

No `build.gradle.kts` do módulo `app`:

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

**Sync Now.**

---

## Parte 1 — App de anotações

### 1. A tabela — `Anotacao.kt`

```kotlin
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Anotacao(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val titulo: String,
    val texto: String
)
```

Comparando com a aula: é a mesma coisa, só com **dois** campos de texto em vez de um.

### 2. As ações — `AnotacaoDao.kt`

```kotlin
import androidx.room.*

@Dao
interface AnotacaoDao {

    @Query("SELECT * FROM Anotacao ORDER BY id DESC")
    suspend fun listar(): List<Anotacao>

    @Insert
    suspend fun inserir(anotacao: Anotacao)

    @Delete
    suspend fun apagar(anotacao: Anotacao)
}
```

O `ORDER BY id DESC` é a única novidade: mostra as anotações **mais novas primeiro**. Sem ele, as antigas ficariam no topo.

### 3. O banco — `Banco.kt`

```kotlin
import android.content.Context
import androidx.room.*

@Database(entities = [Anotacao::class], version = 1)
abstract class Banco : RoomDatabase() {

    abstract fun anotacaoDao(): AnotacaoDao

    companion object {
        private var instancia: Banco? = null

        fun pegar(context: Context): Banco {
            if (instancia == null) {
                instancia = Room.databaseBuilder(context, Banco::class.java, "anotacoes").build()
            }
            return instancia!!
        }
    }
}
```

### 4. O ViewModel — `AnotacaoViewModel.kt`

```kotlin
import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class AnotacaoViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = Banco.pegar(app).anotacaoDao()

    var anotacoes by mutableStateOf(listOf<Anotacao>())
        private set

    init {
        carregar()
    }

    private fun carregar() = viewModelScope.launch {
        anotacoes = dao.listar()
    }

    fun adicionar(titulo: String, texto: String) = viewModelScope.launch {
        if (titulo.isBlank()) return@launch      // título é obrigatório, texto não
        dao.inserir(Anotacao(titulo = titulo, texto = texto))
        carregar()
    }

    fun apagar(anotacao: Anotacao) = viewModelScope.launch {
        dao.apagar(anotacao)
        carregar()
    }
}
```

### 5. A tela — `TelaAnotacoes.kt`

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
fun TelaAnotacoes(vm: AnotacaoViewModel = viewModel()) {

    var titulo by remember { mutableStateOf("") }
    var texto by remember { mutableStateOf("") }

    Column(Modifier.padding(16.dp)) {

        Text("Minhas Anotações", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = titulo,
            onValueChange = { titulo = it },
            label = { Text("Título") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = texto,
            onValueChange = { texto = it },
            label = { Text("Anotação") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                vm.adicionar(titulo, texto)
                titulo = ""
                texto = ""
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Salvar")
        }

        Spacer(Modifier.height(16.dp))

        if (vm.anotacoes.isEmpty()) {
            Text("Nenhuma anotação ainda. Escreva a primeira! 📝")
        }

        LazyColumn {
            items(vm.anotacoes) { anotacao ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(anotacao.titulo, style = MaterialTheme.typography.titleMedium)
                            Text(anotacao.texto, style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick = { vm.apagar(anotacao) }) {
                            Text("Apagar")
                        }
                    }
                }
            }
        }
    }
}
```

Na `MainActivity`, chame `TelaAnotacoes()` dentro do `setContent { MaterialTheme { ... } }`.

---

## Teste se funcionou

- [ ] Salvei uma anotação com título e texto, e ela apareceu na lista
- [ ] A anotação mais nova aparece **em cima**
- [ ] Tentei salvar com o título vazio e nada foi salvo
- [ ] Fechei o app de verdade, reabri, e as anotações continuam lá
- [ ] Apaguei uma anotação e ela sumiu de vez

---

## Exercícios

### 1. Contador de anotações

Mostre no topo da tela quantas anotações existem: *"Minhas Anotações (3)"*.

> *Dica:* `vm.anotacoes.size` já te dá o número. Não precisa mexer no banco.

### 2. Campo de busca

Adicione um campo de busca que filtra a lista pelo título enquanto você digita.

> *Dica:* crie `var busca by remember { mutableStateOf("") }` na tela e filtre **a lista que já está na memória**, sem mexer no DAO:
> ```kotlin
> val lista = vm.anotacoes.filter { it.titulo.contains(busca, ignoreCase = true) }
> ```
> Depois use `items(lista)` no lugar de `items(vm.anotacoes)`.

### 3. Confirmar antes de apagar

Hoje um toque em "Apagar" já apaga. Peça confirmação antes.

> *Dica:* guarde qual anotação está esperando confirmação:
> ```kotlin
> var paraApagar by remember { mutableStateOf<Anotacao?>(null) }
> ```
> Quando `paraApagar` não for `null`, mostre um `AlertDialog` com os botões "Apagar" e "Cancelar".

### 4. Editar uma anotação *(desafio)*

Ao tocar em uma anotação, carregue o título e o texto dela nos campos de cima para editar.

> *Dica:* o jeito mais simples é apagar a antiga e salvar a nova. Adicione no DAO:
> ```kotlin
> @Update
> suspend fun atualizar(anotacao: Anotacao)
> ```
> e chame `dao.atualizar(anotacao.copy(titulo = novoTitulo, texto = novoTexto))` — o `copy` mantém o mesmo `id`, então o Room substitui a linha certa.

---

## Parte 2 — Favoritos *(desafio opcional)*

Este é o padrão por trás do coraçãozinho de qualquer app: tocar uma vez favorita, tocar de novo desfavorita.

A diferença aqui é a chave primária: **não** usamos `autoGenerate`. O `id` do próprio filme vira a chave, e isso garante sozinho que o mesmo filme não entre duas vezes.

```kotlin
@Entity
data class Favorito(
    @PrimaryKey val id: Int,     // id do filme, NÃO gerado pelo Room
    val titulo: String
)
```

```kotlin
@Dao
interface FavoritoDao {

    @Query("SELECT * FROM Favorito")
    suspend fun listar(): List<Favorito>

    @Query("SELECT COUNT(*) FROM Favorito WHERE id = :id")
    suspend fun contar(id: Int): Int      // 0 = não é favorito, 1 = é

    @Insert
    suspend fun adicionar(favorito: Favorito)

    @Delete
    suspend fun remover(favorito: Favorito)
}
```

E no ViewModel, uma função só resolve os dois casos:

```kotlin
fun alternar(favorito: Favorito) = viewModelScope.launch {
    if (dao.contar(favorito.id) > 0) {
        dao.remover(favorito)
    } else {
        dao.adicionar(favorito)
    }
    carregar()
}
```

**Exercício:** monte uma tela com uma lista fixa de 5 filmes (escritos direto no código) e um botão ⭐/☆ em cada um, que chama `alternar`. O que estiver favoritado deve continuar favoritado depois de fechar o app.

---

## Erros comuns

| Erro | Causa | Solução |
|------|-------|---------|
| `Cannot access database on the main thread` | Chamou o DAO fora de um `launch` | Coloque dentro de `viewModelScope.launch { }` |
| A lista não muda depois de salvar | Faltou chamar `carregar()` | Chame `carregar()` no fim de `adicionar` e `apagar` |
| `no such table: Anotacao` | Mudou a `@Entity` sem mudar a `version` | Desinstale o app do celular e instale de novo |
| `UNIQUE constraint failed` | Inseriu duas vezes o mesmo `id` (na Parte 2) | Use `@Insert(onConflict = OnConflictStrategy.REPLACE)` ou confira antes com `contar` |
| Erro de compilação em `@Dao` | Nome de coluna errado na `@Query` | O nome na query tem que ser igual ao da `data class`, letra por letra |

---

## Resumo

| Anotação | Para que serve |
|----------|----------------|
| `@Entity` | Vira uma tabela |
| `@PrimaryKey` | O identificador único de cada linha |
| `@Dao` | A lista de ações no banco |
| `@Insert` / `@Delete` / `@Update` | Inserir, apagar, atualizar |
| `@Query` | SQL escrito por você (`SELECT`, `ORDER BY`, `WHERE`) |
| `@Database` | Junta tudo e cria o banco |

O ciclo é sempre o mesmo:

```
usuário toca → ViewModel chama o DAO dentro de launch → carregar() de novo → mutableStateOf muda → tela redesenha
```

👉 Próxima prática: [Retrofit — buscando dados da internet](08_retrofit_api.md)
