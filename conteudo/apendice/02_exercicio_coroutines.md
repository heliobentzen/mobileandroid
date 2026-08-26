# Exercício Prático: Buscador de Usuários com Coroutines

Este é um exercício único, completo e integrado que combina todos os conceitos de coroutines: **Dispatchers**, **Concorrência Estruturada**, **Cancelamento Cooperativo** e **Ciclo de Vida**.

## O Desafio

Você vai criar um **buscador de usuários em tempo real** que:
- Busca usuários enquanto o usuário digita (sem travar a UI)
- Cancela buscas anteriores quando uma nova busca é iniciada
- Aguarda 500ms após o último toque antes de executar a busca (debounce)
- Exibe o resultado e o tempo total da busca
- Cancela automaticamente quando a tela é fechada

---

## Passo 1: Criar o ViewModel

Crie um arquivo `UserSearchViewModel.kt`:

```kotlin
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlin.system.measureTimeMillis

data class User(val id: Int, val name: String, val email: String)

data class SearchState(
    val query: String = "",
    val results: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val elapsedTimeMs: Long = 0,
    val errorMessage: String? = null
)

class UserSearchViewModel : ViewModel() {
    
    private val _searchState = MutableStateFlow(SearchState())
    val searchState: StateFlow<SearchState> = _searchState

    // Referência ao Job atual para cancelamento
    private var searchJob: Job? = null

    // Simulando um banco de dados de usuários
    private val allUsers = listOf(
        User(1, "Alice Silva", "alice@example.com"),
        User(2, "Ana Santos", "ana@example.com"),
        User(3, "Bruno Costa", "bruno@example.com"),
        User(4, "Carlos Oliveira", "carlos@example.com"),
        User(5, "Daniela Rocha", "daniela@example.com"),
        User(6, "Ethan Smith", "ethan@example.com"),
        User(7, "Fernanda Lima", "fernanda@example.com"),
        User(8, "Gabriel Martins", "gabriel@example.com"),
        User(9, "Helena Pereira", "helena@example.com"),
        User(10, "Ivan Neves", "ivan@example.com"),
    )

    /**
     * Método chamado a cada toque do usuário.
     * Implementa debounce de 500ms automaticamente.
     */
    fun onSearchQueryChanged(query: String) {
        // Atualiza o estado com a query (sem buscar ainda)
        _searchState.value = _searchState.value.copy(
            query = query,
            isLoading = false,
            results = emptyList()
        )

        // Cancela a busca anterior
        searchJob?.cancel()

        // Se a query está vazia, limpa os resultados
        if (query.isEmpty()) {
            return
        }

        // Lança uma nova busca com debounce
        searchJob = viewModelScope.launch {
            // Aguarda 500ms (debounce)
            delay(500)

            // Inicia a busca
            search(query)
        }
    }

    /**
     * Executa a busca no background e atualiza o estado.
     * Esta função é suspensa (suspend fun).
     */
    private suspend fun search(query: String) {
        try {
            // Atualiza o estado para "carregando"
            _searchState.value = _searchState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            // Mede o tempo total da busca
            val elapsedTime = measureTimeMillis {
                // Troca para Dispatchers.IO para simular uma operação de banco de dados
                val filteredUsers = withContext(Dispatchers.IO) {
                    // Simula uma chamada de rede ou acesso ao banco de dados
                    delay(1000) // Simula latência de 1s
                    
                    // Filtra os usuários
                    allUsers.filter { user ->
                        user.name.contains(query, ignoreCase = true) ||
                        user.email.contains(query, ignoreCase = true)
                    }
                }

                // Volta automaticamente para Dispatchers.Main aqui
                // Atualiza o estado com os resultados
                _searchState.value = _searchState.value.copy(
                    results = filteredUsers,
                    isLoading = false,
                    elapsedTimeMs = elapsedTime
                )
            }
        } catch (e: Exception) {
            // Se a busca foi cancelada, não faz nada
            if (e is kotlinx.coroutines.CancellationException) {
                throw e
            }

            // Caso contrário, exibe um erro
            _searchState.value = _searchState.value.copy(
                isLoading = false,
                errorMessage = "Erro na busca: ${e.message}"
            )
        }
    }
}
```

---

## Passo 2: Criar a UI com Compose (ou XML)

### Opção A: Jetpack Compose

Crie um arquivo `UserSearchScreen.kt`:

```kotlin
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun UserSearchScreen(viewModel: UserSearchViewModel = viewModel()) {
    val state = viewModel.searchState.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Campo de busca
        TextField(
            value = state.value.query,
            onValueChange = { query -> viewModel.onSearchQueryChanged(query) },
            label = { Text("Buscar usuários") },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Status de carregamento e tempo
        if (state.value.isLoading) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Buscando...")
            }
        } else if (state.value.elapsedTimeMs > 0) {
            Text(
                "Busca concluída em ${state.value.elapsedTimeMs}ms",
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Mensagem de erro
        state.value.errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Lista de resultados
        if (state.value.results.isEmpty() && state.value.query.isNotEmpty()) {
            Text("Nenhum usuário encontrado.")
        } else {
            LazyColumn {
                items(state.value.results) { user ->
                    UserCard(user)
                }
            }
        }
    }
}

@Composable
fun UserCard(user: User) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = user.name, style = MaterialTheme.typography.bodyLarge)
            Text(text = user.email, style = MaterialTheme.typography.bodySmall)
        }
    }
}
```

### Opção B: XML + Views

Crie um arquivo `UserSearchActivity.kt`:

```kotlin
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.collect

class UserSearchActivity : AppCompatActivity() {
    
    private lateinit var viewModel: UserSearchViewModel
    private lateinit var searchEditText: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var statusText: TextView
    private lateinit var resultsList: RecyclerView
    private lateinit var adapter: UserAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_search)

        // Inicializar ViewModel
        viewModel = ViewModelProvider(this).get(UserSearchViewModel::class.java)

        // Referências às views
        searchEditText = findViewById(R.id.search_edit_text)
        progressBar = findViewById(R.id.progress_bar)
        statusText = findViewById(R.id.status_text)
        resultsList = findViewById(R.id.results_list)

        // Configurar RecyclerView
        adapter = UserAdapter()
        resultsList.layoutManager = LinearLayoutManager(this)
        resultsList.adapter = adapter

        // Listener para mudanças na query
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.onSearchQueryChanged(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Observar mudanças no estado
        lifecycleScope.launchWhenStarted {
            viewModel.searchState.collect { state ->
                // Atualizar UI
                progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                statusText.text = when {
                    state.isLoading -> "Buscando..."
                    state.elapsedTimeMs > 0 -> "Busca concluída em ${state.elapsedTimeMs}ms"
                    else -> ""
                }
                adapter.submitList(state.results)
            }
        }
    }
}

class UserAdapter : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {
    private var users: List<User> = emptyList()

    fun submitList(list: List<User>) {
        users = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): UserViewHolder {
        val view = android.widget.LinearLayout(parent.context).apply {
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(users[position])
    }

    override fun getItemCount() = users.size

    class UserViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        fun bind(user: User) {
            val linearLayout = itemView as android.widget.LinearLayout
            linearLayout.removeAllViews()
            
            val nameText = android.widget.TextView(itemView.context).apply {
                text = user.name
                textSize = 16f
            }
            val emailText = android.widget.TextView(itemView.context).apply {
                text = user.email
                textSize = 12f
            }
            
            linearLayout.addView(nameText)
            linearLayout.addView(emailText)
        }
    }
}
```

---

## Passo 3: Arquivo de Layout (XML)

Se usar a opção B, crie `res/layout/activity_user_search.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp">

    <EditText
        android:id="@+id/search_edit_text"
        android:layout_width="match_parent"
        android:layout_height="48dp"
        android:hint="Buscar usuários"
        android:inputType="text" />

    <ProgressBar
        android:id="@+id/progress_bar"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="16dp"
        android:visibility="gone" />

    <TextView
        android:id="@+id/status_text"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="8dp"
        android:textSize="12sp" />

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/results_list"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:layout_marginTop="16dp" />
</LinearLayout>
```

---

## Checkpoints para Validação

Enquanto executa o exercício, valide:

### ✅ Checkpoint 1: Debounce Funcionando
- **O que fazer:** Abra o app e comece a digitar rapidamente "ana".
- **O que esperar:** O carregamento só aparece após você parar de digitar por 500ms.
- **Por quê:** O `delay(500)` no `onSearchQueryChanged` implementa o debounce.

### ✅ Checkpoint 2: Cancelamento Automático
- **O que fazer:** Comece a digitar "ana", depois rapidamente apague e digite "bru".
- **O que esperar:** A busca anterior é cancelada e você só vê resultados para "bru".
- **Por quê:** O `searchJob?.cancel()` cancela a busca em andamento antes de lançar uma nova.

### ✅ Checkpoint 3: Sem Travamento da UI
- **O que fazer:** Enquanto a busca está em andamento, tente digitar novamente no campo.
- **O que esperar:** O app responde normalmente, o campo aceita mais digitação.
- **Por quê:** Usamos `Dispatchers.IO` para a busca e voltamos automaticamente com `withContext`.

### ✅ Checkpoint 4: Tempo Medido Corretamente
- **O que fazer:** Veja o tempo exibido ("Busca concluída em Xms").
- **O que esperar:** Deve ser próximo de 1000ms (o `delay(1000)` na simula de rede).
- **Por quê:** `measureTimeMillis` mede o tempo total da operação suspensa.

### ✅ Checkpoint 5: Ciclo de Vida Respeitado
- **O que fazer:** Abra o app, inicie uma busca, depois feche a Activity/Fragment.
- **O que esperar:** A busca é cancelada automaticamente, sem erros no logcat.
- **Por quê:** `viewModelScope` é cancelado quando o ViewModel é destruído.

---

## Desafios Opcionais

Se quiser ir além, tente:

1. **Adicionar histórico de buscas**: Guarde as últimas 5 buscas em `SharedPreferences`.
2. **Implementar pagination**: Mostre apenas 5 resultados por página, com botão "Carregar mais".
3. **Adicionar filtros**: Permita filtrar por domínio de email ("@example.com", "@gmail.com", etc.).
4. **Melhorar a UI**: Adicione ícones, cores, animações ao exibir/ocultar o progresso.
5. **Testar com Coroutine Rules**: Use `MainDispatcherRule` do `kotlinx.coroutines.test` para testar o ViewModel.

---

## Resumo do Que Você Aprendeu

| Conceito | Onde Aparece neste Exercício |
|---|---|
| **Dispatchers.IO** | `withContext(Dispatchers.IO)` — simula busca de rede |
| **Dispatchers.Main** | Automático ao retornar do `withContext` — atualiza UI |
| **Cancelamento Cooperativo** | `searchJob?.cancel()` — cancela busca anterior |
| **Concorrência Estruturada** | `viewModelScope` — gerencia o ciclo de vida |
| **`suspend fun`** | `private suspend fun search()` — marca função que pode pausar |
| **`withContext`** | Troca de thread de forma segura |
| **`delay()`** | Implementa debounce e simula latência |
| **StateFlow** | Expõe o estado reativo para a UI observar |
| **Medição de Tempo** | `measureTimeMillis` — mostra desempenho real |

Parabéns! Você acabou de construir um exemplo real de coroutines, do tipo que você vai usar em produção. 🎉
