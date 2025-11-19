# Eventos One-Shot: Usando `SharedFlow` para Navegação e Toasts com Jetpack Compose

No desenvolvimento Android com Jetpack Compose, é comum que o `ViewModel` precise comunicar eventos que devem ser consumidos apenas uma vez pela UI. Exemplos clássicos são:

*   Exibir uma mensagem de `Toast`.
*   Navegar para outra tela.
*   Mostrar um `Dialog`.

Esses são chamados de **eventos one-shot** (disparo único).

## O Problema com `LiveData`

Usar `LiveData` para esses eventos pode ser problemático. `LiveData` é um *state holder* (detentor de estado), o que significa que ele armazena o último valor emitido. Se ocorrer uma mudança de configuração (como a rotação da tela), a `Activity` é recriada, um novo observador é registrado e o `LiveData` entrega seu último valor novamente, fazendo com que o evento (como um `Toast`) seja disparado uma segunda vez indesejadamente.

## A Solução: `SharedFlow`

`SharedFlow` é um componente dos Coroutines do Kotlin que funciona como um *hot flow*. Diferente do `StateFlow`, ele pode ser configurado para não re-enviar o último valor para novos coletores, tornando-o ideal para eventos one-shot.

### 1. Definindo os Eventos

É uma boa prática definir os possíveis eventos da UI usando uma `sealed class`. Isso garante segurança de tipo e torna o tratamento dos eventos mais claro.

```kotlin
// UiEvent.kt
sealed class UiEvent {
    data class ShowToast(val message: String) : UiEvent()
    object NavigateToHome : UiEvent()
    // Adicione outros eventos conforme necessário
}
```

### 2. Configurando o `SharedFlow` no ViewModel

No seu `ViewModel`, crie um `MutableSharedFlow` para emitir os eventos e exponha-o como um `SharedFlow` imutável para a UI.

```kotlin
// LoginViewModel.kt
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {

    private val _eventFlow = MutableSharedFlow<UiEvent>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    
    val eventFlow = _eventFlow.asSharedFlow()

    fun onLoginButtonClick(success: Boolean) {
        viewModelScope.launch {
            if (success) {
                _eventFlow.emit(UiEvent.NavigateToHome)
            } else {
                _eventFlow.emit(UiEvent.ShowToast("Usuário ou senha inválidos!"))
            }
        }
    }
}
```

### 3. Coletando os Eventos na UI (Jetpack Compose)

Na sua função composable, você deve coletar o `eventFlow` de uma maneira que respeite o ciclo de vida da UI. A forma recomendada é usar `LaunchedEffect` para coletar os eventos.

```kotlin
// LoginScreen.kt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.collect

@Composable
fun LoginScreen(viewModel: LoginViewModel = viewModel()) {
    val context = LocalContext.current
    val eventFlow = viewModel.eventFlow.collectAsState(initial = null)

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is UiEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                is UiEvent.NavigateToHome -> {
                    // Lógica de navegação
                    println("Navegando para a tela Home...")
                }
            }
        }
    }

    // UI do Login
    Column {
        Button(onClick = { viewModel.onLoginButtonClick(success = true) }) {
            Text("Simular Login com Sucesso")
        }
        Button(onClick = { viewModel.onLoginButtonClick(success = false) }) {
            Text("Simular Login com Falha")
        }
    }
}
```

### Resumo

| Componente | Responsabilidade | Código Chave |
| :--- | :--- | :--- |
| **`sealed class`** | Definir os tipos de eventos de UI de forma segura. | `sealed class UiEvent` |
| **`ViewModel`** | Criar, configurar e emitir eventos via `SharedFlow`. | `MutableSharedFlow<UiEvent>(replay = 0)` e `_eventFlow.emit(...)` |
| **`Composable`** | Coletar os eventos de forma segura e reagir a eles. | `LaunchedEffect` e `viewModel.eventFlow.collect` |

Este padrão fornece uma maneira robusta e eficiente de lidar com a comunicação `ViewModel` -> `UI` para eventos que devem ser executados apenas uma vez, resolvendo as armadilhas comuns do `LiveData` para este caso de uso em aplicações que utilizam Jetpack Compose.