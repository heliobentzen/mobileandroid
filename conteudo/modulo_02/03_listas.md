# Listas com Jetpack Compose (LazyColumn, estado e chaves)

Em Jetpack Compose não usamos `RecyclerView`, `ListAdapter` ou `DiffUtil`. A renderização e atualização são automáticas via recomposição. Você precisa apenas:
1. Manter o estado da lista como uma coleção observável.
2. Fornecer chaves estáveis aos itens (quando necessário).
3. Atualizar a lista criando nova instância ou alterando o estado.

---

## Conceitos-Chave

- **`LazyColumn`**: Equivalente moderno ao RecyclerView para listas verticais.
- **`items()`**: Emite cada item da lista; aceita `key` para estabilidade.
- **Estado**: Use `remember { mutableStateListOf<T>() }` ou `var list by remember { mutableStateOf(listOf<T>()) }`.
- **Diferenças**: Compose reconcilia automaticamente o que mudou; chaves ajudam a preservar identidade visual.
- **Animações**: Use `Modifier.animateItemPlacement()` (opcional) para animações de reposicionamento.

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
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = user.name)
    }
}
```

---

## Passo 3: LazyColumn com Estado

```kotlin
@Composable
fun UserList(users: List<User>) {
    LazyColumn {
        items(users, key = { it.id }) { user ->
            UserItem(user = user, modifier = Modifier.animateItemPlacement())
        }
    }
}
```

---

## Passo 4: Animações Avançadas

Adicione animações para inserção e remoção de itens:

```kotlin
@Composable
fun AnimatedUserList() {
    var users by remember { mutableStateOf(sampleUsers) }

    LazyColumn {
        items(users, key = { it.id }) { user ->
            UserItem(user = user, modifier = Modifier.animateItemPlacement())
        }
    }

    Button(onClick = {
        users = users.toMutableList().apply {
            add(User(id = users.size + 1, name = "Novo Usuário", avatarUrl = ""))
        }
    }) {
        Text("Adicionar Usuário")
    }
}
```

---

## Boas Práticas

1. **Use chaves estáveis**:
   - Sempre forneça uma chave única para cada item em `LazyColumn` para evitar problemas de desempenho.

2. **Gerencie estado corretamente**:
   - Use `remember` para manter o estado local e `ViewModel` para estado compartilhado.

3. **Evite recomposições desnecessárias**:
   - Certifique-se de que os itens da lista sejam imutáveis.

---

## Exercícios Práticos

1. **Lista Simples**:
   - Crie uma lista de tarefas com `LazyColumn` e permita marcar tarefas como concluídas.

2. **Animações**:
   - Adicione animações para remoção de itens da lista.

3. **Desafio**:
   - Implemente uma lista com carregamento paginado (infinite scroll) usando `LazyColumn` e `remember`.

---