# Listas com Jetpack Compose (LazyColumn, estado e chaves)

Em Jetpack Compose não usamos `RecyclerView`, `ListAdapter` ou `DiffUtil`. A renderização e atualização são automáticas via recomposição. Você precisa apenas:
1. Manter o estado da lista como uma coleção observável.
2. Fornecer chaves estáveis aos itens (quando necessário).
3. Atualizar a lista criando nova instância ou alterando o estado.

---

## Conceitos-Chave

- `LazyColumn`: Equivalente moderno ao RecyclerView para listas verticais.
- `items()`: Emite cada item da lista; aceita `key` para estabilidade.
- Estado: Use `remember { mutableStateListOf<T>() }` ou `var list by remember { mutableStateOf(listOf<T>()) }`.
- Diferenças: Compose reconcilia automaticamente o que mudou; chaves ajudam a preservar identidade visual.
- Animações: Use `Modifier.animateItemPlacement()` (opcional) para animações de reposicionamento.

---

## Passo 1: Modelo de Dados

`User.kt`
```kotlin
data class User(
    val id: Int,
    val name: String,
    val avatarUrl: String
)
```

---

## Passo 2: Composable do Item

```kotlin
@Composable
fun UserItem(
    user: User,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Exemplo simples sem carregamento real de imagem
        Image(
            painter = painterResource(id = R.mipmap.ic_launcher_round),
            contentDescription = "Avatar",
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = user.name,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
```

Para carregar imagens remotas, use Coil:
```kotlin
// implementation("io.coil-kt:coil-compose:<versao>")
Image(
    painter = rememberAsyncImagePainter(user.avatarUrl),
    contentDescription = null,
    modifier = Modifier.size(48.dp).clip(CircleShape)
)
```

---

## Passo 3: Lista com LazyColumn

```kotlin
@Composable
fun UserList(
    users: List<User>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize()
    ) {
        items(
            items = users,
            key = { it.id } // Preserva identidade
        ) { user ->
            UserItem(
                user = user,
                modifier = Modifier.animateItemPlacement()
            )
            Divider()
        }
    }
}
```

---

## Passo 4: Tela Integrando Estado

```kotlin
@Composable
fun UserScreen() {
    // Estado inicial
    var users by remember {
        mutableStateOf(
            listOf(
                User(1, "Ana", "url_avatar_1"),
                User(2, "Bruno", "url_avatar_2"),
                User(3, "Carlos", "url_avatar_3")
            )
        )
    }

    // Simula atualização após 3 segundos
    LaunchedEffect(Unit) {
        delay(3000)
        users = listOf(
            User(1, "Ana Silva", "url_avatar_1"), // Alterado
            User(3, "Carlos", "url_avatar_3"),    // Reposicionado
            User(4, "Daniela", "url_avatar_4")    // Novo
            // Removido: id 2
        )
    }

    UserList(users = users)
}
```

---

## Activity com Compose

`MainActivity.kt`
```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                UserScreen()
            }
        }
    }
}
```

---

## Vantagens em Compose

1. Sem Adapter ou DiffUtil manual.
2. Menos boilerplate, tudo é função.
3. Atualizações reativas simples via mudança de estado.
4. Chaves mantêm estabilidade e evitam recriações desnecessárias.
5. Fácil adicionar animações e gestos.

---

## Exercício Prático: Lista de Tarefas Interativa

Objetivo: Criar lista de tarefas com adicionar, remover e marcar concluída.

### 1. Modelo
```kotlin
data class Task(
    val id: Long,
    val title: String,
    val isCompleted: Boolean
)
```

### 2. Item (`TaskItem`)
```kotlin
@Composable
fun TaskItem(
    task: Task,
    onToggle: (Task) -> Unit,
    onDelete: (Task) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = task.isCompleted,
            onCheckedChange = { onToggle(task) }
        )
        Text(
            text = task.title,
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
            style = if (task.isCompleted)
                MaterialTheme.typography.bodyLarge.copy(textDecoration = TextDecoration.LineThrough)
            else MaterialTheme.typography.bodyLarge
        )
        IconButton(onClick = { onDelete(task) }) {
            Icon(Icons.Default.Delete, contentDescription = "Remover")
        }
    }
}
```

### 3. Lista
```kotlin
@Composable
fun TaskList(
    tasks: List<Task>,
    onToggle: (Task) -> Unit,
    onDelete: (Task) -> Unit
) {
    LazyColumn {
        items(tasks, key = { it.id }) { task ->
            TaskItem(
                task = task,
                onToggle = onToggle,
                onDelete = onDelete,
                modifier = Modifier.animateItemPlacement()
            )
            Divider()
        }
    }
}
```

### 4. Tela Completa
```kotlin
@Composable
fun TaskScreen() {
    var tasks by remember { mutableStateOf(listOf<Task>()) }
    var input by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row {
            TextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Nova tarefa") }
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    if (input.isNotBlank()) {
                        tasks = tasks + Task(
                            id = System.currentTimeMillis(),
                            title = input.trim(),
                            isCompleted = false
                        )
                        input = ""
                    }
                }
            ) { Text("Adicionar") }
        }
        Spacer(Modifier.height(16.dp))
        TaskList(
            tasks = tasks,
            onToggle = { t ->
                tasks = tasks.map {
                    if (it.id == t.id) it.copy(isCompleted = !it.isCompleted) else it
                }
            },
            onDelete = { t ->
                tasks = tasks.filter { it.id != t.id }
            }
        )
    }
}
```

### 5. Uso na Activity
```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                TaskScreen()
            }
        }
    }
}
```

---

## Dicas Finais

- Prefira `immutable` + recriar lista (ex: `tasks = tasks + novoItem`) para clareza.
- Use chaves (`key = { it.id }`) ao animar ou preservar estado interno.
- Para listas grandes, otimize usando tipos estáveis (`@Stable`) se necessário.
- Combine com `LazyColumn` + `rememberLazyListState()` para scroll controlado.
- Animações extras: `AnimatedVisibility`, `animateContentSize()`, `crossfade()`.

---

## Resumo

Compose elimina a necessidade de Adapter e DiffUtil: o estado dirige a UI. Atualize a lista, recomposição cuida do resto. Menos código, mais legibilidade, fácil ampliar com eventos, animações e side-effects.