# Guia Prático: Jetpack Compose e Navegação para Iniciantes

Este guia irá demonstrar como criar um aplicativo Android simples usando Jetpack Compose, com duas telas e navegação entre elas. É ideal para quem está começando com o Compose e quer entender os conceitos básicos de navegação.

## 1. Configuração do Projeto

Primeiro, vamos configurar um novo projeto no Android Studio.

1.  Abra o Android Studio.
2.  Clique em `New Project`.
3.  Selecione o template `Empty Activity` na aba `Phone and Tablet` e clique em `Next`.
4.  Configure seu projeto:
    *   **Name:** `ComposeNavigationApp`
    *   **Package name:** `com.example.composenavigationapp` (ou o que preferir)
    *   **Save location:** Escolha um diretório para salvar o projeto.
    *   **Language:** `Kotlin`
    *   **Minimum SDK version:** `API 21: Android 5.0 (Lollipop)` (ou superior)
    *   **Build configuration language:** `Kotlin DSL`
5.  Clique em `Finish`.

O Android Studio criará um projeto com a configuração básica do Jetpack Compose.

## 2. Adicionando Dependências de Navegação

Para usar o componente de navegação do Jetpack Compose, precisamos adicionar as dependências necessárias ao arquivo `build.gradle.kts` (Module: app).

Abra `app/build.gradle.kts` e adicione as seguintes linhas dentro do bloco `dependencies { ... }`:

```kotlin
dependencies {
    // ... outras dependências existentes

    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.7.7")
}
```

Sincronize o projeto com os arquivos Gradle clicando no botão `Sync Now` que aparecerá no canto superior direito do Android Studio.

## 3. Criando as Telas (Composables)

Vamos criar duas funções Componíveis que representarão nossas telas: `ScreenA` e `ScreenB`.

Abra o arquivo `MainActivity.kt` e adicione as seguintes funções Componíveis fora da função `onCreate` e da classe `MainActivity` (ou em um novo arquivo Kotlin, se preferir):

```kotlin
package com.example.composenavigationapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.composenavigationapp.ui.theme.ComposeNavigationAppTheme

// ... (código existente da MainActivity)

@Composable
fun ScreenA(navController: NavController) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Esta é a Tela A")
        Button(onClick = { navController.navigate("screen_b") }) {
            Text("Ir para Tela B")
        }
    }
}

@Composable
fun ScreenB(navController: NavController, message: String?) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Esta é a Tela B")
        message?.let { Text(text = "Mensagem da Tela A: $it") }
        Button(onClick = { navController.popBackStack() }) {
            Text("Voltar para Tela A")
        }
    }
}

// Pré-visualizações (opcional)
@Preview(showBackground = true)
@Composable
fun ScreenAPreview() {
    ComposeNavigationAppTheme {
        ScreenA(rememberNavController())
    }
}

@Preview(showBackground = true)
@Composable
fun ScreenBPreview() {
    ComposeNavigationAppTheme {
        ScreenB(rememberNavController(), "Olá da Tela A")
    }
}
```

**Explicação:**

*   Ambas as telas recebem um `NavController` como parâmetro, que é essencial para a navegação.
*   `ScreenA` tem um botão que, ao ser clicado, chama `navController.navigate("screen_b")` para ir para a `ScreenB`.
*   `ScreenB` tem um botão que usa `navController.popBackStack()` para voltar para a tela anterior (neste caso, `ScreenA`).
*   `ScreenB` também demonstra como receber um argumento (`message`) da tela anterior.

## 4. Configurando a Navegação Principal

Agora, vamos configurar o `NavHost` na `MainActivity` para gerenciar a navegação entre as telas.

Modifique a função `onCreate` e a função `App` (ou o Composable principal que você usa) em `MainActivity.kt` para incluir o `NavHost`:

```kotlin
package com.example.composenavigationapp

// ... (imports existentes)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ComposeNavigationAppTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "screen_a") {
        composable("screen_a") {
            ScreenA(navController = navController)
        }
        composable("screen_b?message={message}") {
            val message = it.arguments?.getString("message")
            ScreenB(navController = navController, message = message)
        }
    }
}

// ... (ScreenA, ScreenB e Previews)
```

**Explicação:**

*   `rememberNavController()`: Cria e lembra uma instância de `NavController`, que é o coração da navegação.
*   `NavHost`: É o Composable que hospeda o gráfico de navegação. Ele precisa de um `navController` e de uma `startDestination` (a rota inicial).
*   `composable("screen_a")`: Define uma rota para a `ScreenA`. O nome da rota (`"screen_a"`) é usado para navegar até ela.
*   `composable("screen_b?message={message}")`: Define uma rota para a `ScreenB`. Observe o `?message={message}`. Isso indica que a `ScreenB` pode receber um argumento opcional chamado `message`.
    *   Dentro do bloco `composable` para `screen_b`, recuperamos o argumento `message` usando `it.arguments?.getString("message")`.

## 5. Navegando e Passando Dados

Para navegar da `ScreenA` para a `ScreenB` e passar um dado, você modificaria a chamada `navController.navigate` na `ScreenA` da seguinte forma:

```kotlin
@Composable
fun ScreenA(navController: NavController) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Esta é a Tela A")
        Button(onClick = { navController.navigate("screen_b?message=Ola da Tela A!") }) {
            Text("Ir para Tela B com Mensagem")
        }
        Button(onClick = { navController.navigate("screen_b") }) {
            Text("Ir para Tela B (sem Mensagem)")
        }
    }
}
```

Agora, ao clicar no primeiro botão, a `ScreenB` será aberta e exibirá a mensagem "Olá da Tela A!". O segundo botão ainda navega sem passar a mensagem.

## Conclusão

Você acabou de criar um aplicativo Android com Jetpack Compose, implementando duas telas e a navegação básica entre elas, incluindo a passagem de dados. Este é um ponto de partida sólido para construir UIs mais complexas e interativas com o Jetpack Compose.

Para aprofundar seus conhecimentos, explore a documentação oficial do Android sobre navegação no Compose [1].

## Referências

[1] Navegação no Compose. Disponível em: [https://developer.android.com/jetpack/compose/navigation](https://developer.android.com/jetpack/compose/navigation)
