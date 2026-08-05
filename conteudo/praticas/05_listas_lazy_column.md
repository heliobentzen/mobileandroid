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

Este é o ponto de partida para qualquer lista no Compose. A diferença entre um `LazyColumn` e uma `Column` comum pode não parecer relevante em uma lista pequena de 8 itens, mas se torna crítica quando a lista tem centenas ou milhares de itens (um catálogo de produtos, um feed de posts): o `LazyColumn` só desenha o que está visível na tela, economizando processamento e memória do dispositivo do usuário.

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

> **💡 Por trás dos panos**
> Dentro do bloco `LazyColumn { ... }` você não escreve composables diretamente — você descreve um "roteiro" usando funções como `items(...)` e `item { }`, que dizem ao Compose quais itens existem e como desenhá-los. O Compose usa isso para calcular só o que precisa desenhar naquele momento, e recicla os componentes visuais conforme o usuário rola a tela — de forma parecida com o antigo `RecyclerView`, mas sem você precisar escrever o código de reciclagem manualmente.

### Exercícios

1. Adicione um cabeçalho (`item { }`) acima da lista com o texto `"Melhores filmes de todos os tempos"`.
   - *Dica se travar*: o `item { }` vai dentro do mesmo bloco `LazyColumn { }`, antes da chamada de `items(...)`.
2. Adicione um rodapé (`item { }`) ao final da lista mostrando quantos filmes existem no total.
3. Ordene a lista por nota (do maior para o menor) antes de exibir.
   - *Dica se travar*: `filmes.sortedByDescending { it.nota }` retorna uma nova lista já ordenada, sem precisar de loop manual.

---

## Prática 2: Lista com Cabeçalhos Agrupados

### Objetivo
Exibir itens agrupados por categoria usando `itemsIndexed` e cabeçalhos.

Listas agrupadas por categoria são muito comuns: contatos por letra inicial (como nesta prática), produtos por seção em um app de compras, mensagens por data. Misturar cabeçalhos e itens normais dentro do mesmo `LazyColumn` é uma técnica que você vai reutilizar sempre que precisar organizar dados em grupos visuais.

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

> **💡 Por trás dos panos**
> `contatos.groupBy { it.nome.first().uppercase() }` transforma a lista original em um `Map<String, List<Contato>>` — as chaves são as letras ("A", "B", "C"...) e os valores são as listas de contatos daquela letra. Ao percorrer esse mapa com `agrupado.forEach { (letra, lista) -> ... }`, cada grupo gera um `item` de cabeçalho seguido de vários `items` de conteúdo — tudo dentro do mesmo `LazyColumn`, que trata cabeçalhos e itens normais da mesma forma ao rolar a tela.

### Exercícios

1. Adicione uma barra de pesquisa acima da lista. Ao digitar, filtre os contatos pelo nome em tempo real.
   - *Dica se travar*: guarde o texto digitado em um `remember { mutableStateOf("") }` e aplique `.filter { it.nome.contains(texto, ignoreCase = true) }` na lista de contatos antes de agrupar.
2. Exiba o número de contatos ao lado de cada letra de cabeçalho (ex.: `"A (2)"`).
3. Permita clicar em um contato para selecioná-lo (mude a cor de fundo ao selecionar). Use `remember { mutableStateOf<Int?>(null) }` para guardar o ID selecionado.
   - *Dica se travar*: compare `contato.id == selecionadoId` para decidir a cor de fundo de cada item.

---

## Prática 3: Lista Dinâmica com Adição e Remoção

### Objetivo
Gerenciar uma lista que o usuário pode editar em tempo real.

Nas práticas anteriores, a lista era fixa (definida no código). Aqui o usuário mesmo adiciona e remove itens, o que é o cenário mais comum em apps reais (listas de tarefas, carrinho de compras, notas). O ponto chave é entender como o estado da lista (`notas`) precisa ser recriado a cada mudança para o Compose perceber que algo mudou e redesenhar a tela.

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
                    val texto = textoNova.trim() // remove espaços extras no início/fim
                    if (texto.isNotEmpty()) {
                        // 'notas + Nota(...)' cria uma NOVA lista com o item adicionado ao final.
                        // Não modificamos a lista antiga — substituímos 'notas' por essa nova lista,
                        // o que faz o Compose perceber a mudança e redesenhar a LazyColumn.
                        notas = notas + Nota(id = proximoId++, texto = texto)
                        textoNova = "" // limpa o campo de texto depois de adicionar
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
                            // filter cria uma nova lista sem o item removido (mantém todos os outros)
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

> **💡 Por trás dos panos**
> Note que usamos `var notas by remember { mutableStateOf(emptyList<Nota>()) }` em vez de `mutableStateListOf` (visto na Prática 1 deste guia). Com `mutableStateOf`, toda alteração precisa criar uma lista nova (`notas + item`, `notas.filter { ... }`) para o Compose perceber a mudança. Já `mutableStateListOf` permite alterar a lista existente diretamente (`.add()`, `.remove()`). Ambas as abordagens funcionam — a escolha costuma depender de você preferir imutabilidade explícita (mais próxima do estilo usado em MVVM com `StateFlow`) ou conveniência local.

### Exercícios

1. Adicione a funcionalidade de **editar** uma nota. Ao clicar no texto da nota, abra um `AlertDialog` com um campo de texto pré-preenchido.
   - *Dica se travar*: guarde qual nota está sendo editada em um `remember { mutableStateOf<Nota?>(null) }` e mostre o diálogo apenas quando esse valor não for nulo.
2. Adicione um botão `"Limpar todas"` que exibe um diálogo de confirmação antes de apagar as notas.
3. Mostre no topo da lista o número total de notas.

---

## Prática 4: Lista com Scroll e Sticky Headers

### Objetivo
Criar uma lista de produtos por categoria com cabeçalhos que ficam fixos enquanto o usuário rola.

Sticky headers (cabeçalhos "grudentos") são um detalhe de UX muito usado em apps de compras e organizadores de conteúdo — pense em como o cabeçalho de uma seção de contatos no seu celular fica fixo no topo enquanto você rola. Esse recurso ajuda o usuário a sempre saber em qual grupo/categoria ele está, mesmo em listas longas.

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

> **💡 Por trás dos panos**
> `stickyHeader` funciona parecido com `item`, mas com uma diferença: enquanto os itens normais rolam livremente para fora da tela, o `stickyHeader` mais recente fica "grudado" no topo até que o próximo cabeçalho o empurre para fora. O `@OptIn(ExperimentalFoundationApi::class)` no topo da função avisa o compilador que você está ciente de usar uma API que ainda pode mudar em versões futuras do Compose — é comum encontrar esse aviso em recursos mais novos da biblioteca.

### Exercícios

1. Adicione um filtro por categoria: crie um `Row` com chips clicáveis no topo da tela. Ao selecionar uma categoria, exiba apenas os produtos dela.
   - *Dica se travar*: use `FilterChip` do Material3 para cada categoria, e um `remember { mutableStateOf<String?>(null) }` para guardar a categoria selecionada (`null` = mostrar todas).
2. Ordene os produtos dentro de cada categoria por preço (do menor para o maior).
   - *Dica se travar*: aplique `.sortedBy { it.preco }` na lista de cada categoria, dentro do `forEach`, antes de passar para `items(...)`.
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
