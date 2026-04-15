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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun Greeting(name: String) {
    Text("Olá, $name!")
}

@Preview(showBackground = true)
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
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun SimpleCounter() {
    // 'remember' mantém o valor entre recomposições
    // 'by' é um atalho do Kotlin para getter/setter do State
    var count by remember { mutableStateOf(0) }

    Column {
        Text("Você clicou $count vezes.")
        Button(onClick = { count++ }) {
            Text("Incrementar")
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
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

// Composable "burro": apenas recebe dados e emite eventos
@Composable
fun CounterStateless(count: Int, onIncrement: () -> Unit) {
    Column {
        Text("Clique: $count")
        Button(onClick = onIncrement) {
            Text("Adicionar")
        }
    }
}

// Composable "inteligente": gerencia o estado e delega para o stateless
@Composable
fun CounterStateful() {
    var count by remember { mutableStateOf(0) }
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable

@Composable
fun AppRoot() {
    // Substitua pelo tema do seu projeto: MyAppTheme { ... }
    MaterialTheme {
        Surface(
            color = MaterialTheme.colorScheme.background // Material 3 usa colorScheme
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
