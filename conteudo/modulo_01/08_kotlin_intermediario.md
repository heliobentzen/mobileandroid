# Kotlin Intermediário para Android

Este arquivo cobre conceitos de Kotlin essenciais para o Módulo 2 (MVVM, StateFlow, listas): ferramentas da linguagem que tornam o código Android mais expressivo, seguro e conciso. Você já viu o básico de Kotlin no arquivo 02 — aqui vamos avançar um pouco mais, sempre explicando o "porquê" de cada recurso antes de usá-lo.

> **Pré-requisito:** leia [02 – Kotlin Essencial](02_kotlin_essencial.md) antes. Espera-se familiaridade com tipos, null safety, data classes e lambdas.

---

## 1. Scope Functions (let, run, with, apply, also)

**O que é.** As **scope functions** ("funções de escopo") são um conjunto de cinco funções da biblioteca padrão do Kotlin (`let`, `run`, `with`, `apply`, `also`) que executam um bloco de código no **contexto de um objeto** — ou seja, dentro do bloco, você tem acesso direto às propriedades e métodos daquele objeto, sem precisar repetir o nome dele a cada linha.

**Por que isso importa.** Sem scope functions, código que configura um objeto ou faz uma checagem de nulo com transformação fica mais verboso e repetitivo — você precisaria repetir o nome da variável várias vezes, ou usar blocos `if` mais longos. As scope functions deixam esse tipo de código mais curto e legível, desde que você saiba escolher a função certa. A diferença entre as cinco está em **como acessar o objeto** (`this` ou `it`) e **o que a função retorna** (o resultado do bloco, ou o próprio objeto).

Vamos ver o "antes e depois" com duas delas — `apply` e `let` — e depois olhar as outras três com o mesmo princípio.

#### Passo 1 — o problema que `apply` resolve

```kotlin
val params = Bundle()
params.putString("titulo", "Meu App") // repetindo "params." em toda linha
params.putInt("versao", 1)
// 'params' já configurado
```

Isso funciona, mas repete `params.` a cada linha — puro ruído visual quando você está só configurando um objeto recém-criado.

```kotlin
// -- apply: acessa via 'this', retorna o próprio objeto --
val params = Bundle().apply {
    putString("titulo", "Meu App") // dentro do bloco, 'this' já é o Bundle
    putInt("versao", 1)            // então não precisamos repetir "params."
} // 'params' é o Bundle já configurado, porque apply retorna o próprio objeto
```

#### Passo 2 — o problema que `let` resolve

```kotlin
val nome: String? = intent.getStringExtra("usuario") // pode ser nulo
if (nome != null) {
    textView.text = "Bem-vindo, $nome"
}
```

Um `if` para tratar um valor opcional funciona, mas em uma cadeia de transformações (`?.map { }.let { }` etc.) fica mais difícil de encadear do que uma expressão única.

```kotlin
// -- let: acessa via 'it', retorna resultado do bloco --
nome?.let {
    textView.text = "Bem-vindo, $it" // o bloco só roda se 'nome' não for nulo
}
```

#### As outras três, mesmo princípio

```kotlin
// -- run: acessa via 'this', retorna resultado do bloco (como 'let', mas sem 'it') --
val tamanho = "Kotlin Android".run {
    uppercase() // transforma em maiúsculas
    length      // retorna o comprimento (último valor do bloco)
}

// -- with: igual a 'run', mas o objeto vem como argumento, não como receiver --
val config = StringBuilder()
val resultado = with(config) {
    append("debug=true")  // 'this' é o StringBuilder
    append("&lang=pt-BR") // encadeia chamadas
    toString()             // retorna a string final
}

// -- also: acessa via 'it', retorna o próprio objeto (como 'apply', mas com 'it') --
val lista = mutableListOf("A", "B").also {
    println("Lista criada com ${it.size} itens") // log sem alterar a lista
}
```

**Quando usar cada uma:**

| Função | Acesso | Retorno | Caso típico |
|--------|--------|---------|-------------|
| `let` | `it` | bloco | Null-check + transformação |
| `run` | `this` | bloco | Calcular valor com contexto |
| `with` | `this` | bloco | Agrupar chamadas em objeto existente |
| `apply` | `this` | objeto | Configurar objeto (Builder pattern) |
| `also` | `it` | objeto | Efeitos colaterais (log, debug) |

> **Dica para memorizar:** as que terminam em "**et**"/"**un**" (let, run) retornam o resultado do bloco. As que "**aplicam**"/"**também fazem**" algo no objeto (apply, also) retornam o próprio objeto. `with` é a exceção — não é chamada como método (`objeto.with { }`), e sim como função (`with(objeto) { }`).

### Erros comuns / Pegadinhas

- **Usar `apply` quando você queria o resultado do bloco**: como `apply` sempre retorna o próprio objeto, se você esperava o último valor calculado no bloco, o resultado vai ser diferente do esperado. Troque para `run` ou `let` nesse caso.
- **Encadear scope functions demais**: `a.let { }.also { }.run { }` em sequência fica difícil de ler. Use no máximo uma ou duas por expressão.
- **Usar `let` só para "envolver" código sem necessidade real de checagem de nulo**: se não há um `?.` antes do `let`, considere se uma variável comum não seria mais clara.

---

## 2. Sealed Classes e Sealed Interfaces

**O que é.** Você já viu sealed classes no arquivo 02 — aqui vamos revisar e aprofundar. Sealed classes/interfaces restringem a hierarquia de subtipos a um conjunto **finito e conhecido**. Isso é diferente de uma classe aberta (`open class`), que qualquer código, em qualquer lugar, poderia estender.

**Por que isso importa.** Essa restrição é o que permite ao compilador garantir que um `when` cubra **todos** os casos possíveis, sem precisar de um `else` — ideal para modelar **estados de UI**, onde você quer ter certeza absoluta de que tratou toda possibilidade (carregando, sucesso, erro), sem esquecer nenhuma.

#### Passo 1 — o problema: uma classe comum não é limitada

```kotlin
open class UiState
class Carregando : UiState()
class Sucesso(val itens: List<String>) : UiState()
class Erro(val mensagem: String) : UiState()

fun renderizar(estado: UiState) = when (estado) {
    is Carregando -> mostrarLoading()
    is Sucesso    -> mostrarLista(estado.itens)
    is Erro       -> mostrarErro(estado.mensagem)
    else -> {} // o compilador EXIGE esse 'else', porque 'open class' pode ter
                // subtipos criados em qualquer outro lugar do código-fonte
}
```

Como `UiState` é uma `open class` comum, o compilador não tem como saber se existem outros subtipos além desses três — talvez definidos em outro arquivo, ou até em outro módulo. Por isso ele obriga um `else`, mesmo que hoje você tenha certeza de que só existem esses três estados. Se amanhã alguém adicionar um quarto estado e esquecer de tratá-lo aqui, o `else` "engole" o erro silenciosamente.

#### Passo 2 — trocando por `sealed class`

```kotlin
// Modela os estados possíveis de uma tela de listagem
sealed class UiState {
    object Carregando : UiState()                       // sem dados extras
    data class Sucesso(val itens: List<String>) : UiState() // carrega a lista
    data class Erro(val mensagem: String) : UiState()   // mensagem de falha
}

fun renderizar(estado: UiState) = when (estado) {
    is UiState.Carregando -> mostrarLoading()       // exibe indicador de progresso
    is UiState.Sucesso    -> mostrarLista(estado.itens) // smart cast automático
    is UiState.Erro       -> mostrarErro(estado.mensagem)
    // sem 'else' — o compilador SABE que só existem esses três subtipos
    // (todos declarados aqui dentro), e exige que você trate todos eles
}
```

Agora, se alguém adicionar um quarto subtipo de `UiState` no futuro, o compilador vai apontar erro em todo `when` que não tratar esse novo caso — o bug é pego em tempo de compilação, não descoberto depois em produção.

**Sealed interface** funciona da mesma forma, mas permite que os subtipos herdem de outras classes ao mesmo tempo (uma classe em Kotlin só pode herdar de uma classe, mas pode implementar várias interfaces):

```kotlin
sealed interface Resultado   // interface selada
data class Ok(val dado: String) : Resultado  // pode herdar de outra classe também
data class Falha(val erro: Throwable) : Resultado
```

> No Módulo 2 você usará `sealed interface UiState` para controlar o estado das telas com MVVM (Model-View-ViewModel, o padrão de arquitetura que você vai estudar em detalhe lá).

### Erros comuns / Pegadinhas

- **Adicionar `else ->` em um `when` de sealed class "por hábito"**: isso anula a principal vantagem — o compilador para de te avisar quando um novo subtipo é adicionado e não tratado em algum lugar do código.
- **Usar `class` normal em vez de `sealed class` para modelar estados**: sem o "selo", o compilador não consegue garantir cobertura completa no `when`, e um novo estado adicionado no futuro pode passar despercebido em algum lugar do código que deveria tratá-lo.

---

## 3. Extension Functions

**O que é.** Você viu o básico de extension functions no arquivo 02 — funções que adicionam comportamento a classes existentes **sem modificá-las** (sem precisar herdar ou alterar o código-fonte original). Muito comuns em projetos Android para "turbinar" classes do próprio sistema.

**Por que isso importa.** Sem extensões, você precisaria de funções utilitárias soltas, que exigem passar o objeto como argumento em vez de chamar como método — menos legível e menos natural de encadear com outras chamadas.

#### Passo 1 — uma função utilitária solta

```kotlin
fun formatarComoReais(centavos: Int): String {
    val reais = centavos / 100        // parte inteira
    val resto = centavos % 100        // parte decimal
    return "R$ $reais,${resto.toString().padStart(2, '0')}" // formata com duas casas
}

val preco = 1999 // centavos
println(formatarComoReais(preco)) // "R$ 19,99"
```

Funciona, mas a chamada `formatarComoReais(preco)` lê "de fora para dentro" — não parece que `preco` está fazendo algo, parece que uma função externa está manipulando ele.

#### Passo 2 — convertendo em extension function

```kotlin
// Extensão para formatar centavos como moeda brasileira
fun Int.formatarReais(): String {
    val reais = this / 100        // 'this' é o próprio Int em que a extensão foi chamada
    val centavos = this % 100
    return "R$ $reais,${centavos.toString().padStart(2, '0')}"
}

val preco = 1999
println(preco.formatarReais()) // "R$ 19,99" — agora lê como um método do próprio Int
```

Note que o corpo da função é praticamente o mesmo — a diferença é só declarar `Int.formatarReais()` em vez de `formatarComoReais(centavos: Int)`, o que muda como ela é chamada.

Uma extensão também pode receber parâmetros normalmente, além do `this` implícito — útil para casos que precisam de um dado externo (como o `Context`, no exemplo comum de converter dp para pixels no Android):

```kotlin
fun Int.dpToPx(context: Context): Int {
    val densidade = context.resources.displayMetrics.density // fator de escala da tela
    return (this * densidade).toInt() // multiplica e arredonda
}
```

### Erros comuns / Pegadinhas

- **Esperar polimorfismo dinâmico de uma extensão**: como você viu no arquivo 02, extensões são resolvidas em tempo de compilação com base no tipo declarado da variável, não no tipo real do objeto. Se você precisa de comportamento diferente por subtipo, use um método real dentro da classe.
- **Criar extensões que fazem sentido só em um lugar específico, mas colocá-las em um arquivo genérico**: isso polui o autocomplete em todo o projeto. Mantenha extensões organizadas por contexto de uso.

---

## 4. Generics Básicos

**O que é.** **Generics** ("genéricos") permitem criar classes e funções que operam sobre **qualquer tipo**, mantendo a segurança de tipos do compilador — ou seja, o compilador continua verificando que você está usando os tipos corretos, mesmo sem saber de antemão qual tipo específico será usado.

**Por que isso importa.** Sem generics, você precisaria criar uma classe separada para cada tipo de dado, ou usar um tipo genérico demais como `Any` (que aceita qualquer coisa, mas perde a segurança de tipo — você só descobriria um erro de tipo em tempo de execução, não em compilação). Generics resolvem isso com uma única classe reutilizável e segura.

#### Passo 1 — o problema: uma classe por tipo

```kotlin
class RespostaString(val dados: String?, val erro: String? = null)
class RespostaInt(val dados: Int?, val erro: String? = null)
// ...e assim por diante, uma classe nova para cada tipo de dado que a rede retornar
```

O código das duas classes é idêntico, exceto pelo tipo de `dados` — duplicação pura, que só cresce conforme aparecem novos tipos de resposta.

#### Passo 2 — generalizando com `<T>`

```kotlin
// Classe genérica que encapsula um resultado de rede
class Resposta<T>(
    val dados: T?,          // tipo genérico — definido no uso
    val erro: String? = null // mensagem de erro opcional
)

val respostaUsuario = Resposta(dados = "João") // T = String
val respostaIdade = Resposta(dados = 25)       // T = Int
```

Uma única classe atende qualquer tipo, e o compilador continua checando os tipos normalmente (`respostaUsuario.dados` é `String?`, não `Any?`).

#### Restringindo o tipo genérico (upper bound)

Às vezes o corpo da função genérica precisa de um comportamento específico do tipo — por exemplo, comparar dois valores com `>`, que nem todo tipo suporta:

```kotlin
fun <T : Comparable<T>> maiorEntre(a: T, b: T): T {
    // '<T : Comparable<T>>' restringe T a tipos que sabem se comparar entre si
    // (como Int, String) — sem essa restrição, "a > b" não compilaria,
    // pois nem todo tipo genérico suporta comparação.
    return if (a > b) a else b // funciona com qualquer Comparable
}
```

**Covariância (`out`) e contravariância (`in`):** estes são conceitos mais avançados — não se preocupe em dominá-los agora, apenas reconheça os termos:

```kotlin
interface Fonte<out T> { fun proximo(): T }   // 'out' — só produz T (leitura)
interface Destino<in T> { fun enviar(item: T) } // 'in' — só consome T (escrita)
```

### Erros comuns / Pegadinhas

- **Usar `Any` em vez de generics quando o tipo real é conhecido em tempo de compilação**: você perde a verificação de tipo do compilador e precisa fazer casts manuais (`as String`), que podem falhar em tempo de execução.
- **Esquecer a restrição (`<T : Comparable<T>>`) quando a função precisa de um comportamento específico do tipo**: sem a restrição, o compilador não permite chamar métodos que não existem em todo tipo genérico.

---

## 5. Enum Classes

**O que é.** Uma **enum class** representa um conjunto fixo e conhecido de **valores constantes** — diferente de sealed class, os valores de um enum **não carregam dados diferentes por instância** (todos os valores do enum têm exatamente a mesma "forma").

**Por que isso importa.** Enums são ideais quando você só precisa de rótulos fixos (como estados simples de conexão), evitando o uso de strings ou números "soltos" no código (por exemplo, usar `"CONECTADO"` como string em vários lugares, correndo o risco de erro de digitação em algum deles).

```kotlin
// Enum para status de conexão de rede
enum class StatusRede {
    CONECTADO,    // dispositivo online
    DESCONECTADO, // sem conexão
    LIMITADO      // conexão instável
}

fun iconeParaStatus(status: StatusRede): Int = when (status) {
    StatusRede.CONECTADO    -> R.drawable.ic_wifi       // ícone cheio
    StatusRede.DESCONECTADO -> R.drawable.ic_wifi_off   // ícone cortado
    StatusRede.LIMITADO     -> R.drawable.ic_wifi_fraco // ícone parcial
}
```

**Enum vs Sealed Class:**

| Critério | Enum | Sealed Class |
|----------|------|--------------|
| Valores fixos e simples | ✅ Ideal | Excesso |
| Dados diferentes por instância | ❌ | ✅ Ideal |
| Precisa de `ordinal`, `values()` | ✅ Nativo | Manual |
| Modelar estados de UI complexos | ❌ | ✅ Ideal |

Regra prática: se cada caso precisa de **propriedades diferentes**, use sealed class. Se são apenas **rótulos constantes**, use enum.

### Erros comuns / Pegadinhas

- **Usar enum quando cada estado precisaria de dados próprios**: por exemplo, tentar forçar uma mensagem de erro variável dentro de um enum `Erro` — isso não é possível de forma natural com enums simples; use sealed class nesse caso.
- **Depender de `ordinal` (a posição do valor no enum) para lógica importante**: se a ordem dos valores mudar no código (alguém reorganiza o enum), o `ordinal` muda junto, quebrando qualquer lógica que dependia dele silenciosamente.

---

## 6. Object e Companion Object

**O que é.** Você viu `object` (singleton) no arquivo 02. Aqui, o complemento: `companion object` é um bloco especial dentro de uma classe que simula membros "estáticos" — ou seja, acessíveis diretamente pelo nome da classe, sem precisar criar uma instância dela.

**Por que isso importa.** Diferente de Java, Kotlin não tem a palavra-chave `static`. `companion object` é a forma idiomática de ter, por exemplo, um método de fábrica (factory method) que cria instâncias de uma classe de um jeito customizado, ou constantes ligadas a uma classe específica.

```kotlin
// Singleton para constantes de navegação
object Rotas {
    const val HOME = "home"         // rota da tela inicial
    const val PERFIL = "perfil"     // rota do perfil do usuário
    const val CONFIG = "config"     // rota de configurações
}

// Companion object — "métodos estáticos" em Kotlin
class Usuario(val nome: String, val email: String) {
    companion object {
        // Factory method — cria instância a partir de Map
        fun deMap(map: Map<String, String>): Usuario {
            val nome = map["nome"] ?: "Desconhecido"   // valor padrão se ausente
            val email = map["email"] ?: ""              // string vazia como fallback
            return Usuario(nome, email)                 // retorna instância criada
        }
    }
}

// Uso — acessa diretamente pela classe, sem instanciar
val user = Usuario.deMap(mapOf("nome" to "Ana", "email" to "ana@mail.com"))
```

### Erros comuns / Pegadinhas

- **Confundir `object` (singleton independente) com `companion object` (bloco dentro de uma classe)**: são conceitos relacionados, mas `object Rotas { }` cria uma classe/instância nova; `companion object { }` vive dentro de outra classe.
- **Abusar de `companion object` para guardar estado mutável compartilhado**: como qualquer parte do código pode acessar e modificar esse estado, isso pode gerar bugs difíceis de rastrear em apps maiores. Prefira injeção de dependência (você vai ver isso em módulos futuros, com Hilt).

---

## 7. Operadores de Coleção

**O que é.** Você já viu `map` e `filter` no arquivo 02 — aqui vamos expandir o vocabulário de operações funcionais em listas, fundamentais para transformar dados vindos de APIs e repositórios em algo que a UI possa exibir.

**Por que isso importa.** Sem essas operações, transformar e filtrar listas exigiria loops manuais (`for`) com variáveis mutáveis acumulando resultado — mais código, mais chance de erro, e menos legível do que uma cadeia de operações que descreve claramente a intenção.

```kotlin
data class Tarefa(
    val titulo: String,   // nome da tarefa
    val concluida: Boolean, // se já foi finalizada
    val prioridade: Int   // 1 = alta, 2 = média, 3 = baixa
)

val tarefas = listOf(
    Tarefa("Estudar Kotlin", false, 1),
    Tarefa("Fazer layout", true, 2),
    Tarefa("Testar app", false, 1),
    Tarefa("Publicar na Play Store", false, 3)
)

// map — transforma cada item
val titulos = tarefas.map { it.titulo } // lista de strings com os títulos

// filter — mantém apenas itens que satisfazem a condição
val pendentes = tarefas.filter { !it.concluida } // apenas tarefas não concluídas

// groupBy — agrupa em um Map<Chave, Lista>
val porPrioridade = tarefas.groupBy { it.prioridade } // Map<Int, List<Tarefa>>

// fold — acumula um valor a partir de um inicial
val resumo = tarefas.fold("") { acc, tarefa ->
    val status = if (tarefa.concluida) "✅" else "⬜" // ícone por status
    "$acc$status ${tarefa.titulo}\n"                   // concatena ao acumulador
}

// sortedBy — ordena por campo
val ordenadas = tarefas.sortedBy { it.prioridade } // prioridade 1 primeiro
```

Encadeamento é comum e legível — cada linha lê quase como uma frase em português:

```kotlin
// Títulos de tarefas pendentes de alta prioridade, em ordem alfabética
val resultado = tarefas
    .filter { !it.concluida }        // remove concluídas
    .filter { it.prioridade == 1 }   // mantém alta prioridade
    .map { it.titulo }               // extrai títulos
    .sorted()                        // ordena A-Z
```

### Erros comuns / Pegadinhas

- **Encadear muitos `filter`/`map` em listas muito grandes sem pensar em performance**: cada etapa cria uma lista intermediária nova. Para listas pequenas (dezenas/centenas de itens, comum em telas de app) isso não é problema; para milhares de itens, considere `asSequence()` (visto no arquivo 02).
- **Confundir `fold` com `reduce`**: `fold` recebe um valor inicial explícito (funciona mesmo com lista vazia); `reduce` usa o primeiro elemento da lista como inicial (lança exceção se a lista estiver vazia).

---

## 8. Resumo — Referência Rápida

| Conceito | Uso Principal | Exemplo |
|----------|--------------|---------|
| `let` | Null-check + transformação | `nome?.let { ... }` |
| `apply` | Configurar objeto | `Bundle().apply { ... }` |
| `also` | Log / debug | `lista.also { log(it) }` |
| Sealed class | Estados com dados variáveis | `UiState.Sucesso(dados)` |
| Enum class | Constantes fixas | `StatusRede.CONECTADO` |
| Extension fun | Adicionar métodos a tipos existentes | `Int.formatarReais()` |
| Generics `<T>` | Código reutilizável type-safe | `Resposta<Usuario>` |
| `object` | Singleton | `object Rotas { ... }` |
| `companion object` | Factory / constantes na classe | `Usuario.deMap(...)` |
| `map / filter` | Transformar e filtrar coleções | `lista.map { ... }` |
| `groupBy` | Agrupar itens | `lista.groupBy { it.campo }` |
| `fold` | Acumular valor | `lista.fold(0) { acc, x -> ... }` |

## Resumo dos Conceitos

- Scope functions (`let`, `run`, `with`, `apply`, `also`) executam um bloco no contexto de um objeto — escolha pela combinação de acesso (`this`/`it`) e retorno (bloco/objeto) que você precisa.
- Sealed classes/interfaces restringem subtipos a um conjunto finito, permitindo `when` exaustivo (sem `else`) — ideais para modelar estados de UI.
- Extension functions adicionam métodos a classes existentes, mas não têm polimorfismo dinâmico.
- Generics (`<T>`) criam código reutilizável e type-safe, evitando duplicar classes para cada tipo.
- Enum class modela valores fixos e simples; sealed class modela estados com dados variáveis — escolha conforme a necessidade.
- `object` cria singletons; `companion object` simula membros estáticos dentro de uma classe.
- Operadores de coleção (`map`, `filter`, `groupBy`, `fold`, `sortedBy`) transformam dados de forma declarativa, sem loops manuais.

## Próximos Passos

Com esses conceitos você está preparado para o **Módulo 2** (MVVM):

- **Sealed interfaces** → modelar `UiState` no ViewModel
- **Operadores de coleção** → transformar dados do repositório para a UI
- **Scope functions** → configurar objetos e tratar nulos de forma idiomática

Este é o último arquivo do Módulo 1 — parabéns por chegar até aqui! Você já tem a base de Kotlin, estrutura de projeto, Activities, componentes do Android, Intents e Jetpack Compose necessária para começar a construir apps de verdade.

👉 Siga para [Módulo 2 – MVVM + Fluxo Unidirecional](../modulo_02/01_mvvm.md)
