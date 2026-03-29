# Prática: Listas com LazyColumn para Iniciantes

Este guia apresenta exercícios práticos para criar e gerenciar listas no Jetpack Compose usando `LazyColumn`. O Compose não usa `RecyclerView` — o `LazyColumn` é sua ferramenta principal para listas longas e dinâmicas.

---

## Por que LazyColumn?

- **Eficiente**: renderiza apenas os itens visíveis na tela (como o `RecyclerView`).
- **Simples**: não precisa de adapter, ViewHolder ou DiffUtil.
- **Reativo**: quando a lista de dados muda, a UI atualiza automaticamente.

---

## Prática 1: Lista Estática Simples

### Objetivo
Criar uma lista básica de itens com `LazyColumn`.

### Passo a Passo

1. Crie o arquivo `ListaFilmes.kt`:

```kotlin
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

data class Filme(
    val id: Int,
    val titulo: String,
    val ano: Int,
    val nota: Float
)

val filmesExemplo = listOf(
    Filme(1, "O Poderoso Chefão", 1972, 9.2f),
    Filme(2, "Clube da Luta", 1999, 8.8f),
    Filme(3, "Matrix", 1999, 8.7f),
    Filme(4, "Pulp Fiction", 1994, 8.9f),
    Filme(5, "Forrest Gump", 1994, 8.8f),
    Filme(6, "O Senhor dos Anéis", 2001, 8.9f),
    Filme(7, "Inception", 2010, 8.8f),
    Filme(8, "Interestelar", 2014, 8.6f)
)

@Composable
fun ItemFilme(filme: Filme) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = filme.titulo, fontWeight = FontWeight.Bold)
                Text(
                    text = "${filme.ano}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Text(
                text = "⭐ ${filme.nota}",
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun ListaFilmes(filmes: List<Filme> = filmesExemplo) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(
            items = filmes,
            key = { it.id }    // Chave única para melhor desempenho
        ) { filme ->
            ItemFilme(filme = filme)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ListaFilmesPreview() {
    MaterialTheme { ListaFilmes() }
}
```

2. Abra o Preview e veja a lista.

### Exercícios

1. Adicione um cabeçalho (`item { }`) acima da lista com o texto `"Melhores filmes de todos os tempos"`.
2. Adicione um rodapé (`item { }`) ao final da lista mostrando quantos filmes existem no total.
3. Ordene a lista por nota (do maior para o menor) antes de exibir.

---

## Prática 2: Lista com Cabeçalhos Agrupados

### Objetivo
Exibir itens agrupados por categoria usando `itemsIndexed` e cabeçalhos.

### Passo a Passo

```kotlin
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class Contato(val id: Int, val nome: String, val telefone: String)

val contatos = listOf(
    Contato(1, "Ana Lima", "(11) 9999-1111"),
    Contato(2, "André Costa", "(11) 9999-2222"),
    Contato(3, "Beatriz Souza", "(21) 9999-3333"),
    Contato(4, "Bruno Mendes", "(21) 9999-4444"),
    Contato(5, "Carlos Silva", "(31) 9999-5555"),
    Contato(6, "Carla Rocha", "(31) 9999-6666")
).sortedBy { it.nome }

@Composable
fun CabecalhoLetra(letra: String) {
    Text(
        text = letra,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 4.dp)
    )
}

@Composable
fun ItemContato(contato: Contato) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(text = contato.nome, fontWeight = FontWeight.Medium)
        Text(
            text = contato.telefone,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
    HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
}

@Composable
fun ListaContatos() {
    // Agrupando por primeira letra do nome
    val agrupado = contatos.groupBy { it.nome.first().uppercase() }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        agrupado.forEach { (letra, lista) ->
            // Cabeçalho da seção
            item(key = "header_$letra") {
                CabecalhoLetra(letra)
            }
            // Itens da seção
            items(items = lista, key = { it.id }) { contato ->
                ItemContato(contato)
            }
        }
    }
}
```

### Exercícios

1. Adicione uma barra de pesquisa acima da lista. Ao digitar, filtre os contatos pelo nome em tempo real.
2. Exiba o número de contatos ao lado de cada letra de cabeçalho (ex.: `"A (2)"`).
3. Permita clicar em um contato para selecioná-lo (mude a cor de fundo ao selecionar). Use `remember { mutableStateOf<Int?>(null) }` para guardar o ID selecionado.

---

## Prática 3: Lista Dinâmica com Adição e Remoção

### Objetivo
Gerenciar uma lista que o usuário pode editar em tempo real.

### Passo a Passo

```kotlin
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class Nota(val id: Int, val texto: String)

@Composable
fun ListaNotas() {
    var notas by remember { mutableStateOf(emptyList<Nota>()) }
    var proximoId by remember { mutableStateOf(1) }
    var textoNova by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Minhas Notas", style = MaterialTheme.typography.headlineSmall)

        Spacer(Modifier.height(8.dp))

        // Campo para adicionar nova nota
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = textoNova,
                onValueChange = { textoNova = it },
                placeholder = { Text("Digite uma nota...") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    val texto = textoNova.trim()
                    if (texto.isNotEmpty()) {
                        notas = notas + Nota(id = proximoId++, texto = texto)
                        textoNova = ""
                    }
                }
            ) {
                Text("Adicionar")
            }
        }

        Spacer(Modifier.height(8.dp))

        if (notas.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nenhuma nota ainda. Adicione acima! 📝")
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(notas, key = { it.id }) { nota ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = nota.texto,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { notas = notas.filter { it.id != nota.id } }) {
                                Icon(Icons.Default.Delete, contentDescription = "Excluir nota")
                            }
                        }
                    }
                }
            }
        }
    }
}
```

### Exercícios

1. Adicione a funcionalidade de **editar** uma nota. Ao clicar no texto da nota, abra um `AlertDialog` com um campo de texto pré-preenchido.
2. Adicione um botão `"Limpar todas"` que exibe um diálogo de confirmação antes de apagar as notas.
3. Mostre no topo da lista o número total de notas.

---

## Prática 4: Lista com Scroll e Sticky Headers

### Objetivo
Criar uma lista de produtos por categoria com cabeçalhos que ficam fixos enquanto o usuário rola.

### Passo a Passo

```kotlin
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class Produto(val id: Int, val nome: String, val preco: Double, val categoria: String)

val produtos = listOf(
    Produto(1, "Notebook", 3500.0, "Eletrônicos"),
    Produto(2, "Smartphone", 1800.0, "Eletrônicos"),
    Produto(3, "Fone de ouvido", 350.0, "Eletrônicos"),
    Produto(4, "Camiseta", 79.9, "Roupas"),
    Produto(5, "Calça Jeans", 149.9, "Roupas"),
    Produto(6, "Tênis", 299.9, "Roupas"),
    Produto(7, "Arroz 5kg", 25.9, "Alimentos"),
    Produto(8, "Feijão 1kg", 12.5, "Alimentos"),
    Produto(9, "Azeite 500ml", 38.0, "Alimentos")
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ListaProdutos() {
    val porCategoria = produtos.groupBy { it.categoria }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        porCategoria.forEach { (categoria, lista) ->
            // stickyHeader: fica fixo no topo enquanto a seção está visível
            stickyHeader(key = categoria) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = categoria,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            items(lista, key = { it.id }) { produto ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(produto.nome)
                    Text("R$ ${"%.2f".format(produto.preco)}", color = MaterialTheme.colorScheme.primary)
                }
                HorizontalDivider()
            }
        }
    }
}
```

### Exercícios

1. Adicione um filtro por categoria: crie um `Row` com chips clicáveis no topo da tela. Ao selecionar uma categoria, exiba apenas os produtos dela.
2. Ordene os produtos dentro de cada categoria por preço (do menor para o maior).
3. Adicione um botão `"Adicionar ao carrinho"` em cada produto e mantenha um contador de itens no carrinho no topo da tela.

---

## Boas Práticas com LazyColumn

| Prática | Motivo |
|---------|--------|
| Sempre forneça `key` nos `items` | Evita recomposições desnecessárias e anima corretamente |
| Use `contentPadding` em vez de `Modifier.padding` | Garante que o padding não esconda o conteúdo ao rolar |
| Prefira `ElevatedCard` ou `Card` para itens | Dá profundidade visual e separa visualmente os itens |
| Não coloque `LazyColumn` dentro de `Column` com scroll | Causa conflito de gestos e comportamento imprevisível |
| Use `mutableStateListOf` para listas mutáveis reativas | Permite atualizações granulares sem recriar a lista inteira |

---

## Próximos Passos

- Estude o módulo `03_listas.md` para aprofundar o uso de `LazyColumn` com animações.
- Avance para `06_coroutines.md` para aprender a carregar listas de uma API de forma assíncrona.
- Combine com `04_mvvm_stateflow.md` para mover o estado das listas para um ViewModel.
