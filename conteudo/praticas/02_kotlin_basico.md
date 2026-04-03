# Prática: Kotlin Básico para Iniciantes

Este guia apresenta exercícios práticos para consolidar os fundamentos do Kotlin usados no desenvolvimento Android. Cada exercício traz uma situação real e um passo a passo para resolvê-la.

---

## Prática 1: Variáveis e Tipos

### Objetivo
Entender a diferença entre `val` (imutável) e `var` (mutável) e os tipos básicos do Kotlin.

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

### Exercícios

1. Crie variáveis para armazenar: o nome de um produto, o preço (com casas decimais) e a quantidade em estoque. Imprima uma linha como: `"Produto: Café | Preço: R$12.50 | Estoque: 30"`.
2. Declare uma variável `temperatura` como `var` e simule a variação ao longo do dia imprimindo três valores diferentes.
3. Tente reatribuir um `val` e observe o erro do compilador. Anote a mensagem de erro.

---

## Prática 2: Null Safety

### Objetivo
Entender como o Kotlin protege o código contra erros de NullPointerException usando tipos anuláveis e operadores específicos.

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

### Exercícios

1. Crie uma função `saudar(nome: String?): String` que retorne `"Olá, $nome!"` quando o nome for fornecido, ou `"Olá, visitante!"` quando for nulo. Use o operador Elvis.
2. Crie uma variável `email: String?` que começa nula. Antes de usá-la, verifique com `if (email != null)` e imprima o domínio (parte após o `@`). Caso seja nulo, imprima `"E-mail não informado"`.
3. Simule um dado vindo de uma API: `val dado: String? = intent.getStringExtra("payload")` (pode fingir que é `null` ou um texto). Use safe call e Elvis para exibir o conteúdo ou um fallback.

---

## Prática 3: Funções

### Objetivo
Criar funções simples, com parâmetros padrão e como expressões de uma linha.

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

### Exercícios

1. Escreva uma função `calcularMedia(notas: List<Double>): Double` que retorne a média das notas. Teste com `listOf(7.0, 8.5, 9.0)`.
2. Crie uma função `estaAprovado(media: Double, minima: Double = 6.0): Boolean` que retorne `true` se a média for maior ou igual ao mínimo.
3. Combine as funções: leia uma lista de notas, calcule a média e imprima se o aluno está aprovado ou reprovado.

---

## Prática 4: Classes e Data Classes

### Objetivo
Entender classes, data classes e a diferença entre elas.

### Passo a Passo

1. Crie o arquivo `Classes.kt`:

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

// Data class: ideal para guardar dados (gera equals, hashCode, toString e copy automaticamente)
data class Produto(
    val id: Int,
    val nome: String,
    val preco: Double
)

fun main() {
    val conta = ContaBancaria("Ana", 100.0)
    conta.depositar(50.0)
    conta.sacar(30.0)
    conta.sacar(200.0)

    println("---")

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

2. Execute e observe como `data class` simplifica o trabalho com dados.

### Exercícios

1. Crie uma data class `Aluno(val nome: String, val matricula: String, val nota: Double)`. Crie três alunos e imprima o que tem a maior nota.
2. Adicione um método `descricao()` à data class `Produto` que retorne uma string formatada como `"[id] nome - R$preco"`.
3. Crie uma classe `Calculadora` com os métodos `somar`, `subtrair`, `multiplicar` e `dividir`. No método `dividir`, verifique se o divisor é zero antes de calcular.

---

## Prática 5: Coleções e Lambdas

### Objetivo
Trabalhar com listas, mapas e as funções de ordem superior mais comuns do Kotlin.

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

### Exercícios

1. Crie uma lista de notas `listOf(4.5, 7.0, 9.5, 6.0, 3.0, 8.5)` e:
   - Filtre apenas as notas maiores ou iguais a 6.0.
   - Calcule a média das notas aprovadas.
   - Imprima quantos alunos foram aprovados e quantos reprovaram.
2. Crie um `MutableMap<String, Int>` representando um inventário de itens. Adicione e remova itens, depois imprima os itens em ordem alfabética com suas quantidades.
3. Dada uma lista de palavras, retorne uma nova lista sem duplicatas, ordenada e com apenas palavras que tenham mais de 4 letras. Use encadeamento de funções (chain).

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
