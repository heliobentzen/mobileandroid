# Jetpack Compose: O Básico

Jetpack Compose é o kit de ferramentas moderno do Android para criar UIs nativas. Ele simplifica e acelera o desenvolvimento de UIs no Android com menos código, ferramentas poderosas e APIs intuitivas do Kotlin.

## 1. Funções `@Composable`

As funções `@Composable` são os blocos de construção fundamentais da sua UI no Compose. Elas descrevem como a UI deve ser, dados os inputs, e o Compose se encarrega de atualizá-la quando os dados mudam.

-   Uma função marcada com a anotação `@Composable` pode ser chamada por outras funções `@Composable`.
-   Elas emitem UI.
-   Não retornam nada (`Unit`).

**Exemplo:**

```kotlin
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun Greeting(name: String) {
    Text(text = "Olá, $name!")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    Greeting("Jetpack Compose")
}
```

## 2. State e Recomposition

**State** em um app é qualquer valor que pode mudar ao longo do tempo. No Compose, quando o estado de um Composable muda, a framework automaticamente "redesenha" (recompõe) as partes da UI que dependem desse estado.

### `remember` e `mutableStateOf`

Para que o Compose rastreie uma variável como um estado, você precisa usar `mutableStateOf`. Para que esse estado sobreviva às recomposições, você deve envolvê-lo com `remember`.

-   `mutableStateOf(initialValue)`: Cria um objeto de estado observável.
-   `remember { ... }`: Armazena o valor produzido pelo lambda na composição e o mantém durante as recomposições.

**Exemplo: Um contador simples**

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
    // 'count' é o estado. 'remember' garante que ele não seja resetado para 0 a cada recomposição.
    var count by remember { mutableStateOf(0) }

    Column {
        Text(text = "Você clicou $count vezes.")
        Button(onClick = { count++ }) { // Quando o botão é clicado, o estado 'count' muda.
            Text("Clique aqui")
        }
    }
    // A mudança em 'count' faz com que SimpleCounter seja recomposto,
    // atualizando o Text com o novo valor.
}
```

### Recomposition

É o processo de chamar suas funções `@Composable` novamente quando o estado que elas dependem muda. O Compose é inteligente e só recompõe as funções que realmente precisam ser atualizadas, tornando o processo eficiente.

## 3. State Hoisting (Elevação de Estado)

State hoisting é um padrão no Compose onde você move o estado para o "ancestral" comum mais próximo dos composables que o utilizam. Isso torna os composables mais reutilizáveis, testáveis e menos propensos a erros, pois eles se tornam "stateless" (sem estado).

Um composable "stateless" não possui seu próprio estado. Ele recebe o estado de seu parente e expõe eventos (lambdas) para notificar o parente sobre mudanças.

**Exemplo: Refatorando o contador com State Hoisting**

```kotlin
// 1. Composable "Stateless" (sem estado)
@Composable
fun StatelessCounter(count: Int, onIncrement: () -> Unit) {
    Column {
        Text(text = "Você clicou $count vezes.")
        Button(onClick = onIncrement) { // Chama o lambda recebido
            Text("Clique aqui")
        }
    }
}

// 2. Composable "Stateful" (com estado) que gerencia o estado
@Composable
fun StatefulCounter() {
    var count by remember { mutableStateOf(0) }

    // O estado (count) e a lógica para atualizá-lo (onIncrement)
    // são passados para o composable stateless.
    StatelessCounter(
        count = count,
        onIncrement = { count++ }
    )
}
```

**Vantagens:**
-   `StatelessCounter` pode ser reutilizado em qualquer lugar.
-   O estado está centralizado em `StatefulCounter`, facilitando o gerenciamento.
-   `StatelessCounter` é mais fácil de testar, pois depende apenas de seus parâmetros.

## 4. Theming Mínimo com Material 3

Jetpack Compose vem com uma implementação do Material Design, facilitando a criação de apps com uma aparência consistente e moderna. O `MaterialTheme` é o composable central para aplicar temas.

O `MaterialTheme` fornece:
-   `colorScheme`: Paleta de cores (primária, secundária, fundo, etc.).
-   `typography`: Estilos de texto (títulos, corpo, legendas).
-   `shapes`: Formas dos componentes (arredondamento de cantos).

Quando você cria um novo projeto Compose, o Android Studio já gera uma configuração básica de tema em `ui/theme/Theme.kt`.

**Exemplo de uso:**

```kotlin
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
// Importe seu tema gerado pelo Android Studio
// import com.example.myapp.ui.theme.MyAppTheme

@Composable
fun ThemedApp() {
    // Envolva sua UI com o MaterialTheme do seu projeto.
    // MyAppTheme { // Descomente quando tiver seu tema
        // Surface usa a cor de fundo do tema.
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            // Text usará a cor e a tipografia padrão do tema.
            Greeting("Android")
        }
    // }
}
```

Ao usar os componentes do `androidx.compose.material3` (como `Button`, `Card`, `Text`), eles automaticamente usarão os valores definidos no `MaterialTheme` mais próximo na árvore de UI.