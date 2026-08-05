# UI Moderna com Jetpack Compose

Jetpack Compose é o toolkit (conjunto de ferramentas) moderno do Android para construir interfaces nativas de forma declarativa, rápida e concisa usando Kotlin. Você já usou Compose nos arquivos anteriores sem entrar em detalhes — este arquivo é onde você entende de verdade o que está acontecendo por baixo dos panos.

## Visão Geral Rápida

1. Você descreve a UI com funções `@Composable`.
2. O estado (dado que pode mudar) muda → o Compose **recompõe** (redesenha) apenas o que precisa mudar, não a tela inteira.
3. **Elevação de estado** (hoisting) → move o estado para fora do componente visual, deixando-o mais reutilizável.
4. Tema Material 3 → garante aparência consistente em todo o app.

Se algum desses termos ainda não fizer sentido, não se preocupe — vamos explicar cada um em detalhe nas próximas seções.

---

## 1. Funções `@Composable`

**O que é.** Uma função `@Composable` é uma função Kotlin normal, mas marcada com a anotação `@Composable`, que a habilita a **emitir UI** (descrever pedaços de interface visual, como um texto, um botão ou uma coluna de itens). É a unidade básica de construção de qualquer tela em Compose.

**Por que isso importa.** Antes do Compose, construir uma interface Android exigia escrever arquivos XML separados do código, e depois "conectar" os dois manualmente (buscando cada elemento pelo ID). Com Compose, a interface é código Kotlin de verdade: você pode usar `if`, `for`, funções e todo o poder da linguagem diretamente para descrever a tela, sem essa camada extra de indireção.

Características:
- Composables só podem ser chamados dentro de outros composables (ou de funções especiais de preview/teste).
- Não retornam valores úteis para lógica — o "retorno" de um Composable é a própria UI que ele desenha (tecnicamente, o tipo de retorno é `Unit`, que significa "nenhum valor útil").
- Devem ser, idealmente, **puros** em relação à UI: a mesma entrada (os mesmos parâmetros) deve sempre produzir a mesma saída visual. Isso é o que permite ao Compose otimizar recomposições.

Exemplo simples:

```kotlin
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun Greeting(name: String) {
    // Um Composable simples: recebe um parâmetro e desenha um Text com ele
    Text("Olá, $name!")
}

@Preview(showBackground = true)
// @Preview permite visualizar este Composable diretamente no Android Studio,
// sem precisar rodar o app inteiro no emulador — acelera muito o ciclo de
// desenvolvimento visual.
@Composable
fun GreetingPreview() {
    Greeting("Compose")
}
```

Dica: pense em cada Composable como uma pequena função de transformação de dados em elementos visuais — assim como uma função normal transforma um número em outro número, um Composable "transforma" um estado em uma tela.

### Erros comuns / Pegadinhas

- **Chamar um Composable fora de outro Composable** (por exemplo, dentro de `onCreate` sem estar dentro do bloco `setContent { }`): isso gera erro de compilação — "Composable invocations can only happen from the context of a @Composable function".
- **Esperar que o Composable retorne um valor útil**: como ele retorna `Unit`, tentar fazer `val resultado = MeuComposable()` não faz sentido — o "resultado" de um Composable é a UI desenhada, não um valor de retorno.
- **Colocar lógica com efeitos colaterais direto no corpo do Composable** (por exemplo, uma chamada de rede): isso pode rodar a cada recomposição, de forma imprevisível. Use os efeitos (`LaunchedEffect`, etc.) vistos no arquivo 05.

---

## 2. Estado e Recomposition

**O que é estado.** **Estado** (state) é qualquer dado que pode mudar ao longo do tempo e que afeta o que aparece na tela — por exemplo, o texto que o usuário digitou, se um checkbox está marcado, ou quantos itens uma lista tem.

**O que é recomposição.** **Recomposição** (recomposition) é o processo pelo qual o Compose reexecuta as funções `@Composable` que dependem de um estado, sempre que esse estado muda, atualizando a tela automaticamente.

**Por que isso importa.** Sem esse mecanismo, você precisaria escrever manualmente código para: detectar que um dado mudou, encontrar os elementos visuais afetados, e atualizá-los um a um — um processo trabalhoso e propenso a erros (esquecer de atualizar algum elemento, atualizar o elemento errado). O Compose faz tudo isso automaticamente, e de forma otimizada: só a parte da tela que realmente depende do estado alterado é recomposta, não a tela inteira.

### `remember` + `mutableStateOf`

- `mutableStateOf(valor)`: cria um **estado observável** — um contêiner que "avisa" o Compose sempre que seu valor interno muda.
- `remember { ... }`: mantém esse estado vivo **entre recomposições**, dentro do mesmo escopo (ou seja, sem perder o valor toda vez que a função Composable é reexecutada).

> **Por que precisamos de `remember`?** Sem ele, toda vez que o Composable fosse recomposto, uma nova variável de estado seria criada do zero, perdendo o valor anterior — como se a "memória" do componente fosse apagada a cada atualização de tela.

Vamos construir um contador simples em três passos, para ver exatamente o papel de cada peça.

#### Passo 1 — uma variável comum (não funciona)

```kotlin
@Composable
fun SimpleCounter() {
    var count = 0 // variável Kotlin comum, sem relação com o Compose

    Column {
        Text("Você clicou $count vezes.")
        Button(onClick = { count++ }) {
            Text("Incrementar")
        }
    }
}
```

Clique no botão e nada muda na tela. O Compose não tem como saber que `count` mudou — incrementar uma variável comum não avisa ninguém, então não há recomposição.

#### Passo 2 — usando `mutableStateOf` (ainda incompleto)

```kotlin
@Composable
fun SimpleCounter() {
    val count = mutableStateOf(0)
    // mutableStateOf cria um estado OBSERVÁVEL: mudanças nele avisam o Compose

    Column {
        Text("Você clicou ${count.value} vezes.")
        Button(onClick = { count.value++ }) {
            Text("Incrementar")
        }
    }
}
```

Melhor, mas ainda tem um problema: toda vez que `SimpleCounter` é recomposto, a linha `mutableStateOf(0)` roda de novo e cria um **novo** estado, zerado. Na prática, o contador nunca passa de 1 antes de resetar.

#### Passo 3 — adicionando `remember` (e o atalho `by`)

```kotlin
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@Composable
fun SimpleCounter() {
    // 'remember' mantém o valor entre recomposições — o mutableStateOf
    // só roda uma vez, na primeira composição.
    // 'by' é um atalho do Kotlin (delegated property) que permite ler/escrever
    // 'count' diretamente, sem precisar escrever 'count.value'.
    var count by remember { mutableStateOf(0) }

    Column {
        Text("Você clicou $count vezes.")
        Button(onClick = {
            count++
            // Ao incrementar 'count', o Compose detecta a mudança no estado
            // observável e recompõe automaticamente o Text acima, que depende dele
        }) {
            Text("Incrementar")
        }
    }
}
```

Agora o contador funciona como esperado: o estado sobrevive às recomposições e a tela se atualiza sozinha a cada clique.

Evite guardar em estado do Compose:
- Objetos pesados sem necessidade (isso pode deixar recomposições mais lentas).
- Referências a `Context` fora de escopos seguros (pode causar vazamento de memória, mantendo uma Activity inteira viva na memória mesmo depois de destruída).

### Erros comuns / Pegadinhas

- **Esquecer o `remember`**: declarar `var count = mutableStateOf(0)` sem `remember` faz o valor resetar para `0` a cada recomposição, porque uma nova instância é criada toda vez.
- **Esquecer o `by` e tentar usar `count` diretamente**: sem `by`, a variável é do tipo `MutableState<Int>`, e você precisaria escrever `count.value++` em vez de `count++`. O `by` é só um atalho, mas esquecê-lo é uma fonte comum de erro de compilação para iniciantes.
- **Modificar estado fora da thread principal sem cuidado**: o Compose espera que atualizações de estado observável aconteçam de forma segura; combine com coroutines corretamente (você vai ver isso com mais detalhe no Módulo 2, com `StateFlow`).

---

## 3. State Hoisting (Elevação de Estado)

**O que é.** **State hoisting** ("elevação de estado") é uma técnica onde você move o estado de um Composable para **fora** dele — geralmente para o Composable "pai" ou para um `ViewModel` — e o Composable original passa a receber esse estado como parâmetro, junto com callbacks (funções) para notificar mudanças.

**Objetivo:** separar apresentação (como algo aparece na tela) de lógica (o que acontece quando o usuário interage).

**Por que isso importa.** Um Composable que gerencia seu próprio estado internamente ("stateful") só pode ser usado de um jeito: sempre com aquele comportamento fixo embutido. Um Composable que recebe estado de fora ("stateless") pode ser reutilizado em diferentes contextos — testado isoladamente passando valores simulados, ou conectado depois a um `ViewModel` sem precisar reescrever a parte visual.

Regra prática:
- Composable **stateless** ("burro") recebe: valor + callbacks (funções chamadas quando algo acontece).
- Composable **stateful** ("inteligente") guarda: estado interno + transforma eventos em mudanças de estado.

#### Passo 1 — o problema: o `SimpleCounter` da seção 2

O `SimpleCounter` que você acabou de construir guarda o `remember` **dentro dele mesmo**. Isso funciona, mas tem um custo: esse Composable só pode ser usado exatamente daquele jeito. Você não consegue reaproveitar o visual do contador com outra fonte de dados (por exemplo, um valor vindo de um `ViewModel`), nem testar a parte visual sem também testar a lógica do `remember` junto.

#### Passo 2 — separando em stateless + stateful

A solução é dividir em dois Composables: um que só desenha (`CounterStateless`) e outro que guarda o estado e passa para o primeiro (`CounterStateful`).

```kotlin
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

// Composable "burro" (stateless): apenas recebe dados e emite eventos.
// Não sabe DE ONDE o valor 'count' vem, nem O QUE acontece quando 'onIncrement'
// é chamado — só sabe desenhar a tela e notificar a intenção do usuário.
@Composable
fun CounterStateless(count: Int, onIncrement: () -> Unit) {
    Column {
        Text("Clique: $count")
        Button(onClick = onIncrement) {
            Text("Adicionar")
        }
    }
}

// Composable "inteligente" (stateful): gerencia o estado e delega
// a parte visual para o composable stateless.
@Composable
fun CounterStateful() {
    var count by remember { mutableStateOf(0) }
    CounterStateless(
        count = count,
        onIncrement = { count++ } // aqui decidimos O QUE fazer quando o botão é clicado
    )
}
```

Benefícios:
- **Reutilização**: `CounterStateless` pode ser usado em qualquer lugar, com qualquer fonte de dados.
- **Testes mais simples**: para testar `CounterStateless`, basta passar valores simulados (`count = 5`) e verificar se `onIncrement` é chamado — sem precisar simular todo o gerenciamento de estado.
- **Facilidade para mover lógica para `ViewModel` depois**: quando você chegar ao Módulo 2 (MVVM), vai substituir o `remember` interno de `CounterStateful` por um `ViewModel`, sem precisar tocar em `CounterStateless`.

### Erros comuns / Pegadinhas

- **Elevar estado demais, cedo demais**: nem todo estado precisa ser hoisted (elevado). Um estado que só afeta a aparência interna de um componente (como "o menu dropdown está aberto?") pode, muitas vezes, ficar local mesmo.
- **Esquecer de passar o callback**: um erro comum é criar `CounterStateless` recebendo `count`, mas esquecer o parâmetro `onIncrement`, fazendo o botão não fazer nada quando clicado.

---

## 4. Theming Básico (Material 3)

**O que é.** **Material Design** é o guia visual criado pelo Google, com padrões de cores, tipografia (fontes) e formas para criar interfaces consistentes e familiares. `MaterialTheme` é o Composable que aplica esse tema em toda a árvore de Composables abaixo dele.

**Por que isso importa.** Sem um sistema de temas centralizado, cada tela do app poderia acabar com cores e fontes ligeiramente diferentes, prejudicando a identidade visual e dificultando mudanças globais (como trocar a cor principal do app — sem tema centralizado, você teria que caçar e trocar essa cor em cada arquivo).

`MaterialTheme` fornece:
- `colorScheme` — as cores do app (primária, de fundo, de erro, etc.)
- `typography` — os estilos de texto (título, corpo, legenda, etc.)
- `shapes` — os formatos de cantos arredondados usados em botões, cartões, etc.

Uso básico:

```kotlin
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable

@Composable
fun AppRoot() {
    // Substitua pelo tema do seu projeto: MyAppTheme { ... }
    // (o Android Studio gera um tema customizado automaticamente ao criar o projeto)
    MaterialTheme {
        Surface(
            color = MaterialTheme.colorScheme.background
            // Material 3 usa colorScheme para acessar as cores do tema —
            // em vez de escrever uma cor "fixa" (hardcoded), como Color.White
        ) {
            Greeting("Android")
        }
    }
}
```

Dica: mantenha cores, tipografia e formas em um arquivo central (o Android Studio já gera um arquivo de tema ao criar o projeto). Use somente tokens do tema (como `MaterialTheme.colorScheme.primary`) nos componentes, em vez de valores fixos — isso garante consistência e facilita dar suporte a modo escuro (dark mode) automaticamente.

### Erros comuns / Pegadinhas

- **Usar cores fixas (`Color(0xFF6200EE)`) em vez de tokens do tema**: isso quebra a consistência visual e o suporte a temas claro/escuro. Prefira `MaterialTheme.colorScheme.primary`.
- **Esquecer de envolver a árvore de Composables em `MaterialTheme { }`**: sem isso, componentes do Material 3 podem não ter acesso às cores/tipografia esperadas e usar valores padrão inadequados.

---

## 5. Checklist Mental

Antes de criar um novo Composable, pergunte-se:
- Ele precisa guardar estado? Se não, torne-o stateless (recebe tudo por parâmetro).
- O estado pode ser elevado (hoisted)? Prefira elevar, seguindo o padrão da seção 3.
- O que muda na recomposição? Minimize os escopos grandes que dependem de um estado — isso melhora a performance.
- Está usando valores do tema (`MaterialTheme.colorScheme`, `.typography`)? Isso garante consistência visual.

---

## 6. Exercício Sugerido

Checkpoint: 20-30 minutos.

Crie:
1. Um campo de texto (`TextField`) que conta caracteres digitados.
2. Separe em dois Composables:
   - **Stateless**: mostra o texto e o contador, recebendo ambos por parâmetro, mais um callback `onTextChange`.
   - **Stateful**: gerencia o `remember` do texto e chama o Composable stateless, seguindo exatamente o padrão da seção 3.

💡 **Dica**: comece escrevendo tudo em um único Composable (stateful, com `remember` interno) até funcionar. Só depois separe em stateless + stateful — fica mais fácil entender a divisão quando você já viu o código funcionando de um jeito só.

---

## Resumo

- `@Composable` marca funções que descrevem UI; elas só podem ser chamadas de dentro de outros Composables.
- Estado (`mutableStateOf` + `remember`) é o dado que, ao mudar, dispara recomposição automática da parte da tela que depende dele.
- State hoisting separa componentes "burros" (stateless, reutilizáveis, fáceis de testar) de componentes "inteligentes" (stateful, que gerenciam o estado real).
- `MaterialTheme` centraliza cores, tipografia e formas — sempre prefira os tokens do tema a valores fixos.
- Compose = UI declarativa: você descreve o resultado desejado; o framework cuida de atualizar a tela quando o estado muda.

Comece pequeno, componha blocos, eleve estado conforme necessário — você vai internalizar esse padrão com a prática.

**Próximo passo:** no arquivo 08, você vai revisar conceitos intermediários de Kotlin (scope functions, sealed classes, generics) que preparam terreno para o Módulo 2, onde você vai integrar `ViewModel` e fluxo de dados (`StateFlow`/`LiveData`) com tudo que aprendeu sobre Compose aqui.
