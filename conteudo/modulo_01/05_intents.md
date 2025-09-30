# Intents no Android

`Intents` são objetos de mensagem que você pode usar para solicitar uma ação de outro componente de aplicativo. Eles são um dos conceitos fundamentais do Android e servem como o "cimento" que liga os componentes (como Activities, Services e Broadcast Receivers) entre si.

Existem dois tipos principais de intents:

1.  **Intents Explícitas**: Especificam o componente exato a ser iniciado (pelo nome da classe). São usadas principalmente para iniciar componentes dentro do seu próprio aplicativo.
2.  **Intents Implícitas**: Não especificam o componente, mas declaram uma ação geral a ser executada. Isso permite que um componente de outro aplicativo a manipule. Por exemplo, se você quiser mostrar ao usuário uma localização em um mapa, pode usar uma intent implícita para solicitar que outro aplicativo capaz exiba a localização.

---

## Exemplo 1: Intent Explícita Simples

A forma mais comum de usar uma `Intent` é para iniciar uma nova `Activity`.

**Cenário**: Navegar da `MainActivity` para uma `DetalhesActivity`.

**1. Código na `MainActivity.kt`**

```kotlin
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.suaempresa.seuapp.databinding.ActivityMainBinding // Usando View Binding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.botaoIrParaDetalhes.setOnClickListener {
            // 1. Criar a Intent explícita, especificando o contexto (this)
            //    e a classe da Activity de destino.
            val intent = Intent(this, DetalhesActivity::class.java)

            // 2. Iniciar a nova Activity.
            startActivity(intent)
        }
    }
}
```

**2. `DetalhesActivity.kt` (Apenas a estrutura básica)**

```kotlin
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class DetalhesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalhes)
    }
}
```

---

## Exemplo 2: Passando Dados com uma Intent Explícita

Frequentemente, você precisa passar dados para a próxima tela.

**Cenário**: Enviar um nome de usuário da `MainActivity` para a `DetalhesActivity`.

**1. Código na `MainActivity.kt` (modificado)**

```kotlin
// ...
binding.botaoIrParaDetalhes.setOnClickListener {
    val nomeUsuario = "Maria"
    val intent = Intent(this, DetalhesActivity::class.java)

    // Adiciona dados extras à Intent usando um par chave-valor.
    // É uma boa prática definir as chaves como constantes.
    intent.putExtra("EXTRA_NOME_USUARIO", nomeUsuario)
    intent.putExtra("EXTRA_ID_USUARIO", 123)

    startActivity(intent)
}
// ...
```

**2. Código na `DetalhesActivity.kt` (modificado para receber os dados)**

```kotlin
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class DetalhesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalhes)

        // Recupera os dados da Intent que iniciou esta Activity.
        val nome = intent.getStringExtra("EXTRA_NOME_USUARIO")
        val id = intent.getIntExtra("EXTRA_ID_USUARIO", -1) // O segundo parâmetro é um valor padrão.

        val textViewBoasVindas: TextView = findViewById(R.id.textViewBoasVindas)
        textViewBoasVindas.text = "Olá, $nome! (ID: $id)"
    }
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

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // 1. Registre o "contrato" para iniciar uma activity e receber seu resultado.
    //    Isso deve ser feito no nível da classe, antes de onCreate().
    private val seletorDeItemLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // 4. O resultado chega aqui quando a SelecaoActivity é fechada.
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            val itemSelecionado = data?.getStringExtra("EXTRA_ITEM_SELECIONADO")
            binding.textViewResultado.text = "Item escolhido: $itemSelecionado"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.botaoEscolherItem.setOnClickListener {
            // 2. Crie a intent para a activity de seleção.
            val intent = Intent(this, SelecaoActivity::class.java)
            // 3. Inicie a activity usando o launcher que você registrou.
            seletorDeItemLauncher.launch(intent)
        }
    }
}
```

**2. Código na `SelecaoActivity.kt`**

```kotlin
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.suaempresa.seuapp.databinding.ActivitySelecaoBinding

class SelecaoActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySelecaoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelecaoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.botaoConfirmarSelecao.setOnClickListener {
            val item = "Item 123" // Item que o usuário selecionou

            // 1. Crie uma Intent vazia para carregar o resultado.
            val resultIntent = Intent()
            // 2. Coloque os dados de retorno na Intent.
            resultIntent.putExtra("EXTRA_ITEM_SELECIONADO", item)

            // 3. Defina o resultado como OK e anexe a Intent com os dados.
            setResult(Activity.RESULT_OK, resultIntent)

            // 4. Feche esta activity para retornar à anterior.
            finish()
        }
    }
}
```