# Intents no Android

`Intents` são objetos de mensagem que você pode usar para solicitar uma ação de outro componente de aplicativo. Eles são um dos conceitos fundamentais do Android e servem como o "cimento" que liga os componentes (como Activities, Services e Broadcast Receivers) entre si.

Existem dois tipos principais de intents:

1.  **Intents Explícitas**: Especificam o componente exato a ser iniciado (pelo nome da classe). São usadas principalmente para iniciar componentes dentro do seu próprio aplicativo.
2.  **Intents Implícitas**: Não especificam o componente, mas declaram uma ação geral a ser executada. Isso permite que um componente de outro aplicativo a manipule. Por exemplo, se você quiser mostrar ao usuário uma localização em um mapa, pode usar uma intent implícita para solicitar que outro aplicativo capaz exiba a localização.

---

## Exemplo 1: Intent Explícita Simples

A forma mais comum de usar uma `Intent` é para iniciar uma nova `Activity`, mas com o advento do Jetpack Compose, a navegação interna pode ser feita de forma mais direta e eficiente.

**Cenário**: Navegar da `MainActivity` para uma `DetalhesActivity`.

**1. Código na `MainActivity.kt`**

```kotlin
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            NavHost(navController, startDestination = "main") {
                composable("main") { MainScreen(navController) }
                composable("detalhes") { DetalhesScreen() }
            }
        }
    }
}

@Composable
fun MainScreen(navController: NavController) {
    Button(onClick = { navController.navigate("detalhes") }) {
        Text("Ir para Detalhes")
    }
}

@Composable
fun DetalhesScreen() {
    Text("Tela de Detalhes")
}
```

---

## Exemplo 2: Passando Dados com uma Intent Explícita

Frequentemente, você precisa passar dados para a próxima tela.

**Cenário**: Enviar um nome de usuário da `MainActivity` para a `DetalhesActivity`.

**1. Código na `MainActivity.kt` (modificado)**

```kotlin
@Composable
fun MainScreen(navController: NavController) {
    val nomeUsuario = "Maria"
    Button(onClick = { 
        navController.navigate("detalhes?nome=$nomeUsuario") 
    }) {
        Text("Ir para Detalhes")
    }
}

@Composable
fun DetalhesScreen(nome: String?) {
    Text("Olá, $nome!")
}
```

---

## Exemplo 3: Intent Implícita Comum

Use intents implícitas para delegar tarefas a outros aplicativos.

**Cenário**: Abrir uma página web e discar um número de telefone.

```kotlin
// Em qualquer Activity ou Fragment

// Para abrir um navegador com uma URL
fun abrirPaginaWeb(url: String) {
    val uri = Uri.parse(url)
    val intent = Intent(Intent.ACTION_VIEW, uri)
    startActivity(intent)
}

// Para abrir o discador com um número
fun discarNumero(telefone: String) {
    val uri = Uri.parse("tel:$telefone")
    val intent = Intent(Intent.ACTION_DIAL, uri)
    startActivity(intent)
}

// Exemplo de uso
abrirPaginaWeb("https://github.com")
discarNumero("999998888")
```

O sistema Android procura por aplicativos que possam manipular a ação `ACTION_VIEW` para um URI web ou `ACTION_DIAL` para um URI de telefone e os inicia.

---

## Exemplo 4: Obtendo um Resultado de uma Activity (Forma Moderna)

A antiga API `startActivityForResult()` foi depreciada. A abordagem moderna usa `ActivityResultContracts`.

**Cenário**: A `MainActivity` inicia uma `SelecaoActivity` para que o usuário escolha um item. O item escolhido é retornado para a `MainActivity`.

**1. Código na `MainActivity.kt`**

```kotlin
import android.app.Activity
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : ComponentActivity() {

    // 1. Registre o "contrato" para iniciar uma activity e receber seu resultado.
    private val seletorDeItemLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            val itemSelecionado = data?.getStringExtra("EXTRA_ITEM_SELECIONADO")
            // Atualize a UI com o item selecionado
        }
    }

    // Resto do código...
}
```

**2. Código na `SelecaoActivity.kt`**

```kotlin
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity

class SelecaoActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Lógica para selecionar um item...

        // Quando o item for selecionado:
        val resultIntent = Intent()
        resultIntent.putExtra("EXTRA_ITEM_SELECIONADO", "Item 123")
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }
}
```
