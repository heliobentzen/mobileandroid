# Kotlin Moderno para Android (Visão Essencial)

Kotlin é a linguagem de programação recomendada oficialmente pelo Google para desenvolver apps Android. Ela é considerada mais segura que a alternativa mais antiga (Java) contra um erro muito comum chamado `NullPointerException` (vamos explicar exatamente o que é isso na seção 3), além de ser mais concisa (você escreve menos código para fazer a mesma coisa) e totalmente compatível com bibliotecas escritas em Java.

Este arquivo é a sua base de Kotlin. Você vai usar tudo o que está aqui em praticamente todo arquivo seguinte do curso — vale a pena ler com calma e testar os exemplos você mesmo no Android Studio (ou em um playground online de Kotlin, como o [play.kotlinlang.org](https://play.kotlinlang.org)).

---

## 1. Android Moderno: as bibliotecas que você vai encontrar

**O que é.** O Google mantém um conjunto de bibliotecas chamado **Jetpack**, criadas para resolver problemas recorrentes do desenvolvimento Android sem que cada desenvolvedor precise "reinventar a roda". Você vai ouvir esses nomes o tempo todo no curso, então aqui vai uma explicação simples de cada um:

- **ViewModel + LiveData/StateFlow**: o `ViewModel` é uma classe que guarda o estado da tela (por exemplo, os dados que o usuário já digitou, ou a lista carregada da internet) de um jeito que **sobrevive à rotação da tela**. `LiveData` e `StateFlow` são "contêineres observáveis" — quando o valor dentro deles muda, a tela é avisada automaticamente e se atualiza sozinha.
- **Room**: uma biblioteca para salvar dados localmente no dispositivo, em um banco de dados SQLite, sem você precisar escrever SQL manualmente na maior parte do tempo.
- **Navigation**: gerencia a troca de telas dentro do app de forma organizada e seguro (evita, por exemplo, tentar navegar para uma tela que não existe mais).
- **WorkManager**: agenda tarefas que precisam rodar em segundo plano, mesmo que o app seja fechado — por exemplo, sincronizar dados uma vez por dia.
- **Hilt**: uma biblioteca de **injeção de dependência** — um jeito de organizar como as classes do seu app recebem as outras classes de que precisam para funcionar, sem que cada classe precise "criar" tudo sozinha.

**Por que isso importa.** Sem essas bibliotecas, você teria que escrever manualmente uma lógica bem mais complicada e propensa a bugs para cada um desses problemas (por exemplo, perder os dados da tela toda vez que o usuário girar o celular). Você vai estudar cada uma delas com profundidade nos próximos módulos — por enquanto, só reconheça os nomes.

---

## 2. Sintaxe Básica

**O que é.** Toda linguagem de programação tem uma sintaxe: as regras de como escrever variáveis, funções e valores. Vamos começar pelo mais fundamental do Kotlin.

```kotlin
val nome = "Ana"      // imutável
var idade = 20        // mutável
fun saudacao(n: String) = "Olá, $n"
fun soma(a: Int, b: Int = 10) = a + b
```

### Exemplo comentado

```kotlin
val nome = "Ana"
// 'val' declara uma variável IMUTÁVEL: depois de atribuída, não pode ser reatribuída.
// Tentar fazer "nome = "Bia"" depois desta linha geraria um erro de compilação.

var idade = 20
// 'var' declara uma variável MUTÁVEL: o valor pode mudar depois.
// idade = 21 seria válido em qualquer linha posterior.

fun saudacao(n: String) = "Olá, $n"
// Declara uma função chamada 'saudacao' que recebe um parâmetro 'n' do tipo String
// e RETORNA uma String. O '=' (em vez de chaves { }) indica uma "expressão única":
// um jeito mais curto de escrever uma função que só tem uma linha de retorno.
// É equivalente a: fun saudacao(n: String): String { return "Olá, $n" }

fun soma(a: Int, b: Int = 10) = a + b
// 'b: Int = 10' define um VALOR PADRÃO para o parâmetro b.
// Isso significa que soma(5) é válido e retorna 15 (usa o padrão),
// enquanto soma(5, 20) retorna 25 (sobrescreve o padrão).
```

**Por que isso importa.** Preferir `val` a `var` é uma prática recomendada em Kotlin: variáveis imutáveis tornam o código mais previsível, porque você tem certeza de que aquele valor não vai mudar "escondido" em algum lugar do programa. Isso evita uma classe inteira de bugs difíceis de rastrear.

Repare também nos textos com `$` dentro das aspas — isso se chama **string template** (interpolação de string): você pode inserir o valor de uma variável (`$variavel`) ou o resultado de uma expressão (`${expressao}`) diretamente dentro do texto, sem precisar concatenar com `+`.

### Erros comuns / Pegadinhas

- **Tentar reatribuir um `val`:** o compilador vai acusar erro imediatamente. Se você precisa mudar o valor depois, use `var` — mas prefira sempre `val` quando possível.
- **Esquecer o tipo em parâmetros de função:** diferente de variáveis locais (`val nome = "Ana"`, onde o Kotlin infere o tipo `String` sozinho), parâmetros de função **sempre** precisam do tipo explícito, como em `fun saudacao(n: String)`.
- **Confundir `=` de atribuição com `=` de expressão única:** em `fun soma(a: Int, b: Int = 10) = a + b`, o primeiro `=` (depois de `b: Int`) é valor padrão; o segundo `=` (depois dos parênteses) define o corpo da função como uma expressão.

---

## 3. Null Safety (o superpoder do Kotlin)

**O que é.** Em muitas linguagens de programação, qualquer variável pode acidentalmente estar vazia — ou, no jargão técnico, ser `null` (ausência de valor). Se o código tentar usar essa variável sem checar antes, o programa **crasha** (trava e fecha) com um erro chamado `NullPointerException`, popularmente apelidado de "o erro de um bilhão de dólares" por ser tão comum e caro de corrigir.

O Kotlin resolve isso na raiz: **por padrão, nenhuma variável pode ser nula**. Se você quiser permitir que uma variável seja nula, precisa dizer isso explicitamente adicionando um `?` ao tipo.

**Por que isso importa.** Sem esse mecanismo, seu app pode crashar em produção sempre que uma variável que "deveria ter valor" chegar vazia — por exemplo, um dado que não veio da internet, ou um campo de texto que o usuário deixou em branco. O compilador do Kotlin te obriga a lidar com essa possibilidade *antes* de rodar o app, em vez de descobrir o problema só quando o usuário reportar um crash.

```kotlin
var titulo: String = "OK"
// titulo = null // erro de compilação: 'titulo' não aceita null
var subtitulo: String? = null
// O '?' depois do tipo diz: "esta variável PODE ser null".
```

### Formas de acessar valores possivelmente nulos

```kotlin
subtitulo?.length
// Safe call (chamada segura): se 'subtitulo' for null, a expressão inteira
// retorna null em vez de crashar. Se não for null, retorna o comprimento normalmente.

val tam = subtitulo?.length ?: 0
// Elvis operator (?:): "se o lado esquerdo for null, use o valor à direita".
// Aqui, se subtitulo for null, 'tam' recebe 0 em vez de null.

subtitulo!!.length
// Double-bang (!!): força o Kotlin a tratar o valor como não-nulo.
// Se 'subtitulo' realmente for null nesse momento, o app CRASHA.
// Evite usar isso a menos que você tenha certeza absoluta do valor.

if (subtitulo != null) println(subtitulo.length)
// Smart cast: depois de checar "!= null" dentro de um 'if', o Kotlin entende
// automaticamente que, dentro desse bloco, 'subtitulo' não pode ser null,
// e permite usá-lo sem o '?' ou '!!'.
```

### Uso típico no Android

```kotlin
val intentData: String? = intent.getStringExtra("data")
// getStringExtra pode retornar null se a chave "data" não existir no Intent
// (você vai estudar Intents em detalhe no arquivo 06).
val data = intentData ?: "Valor padrão"
// Usa o Elvis operator para garantir um valor de fallback, evitando null daqui pra frente.
```

### Erros comuns / Pegadinhas

- **Abusar do `!!`:** é tentador usar `!!` para "calar" o compilador quando ele reclama de possível null. Mas isso só transfere o erro para o momento em que o app está rodando (crash em produção) em vez de resolver o problema em tempo de compilação. Trate o `!!` como um alarme: "estou assumindo um risco aqui".
- **Esquecer o `?` no tipo quando o valor pode realmente ser nulo:** por exemplo, dados vindos de uma API ou de um banco de dados frequentemente podem faltar. Se você declarar o tipo sem `?` por engano e o valor vier nulo mesmo assim (algo possível em interoperabilidade com Java), o app pode crashar de um jeito mais difícil de rastrear.
- **Confundir `?.` com `!!.`**: são visualmente parecidos, mas têm comportamentos opostos — um é seguro (retorna null), o outro é arriscado (pode crashar). Releia o código com atenção antes de escolher qual usar.

---

## 4. Coroutines e Concorrência

**O que é.** Uma **coroutine** é uma forma do Kotlin executar tarefas de forma assíncrona (ou seja, "em paralelo", sem travar o restante do app enquanto espera algo demorado, como uma resposta de rede) escrevendo o código de um jeito que *parece* sequencial e fácil de ler. Sem coroutines, código assíncrono tradicionalmente vira um emaranhado de callbacks aninhados, difícil de acompanhar.

**Por que isso importa.** Tarefas como buscar dados na internet, ler um arquivo grande ou consultar um banco de dados são **demoradas** relativamente ao processamento de uma tela. Se você rodar essas tarefas na **thread principal** (a linha de execução responsável por desenhar a tela e responder a toques), o app trava — literalmente congela — enquanto espera a tarefa terminar. Depois de alguns segundos, o Android mostra o famoso diálogo "O app não está respondendo" (ANR) e pode até fechar o app sozinho. Coroutines permitem rodar essas tarefas fora da thread principal, sem travar a interface.

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking {
    // runBlocking cria um escopo de coroutine e BLOQUEIA a thread atual
    // até que tudo dentro dele termine. Útil para testes e para o 'main',
    // mas raramente usado dentro do app de verdade (só em testes).

    launch {
        // launch inicia uma NOVA coroutine que roda "em paralelo" (concorrente)
        // com o restante do código, sem bloquear quem chamou.
        delay(1000L) // suspende esta coroutine por 1 segundo, sem travar a thread
        println("Mundo!")
    }
    println("Olá") // esta linha roda ANTES do "Mundo!", pois launch não bloqueia
}
```

> **O que é uma `suspend function`?** É uma função marcada com a palavra-chave `suspend` que pode ser pausada e retomada depois, sem bloquear a thread em que está rodando. `delay()` é um exemplo: ela "pausa" a coroutine, mas libera a thread para fazer outras coisas enquanto espera.

No Android, use `viewModelScope` para rodar coroutines de forma segura dentro de um `ViewModel`:

```kotlin
class MainViewModel : ViewModel() {
    fun carregarDados() {
        viewModelScope.launch {
            // viewModelScope é um escopo de coroutine amarrado ao ciclo de vida
            // do ViewModel: se a tela for destruída, a coroutine é cancelada
            // automaticamente, evitando vazamento de memória (memory leak).
            val dados = repositorio.buscarDados() // chamada demorada (ex: rede)
            _estado.value = UiState.Sucesso(dados) // atualiza o estado observado pela UI
        }
    }
}
```

### Erros comuns / Pegadinhas

- **Rodar tarefas demoradas na thread principal:** o erro mais comum de iniciantes é chamar uma função de rede diretamente, sem `launch`/coroutine, e travar o app.
- **Usar `GlobalScope` em vez de um escopo amarrado ao ciclo de vida:** isso pode manter coroutines rodando mesmo depois que a tela foi fechada, desperdiçando recursos. Prefira `viewModelScope`, `lifecycleScope` ou escopos equivalentes.
- **Esquecer que `launch` não bloqueia:** código depois de um `launch { }` continua executando imediatamente, sem esperar a coroutine terminar. Se você precisa do resultado antes de continuar, isso exige outra abordagem (como `async`/`await`, fora do escopo deste arquivo introdutório).

---

## 5. Classes, Data Classes, Objetos

**O que é.** Kotlin é uma linguagem orientada a objetos: você modela conceitos do mundo real (um usuário, uma tarefa, um produto) como **classes**, que descrevem quais dados e comportamentos aquele conceito tem.

```kotlin
class Pessoa(val nome: String, var idade: Int) {
    fun aniversario() { idade++ }
}
data class Usuario(val id: Int, val nome: String)
val u = Usuario(1, "Ana").copy(nome = "Bia")
val (id, n) = u
object Config { const val versao = "1.0" }
```

### Exemplo comentado

```kotlin
class Pessoa(val nome: String, var idade: Int) {
    // 'nome' é imutável (val), 'idade' é mutável (var) — ambos definidos
    // diretamente no CONSTRUTOR PRIMÁRIO, economiza escrever isso separadamente.
    fun aniversario() { idade++ } // método que modifica o estado interno do objeto
}

data class Usuario(val id: Int, val nome: String)
// 'data class' é uma classe especial pensada para GUARDAR DADOS.
// O Kotlin gera automaticamente, sem você escrever nada a mais:
//  - equals()/hashCode() (comparar dois usuários pelo conteúdo, não pela referência)
//  - toString() (uma representação em texto legível para debug)
//  - copy() (criar uma cópia alterando só alguns campos)
//  - componentN() (permite "desestruturar" o objeto, como na linha abaixo)

val u = Usuario(1, "Ana").copy(nome = "Bia")
// copy() cria um NOVO objeto Usuario com id=1 (mantido) e nome="Bia" (alterado).
// O objeto original não é modificado — data classes favorecem imutabilidade.

val (id, n) = u
// Desestruturação: extrai 'id' e 'n' diretamente dos campos de 'u', na ordem
// em que foram declarados na data class (id primeiro, nome depois).

object Config { const val versao = "1.0" }
// 'object' cria um SINGLETON: existe uma única instância de Config em todo o app,
// acessível diretamente por Config.versao, sem precisar instanciar com "Config()".
```

**Regra prática:** se o objetivo é "guardar dados" (como um registro vindo de uma API), use `data class`. Se você precisa de um único ponto de acesso global (como uma configuração ou uma constante compartilhada), use `object` (singleton).

### Erros comuns / Pegadinhas

- **Usar `class` comum quando `data class` resolveria melhor:** você perde `equals()`, `toString()` e `copy()` de graça, e vai acabar escrevendo essas comparações manualmente (e errando).
- **Esquecer que `copy()` faz uma cópia rasa (shallow copy):** se um campo da data class for, ele mesmo, um objeto mutável (como uma `MutableList`), `copy()` não duplica esse objeto interno — os dois `Usuario` compartilhariam a mesma lista.
- **Confundir `object` (singleton) com `class` (molde para criar várias instâncias):** um `object` não pode ser instanciado com `Config()` — ele já é a própria instância.

---

## 6. Coleções + Lambdas

**O que é.** **Coleções** são estruturas que guardam vários valores (listas, conjuntos, mapas). **Lambdas** são funções "anônimas" e curtas, passadas como argumento para outras funções — muito usadas para transformar ou filtrar coleções de forma concisa.

```kotlin
val nums = listOf(1,2,3)
val mut = mutableListOf(1,2)
val dobro = nums.map { it * 2 }
val pares = nums.filter { it % 2 == 0 }
val soma = nums.sum()
```

### Exemplo comentado

```kotlin
val nums = listOf(1, 2, 3)
// listOf cria uma lista IMUTÁVEL (não é possível adicionar/remover itens depois)

val mut = mutableListOf(1, 2)
// mutableListOf cria uma lista MUTÁVEL (aceita add(), remove(), etc.)

val dobro = nums.map { it * 2 }
// map TRANSFORMA cada elemento da lista aplicando a lambda.
// 'it' é o nome implícito do parâmetro quando a lambda tem só um argumento.
// Resultado: [2, 4, 6]

val pares = nums.filter { it % 2 == 0 }
// filter mantém apenas os elementos para os quais a lambda retorna 'true'.
// Resultado: [2]

val soma = nums.sum()
// sum() soma todos os elementos numéricos da coleção. Resultado: 6
```

### Processamento preguiçoso (lazy) com sequências

```kotlin
val primeiros = (1..1_000_000)
    .asSequence() // transforma o range em uma Sequence: processamento LAZY
    .filter { it % 3 == 0 } // só é executado item a item, sob demanda
    .take(5) // para assim que encontrar os 5 primeiros — não processa o milhão inteiro
    .toList() // converte o resultado final de volta para uma List
```

> **Por que isso importa.** Sem `asSequence()`, cada operação (`filter`, `map`, etc.) percorreria a coleção inteira e criaria uma lista intermediária completa antes de passar para a próxima etapa — desperdiçando memória e tempo em coleções grandes. `asSequence()` processa item por item, encadeando as operações, o que é bem mais eficiente quando você só precisa de parte do resultado (como no `take(5)` acima).

### Scope functions (contexto rápido)

```kotlin
val p = Pessoa("João", 30).apply { aniversario() }
// apply executa o bloco com 'this' apontando para o objeto recém-criado,
// e retorna o PRÓPRIO objeto — útil para configurar algo em cadeia.
val idade = p.run { idade }
// run executa o bloco com 'this' e retorna o RESULTADO do bloco (não o objeto).
```

> Vamos estudar todas as scope functions (`let`, `run`, `with`, `apply`, `also`) com mais profundidade no arquivo 08 (Kotlin Intermediário).

### Erros comuns / Pegadinhas

- **Usar `listOf` quando precisa adicionar itens depois:** isso gera erro em tempo de execução (`UnsupportedOperationException`). Se a lista vai crescer, use `mutableListOf`.
- **Encadear muitos `map`/`filter` em listas grandes sem `asSequence()`:** cada etapa cria uma lista intermediária inteira na memória. Para poucas dezenas de itens não importa, mas para milhares de itens vale considerar sequências.
- **Confundir `map` com `forEach`:** `map` retorna uma nova lista transformada; `forEach` só executa uma ação para cada item e retorna `Unit` (nada útil). Usar `forEach` esperando um resultado é um erro comum.

---

## 7. Extensões (turbinando APIs existentes)

**O que é.** Uma **extension function** (função de extensão) permite adicionar um novo método a uma classe que você não escreveu — como `String` ou `View` — sem precisar herdar dela ou modificar o código-fonte original.

**Por que isso importa.** No Android, você vai frequentemente querer um comportamento extra em classes do próprio sistema Android (como `Context` ou `View`). Sem extensões, você precisaria criar funções utilitárias soltas (ex: `fun mostrarToast(context: Context, msg: String)`), que são menos legíveis do que `context.toast(msg)`.

```kotlin
fun String.reversa() = reversed()
fun Int.isPar() = this % 2 == 0
"Code".reversa()
5.isPar()
```

### Exemplo comentado

```kotlin
fun String.reversa() = reversed()
// Declara uma extensão na classe String. Dentro do corpo, 'this' se refere
// à própria String em que o método é chamado. reversed() já existe na
// biblioteca padrão do Kotlin — aqui só criamos um apelido mais curto.

fun Int.isPar() = this % 2 == 0
// Extensão em Int: 'this' é o número em questão.

"Code".reversa() // chama a extensão como se fosse um método nativo de String
5.isPar()        // idem, para Int
```

### Extensões em tipos nulos

```kotlin
fun String?.isNullOuVazia() = this == null || isEmpty()
// Repare no '?' depois de String: esta extensão pode ser chamada
// mesmo em uma variável que É null, porque o próprio corpo checa isso.
```

### Extensões comuns no Android

```kotlin
fun View.show() { visibility = View.VISIBLE }
fun View.hide() { visibility = View.GONE }
fun Context.toast(msg: String) =
    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
```

### Limitação importante: dispatch estático

```kotlin
open class Animal
class Gato: Animal()
fun Animal.som() = "?"
fun Gato.som() = "miau"

val a: Animal = Gato() // variável declarada como Animal, mas contém um Gato
a.som() // "?" — e não "miau"!
```

**Por que isso acontece.** Extensões são resolvidas em **tempo de compilação**, com base no **tipo declarado da variável** (`Animal`), não no tipo real do objeto em memória (`Gato`). Isso é diferente de um método real definido dentro da classe, que usa **polimorfismo dinâmico** (resolvido em tempo de execução, com base no tipo real do objeto). Se você precisa desse comportamento — que o método certo seja escolhido de acordo com o tipo real do objeto —, defina um método de verdade dentro da classe, não uma extensão.

### Erros comuns / Pegadinhas

- **Esperar polimorfismo de uma extensão:** como no exemplo acima, isso é a pegadinha clássica. Extensões não substituem herança quando você precisa de comportamento específico por subtipo.
- **Criar extensões demais em classes genéricas (como `Any`):** isso pode poluir o autocomplete do editor em qualquer objeto do projeto. Prefira extensões específicas para o tipo que você realmente precisa.

---

## 8. Sealed Classes, Smart Casts, Pattern-Like

**O que é.** Uma **sealed class** ("classe selada") é uma classe cujos subtipos possíveis são todos conhecidos e declarados no mesmo arquivo (ou módulo). Isso permite ao compilador garantir, em um `when`, que **todos os casos possíveis foram tratados** — sem precisar de um `else` genérico "pra garantir".

**Por que isso importa.** É extremamente útil para modelar **estados finitos**, como o resultado de uma operação: ela pode estar carregando, ter dado certo, ou ter falhado — e nada além disso. Se no futuro você adicionar um novo estado à sealed class, o compilador vai *apontar erro* em todo `when` que não tratar esse novo caso, evitando bugs silenciosos.

```kotlin
sealed class Resultado
data class Sucesso(val dado: String): Resultado()
data class Erro(val msg: String): Resultado()
object Carregando: Resultado()

fun tratar(r: Resultado) = when(r) {
    is Sucesso -> println(r.dado)
    is Erro -> println("Falhou: ${r.msg}")
    Carregando -> println("...")
}
```

### Exemplo comentado

```kotlin
sealed class Resultado
// Declara a "classe-mãe" selada. Só pode ter subtipos definidos no mesmo
// arquivo/módulo — o compilador sabe exatamente quais existem.

data class Sucesso(val dado: String): Resultado() // um dos subtipos possíveis
data class Erro(val msg: String): Resultado()      // outro subtipo possível
object Carregando: Resultado()
// 'object' aqui porque Carregando não precisa guardar nenhum dado extra —
// é só um "marcador" de estado, então um singleton é suficiente.

fun tratar(r: Resultado) = when(r) {
    is Sucesso -> println(r.dado)
    // 'is Sucesso' faz um SMART CAST: dentro deste bloco, o Kotlin já trata
    // 'r' como Sucesso automaticamente, permitindo acessar 'r.dado' direto.
    is Erro -> println("Falhou: ${r.msg}")
    Carregando -> println("...")
    // Sem 'else' — o compilador SABE que esses três casos cobrem tudo.
    // Se você esquecer um deles, o código nem compila.
}
```

### Smart cast fora de sealed classes

```kotlin
fun tamanho(x: Any) {
    if (x is String) println(x.length)
    // Depois de checar "x is String", o Kotlin permite usar x.length
    // diretamente, sem cast manual como "(x as String).length"
}
```

### Erros comuns / Pegadinhas

- **Usar `enum class` quando cada estado precisa de dados diferentes:** enums só guardam valores fixos, sem campos variáveis por instância. Se `Erro` precisa de uma mensagem e `Sucesso` precisa de um dado, sealed class é a escolha certa (veja mais no arquivo 08).
- **Adicionar `else ->` "por segurança" em um `when` de sealed class:** isso anula a principal vantagem — o compilador deixa de te avisar quando você esquecer de tratar um novo subtipo.
- **Esquecer que sealed class exige que todos os subtipos estejam visíveis ao compilador** no mesmo módulo — não é possível estender de fora.

---

## Exercícios Práticos

Resolva na ordem — cada exercício usa conceitos do anterior.

1. **Null Safety** (checkpoint: 10 min)
   - Crie uma variável que pode ser nula (`var nomeUsuario: String? = null`).
   - Use `safe call` (`?.`) e `Elvis operator` (`?:`) para manipulá-la.
   - Exemplo: receba um nome de usuário e exiba `"Olá, [nome]"` ou `"Olá, visitante"` se for nulo.
   - 💡 Dica: comece escrevendo a versão com `if/else` primeiro, depois reescreva usando `?:` — isso ajuda a entender o que o operador está fazendo por baixo dos panos.

2. **Funções e Expressões** (checkpoint: 10 min)
   - Escreva uma função `fun maior(a: Int, b: Int): Int` que receba dois números e retorne o maior deles, usando um bloco `{ }` normal.
   - Transforme a função em uma expressão única (usando `=`).
   - 💡 Dica: o operador `if/else` em Kotlin retorna um valor, então `if (a > b) a else b` pode ser o próprio corpo da função.

3. **Coroutines** (checkpoint: 15 min)
   - Implemente uma função `suspend fun buscarDados()` que simule uma chamada de rede usando `delay(2000)` e depois exiba (`println`) um resultado fictício.
   - Chame essa função dentro de um `runBlocking { }` usando `launch`.
   - 💡 Dica: lembre que código depois de `launch { }` continua executando sem esperar — se quiser ver isso na prática, coloque um `println` logo após o `launch` e observe a ordem em que as mensagens aparecem no console.

4. **Desafio** (checkpoint: 20 min)
   - Crie uma `data class Usuario` com propriedades `nome: String` e `idade: Int`.
   - Crie uma lista de pelo menos 4 usuários com idades variadas.
   - Escreva uma função que receba essa lista e retorne apenas os usuários maiores de idade (18+), usando `filter`.
   - 💡 Opcional: ordene o resultado por idade usando `sortedBy`.

---

## 10. Cheatsheet Relâmpago

Depois de entender cada operação nas seções anteriores, use esta tabela como referência rápida (não como material de estudo — se algum termo aqui não fizer sentido, volte à seção 6):

```text
map          transforma
filter       filtra
flatMap      achata
forEach      efeito colateral
groupBy      agrupa em Map
associateBy  vira Map<chave, elemento>
partition    Pair(match, resto)
fold(init)   acumula com inicial
reduce       acumula sem inicial
sumOf        soma propriedade
asSequence   encadeia lazy
windowed(n)  janelas
chunked(n)   blocos
```

---

## 11. Mentalidade Kotlin

Estes são princípios que vão te guiar conforme você escreve mais código Kotlin:

- **Prefira `val`**; mude para `var` só quando realmente necessário. Imutabilidade evita bugs de estado inesperado.
- **Trate `null` cedo**, perto de onde ele pode aparecer, em vez de deixar `!!` se espalhar pelo código.
- **Nomes claros valem mais que truques de sintaxe.** Um código elegante que ninguém entende não é bom código.
- **Evite `!!`** — é quase sempre sinal de que o design poderia lidar melhor com a ausência de valor.
- **Use `data class`/`sealed class` para modelar o domínio do seu app**, em vez de enums "pobres" que não carregam dados suficientes.
- **Priorize imutabilidade** sempre que possível — isso torna o fluxo de dados do app mais previsível e fácil de debugar.

Comece pequeno: recrie utilidades simples como extensões, depois migre para Compose e coroutines no seu próprio ritmo. Você aprende programação iterando — errando, lendo o erro, corrigindo.

## Resumo

- Kotlin usa `val` (imutável) e `var` (mutável); prefira `val` sempre que possível.
- Null safety é o principal diferencial do Kotlin: tipos não aceitam `null` a menos que você use `?`; prefira `?.` e `?:` a `!!`.
- Coroutines (`launch`, `delay`, `suspend fun`) permitem tarefas assíncronas sem travar a interface; no Android, use `viewModelScope`.
- `data class` modela dados (gera `equals`, `toString`, `copy` automaticamente); `object` cria singletons.
- Coleções (`listOf`, `map`, `filter`) e lambdas (`{ it -> ... }`) são a base para transformar dados no dia a dia.
- Extensões (`fun Tipo.novoMetodo()`) adicionam comportamento a classes existentes, mas não substituem herança (não há polimorfismo dinâmico).
- Sealed classes modelam estados finitos (ex: `Sucesso`/`Erro`/`Carregando`) e o compilador garante que você trate todos os casos em um `when`.

**Próximo passo:** no arquivo 03, você vai aprender como o Android Studio organiza um projeto de verdade — pastas, arquivos de configuração e a diferença entre `namespace` e `applicationId`.
