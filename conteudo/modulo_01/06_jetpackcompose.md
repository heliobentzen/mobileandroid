# UI Moderna com Jetpack Compose

Jetpack Compose é o toolkit moderno do Android para construir interfaces nativas de forma declarativa, rápida e concisa usando Kotlin.

## Visão Geral Rápida
1. Você descreve a UI com funções `@Composable`.
2. O estado muda → Compose recompõe apenas o que precisa.
3. Elevação (hoisting) de estado → componentes mais reutilizáveis.
4. Tema Material 3 → aparência consistente.

---

## 1. Funções `@Composable`
São funções que emitem UI.

Características:
- Chamadas somente dentro de outros composables (ou previews/analisadores).
- Não retornam valores úteis para lógica (retornam `Unit`).
- Devem ser puras em relação à UI: mesma entrada → mesma saída visual (ideal).

Exemplo simples:

```kotlin
@Composable
fun Greeting(name: String) {
    androidx.compose.material3.Text("Olá, $name!")
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    Greeting("Compose")
}
```

Dica: pense em cada composable como uma pequena função de transformação de dados em elementos visuais.

---

## 2. Estado e Recomposition

Estado = dado que pode mudar ao longo do tempo e afeta a UI.

Quando um estado observado muda:
- A função composable que depende dele é reexecutada.
- Somente a parte afetada é atualizada.

### `remember` + `mutableStateOf`
- `mutableStateOf(valor)`: cria um estado observável.
- `remember { ... }`: mantém o estado entre recomposições (no mesmo escopo).

Exemplo contador:

```kotlin
@Composable
fun SimpleCounter() {
    var count by androidx.compose.runtime.remember { 
        androidx.compose.runtime.mutableStateOf(0) 
    }

    androidx.compose.foundation.layout.Column {
        androidx.compose.material3.Text("Você clicou $count vezes.")
        androidx.compose.material3.Button(onClick = { count++ }) {
            androidx.compose.material3.Text("Incrementar")
        }
    }
}
```

Evite guardar:
- Objetos pesados sem necessidade.
- Referências a context fora de escopos seguros.

---

## 3. State Hoisting (Elevação de Estado)

Objetivo: separar apresentação de lógica.

Regra prática:
- Composable stateless recebe: valor + callbacks.
- Composable stateful guarda: estado interno + transformação de eventos.

Exemplo refatorado:

```kotlin
@Composable
fun CounterStateless(count: Int, onIncrement: () -> Unit) {
    androidx.compose.foundation.layout.Column {
        androidx.compose.material3.Text("Clique: $count")
        androidx.compose.material3.Button(onClick = onIncrement) {
            androidx.compose.material3.Text("Adicionar")
        }
    }
}

@Composable
fun CounterStateful() {
    var count by androidx.compose.runtime.remember { 
        androidx.compose.runtime.mutableStateOf(0) 
    }
    CounterStateless(
        count = count,
        onIncrement = { count++ }
    )
}
```

Benefícios:
- Reutilização.
- Testes mais simples (passa valores simulados).
- Facilidade para mover lógica para ViewModel depois.

---

## 4. Theming Básico (Material 3)

`MaterialTheme` fornece:
- `colorScheme`
- `typography`
- `shapes`

Uso básico:

```kotlin
@Composable
fun AppRoot() {
    // Substitua pelo tema do seu projeto: MyAppTheme { ... }
    androidx.compose.material3.MaterialTheme {
        androidx.compose.material3.Surface(
            color = androidx.compose.material3.MaterialTheme.colorScheme.background
        ) {
            Greeting("Android")
        }
    }
}
```

Dica: mantenha cores, tipografia e formas em um arquivo central (gerado pelo template). Use somente tokens do tema nos componentes.

---

## 5. Checklist Mental

Antes de criar novo composable:
- Ele precisa guardar estado? Se não, torne-o stateless.
- O estado pode ser elevado? Prefira elevar.
- O que muda na recomposição? Minimizar escopos grandes.
- Está usando valores do tema? Consistência visual.

---

## 6. Exercício Sugerido

Crie:
1. Um campo de texto que conta caracteres digitados.
2. Separe em:
   - Composable stateless: mostra texto + contador.
   - Composable stateful: gerencia `remember` do texto.

---

## Resumo

Compose = UI declarativa.  
Estado muda → UI atualiza.  
Hoisting → componentes limpos.  
MaterialTheme → aparência unificada.

Comece pequeno, componha blocos, eleve estado conforme necessário.

Próximo passo: integrar ViewModel e fluxo de dados (StateFlow / LiveData).
