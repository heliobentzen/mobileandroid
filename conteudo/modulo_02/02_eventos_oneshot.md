# Eventos One-Shot: Usando `SharedFlow` para Navegação e Toasts

No desenvolvimento Android com arquiteturas modernas como MVVM ou MVI, é comum que o `ViewModel` precise comunicar eventos que devem ser consumidos apenas uma vez pela UI (Activity/Fragment). Exemplos clássicos são:

*   Exibir uma mensagem de `Toast` ou `Snackbar`.
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

*   **`_eventFlow` (Mutable):** Privado e usado internamente no ViewModel para emitir novos eventos.
*   **`eventFlow` (Imutável):** Público e exposto para a UI apenas para coleta (observação).

```kotlin
// LoginViewModel.kt
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {

    // 1. Cria o MutableSharedFlow
    private val _eventFlow = MutableSharedFlow<UiEvent>(
        replay = 0, // Não reenvia o último evento para novos coletores
        extraBufferCapacity = 1, // Garante espaço para um evento mesmo sem coletor ativo
        onBufferOverflow = BufferOverflow.DROP_OLDEST // Descarta o evento mais antigo se o buffer encher
    )
    
    // 2. Expõe como um SharedFlow imutável
    val eventFlow = _eventFlow.asSharedFlow()

    fun onLoginButtonClick(success: Boolean) {
        viewModelScope.launch {
            if (success) {
                // 3. Emite um evento de navegação
                _eventFlow.emit(UiEvent.NavigateToHome)
            } else {
                // 3. Emite um evento de Toast
                _eventFlow.emit(UiEvent.ShowToast("Usuário ou senha inválidos!"))
            }
        }
    }
}
```

**Explicação da Configuração:**
*   `replay = 0`: Esta é a configuração chave. Garante que novos coletores (como uma Activity recriada) não recebam o último evento emitido.
*   `extraBufferCapacity = 1`: Cria um pequeno buffer para armazenar um evento mesmo que não haja um coletor ativo no momento da emissão. Isso evita que a corrotina que chama `emit` suspenda desnecessariamente.
*   `onBufferOverflow = BufferOverflow.DROP_OLDEST`: Se eventos forem emitidos mais rápido do que podem ser consumidos, o mais antigo é descartado. Para eventos de UI, isso geralmente é o comportamento desejado.

### 3. Coletando os Eventos na UI (Activity/Fragment)

Na sua `Activity` ou `Fragment`, você deve coletar o `eventFlow` de uma maneira que respeite o ciclo de vida da UI. A forma recomendada é usar `lifecycleScope.launch` com `repeatOnLifecycle`.

```kotlin
// LoginActivity.kt
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ... setContentView, etc.

        // Exemplo de como disparar os eventos
        // binding.loginButton.setOnClickListener {
        //     viewModel.onLoginButtonClick(success = true) 
        // }

        collectUiEvents()
    }

    private fun collectUiEvents() {
        lifecycleScope.launch {
            // Inicia a coleta quando o ciclo de vida está em STARTED
            // e para quando está em STOPPED. Reinicia se o ciclo de vida
            // passar por STARTED novamente.
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.eventFlow.collect { event ->
                    handleEvent(event)
                }
            }
        }
    }

    private fun handleEvent(event: UiEvent) {
        when (event) {
            is UiEvent.ShowToast -> {
                Toast.makeText(this, event.message, Toast.LENGTH_SHORT).show()
            }
            is UiEvent.NavigateToHome -> {
                // Lógica de navegação
                // startActivity(Intent(this, HomeActivity::class.java))
                // finish()
                println("Navegando para a tela Home...")
            }
        }
    }
}
```

**Por que `repeatOnLifecycle`?**
Esta API garante que a coleta do flow só aconteça quando a UI está visível para o usuário (`STARTED`). Ela automaticamente cancela e recria a coleta conforme o ciclo de vida muda, prevenindo o consumo de eventos quando o app está em segundo plano e evitando vazamentos de memória.

### Resumo

| Componente | Responsabilidade | Código Chave |
| :--- | :--- | :--- |
| **`sealed class`** | Definir os tipos de eventos de UI de forma segura. | `sealed class UiEvent` |
| **`ViewModel`** | Criar, configurar e emitir eventos via `SharedFlow`. | `MutableSharedFlow<UiEvent>(replay = 0)` e `_eventFlow.emit(...)` |
| **`Activity/Fragment`** | Coletar os eventos de forma segura em relação ao ciclo de vida e reagir a eles. | `repeatOnLifecycle(State.STARTED)` e `viewModel.eventFlow.collect` |

Este padrão fornece uma maneira robusta e eficiente de lidar com a comunicação `ViewModel` -> `UI` para eventos que devem ser executados apenas uma vez, resolvendo as armadilhas comuns do `LiveData` para este caso de uso.

### Exemplo Completo

A seguir, um exemplo prático de como as peças se conectam em uma tela de login simples, usando View Binding para interagir com o layout XML.

**1. Layout (`res/layout/activity_login.xml`)**

Um layout simples com dois botões para simular um login bem-sucedido ou com falha.

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:gravity="center"
    android:orientation="vertical"
    android:padding="16dp"
    tools:context=".LoginActivity">

    <Button
        android:id="@+id/loginSuccessButton"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Simular Login com Sucesso" />

    <Button
        android:id="@+id/loginFailButton"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="8dp"
        android:text="Simular Login com Falha" />

</LinearLayout>
```

**2. Eventos da UI (`UiEvent.kt`)**

A `sealed class` que define os eventos possíveis.

```kotlin
sealed class UiEvent {
    data class ShowToast(val message: String) : UiEvent()
    object NavigateToHome : UiEvent()
}
```

**3. ViewModel (`LoginViewModel.kt`)**

O `ViewModel` que orquestra a lógica e emite os eventos.

```kotlin
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

**4. Activity (`LoginActivity.kt`)**

A `Activity` que infla o layout, configura os listeners dos botões e coleta os eventos do `ViewModel`.

```kotlin
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.yourapp.databinding.ActivityLoginBinding // Importe sua classe de binding
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
        collectUiEvents()
    }

    private fun setupClickListeners() {
        binding.loginSuccessButton.setOnClickListener {
            viewModel.onLoginButtonClick(success = true)
        }
        binding.loginFailButton.setOnClickListener {
            viewModel.onLoginButtonClick(success = false)
        }
    }

    private fun collectUiEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.eventFlow.collect { event ->
                    handleEvent(event)
                }
            }
        }
    }

    private fun handleEvent(event: UiEvent) {
        when (event) {
            is UiEvent.ShowToast -> {
                Toast.makeText(this, event.message, Toast.LENGTH_SHORT).show()
            }
            is UiEvent.NavigateToHome -> {
                // Aqui você colocaria a lógica real de navegação
                Log.d("LoginActivity", "Navegando para a tela Home...")
                // startActivity(Intent(this, HomeActivity::class.java))
                // finish()
            }
        }
    }
}
```