# Kotlin Intermediário para Android

Conceitos de Kotlin essenciais para o Módulo 2 (MVVM, StateFlow, listas). Ferramentas da linguagem que tornam o código Android mais expressivo, seguro e conciso.

> **Pré-requisito:** leia [02 – Kotlin Essencial](02_kotlin_essencial.md) antes. Espera-se familiaridade com tipos, null safety, data classes e lambdas.

---

## 1. Scope Functions (let, run, with, apply, also)

Scope functions executam um bloco no contexto de um objeto. A diferença está em **como acessar o objeto** (`this` ou `it`) e **o que retornam**.

```kotlin
// -- let: acessa via 'it', retorna resultado do bloco --
val nome: String? = intent.getStringExtra("usuario") // pode ser nulo
nome?.let {
    textView.text = "Bem-vindo, $it" // exibe só se não for nulo
}

// -- run: acessa via 'this', retorna resultado do bloco --
val tamanho = "Kotlin Android".run {
    uppercase() // transforma em maiúsculas
    length      // retorna o comprimento (último valor do bloco)
}

// -- with: igual a run, mas o objeto vem como argumento --
val config = StringBuilder()
val resultado = with(config) {
    append("debug=true")  // 'this' é o StringBuilder
    append("&lang=pt-BR") // encadeia chamadas
    toString()             // retorna a string final
}

// -- apply: acessa via 'this', retorna o próprio objeto --
val params = Bundle().apply {
    putString("titulo", "Meu App") // configura o Bundle
    putInt("versao", 1)            // adiciona outro parâmetro
} // 'params' é o Bundle já configurado

// -- also: acessa via 'it', retorna o próprio objeto --
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

---

## 2. Sealed Classes e Sealed Interfaces

Sealed classes/interfaces restringem a hierarquia a subtipos finitos. O compilador garante que `when` cubra todos os casos — ideal para **estados de UI**.

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
    // sem 'else' — o compilador exige todos os subtipos
}
```

**Sealed interface** funciona da mesma forma, mas permite que subtipos herdem de outras classes:

```kotlin
sealed interface Resultado   // interface selada
data class Ok(val dado: String) : Resultado  // pode herdar de outra classe também
data class Falha(val erro: Throwable) : Resultado
```

> No Módulo 2 você usará `sealed interface UiState` para controlar o estado das telas com MVVM.

---

## 3. Extension Functions

Extensões adicionam comportamento a classes existentes **sem modificá-las**. Muito comuns em projetos Android.

```kotlin
// Extensão para formatar centavos como moeda brasileira
fun Int.formatarReais(): String {
    val reais = this / 100        // parte inteira
    val centavos = this % 100     // parte decimal
    return "R$ $reais,${centavos.toString().padStart(2, '0')}" // formata com duas casas
}

val preco = 1999 // centavos
println(preco.formatarReais()) // "R$ 19,99"

// Extensão útil: converter dp para pixels no Android
fun Int.dpToPx(context: Context): Int {
    val densidade = context.resources.displayMetrics.density // fator de escala
    return (this * densidade).toInt() // multiplica e arredonda
}
```

---

## 4. Generics Básicos

Generics permitem criar classes e funções que operam sobre **qualquer tipo**, com segurança.

```kotlin
// Classe genérica que encapsula um resultado de rede
class Resposta<T>(
    val dados: T?,          // tipo genérico — definido no uso
    val erro: String? = null // mensagem de erro opcional
)

val respostaUsuario = Resposta(dados = "João") // T = String
val respostaIdade = Resposta(dados = 25)       // T = Int

// Função genérica com restrição (upper bound)
fun <T : Comparable<T>> maiorEntre(a: T, b: T): T {
    return if (a > b) a else b // funciona com qualquer Comparable
}
```

**Covariância (`out`) e contravariância (`in`):**

```kotlin
interface Fonte<out T> { fun proximo(): T }   // 'out' — só produz T (leitura)
interface Destino<in T> { fun enviar(item: T) } // 'in' — só consome T (escrita)
```

---

## 5. Enum Classes

Enums representam um conjunto fixo de **valores constantes**, sem dados variáveis por instância.

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

---

## 6. Object e Companion Object

`object` cria um **singleton** — uma única instância garantida pela linguagem. `companion object` simula membros "estáticos" dentro de uma classe.

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

---

## 7. Operadores de Coleção

Operações funcionais em listas são fundamentais para manipular dados de APIs e repositórios.

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

Encadeamento é comum e legível:

```kotlin
// Títulos de tarefas pendentes de alta prioridade, em ordem alfabética
val resultado = tarefas
    .filter { !it.concluida }        // remove concluídas
    .filter { it.prioridade == 1 }   // mantém alta prioridade
    .map { it.titulo }               // extrai títulos
    .sorted()                        // ordena A-Z
```

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

---

## 9. Próximos Passos

Com esses conceitos você está preparado para o **Módulo 2** (MVVM):

- **Sealed interfaces** → modelar `UiState` no ViewModel
- **Operadores de coleção** → transformar dados do repositório para a UI
- **Scope functions** → configurar objetos e tratar nulos de forma idiomática

👉 Siga para [Módulo 2 – MVVM + Fluxo Unidirecional](../modulo_02/01_mvvm.md)
