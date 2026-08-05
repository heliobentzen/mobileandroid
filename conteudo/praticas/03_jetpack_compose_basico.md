# Prática: Jetpack Compose Básico para Iniciantes

Este guia apresenta exercícios práticos para quem está começando a criar interfaces com Jetpack Compose. Cada prática é um exercício independente que você pode adicionar ao seu projeto de estudos.

---

## Pré-requisitos

- Projeto Android criado com o template **Empty Activity** (Compose).
- Dependências básicas do Compose já incluídas pelo template.

---

## Prática 1: Seu Primeiro Composable Personalizado

### Objetivo
Criar uma função `@Composable` simples e entender como ela aparece na tela.

Composables são os blocos de construção de toda interface no Jetpack Compose — é assim que se desenha qualquer coisa na tela: um texto, um botão, uma tela inteira. Diferente do sistema antigo de Views do Android (XML + `findViewById`), no Compose você descreve a interface com código Kotlin puro, e o próprio Compose se encarrega de desenhar e atualizar a tela. Entender como criar e reaproveitar um composable simples é o primeiro passo para construir qualquer tela do seu app.

### Passo a Passo

1. Abra o arquivo `MainActivity.kt` do seu projeto.
2. Fora da classe `MainActivity`, adicione o seguinte composable:

```kotlin
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// @Composable marca a função como parte da UI — o Compose sabe que pode "desenhá-la" na tela.
// O parâmetro 'nome' torna a função reutilizável: você pode chamá-la várias vezes com nomes diferentes.
@Composable
fun CartaoBoasVindas(nome: String) {
    Text(
        text = "Olá, $nome! 👋",
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold
    )
}

// @Preview permite visualizar o composable dentro do Android Studio sem rodar o app inteiro no emulador.
// A função de preview normalmente só chama o composable real com valores fixos de exemplo.
@Preview(showBackground = true)
@Composable
fun CartaoBoasVindasPreview() {
    CartaoBoasVindas(nome = "Estudante")
}
```

3. Abra o painel de **Preview** no Android Studio (ícone de tela dividida no canto superior direito) para ver o resultado sem rodar o emulador.
4. Substitua o conteúdo de `setContent { }` na `MainActivity` para exibir seu composable:

```kotlin
setContent {
    // MaterialTheme aplica cores, tipografia e formas consistentes a tudo dentro dele
    MaterialTheme {
        // Surface é um "fundo" que ocupa a tela toda e recebe a cor de fundo do tema
        Surface(modifier = Modifier.fillMaxSize()) {
            CartaoBoasVindas(nome = "Android")
        }
    }
}
```

> **💡 Por trás dos panos**
> Uma função `@Composable` não "desenha" a tela diretamente — ela descreve **o que** deve aparecer, e o Compose decide **como** desenhar isso de forma eficiente. Quando algum dado muda (por exemplo, o parâmetro `nome`), o Compose executa a função de novo (isso se chama recomposição) e atualiza só as partes da tela que realmente mudaram, sem redesenhar tudo do zero. É essa característica que torna o Compose rápido mesmo em telas complexas.

### Exercícios

1. Adicione um segundo `Text` abaixo do primeiro com a mensagem `"Bem-vindo ao Compose!"` em tamanho 16sp.
   - *Dica se travar*: dois `Text` soltos, um em seguida do outro, dentro da mesma função, já aparecem empilhados verticalmente — não precisa de `Column` ainda neste exercício, mas usar uma não atrapalha.
2. Adicione um parâmetro `profissao: String` ao `CartaoBoasVindas` e exiba-o abaixo do nome.
3. Crie um composable `Subtitulo(texto: String)` reutilizável que exiba texto em itálico e cor cinza.
   - *Dica se travar*: use `fontStyle = FontStyle.Italic` e `color = Color.Gray` nos parâmetros do `Text`.

---

## Prática 2: Layouts com Column e Row

### Objetivo
Organizar elementos na tela usando `Column` (vertical) e `Row` (horizontal).

Quase nenhuma tela real tem só um elemento — normalmente há um título, uma imagem, alguns botões, tudo organizado de um jeito específico. `Column` e `Row` são os layouts mais usados no Compose porque cobrem a maioria dos casos: empilhar coisas verticalmente ou organizá-las lado a lado. Sem entender bem esses dois, fica difícil montar qualquer tela um pouco mais elaborada.

### Passo a Passo

1. Crie o composable `PerfilUsuario`:

```kotlin
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PerfilUsuario(nome: String, cargo: String, pontos: Int) {
    // Column organiza os filhos verticalmente
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = nome, fontSize = 22.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        Text(text = cargo, color = MaterialTheme.colorScheme.primary)

        // Row organiza os filhos horizontalmente
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "⭐")
            Text(text = "$pontos pontos")
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        Button(onClick = { /* ação futura */ }) {
            Text("Ver perfil completo")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PerfilUsuarioPreview() {
    MaterialTheme {
        PerfilUsuario(nome = "Maria Silva", cargo = "Desenvolvedora Android", pontos = 1250)
    }
}
```

2. Veja o Preview e experimente trocar os valores.

> **💡 Por trás dos panos**
> `Column` e `Row` funcionam com o mesmo princípio: cada um recebe uma lista de composables "filhos" (o que está dentro das chaves `{ }`) e decide como posicioná-los no eixo principal (vertical para `Column`, horizontal para `Row`). Os parâmetros `verticalArrangement`/`horizontalArrangement` controlam o espaçamento **entre** os filhos, enquanto `horizontalAlignment`/`verticalAlignment` controlam o alinhamento no eixo cruzado (por exemplo, centralizar horizontalmente dentro de uma `Column`). Pensar sempre em "qual é o eixo principal e qual é o eixo cruzado" ajuda a escolher entre `Column` e `Row` rapidamente.

### Exercícios

1. Adicione um segundo botão `"Editar perfil"` ao lado do primeiro, usando um `Row` para envolvê-los.
   - *Dica se travar*: o `Row` precisa envolver os dois `Button`, e você pode usar `Arrangement.spacedBy(8.dp)` para dar espaço entre eles.
2. Crie um composable `EstatisticaItem(label: String, valor: String)` e exiba três estatísticas em linha (ex.: Seguidores, Seguindo, Posts) usando um `Row` com `weight`.
   - *Dica se travar*: `Modifier.weight(1f)` em cada `EstatisticaItem` faz os três dividirem o espaço disponível igualmente dentro do `Row`.
3. Adicione `Spacer(modifier = Modifier.height(16.dp))` entre os elementos para melhorar o espaçamento visual.

---

## Prática 3: Estado com remember e mutableStateOf

### Objetivo
Entender como o estado funciona no Compose e como atualizar a UI sem recarregar tudo.

Este é, sem exagero, um dos conceitos mais importantes do Compose. "Estado" é qualquer valor que pode mudar ao longo do tempo e que a tela precisa refletir — um contador, o texto de um campo, se um checkbox está marcado. Sem entender `remember` e `mutableStateOf`, é impossível criar uma tela interativa: sem eles, mudar uma variável no código não faz a tela atualizar sozinha. Dominar esse conceito é o que separa uma tela estática de um app que realmente responde ao usuário.

### Passo a Passo

1. Crie o composable `ContadorSimples`:

```kotlin
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ContadorSimples() {
    // 'remember' mantém o valor entre recomposições
    // 'mutableStateOf' cria um estado observável
    var contador by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Você clicou $contador vezes",
            fontSize = 20.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = { if (contador > 0) contador-- }) {
                Text("−")
            }
            Button(onClick = { contador++ }) {
                Text("+")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = { contador = 0 }) {
            Text("Resetar")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ContadorSimplesPreview() {
    MaterialTheme { ContadorSimples() }
}
```

2. Execute no emulador e clique nos botões para ver o estado sendo atualizado.

> **💡 Por trás dos panos**
> `mutableStateOf(0)` cria um "estado observável" — um contêiner que guarda um valor e avisa o Compose sempre que esse valor muda. `remember { }` garante que esse contêiner **sobreviva** às recomposições (as vezes em que a função é executada de novo); sem `remember`, o Compose criaria um novo contador zerado a cada recomposição, e o valor nunca pareceria mudar. O `by` (em vez de `=`) é um atalho do Kotlin chamado "delegated property": ele permite escrever `contador` e `contador++` diretamente, em vez de `contador.value` e `contador.value++` toda vez.

### Exercícios

1. Adicione uma mensagem condicional: quando o contador for 0 mostre `"Comece a clicar!"`, quando for maior que 10 mostre `"Uau, já são mais de 10!"`.
   - *Dica se travar*: use `when` ou uma cadeia de `if/else if` dentro do `Text`, calculando o texto certo antes de passá-lo para `text = `.
2. Crie um composable `Semaforo` com três botões (Vermelho, Amarelo, Verde) e um `Text` que muda conforme o botão clicado. Use `var cor by remember { mutableStateOf("Vermelho") }`.
   - *Dica se travar*: cada botão só precisa fazer `onClick = { cor = "Vermelho" }` (trocando o texto correspondente) — o `Text` que exibe `cor` atualiza sozinho.
3. Implemente um composable `Calculadora` simples com dois campos de texto e botões para as quatro operações. O resultado deve aparecer abaixo.
   - *Dica se travar*: quebre em passos — primeiro faça os dois campos de texto guardarem `String` em estado; depois converta com `.toDoubleOrNull() ?: 0.0` antes de calcular; só então trate o clique dos botões.

---

## Prática 4: Entrada de Texto com TextField

### Objetivo
Capturar texto do usuário e exibir o resultado em tempo real.

Formulários (login, cadastro, busca, comentários) estão em praticamente todo app. `TextField` é o componente que captura o que o usuário digita, mas no Compose ele **não guarda o texto sozinho** — você precisa conectá-lo a um estado (o `remember { mutableStateOf("") }` que você aprendeu na prática anterior). Entender esse padrão "campo + estado" é essencial para qualquer tela com entrada de dados.

### Passo a Passo

1. Crie o composable `FormularioNome`:

```kotlin
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FormularioNome() {
    var nome by remember { mutableStateOf("") }
    var mensagem by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Qual é o seu nome?", style = MaterialTheme.typography.titleMedium)

        OutlinedTextField(
            // 'value' é o texto atual exibido no campo — vem do estado 'nome'
            value = nome,
            // onValueChange é chamado a cada tecla digitada; 'it' é o novo texto completo
            // Precisamos atualizar 'nome' manualmente para o campo continuar mostrando o que foi digitado
            onValueChange = { nome = it },
            label = { Text("Nome") },
            placeholder = { Text("Digite seu nome aqui") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = { mensagem = if (nome.isBlank()) "Por favor, digite seu nome." else "Olá, $nome! 🎉" },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Confirmar")
        }

        if (mensagem.isNotEmpty()) {
            Text(
                text = mensagem,
                style = MaterialTheme.typography.bodyLarge,
                color = if (nome.isBlank()) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary
            )
        }
    }
}
```

2. Adicione ao `setContent` da `MainActivity` e teste no emulador.

> **💡 Por trás dos panos**
> Esse padrão — `value` mostrando o estado atual e `onValueChange` atualizando esse estado — é chamado de **"state hoisting"** (o estado "sobe" para fora do componente de UI). O `TextField` em si não guarda nada; ele só exibe o `value` que você fornece e avisa quando o usuário digita algo, através do `onValueChange`. É esse padrão de mão única (estado desce, eventos sobem) que torna o Compose previsível: você sempre sabe onde o dado "de verdade" está guardado.

### Exercícios

1. Adicione um segundo campo `Sobrenome` e exiba a mensagem com o nome completo.
   - *Dica se travar*: crie um segundo `var sobrenome by remember { mutableStateOf("") }` e um segundo `OutlinedTextField` seguindo o mesmo padrão do primeiro.
2. Implemente um campo de `senha` com `visualTransformation = PasswordVisualTransformation()` para ocultar os caracteres digitados.
3. Crie um formulário de cadastro com campos: Nome, E-mail e Idade. Valide que o e-mail contém `@` e que a idade é um número positivo antes de exibir a mensagem de confirmação.
   - *Dica se travar*: comece só com o campo Nome funcionando, depois adicione E-mail, depois Idade — teste cada campo isoladamente antes de juntar a validação dos três.

---

## Prática 5: Listas Simples com Column e LazyColumn

### Objetivo
Exibir listas de itens de forma eficiente.

Listas de compras, feeds de rede social, catálogos de produtos — a maioria dos apps mostra alguma coleção de itens. Usar uma `Column` comum para isso funciona só para listas curtas, porque ela desenha **todos** os itens de uma vez, mesmo os que estão fora da tela. Já vamos ver uma prévia do `LazyColumn` aqui (o assunto completo está no guia `05_listas_lazy_column.md`), que resolve isso desenhando só o que está visível — essencial para o desempenho de qualquer lista maior.

### Passo a Passo

1. Crie o composable `ListaDeCompras`:

```kotlin
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class ItemCompra(val id: Int, val nome: String, var comprado: Boolean = false)

@Composable
fun ListaDeCompras() {
    val itens = remember {
        mutableStateListOf(
            ItemCompra(1, "Pão"),
            ItemCompra(2, "Leite"),
            ItemCompra(3, "Ovos"),
            ItemCompra(4, "Manteiga"),
            ItemCompra(5, "Café")
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Lista de Compras",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(itens, key = { it.id }) { item ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = item.comprado,
                        onCheckedChange = { marcado ->
                            val indice = itens.indexOf(item)
                            itens[indice] = item.copy(comprado = marcado)
                        }
                    )
                    Text(
                        text = item.nome,
                        modifier = Modifier.padding(start = 8.dp),
                        style = if (item.comprado)
                            MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.outline
                            )
                        else MaterialTheme.typography.bodyLarge
                    )
                }
                HorizontalDivider()
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Comprados: ${itens.count { it.comprado }} / ${itens.size}",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
```

2. Execute no emulador e marque itens como comprados.

> **💡 Por trás dos panos**
> O `LazyColumn` só desenha (e mantém na memória) os itens que estão realmente visíveis na tela — quando você rola a lista, itens que saem de vista são "reciclados", parecido com o que o antigo `RecyclerView` fazia, mas sem precisar configurar Adapter ou ViewHolder manualmente. O parâmetro `key = { it.id }` ajuda o Compose a saber exatamente qual item é qual entre uma recomposição e outra — isso evita bugs visuais e melhora o desempenho de animações quando itens são adicionados, removidos ou reordenados.

### Exercícios

1. Adicione um campo de texto e um botão para inserir novos itens à lista.
   - *Dica se travar*: como a lista é um `mutableStateListOf`, basta chamar `itens.add(ItemCompra(novoId, texto))` — não precisa recriar a lista inteira.
2. Adicione um botão `"Remover comprados"` que apaga da lista todos os itens marcados.
   - *Dica se travar*: `itens.removeAll { it.comprado }` remove diretamente todos os itens marcados de uma vez.
3. Exiba uma mensagem `"Parabéns, você fez todas as compras! 🛒"` quando todos os itens estiverem marcados.

---

## Resumo dos Conceitos Praticados

| Composable/Conceito | Quando usar |
|---------------------|-------------|
| `Text` | Exibir qualquer texto na tela |
| `Column` | Empilhar elementos verticalmente |
| `Row` | Posicionar elementos horizontalmente |
| `Button` / `TextButton` | Ações do usuário |
| `TextField` / `OutlinedTextField` | Entrada de texto |
| `remember` + `mutableStateOf` | Guardar e atualizar estado local |
| `LazyColumn` | Listas longas ou dinâmicas |
| `Modifier` | Ajustar tamanho, espaçamento, alinhamento |

---

## Próximos Passos

- Revise o módulo `07_jetpackcompose.md` para aprofundar temas como State Hoisting e Theming.
- Avance para a prática de MVVM (`04_mvvm_stateflow.md`) para aprender a separar lógica da interface.
