# Eventos One-Shot: Usando `SharedFlow` para Navegação e Toasts com Jetpack Compose

No desenvolvimento Android com Jetpack Compose, é comum que o `ViewModel` (a classe que guarda o estado e a lógica de uma tela — veja a aula anterior, `01_mvvm.md`) precise comunicar eventos que devem ser consumidos **apenas uma vez** pela UI. Exemplos clássicos são:

*   Exibir uma mensagem de `Toast` (aquela caixinha de texto que aparece por alguns segundos na tela).
*   Navegar para outra tela.
*   Mostrar um `Dialog` (janela de alerta sobreposta ao conteúdo).

Esses são chamados de **eventos one-shot** (disparo único): eles representam uma **ação** que deve acontecer uma vez, e não um **dado** que a tela deve continuar exibindo.

## O que é um evento one-shot?

Pense na diferença entre **estado** e **evento**:

- **Estado** é algo que a tela deve continuar mostrando enquanto for verdade. Exemplo: "o campo de e-mail contém erro" — isso deve ficar visível até o usuário corrigir. É o que vimos com `StateFlow` na aula anterior.
- **Evento** é algo que acontece **uma vez** e depois não deve se repetir. Exemplo: "mostrar um Toast dizendo 'Login inválido'" — se a tela for redesenhada de novo (por exemplo, após girar o celular), esse Toast não deveria aparecer outra vez sozinho.

Analogia: estado é como o placar de um jogo, sempre visível no painel. Evento é como o apito do juiz — acontece uma vez, é ouvido, e não deve "tocar de novo sozinho" só porque alguém olhou para o painel de novo.

## Por que isso importa: o Problema com `LiveData`

Antigamente (e ainda em código legado), muitos apps usavam `LiveData` para tudo, inclusive para eventos. Isso pode ser problemático. `LiveData` é um *state holder* (detentor de estado), o que significa que ele armazena o último valor emitido e o entrega a qualquer novo observador. Se ocorrer uma mudança de configuração (como a rotação da tela), a `Activity` é recriada, um novo observador é registrado, e o `LiveData` entrega seu último valor novamente — fazendo com que o evento (como um `Toast`) seja disparado **uma segunda vez indesejadamente**, mesmo sem nenhuma ação nova do usuário.

Se você não resolver isso, o usuário pode ver um Toast de erro reaparecer sozinho depois de girar o celular, ou o app tentar navegar de novo para uma tela que ele já tinha saído — um bug confuso e difícil de reproduzir.

## A Solução: `SharedFlow`

`SharedFlow` é um componente dos Coroutines do Kotlin que funciona como um **hot flow**: um fluxo de dados que existe e emite valores independentemente de haver alguém "ouvindo" no momento (diferente de um *cold flow*, que só começa a produzir valores quando alguém começa a coletá-lo). Diferente do `StateFlow` (que sempre guarda e reenvia o último valor, ótimo para *estado*), o `SharedFlow` pode ser configurado para **não** re-enviar o último valor para novos coletores — tornando-o ideal para eventos one-shot, que devem ser "consumidos" e esquecidos.

### 1. Definindo os Eventos

É uma boa prática definir os possíveis eventos da UI usando uma `sealed class` — um tipo do Kotlin que restringe quais subtipos podem existir, todos declarados no mesmo lugar. Isso garante segurança de tipo (o compilador sabe todos os eventos possíveis) e torna o tratamento dos eventos mais claro, já que um `when` sobre uma `sealed class` obriga você a tratar cada caso.

```kotlin
// UiEvent.kt
sealed class UiEvent {
    data class ShowToast(val message: String) : UiEvent() // evento com dado (a mensagem)
    object NavigateToHome : UiEvent()                      // evento sem dado adicional
    // Adicione outros eventos conforme necessário
}
```

### 2. Configurando o `SharedFlow` no ViewModel

No seu `ViewModel`, crie um `MutableSharedFlow` para emitir os eventos e exponha-o como um `SharedFlow` imutável para a UI — o mesmo princípio de "privado mutável / público somente-leitura" que usamos com `StateFlow` na aula anterior.

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
        replay = 0,                                   // Não re-emite eventos antigos para novos coletores (essencial para one-shot)
        extraBufferCapacity = 1,                      // Permite emit() sem suspender mesmo sem coletor ativo no momento
        onBufferOverflow = BufferOverflow.DROP_OLDEST // Se o buffer estiver cheio, descarta o evento mais antigo
    )

    // asSharedFlow() converte o Mutable-flow privado em uma versão pública
    // somente-leitura — a View pode observar, mas nunca emitir eventos por conta própria.
    val eventFlow = _eventFlow.asSharedFlow()

    fun onLoginButtonClick(success: Boolean) {
        // emit() é uma função suspend, por isso precisa rodar dentro de uma coroutine
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

**Entendendo os três parâmetros do `MutableSharedFlow`:**

| Parâmetro | O que faz | Por que esse valor aqui |
|---|---|---|
| `replay = 0` | Quantos dos últimos valores emitidos são re-entregues a um novo coletor. | `0` garante que um evento já disparado nunca seja "reproduzido" de novo — é o coração da solução para o problema do `LiveData`. |
| `extraBufferCapacity = 1` | Quantos valores podem ficar em espera se ainda não houver coletor ativo. | Evita que `emit()` fique suspenso (travado) esperando alguém coletar, no raro caso de a UI ainda não estar observando. |
| `onBufferOverflow` | O que fazer se o buffer encher. | `DROP_OLDEST` descarta o evento mais antigo em vez de travar o app — apropriado para eventos de UI, onde perder um Toast antigo é aceitável. |

### 3. Coletando os Eventos na UI (Jetpack Compose)

Na sua função composable, você deve coletar o `eventFlow` de uma maneira que respeite o ciclo de vida da UI. A forma recomendada é usar `LaunchedEffect` — um composable especial que executa um bloco de código (que pode conter chamadas `suspend`) quando entra na composição, e cancela automaticamente esse código se sair da composição.

```kotlin
// LoginScreen.kt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.collect

@Composable
fun LoginScreen(viewModel: LoginViewModel = viewModel()) {
    // LocalContext.current dá acesso ao Context do Android, necessário para
    // criar um Toast (que não é um conceito do Compose, e sim do Android "clássico").
    val context = LocalContext.current

    // LaunchedEffect(Unit): a chave "Unit" nunca muda, então este bloco roda
    // apenas UMA VEZ quando o composable entra em composição (e é cancelado
    // automaticamente quando o composable sai de tela) — ideal para "ficar
    // escutando" o eventFlow durante toda a vida da tela.
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is UiEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                is UiEvent.NavigateToHome -> {
                    // Lógica de navegação (veja a aula "Navegação com Jetpack Compose")
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

> **Nota:** para o `Toast` funcionar, é preciso importar `android.widget.Toast`. É um detalhe fácil de esquecer — veja a seção de erros comuns abaixo.

### Erros comuns / Pegadinhas

- **Usar `StateFlow` para eventos one-shot**: como `StateFlow` sempre guarda e reenvia o último valor, um evento de navegação ou Toast pode "disparar de novo sozinho" após rotação de tela — exatamente o mesmo bug que tínhamos com `LiveData`. Use `SharedFlow` com `replay = 0` para eventos.
- **Esquecer o `import android.widget.Toast`**: como o exemplo mistura Compose com uma API "clássica" do Android (`Toast`), é fácil esquecer o import e ver um erro de compilação confuso. Sempre confira os imports ao copiar exemplos.
- **Coletar o `eventFlow` fora de um `LaunchedEffect` (ex: direto no corpo do composable)**: chamar `collect` fora de uma coroutine gerenciada pelo ciclo de vida do Compose pode causar coleta duplicada a cada recomposição, ou vazamento de coroutine. Sempre use `LaunchedEffect` (ou `repeatOnLifecycle`, em telas baseadas em `View`/XML).

### Resumo

| Componente | Responsabilidade | Código Chave |
| :--- | :--- | :--- |
| **`sealed class`** | Definir os tipos de eventos de UI de forma segura. | `sealed class UiEvent` |
| **`ViewModel`** | Criar, configurar e emitir eventos via `SharedFlow`. | `MutableSharedFlow<UiEvent>(replay = 0)` e `_eventFlow.emit(...)` |
| **`Composable`** | Coletar os eventos de forma segura e reagir a eles. | `LaunchedEffect` e `viewModel.eventFlow.collect` |

Este padrão fornece uma maneira robusta e eficiente de lidar com a comunicação `ViewModel` → `UI` para eventos que devem ser executados apenas uma vez, resolvendo as armadilhas comuns do `LiveData` para este caso de uso em aplicações que utilizam Jetpack Compose.

**Próximo passo**: na próxima aula (`03_listas.md`) você vai aprender a exibir e atualizar listas de forma eficiente com `LazyColumn`, entendendo por que chaves estáveis (`key`) são tão importantes quanto o cuidado que tivemos aqui com eventos.

---

## Exercícios Práticos

1. **Adicionar um novo evento**
   - Checkpoint 1: adicione `data class ShowError(val message: String) : UiEvent()` à sealed class.
   - Checkpoint 2: emita esse evento no `LoginViewModel` em um novo cenário (ex: campo vazio).
   - Checkpoint 3: trate o novo caso no `when` do `LaunchedEffect`, mostrando outro `Toast` ou um `Snackbar`.

2. **Testar o comportamento one-shot**
   - Gire o dispositivo (ou force uma recomposição) logo após um evento ser emitido e confirme que o Toast **não** reaparece sozinho — esse é o comportamento que o `SharedFlow` com `replay = 0` garante.

3. **Desafio**: substitua o `Toast` por um `Snackbar` do Material 3 (`SnackbarHostState`), que é a forma mais moderna e idiomática de mostrar mensagens rápidas em Compose. Dica: você vai precisar de um `SnackbarHostState` lembrado com `remember` e chamar `snackbarHostState.showSnackbar(...)` dentro do `LaunchedEffect`.

---
