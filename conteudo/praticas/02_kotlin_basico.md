# Prática: Kotlin Básico para Iniciantes

Este guia apresenta exercícios práticos para consolidar os fundamentos do Kotlin usados no desenvolvimento Android. Cada exercício traz uma situação real e um passo a passo para resolvê-la.

---

## Prática 1: Variáveis e Tipos

### Objetivo
Entender a diferença entre `val` (imutável) e `var` (mutável) e os tipos básicos do Kotlin.

Essa distinção parece pequena, mas é a base de todo código Kotlin/Android que você vai escrever. Usar `val` sempre que possível evita bugs difíceis de rastrear — como uma variável que muda de valor "sozinha" em algum lugar do código sem você perceber. No Android, isso é ainda mais importante: estados de tela mal controlados (variáveis que deveriam ser fixas mas foram declaradas como mutáveis, ou vice-versa) são uma das causas mais comuns de comportamento inesperado na interface.

### Passo a Passo

1. Abra o Android Studio e crie um novo arquivo Kotlin chamado `Variaveis.kt` (pode ser em um projeto de estudos ou em um arquivo Kotlin puro).
2. Escreva o seguinte código:

```kotlin
fun main() {
    // val: não pode ser reatribuído após a primeira atribuição
    val nome = "Ana"
    val anoNascimento = 2000

    // var: pode ser reatribuído
    var idade = 24
    var pontuacao = 0.0

    println("Nome: $nome")
    println("Ano de nascimento: $anoNascimento")
    println("Idade: $idade")

    // Atualizando variáveis mutáveis
    idade = 25
    pontuacao = 9.5
    println("Idade atualizada: $idade")
    println("Pontuação: $pontuacao")

    // Tentativa de reatribuir um val causaria erro de compilação:
    // nome = "Bia" // ERRO: Val cannot be reassigned
}
```

3. Execute e observe a saída no console.

> **💡 Por trás dos panos**
> Quando você declara `val nome = "Ana"`, o compilador do Kotlin reserva um espaço na memória para esse valor e trava qualquer tentativa de sobrescrevê-lo — é uma garantia verificada em tempo de compilação, ou seja, antes mesmo do app rodar. Já `var` permite reatribuição porque o compilador libera essa trava. Prefira `val` como padrão: se o código não compilar quando você tentar mudar o valor, é um sinal de que você não precisava de uma variável mutável ali.

### Exercícios

1. Crie variáveis para armazenar: o nome de um produto, o preço (com casas decimais) e a quantidade em estoque. Imprima uma linha como: `"Produto: Café | Preço: R$12.50 | Estoque: 30"`.
   - *Dica se travar*: para o preço, use o tipo `Double` (ex.: `12.50`); para exibir dentro do texto, use `$preco` dentro de aspas duplas (string template).
2. Declare uma variável `temperatura` como `var` e simule a variação ao longo do dia imprimindo três valores diferentes.
3. Tente reatribuir um `val` e observe o erro do compilador. Anote a mensagem de erro.
   - *Dica se travar*: a mensagem costuma ser algo como `Val cannot be reassigned` — isso é esperado, é o compilador te protegendo.

---

## Prática 2: Null Safety

### Objetivo
Entender como o Kotlin protege o código contra erros de NullPointerException usando tipos anuláveis e operadores específicos.

O `NullPointerException` (carinhosamente apelidado de "NPE") é um dos erros mais comuns em apps mal escritos — ele acontece quando o código tenta usar um valor que na verdade é `null` (vazio/ausente), e o app simplesmente fecha sozinho (crash). Em Android é muito comum lidar com dados que podem não existir ainda: um campo que o usuário não preencheu, uma resposta de rede que falhou, um resultado de busca vazio. O Kotlin obriga você a pensar nesses casos desde já, em vez de descobrir o problema só quando o app já crashou na mão do usuário.

### Passo a Passo

1. Crie o arquivo `NullSafety.kt` e escreva:

```kotlin
fun main() {
    // Variável não-nulável: o compilador garante que nunca será null
    val cidade: String = "São Paulo"

    // Variável nulável: pode conter null
    var apelido: String? = null

    println("Cidade: $cidade")

    // Safe call (?.) — retorna null se apelido for null, sem crash
    println("Tamanho do apelido: ${apelido?.length}")

    // Operador Elvis (?:) — fornece um valor padrão quando o resultado é null
    val tamanho = apelido?.length ?: 0
    println("Tamanho (com padrão): $tamanho")

    // Atribuindo um valor e usando novamente
    apelido = "Aninha"
    println("Apelido: $apelido")
    println("Tamanho do apelido: ${apelido?.length}")
}
```

2. Execute e analise cada linha de saída.

> **💡 Por trás dos panos**
> O Kotlin diferencia tipos anuláveis (`String?`) de não-anuláveis (`String`) diretamente no sistema de tipos — isso significa que o compilador sabe, em cada linha do código, se uma variável pode ou não ser `null`. Por isso ele te obriga a tratar o caso nulo antes de deixar compilar (com `?.`, `?:` ou uma verificação explícita). É uma diferença fundamental em relação a linguagens como Java, onde qualquer referência pode ser `null` sem aviso — e é exatamente isso que evita boa parte dos crashes por NPE em apps Kotlin.

### Exercícios

1. Crie uma função `saudar(nome: String?): String` que retorne `"Olá, $nome!"` quando o nome for fornecido, ou `"Olá, visitante!"` quando for nulo. Use o operador Elvis.
   - *Dica se travar*: `nome ?: "visitante"` já resolve o valor padrão antes de montar a frase.
2. Crie uma variável `email: String?` que começa nula. Antes de usá-la, verifique com `if (email != null)` e imprima o domínio (parte após o `@`). Caso seja nulo, imprima `"E-mail não informado"`.
   - *Dica se travar*: dentro do `if (email != null)`, o Kotlin já entende que `email` não é mais nulo (smart cast) e você pode usar `email.substringAfter("@")` sem o `?`.
3. Simule um dado vindo de uma API: `val dado: String? = intent.getStringExtra("payload")` (pode fingir que é `null` ou um texto). Use safe call e Elvis para exibir o conteúdo ou um fallback.

---

## Prática 3: Funções

### Objetivo
Criar funções simples, com parâmetros padrão e como expressões de uma linha.

Funções são a forma de organizar e reaproveitar lógica em qualquer programa. Em um app Android, praticamente tudo que acontece — validar um formulário, calcular um preço, buscar dados — vive dentro de funções. Parâmetros padrão (como `mensagem: String = "Bem-vindo"`) evitam código repetido: em vez de criar várias versões da mesma função para casos ligeiramente diferentes, você cria uma função só, flexível o suficiente para cobrir todos os casos.

### Passo a Passo

1. Crie o arquivo `Funcoes.kt`:

```kotlin
// Função tradicional com retorno
fun somar(a: Int, b: Int): Int {
    return a + b
}

// Função como expressão (mais concisa)
fun subtrair(a: Int, b: Int) = a - b

// Função com parâmetro padrão
fun saudacao(nome: String, mensagem: String = "Bem-vindo") = "$mensagem, $nome!"

// Função que não retorna valor (Unit é opcional escrever)
fun imprimirSeparador(caractere: Char = '-', quantidade: Int = 20) {
    println(caractere.toString().repeat(quantidade))
}

fun main() {
    println(somar(3, 4))          // 7
    println(subtrair(10, 6))      // 4
    println(saudacao("Carlos"))   // Bem-vindo, Carlos!
    println(saudacao("Maria", "Olá")) // Olá, Maria!

    imprimirSeparador()
    imprimirSeparador('=', 10)
}
```

2. Execute e observe os resultados.

> **💡 Por trás dos panos**
> Uma função como `fun subtrair(a: Int, b: Int) = a - b` (sem chaves `{ }` e sem `return`) é chamada de "função de expressão" — o Kotlin entende que o resultado da expressão à direita do `=` já é o valor de retorno, e infere o tipo automaticamente. Por baixo dos panos, ela se comporta exatamente como a versão com `{ return a - b }`; é só uma forma mais curta de escrever quando o corpo da função cabe em uma linha. Parâmetros padrão (`mensagem: String = "Bem-vindo"`) funcionam de forma parecida: se você não passar aquele argumento na chamada, o Kotlin usa o valor padrão automaticamente.

### Exercícios

1. Escreva uma função `calcularMedia(notas: List<Double>): Double` que retorne a média das notas. Teste com `listOf(7.0, 8.5, 9.0)`.
   - *Dica se travar*: some todos os valores com `notas.sum()` e divida por `notas.size`.
2. Crie uma função `estaAprovado(media: Double, minima: Double = 6.0): Boolean` que retorne `true` se a média for maior ou igual ao mínimo.
3. Combine as funções: leia uma lista de notas, calcule a média e imprima se o aluno está aprovado ou reprovado.
   - *Dica se travar*: chame `calcularMedia(...)` primeiro, guarde o resultado em uma variável e passe essa variável para `estaAprovado(...)` — não precisa repetir o cálculo.

---

## Prática 4: Classes e Data Classes

### Objetivo
Entender classes, data classes e a diferença entre elas.

Classes são usadas para modelar "coisas" do seu app: um usuário, um produto, uma conta bancária. Uma classe normal costuma combinar dados com comportamento (métodos que fazem algo), enquanto uma `data class` é pensada para representar apenas dados — como a resposta de uma API ou uma linha de um banco de dados. Saber quando usar cada uma é essencial: no Android, quase todo modelo de dados (o "M" do MVVM) é uma `data class`, porque ela já vem com `equals`, `toString` e `copy` prontos, economizando dezenas de linhas de código repetitivo.

### Passo a Passo

1. Crie o arquivo `Classes.kt` e comece com uma classe normal, que combina estado (`saldo`) e comportamento (métodos que alteram esse estado):

```kotlin
// Classe normal: comportamento + estado
class ContaBancaria(val titular: String, var saldo: Double = 0.0) {
    fun depositar(valor: Double) {
        saldo += valor
        println("Depósito de R$${"%.2f".format(valor)}. Novo saldo: R$${"%.2f".format(saldo)}")
    }

    fun sacar(valor: Double) {
        if (valor > saldo) {
            println("Saldo insuficiente!")
        } else {
            saldo -= valor
            println("Saque de R$${"%.2f".format(valor)}. Novo saldo: R$${"%.2f".format(saldo)}")
        }
    }
}

fun main() {
    val conta = ContaBancaria("Ana", 100.0)
    conta.depositar(50.0)
    conta.sacar(30.0)
    conta.sacar(200.0)
}
```

Execute e veja o saldo mudando a cada operação — isso é uma classe comum, com lógica própria.

2. Agora adicione uma `data class`, pensada apenas para guardar dados (sem lógica de negócio própria). Acrescente ao mesmo arquivo:

```kotlin
// Data class: ideal para guardar dados (gera equals, hashCode, toString e copy automaticamente)
data class Produto(
    val id: Int,
    val nome: String,
    val preco: Double
)

fun main() {
    // ... chamadas de ContaBancaria do passo anterior ...

    val produto1 = Produto(1, "Caderno", 12.50)
    val produto2 = produto1.copy(id = 2, nome = "Caneta", preco = 3.99)

    println(produto1)
    println(produto2)
    println("São iguais? ${produto1 == produto2}")

    // Desestruturação (destructuring)
    val (id, nome, preco) = produto1
    println("ID: $id | Nome: $nome | Preço: R$$preco")
}
```

3. Execute e observe como `data class` simplifica o trabalho com dados: você não escreveu `equals`, `toString` nem `copy`, mas os três já funcionam.

> **💡 Por trás dos panos**
> Quando você marca uma classe com `data`, o compilador gera automaticamente métodos como `equals()` (compara se dois objetos têm os mesmos valores), `toString()` (imprime os campos de forma legível) e `copy()` (cria uma cópia alterando só alguns campos). Isso é o que permite `produto1.copy(id = 2, nome = "Caneta")` funcionar: em vez de reescrever o objeto inteiro, você pede uma cópia e só especifica o que muda — os demais campos são copiados automaticamente. Esse padrão (criar uma nova cópia em vez de alterar o objeto original) é o mesmo usado depois em `StateFlow` e MVVM para atualizar o estado da tela.

### Exercícios

1. Crie uma data class `Aluno(val nome: String, val matricula: String, val nota: Double)`. Crie três alunos e imprima o que tem a maior nota.
   - *Dica se travar*: uma lista de alunos tem a função `maxByOrNull { it.nota }`, que retorna o aluno com a maior nota diretamente.
2. Adicione um método `descricao()` à data class `Produto` que retorne uma string formatada como `"[id] nome - R$preco"`.
3. Crie uma classe `Calculadora` com os métodos `somar`, `subtrair`, `multiplicar` e `dividir`. No método `dividir`, verifique se o divisor é zero antes de calcular.
   - *Dica se travar*: se o divisor for zero, não faça a divisão — retorne `null` (mude o tipo de retorno para `Double?`) ou lance uma exceção com `throw IllegalArgumentException("Não é possível dividir por zero")`.

---

## Prática 5: Coleções e Lambdas

### Objetivo
Trabalhar com listas, mapas e as funções de ordem superior mais comuns do Kotlin.

Quase todo app Android exibe listas: contatos, produtos, mensagens, tarefas. As funções de coleções (`map`, `filter`, `sorted`, etc.) permitem transformar esses dados sem escrever loops manuais com `for`, o que deixa o código mais curto e mais fácil de ler. Dominar essas funções agora vai facilitar muito quando você chegar em `LazyColumn` (para exibir listas na tela) e em chamadas de API (para transformar respostas de rede em algo que a tela consegue exibir).

### Passo a Passo

1. Crie o arquivo `Colecoes.kt`:

```kotlin
fun main() {
    val frutas = listOf("Maçã", "Banana", "Laranja", "Uva", "Abacaxi")

    // map: transforma cada elemento
    val emMaiusculas = frutas.map { it.uppercase() }
    println("Maiúsculas: $emMaiusculas")

    // filter: seleciona elementos que atendem uma condição
    val frutasComA = frutas.filter { it.startsWith("A") }
    println("Começa com A: $frutasComA")

    // sorted e sortedByDescending
    val ordenadas = frutas.sorted()
    println("Ordenadas: $ordenadas")

    // forEach: executa um efeito colateral para cada elemento
    println("\nFrutas disponíveis:")
    frutas.forEach { println("  - $it") }

    // any / all / none
    println("\nTem banana? ${frutas.any { it == "Banana" }}")
    println("Todas têm mais de 3 letras? ${frutas.all { it.length > 3 }}")

    // count com condição
    println("Quantas têm mais de 5 letras? ${frutas.count { it.length > 5 }}")

    // Lista mutável
    val carrinho = mutableListOf("Pão", "Leite")
    carrinho.add("Queijo")
    carrinho.remove("Leite")
    println("\nCarrinho: $carrinho")

    // Map (dicionário)
    val estoque = mapOf("Pão" to 10, "Leite" to 5, "Queijo" to 8)
    println("\nEstoque de Leite: ${estoque["Leite"]}")
}
```

2. Execute e explore cada função.

> **💡 Por trás dos panos**
> Funções como `map` e `filter` são chamadas de "funções de ordem superior" porque recebem outra função (o `{ it.uppercase() }`, por exemplo) como argumento. Elas não alteram a lista original — cada uma cria e retorna uma **nova** lista com o resultado. É por isso que `frutas.map { ... }` não muda `frutas`; você precisa guardar o resultado em uma nova variável, como `val emMaiusculas = frutas.map { ... }`. Esse comportamento (não alterar o original, sempre devolver algo novo) é o mesmo princípio usado depois em `StateFlow.update { it.copy(...) }` no MVVM — vale a pena já se acostumar com essa forma de pensar.

### Exercícios

1. Crie uma lista de notas `listOf(4.5, 7.0, 9.5, 6.0, 3.0, 8.5)` e:
   - Filtre apenas as notas maiores ou iguais a 6.0.
   - Calcule a média das notas aprovadas.
   - Imprima quantos alunos foram aprovados e quantos reprovaram.
   - *Dica se travar*: resolva um passo de cada vez — primeiro filtre e imprima o resultado, só depois calcule a média em cima da lista já filtrada.
2. Crie um `MutableMap<String, Int>` representando um inventário de itens. Adicione e remova itens, depois imprima os itens em ordem alfabética com suas quantidades.
   - *Dica se travar*: `mapa.toSortedMap()` retorna o mapa já ordenado pelas chaves.
3. Dada uma lista de palavras, retorne uma nova lista sem duplicatas, ordenada e com apenas palavras que tenham mais de 4 letras. Use encadeamento de funções (chain).
   - *Dica se travar*: encadeie assim: `.distinct().filter { it.length > 4 }.sorted()` — a ordem das chamadas importa menos aqui, mas comece removendo duplicatas para não filtrar/ordenar itens repetidos à toa.

---

## Resumo das Boas Práticas Kotlin

| Conceito | Dica |
|----------|------|
| `val` vs `var` | Prefira `val`; use `var` apenas quando precisar mudar o valor |
| Null safety | Evite `!!`; prefira `?.`, `?:` e verificações explícitas |
| Funções | Use expressões de uma linha quando o corpo for simples |
| Data classes | Use para modelos de dados; evite lógica de negócios nelas |
| Coleções | Prefira funções de ordem superior (`map`, `filter`) a loops manuais |

---

## Próximos Passos

- Pratique os exercícios acima em um projeto Kotlin simples (pode ser um projeto "Kotlin only" no IntelliJ IDEA ou no Android Studio).
- Revise o material do módulo `02_kotlin_essencial.md` para aprofundar tópicos como coroutines e sealed classes.
- Avance para a prática de Jetpack Compose após sentir-se confortável com a sintaxe básica.
