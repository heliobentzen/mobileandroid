# Kotlin Moderno para Android (Visão Essencial)

Kotlin é a linguagem recomendada pelo Google: segura contra NullPointerException, concisa e interoperável com Java. Este guia rápido foca no que realmente importa para começar a produzir apps Android modernos.

---

## 1. Android Moderno

Use bibliotecas Jetpack para evitar reinventar a roda:
- ViewModel + LiveData/StateFlow: estado sobrevive a rotação.
- Room: persistência.
- Navigation: fluxo de telas seguro.
- WorkManager: tarefas em segundo plano.
- Hilt: injeção de dependência simples.


---

## 2. Sintaxe Básica

```kotlin
val nome = "Ana"      // imutável
var idade = 20        // mutável
fun saudacao(n: String) = "Olá, $n"
fun soma(a: Int, b: Int = 10) = a + b
```

Expressões únicas evitam boilerplate. Strings com `$variavel` ou `${expressao}`.

---

## 3. Null Safety (o superpoder)

Tipos não aceitam null por padrão:
```kotlin
var titulo: String = "OK"
// titulo = null // erro
var subtitulo: String? = null
```

Acessos:
```kotlin
subtitulo?.length          // safe call
val tam = subtitulo?.length ?: 0 // Elvis
subtitulo!!.length         // evite (pode crashar)
if (subtitulo != null) println(subtitulo.length) // smart cast
```

Uso típico no Android:
```kotlin
val nome = intent.getStringExtra("NOME") ?: "Visitante"
textView?.text = nome
```

---

## 4. Classes, Data Classes, Objetos

```kotlin
class Pessoa(val nome: String, var idade: Int) {
    fun aniversario() { idade++ }
}
data class Usuario(val id: Int, val nome: String)
val u = Usuario(1, "Ana").copy(nome = "Bia")
val (id, n) = u
object Config { const val versao = "1.0" }
```

Se o objetivo é “guardar dados”, use data class. Para um único ponto global, use object (singleton).

---

## 5. Coleções + Lambdas

```kotlin
val nums = listOf(1,2,3)
val mut = mutableListOf(1,2)
val dobro = nums.map { it * 2 }
val pares = nums.filter { it % 2 == 0 }
val soma = nums.sum()
```

Lazy:
```kotlin
val primeiros = (1..1_000_000)
    .asSequence()
    .filter { it % 3 == 0 }
    .take(5)
    .toList()
```

Scope functions (contexto rápido):
```kotlin
val p = Pessoa("João", 30).apply { aniversario() }
val idade = p.run { idade }
```

---

## 6. Extensões (turbinando APIs)

Adicionar comportamento sem herdar:
```kotlin
fun String.reversa() = reversed()
fun Int.isPar() = this % 2 == 0
"Code".reversa()
5.isPar()
```

Nullable:
```kotlin
fun String?.isNullOuVazia() = this == null || isEmpty()
```

Android comum:
```kotlin
fun View.show() { visibility = View.VISIBLE }
fun View.hide() { visibility = View.GONE }
fun Context.toast(msg: String) =
    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
```

Limitação (dispatch estático):
```kotlin
open class Animal
class Gato: Animal()
fun Animal.som() = "?"
fun Gato.som() = "miau"
val a: Animal = Gato()
a.som() // "?"
```

Use método real na classe se quiser polimorfismo dinâmico.

---

## 7. Sealed Classes, Smart Casts, Pattern-Like

Modelar estados finitos:
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

Smart cast:
```kotlin
fun tamanho(x: Any) {
    if (x is String) println(x.length)
}
```

---

## 8. Exercícios

1. Função que recebe 3 Int e retorna o maior; depois use para reduzir uma lista.
2. Data class Produto(nome, preco, categoria) + método aplicarDesconto(percent).
3. Filtrar nomes que começam com A/a e têm > 3 chars; map para maiúsculas.
4. Extensão String: inverter e trocar vogais por *.
5. Sealed ResultadoOperacao: Sucesso(Int), Falha(String), Pendente; imprimir mensagem via when.

---

## 9. Cheatsheet Relâmpago

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

## 10. Mentalidade Kotlin

- Prefira val; mude para var só se necessário.
- Trate null cedo.
- Nomes claros > truques.
- Evite !! — sinal de design frágil.
- Use data/ sealed para modelar domínio, não enums pobres.
- Priorize imutabilidade (fluxos previsíveis).

Comece pequeno: recrie utilidades como extensões, depois migre para Compose e coroutines. Aprenda iterando.

Foque em clareza. Kotlin já reduz o resto.
