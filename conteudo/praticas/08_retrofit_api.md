# Prática 08 — Retrofit: buscando dados da internet

**Pré-requisito:** [Módulo 3 — Aula 2](../modulo_03/02_dados_da_internet.md)

Na aula você baixou uma lista de posts. Aqui você vai fazer um **app de piadas**: aperta o botão, vem uma piada nova da internet.

É o mesmo padrão da aula, com uma diferença útil: os nomes dos campos do JSON estão em inglês e você vai aprender a renomeá-los para português.

---

## Configuração

No `build.gradle.kts` do módulo `app`:

```kotlin
dependencies {
    // ...as dependências que já existem...
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
}
```

No `AndroidManifest.xml`, antes de `<application>`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

**Sync Now.**

---

## Parte 1 — App de piadas

A API que vamos usar é gratuita e não pede cadastro: [official-joke-api](https://official-joke-api.appspot.com/random_joke).

Abra esse link no navegador. Você vai ver algo assim:

```json
{
  "id": 42,
  "type": "general",
  "setup": "Por que o livro de matemática estava triste?",
  "punchline": "Porque tinha muitos problemas."
}
```

### 1. O molde dos dados — `Piada.kt`

```kotlin
import com.google.gson.annotations.SerializedName

data class Piada(
    @SerializedName("setup") val pergunta: String,
    @SerializedName("punchline") val resposta: String
)
```

**O que é `@SerializedName`?** É um tradutor. O JSON traz `setup`, mas no seu código fica mais claro chamar de `pergunta`. A anotação liga os dois.

Só precisa dela quando o nome muda. Se você chamasse a propriedade de `setup`, ela não seria necessária — foi o que fizemos na aula.

E repare: o JSON tem `id` e `type`, mas nós não declaramos. **Campo que você não declara é ignorado** — declare só o que a tela usa.

### 2. Endereço e ações — `Rede.kt`

```kotlin
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

interface Api {
    @GET("random_joke")
    suspend fun piadaAleatoria(): Piada
}

object Rede {
    val api: Api = Retrofit.Builder()
        .baseUrl("https://official-joke-api.appspot.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(Api::class.java)
}
```

`baseUrl` + `@GET` formam o endereço final: `https://official-joke-api.appspot.com/random_joke`.

> A `baseUrl` **precisa** terminar com `/`. Se esquecer, o app fecha na hora com uma mensagem sobre isso.

### 3. O ViewModel — `PiadaViewModel.kt`

```kotlin
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class PiadaViewModel : ViewModel() {

    var piada by mutableStateOf<Piada?>(null)
        private set
    var carregando by mutableStateOf(true)
        private set
    var deuErro by mutableStateOf(false)
        private set

    init {
        buscar()
    }

    fun buscar() = viewModelScope.launch {
        carregando = true
        deuErro = false
        try {
            piada = Rede.api.piadaAleatoria()
        } catch (e: Exception) {
            deuErro = true
        }
        carregando = false
    }
}
```

Três variáveis, três situações da tela. `Piada?` com interrogação porque, antes da primeira resposta chegar, ainda não existe piada nenhuma.

### 4. A tela — `TelaPiada.kt`

```kotlin
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun TelaPiada(vm: PiadaViewModel = viewModel()) {

    var mostrarResposta by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("😂 Piada do dia", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(32.dp))

        when {
            vm.carregando -> CircularProgressIndicator()

            vm.deuErro -> {
                Text("Não consegui carregar. Verifique sua internet.", textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Button(onClick = { vm.buscar() }) { Text("Tentar de novo") }
            }

            vm.piada != null -> {
                Text(vm.piada!!.pergunta, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))

                if (mostrarResposta) {
                    Text(
                        vm.piada!!.resposta,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                } else {
                    OutlinedButton(onClick = { mostrarResposta = true }) {
                        Text("Revelar resposta 🎭")
                    }
                }

                Spacer(Modifier.height(32.dp))
                Button(onClick = {
                    mostrarResposta = false     // esconde a resposta da piada anterior
                    vm.buscar()
                }) {
                    Text("Próxima piada ➡")
                }
            }
        }
    }
}
```

Na `MainActivity`, chame `TelaPiada()`.

---

## Teste se funcionou

- [ ] Abri o app e veio uma piada
- [ ] Toquei em "Revelar resposta" e a resposta apareceu
- [ ] Toquei em "Próxima piada" e veio outra, com a resposta escondida de novo
- [ ] **Coloquei em modo avião**, toquei em "Próxima piada" e apareceu a mensagem de erro — o app **não** fechou
- [ ] Tirei do modo avião, toquei em "Tentar de novo" e voltou a funcionar

O quarto item é o mais importante da prática.

---

## Exercícios

### 1. Contador de piadas

Mostre quantas piadas você já viu nesta sessão.

> *Dica:* no ViewModel, `var quantidade by mutableStateOf(0)` e `quantidade++` sempre que uma piada chegar com sucesso.

### 2. Botão de compartilhar

Adicione um botão que abre o menu de compartilhamento do Android com a piada inteira.

> *Dica:* é a `Intent` do Módulo 1:
> ```kotlin
> val contexto = LocalContext.current
> // dentro do onClick:
> val envio = Intent(Intent.ACTION_SEND).apply {
>     type = "text/plain"
>     putExtra(Intent.EXTRA_TEXT, "${vm.piada!!.pergunta}\n\n${vm.piada!!.resposta}")
> }
> contexto.startActivity(Intent.createChooser(envio, "Compartilhar piada"))
> ```

### 3. Dez piadas de uma vez

Buscar na internet a cada toque é lento. Baixe 10 piadas de uma vez e navegue entre elas sem usar a rede.

> *Dica:* adicione na `Api`:
> ```kotlin
> @GET("jokes/ten")
> suspend fun dezPiadas(): List<Piada>
> ```
> No ViewModel, guarde a lista e um índice (`var posicao by mutableStateOf(0)`). O botão "Próxima" só faz `posicao++`. Quando `posicao` chegar ao fim da lista, aí sim busque outras dez.

### 4. Salvar as favoritas *(desafio)*

Junte esta prática com a [07](07_room_persistencia.md): um botão ⭐ que salva a piada no Room, e uma segunda tela listando as salvas.

> *Dica:* a `@Entity` precisa de `@PrimaryKey(autoGenerate = true) val id: Int = 0`, mais os campos `pergunta` e `resposta`. Esse é exatamente o desafio da opção C do [projeto final](../modulo_03/06_projeto_final.md).

---

## Parte 2 — Mensagens de erro melhores

Hoje o seu `catch` mostra a mesma mensagem para tudo. Mas "você está sem internet" e "o servidor caiu" são problemas diferentes, e o usuário pode resolver o primeiro.

Dá para separar os dois trocando um `catch` por dois:

```kotlin
import retrofit2.HttpException
import java.io.IOException

// dentro do ViewModel:
var mensagemErro by mutableStateOf("")
    private set

fun buscar() = viewModelScope.launch {
    carregando = true
    deuErro = false
    try {
        piada = Rede.api.piadaAleatoria()
    } catch (e: IOException) {
        // o celular não conseguiu falar com o servidor: sem sinal, Wi-Fi caiu, timeout
        deuErro = true
        mensagemErro = "Sem internet. Verifique sua conexão."
    } catch (e: HttpException) {
        // o servidor respondeu, mas com erro (404, 500...)
        deuErro = true
        mensagemErro = "O servidor está com problema. Tente mais tarde."
    }
    carregando = false
}
```

Na tela, mostre `vm.mensagemErro` no lugar do texto fixo.

**A ordem importa:** o Kotlin usa o **primeiro** `catch` que combina. Se você colocar `catch (e: Exception)` antes dos outros, ele pega tudo e os demais nunca rodam. O genérico vai sempre por último.

**Exercício:** teste os dois casos. Modo avião dispara o `IOException`. Para o `HttpException`, troque a rota `random_joke` por `rota_que_nao_existe` e veja a outra mensagem.

---

## Erros comuns

| Erro | Causa | Solução |
|------|-------|---------|
| `SecurityException: Permission denied` | Faltou a permissão de internet | Adicione `<uses-permission android:name="android.permission.INTERNET" />` |
| `baseUrl must end in /` | Endereço sem a barra final | `https://.../` |
| Campos vindo `null` ou vazios | Nome do JSON diferente do da `data class` | Use `@SerializedName("nomeNoJson")` |
| `NullPointerException` ao mostrar a piada | Usou `vm.piada!!` antes de a resposta chegar | Só use dentro do ramo `vm.piada != null` do `when` |
| App fecha ao ficar sem internet | Faltou o `try / catch` | Toda chamada de rede precisa de `try / catch` |
| A tela fica carregando para sempre | `carregando = false` está dentro do `try` | Deixe fora, para rodar mesmo quando dá erro |

---

## Resumo

| Anotação | O que faz |
|----------|-----------|
| `@GET("rota")` | Busca dados nessa rota |
| `@Query("nome")` | Adiciona `?nome=valor` no endereço |
| `@Path("nome")` | Substitui `{nome}` no endereço |
| `@SerializedName("campo")` | Renomeia um campo do JSON |

O ciclo é sempre o mesmo:

```
tela chama vm.buscar() → launch → try { api } catch { erro } → mutableStateOf muda → tela redesenha
```

E as três regras que valem para qualquer app com internet:

1. Permissão no manifest
2. `try / catch` em toda chamada
3. Três estados na tela: carregando, erro, conteúdo

👉 De volta ao [Módulo 3 — Projeto final](../modulo_03/06_projeto_final.md)
