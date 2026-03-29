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

### Passo a Passo

1. Abra o arquivo `MainActivity.kt` do seu projeto.
2. Fora da classe `MainActivity`, adicione o seguinte composable:

```kotlin
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun CartaoBoasVindas(nome: String) {
    Text(
        text = "Olá, $nome! 👋",
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold
    )
}

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
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            CartaoBoasVindas(nome = "Android")
        }
    }
}
```

### Exercícios

1. Adicione um segundo `Text` abaixo do primeiro com a mensagem `"Bem-vindo ao Compose!"` em tamanho 16sp.
2. Adicione um parâmetro `profissao: String` ao `CartaoBoasVindas` e exiba-o abaixo do nome.
3. Crie um composable `Subtitulo(texto: String)` reutilizável que exiba texto em itálico e cor cinza.

---

## Prática 2: Layouts com Column e Row

### Objetivo
Organizar elementos na tela usando `Column` (vertical) e `Row` (horizontal).

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

### Exercícios

1. Adicione um segundo botão `"Editar perfil"` ao lado do primeiro, usando um `Row` para envolvê-los.
2. Crie um composable `EstatisticaItem(label: String, valor: String)` e exiba três estatísticas em linha (ex.: Seguidores, Seguindo, Posts) usando um `Row` com `weight`.
3. Adicione `Spacer(modifier = Modifier.height(16.dp))` entre os elementos para melhorar o espaçamento visual.

---

## Prática 3: Estado com remember e mutableStateOf

### Objetivo
Entender como o estado funciona no Compose e como atualizar a UI sem recarregar tudo.

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

### Exercícios

1. Adicione uma mensagem condicional: quando o contador for 0 mostre `"Comece a clicar!"`, quando for maior que 10 mostre `"Uau, já são mais de 10!"`.
2. Crie um composable `Semaforo` com três botões (Vermelho, Amarelo, Verde) e um `Text` que muda conforme o botão clicado. Use `var cor by remember { mutableStateOf("Vermelho") }`.
3. Implemente um composable `Calculadora` simples com dois campos de texto e botões para as quatro operações. O resultado deve aparecer abaixo.

---

## Prática 4: Entrada de Texto com TextField

### Objetivo
Capturar texto do usuário e exibir o resultado em tempo real.

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
            value = nome,
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

### Exercícios

1. Adicione um segundo campo `Sobrenome` e exiba a mensagem com o nome completo.
2. Implemente um campo de `senha` com `visualTransformation = PasswordVisualTransformation()` para ocultar os caracteres digitados.
3. Crie um formulário de cadastro com campos: Nome, E-mail e Idade. Valide que o e-mail contém `@` e que a idade é um número positivo antes de exibir a mensagem de confirmação.

---

## Prática 5: Listas Simples com Column e LazyColumn

### Objetivo
Exibir listas de itens de forma eficiente.

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

### Exercícios

1. Adicione um campo de texto e um botão para inserir novos itens à lista.
2. Adicione um botão `"Remover comprados"` que apaga da lista todos os itens marcados.
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

- Revise o módulo `06_jetpackcompose.md` para aprofundar temas como State Hoisting e Theming.
- Avance para a prática de MVVM (`04_mvvm_stateflow.md`) para aprender a separar lógica da interface.
