# Listas com Jetpack Compose (LazyColumn, estado e chaves)

Em Jetpack Compose não usamos `RecyclerView`, `ListAdapter` ou `DiffUtil` (as ferramentas clássicas do Android baseado em Views/XML para listas eficientes). A renderização e atualização são automáticas via **recomposição** — o processo pelo qual o Compose redesenha (executa novamente) uma função `@Composable` quando algum dado que ela lê muda. Você precisa apenas:
1. Manter o estado da lista como uma coleção observável.
2. Fornecer chaves estáveis aos itens (quando necessário).
3. Atualizar a lista criando nova instância ou alterando o estado.

## O que é o `LazyColumn`?

`LazyColumn` é o componente do Compose para exibir listas verticais de forma eficiente. A palavra "Lazy" (preguiçoso) é o ponto-chave: ele só cria e desenha os itens que estão **visíveis na tela** (mais uma pequena margem), em vez de desenhar a lista inteira de uma vez. Se sua lista tem 10.000 itens, o `LazyColumn` desenha só os ~10-15 que cabem na tela naquele momento, e cria os próximos conforme o usuário rola (faz scroll).

## Por que isso importa

Imagine desenhar uma lista de 10.000 itens de uma vez, sem essa otimização: o app gastaria memória e tempo de processamento enormes só para preparar itens que o usuário nunca vai ver naquele instante, deixando a tela lenta ou até travando (ANR — "app não está respondendo"). O `LazyColumn` resolve isso automaticamente, sem você precisar escrever nenhuma lógica de reciclagem manual como no antigo `RecyclerView`.

---

## Conceitos-Chave

- **`LazyColumn`**: equivalente moderno ao `RecyclerView` para listas verticais — mas sem precisar de `Adapter`, `ViewHolder` ou `LayoutManager`; o Compose cuida de tudo isso internamente.
- **`items()`**: função usada dentro do bloco do `LazyColumn` para emitir cada item da lista; aceita um parâmetro `key` para dar estabilidade a cada item (explicado abaixo).
- **Estado**: a forma como o Compose sabe que a lista mudou e precisa recompor. Use `remember { mutableStateListOf<T>() }` (uma lista observável mutável) ou `var list by remember { mutableStateOf(listOf<T>()) }` (uma variável observável que aponta para uma lista imutável nova a cada mudança).
- **Por que chaves importam?**: quando a lista muda (itens reordenados, removidos ou inseridos), o Compose precisa de chaves para distinguir cada item de forma única. Sem chaves, o Compose identifica os itens apenas pela posição no índice — isso pode causar perda de posição de scroll, estados internos (como campos de texto preenchidos) associados ao item errado, e animações quebradas. Com chaves estáveis (ex.: `key = { it.id }`), o Compose rastreia a identidade de cada item corretamente, mesmo quando a ordem muda.
- **Animações**: use `Modifier.animateItemPlacement()` (opcional) para animar a reposição de itens quando a lista muda de ordem.

---

## Passo 1: Modelo de Dados

O primeiro passo é definir o formato dos dados que a lista vai exibir. Aqui usamos uma `data class` — um tipo do Kotlin próprio para guardar dados, que já vem com `equals()`, `toString()` e `copy()` prontos.

`User.kt`
```kotlin
data class User(
    val id: Int,          // identificador único — será usado como chave estável na lista
    val name: String,
    val avatarUrl: String
)
```

---

## Passo 2: Composable do Item

Este composable define como **um único** usuário é desenhado. Ele é reutilizado para cada item da lista.

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
        // Exemplo simples sem carregamento real de imagem (em um app real,
        // você usaria uma biblioteca como Coil para carregar avatarUrl)
        Image(
            painter = painterResource(id = R.mipmap.ic_launcher_round),
            contentDescription = "Avatar", // descrição para leitores de tela (acessibilidade)
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = user.name)
    }
}
```

---

## Passo 3: LazyColumn com Estado

Agora juntamos tudo: uma lista de `User` sendo exibida dentro de um `LazyColumn`.

```kotlin
@Composable
fun UserList(users: List<User>) {
    LazyColumn {
        // key = { it.id } — cada item é identificado pelo seu id, não pela posição.
        // Isso é essencial para o Compose preservar corretamente scroll e estado
        // ao inserir, remover ou reordenar usuários.
        items(users, key = { it.id }) { user ->
            UserItem(user = user, modifier = Modifier.animateItemPlacement())
        }
    }
}
```

---

## Passo 4: Animações Avançadas

Adicione animações para inserção e remoção de itens usando `Modifier.animateItemPlacement()`, que anima suavemente a mudança de posição de um item quando a lista é reordenada.

```kotlin
@Composable
fun AnimatedUserList() {
    // "by" delega a leitura/escrita direto ao valor guardado no State<List<User>>,
    // então "users" se comporta como uma List<User> normal, mas observável
    var users by remember { mutableStateOf(sampleUsers) }

    LazyColumn {
        items(users, key = { it.id }) { user ->
            UserItem(user = user, modifier = Modifier.animateItemPlacement())
        }
    }

    Button(onClick = {
        // Criamos uma NOVA lista (toMutableList().apply{...}) em vez de alterar
        // "users" por dentro. Isso é o que dispara a recomposição corretamente.
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

1. **Use chaves estáveis**

   Sempre forneça uma chave única para cada item em `LazyColumn`. Sem isso, o Compose usa a posição (índice) do item como identidade — o que causa bugs sutis quando a lista muda de ordem.

   ❌ Sem chave — Compose usa o índice e pode confundir itens ao reordenar:
   ```kotlin
   items(users) { user ->
       UserItem(user = user)
   }
   ```
   ✅ Com chave estável — Compose preserva identidade, scroll e estado de cada item:
   ```kotlin
   items(users, key = { it.id }) { user ->
       UserItem(user = user)
   }
   ```

2. **Gerencie estado corretamente**

   Use `remember` para manter o estado local de uma tela (que não precisa sobreviver a rotação nem ser compartilhado) e `ViewModel` para estado compartilhado ou que precisa sobreviver a mudanças de configuração (veja a aula `01_mvvm.md`).

3. **Evite recomposições desnecessárias**

   Certifique-se de que os itens da lista sejam **imutáveis** (`data class` com `val`, nunca `var`). Isso permite ao Compose comparar itens de forma confiável e pular a recomposição de itens que não mudaram, economizando processamento.

---

## Erros comuns / Pegadinhas

- **Não usar `key` no `items()`**: como explicado acima, isso confunde a identidade dos itens ao reordenar/inserir/remover, causando bugs visuais (scroll pulando, campo de texto errado recebendo o valor de outro item).
- **Usar `id` do índice da lista como chave (`key = { index }`)**: isso é equivalente a não usar chave nenhuma — o índice muda toda vez que a lista é reordenada. A chave precisa vir de um identificador **estável do próprio dado** (ex: `user.id`), não da posição.
- **Alterar a lista "por dentro" em vez de criar uma nova**: se você mutar uma `MutableList` já usada em `mutableStateOf` sem trocar a referência, o Compose pode não perceber a mudança e a tela não é atualizada. Prefira criar uma nova lista (como no exemplo de `AnimatedUserList`) ou usar `mutableStateListOf`, que já é observável item a item.

---

## Resumo

- `LazyColumn` desenha só os itens visíveis na tela, economizando memória e processamento — dispensa `Adapter`/`ViewHolder`.
- `items(lista, key = { ... })` é a forma de emitir cada item; sempre forneça uma chave estável baseada em um identificador único do dado.
- O estado da lista deve ser observável (`mutableStateOf`, `mutableStateListOf`) e atualizado criando novas instâncias, nunca mutando "por dentro" sem trocar a referência.
- `Modifier.animateItemPlacement()` anima a reposição de itens quando a ordem muda.

**Próximo passo**: na próxima aula (`04_navegacao.md`) você vai aprender a navegar entre telas — por exemplo, ao clicar em um item da lista, abrir uma tela de detalhes desse item.

---

## Exercícios Práticos

1. **Lista Simples**
   - Checkpoint 1: crie uma `data class Tarefa(val id: Int, val titulo: String, val concluida: Boolean)`.
   - Checkpoint 2: crie uma lista de tarefas com `LazyColumn`, usando `key = { it.id }`.
   - Checkpoint 3: permita marcar tarefas como concluídas usando um `Checkbox` dentro de cada item (semelhante ao `TaskItem` da aula de MVVM).

2. **Animações**
   - Adicione animações para remoção de itens da lista usando `animateItemPlacement()` — remova um item e observe como os itens abaixo dele "deslizam" suavemente para cima.

3. **Desafio**
   - Implemente uma lista com carregamento paginado (infinite scroll) usando `LazyColumn` e `remember`. Dica: use `LazyListState` e observe o índice do último item visível (`listState.layoutInfo.visibleItemsInfo`) para detectar quando o usuário chegou perto do fim da lista e carregar mais itens.

---
