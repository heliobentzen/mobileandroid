# Aula 2 — Buscando dados da internet

**Objetivo:** fazer um app que baixa uma lista da internet e mostra na tela.

Vamos usar o **Retrofit**, a biblioteca que quase todo app Android usa para conversar com a internet. Ela faz o trabalho pesado: pede os dados, recebe o texto de resposta e transforma em objetos Kotlin prontos para usar.

Vamos usar uma API pública e gratuita (`jsonplaceholder.typicode.com`), que devolve uma lista de posts de mentirinha. Não precisa de senha nem cadastro.

---

## 1. Instalar o Retrofit

No `build.gradle.kts` do módulo `app`:

```kotlin
dependencies {
    // ...as dependências que já existem...
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
}
```

E no `AndroidManifest.xml`, **antes** da tag `<application>`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

> Sem essa linha o app é bloqueado pelo Android e você recebe um erro de rede. É a permissão mais esquecida do mundo Android.

Clique em **Sync Now**.

---

## 2. Um arquivo só de código de internet

### O molde dos dados — `Post.kt`

A API devolve um texto assim para cada post:

```json
{ "id": 1, "title": "Meu primeiro post", "body": "Conteúdo do post..." }
```

Você só precisa criar uma `data class` com **os mesmos nomes** dos campos que quer usar:

```kotlin
data class Post(
    val id: Int,
    val title: String,
    val body: String
)
```

Campos que você não declarar são simplesmente ignorados. Não precisa copiar tudo.

### O endereço e as ações — `Rede.kt`

```kotlin
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

// A lista de coisas que dá para pedir para o servidor.
interface Api {
    @GET("posts")
    suspend fun listarPosts(): List<Post>
}

// Monta o Retrofit uma única vez e deixa pronto para usar.
object Rede {
    val api: Api = Retrofit.Builder()
        .baseUrl("https://jsonplaceholder.typicode.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(Api::class.java)
}
```

Três coisas acontecem aqui:

- `baseUrl` é o começo do endereço. **Precisa terminar com `/`.**
- `@GET("posts")` completa o endereço: `https://jsonplaceholder.typicode.com/posts`.
- `GsonConverterFactory` transforma o texto JSON em objetos `Post` automaticamente.

E, de novo, `suspend`: baixar da internet demora, então essa função só roda dentro de um `launch`.

---

## 3. O ViewModel

Baixar da internet pode dar errado (sem sinal, servidor fora do ar). Então a tela tem **três situações**: carregando, deu certo, deu erro.

```kotlin
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class PostViewModel : ViewModel() {

    var posts by mutableStateOf(listOf<Post>())
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
            posts = Rede.api.listarPosts()
        } catch (e: Exception) {
            deuErro = true            // sem internet, servidor fora do ar, etc.
        }
        carregando = false
    }
}
```

> **Por que o `try / catch`?** Sem ele, qualquer falha de rede fecha o app na cara do usuário. Com ele, você mostra uma mensagem e um botão "Tentar de novo". Essa é a diferença entre um app de exercício e um app publicável.

---

## 4. A tela

```kotlin
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun TelaPosts(vm: PostViewModel = viewModel()) {

    when {
        vm.carregando -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        vm.deuErro -> {
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Não consegui carregar. Verifique sua internet.")
                Spacer(Modifier.height(8.dp))
                Button(onClick = { vm.buscar() }) { Text("Tentar de novo") }
            }
        }

        else -> {
            LazyColumn(Modifier.padding(16.dp)) {
                items(vm.posts) { post ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(post.title, style = MaterialTheme.typography.titleMedium)
                            Text(post.body, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
```

O `when` escolhe **uma** das três telas. Nunca aparecem duas ao mesmo tempo.

Na `MainActivity`, troque a tela por `TelaPosts()` e rode.

---

## 5. Teste se funcionou

- [ ] Abri o app e vi a rodinha de carregando por um instante.
- [ ] A lista de posts apareceu.
- [ ] **Coloquei o celular em modo avião**, abri o app e apareceu a mensagem de erro com o botão.
- [ ] Tirei do modo avião, apertei "Tentar de novo" e a lista carregou.

O terceiro item é o mais importante. É ele que separa um app que funciona só no Wi-Fi da escola de um app que funciona no mundo real.

---

## Erros comuns

| Erro | O que está acontecendo | Como resolver |
|------|------------------------|---------------|
| `SecurityException: Permission denied` | Faltou a permissão de internet | Adicione `<uses-permission android:name="android.permission.INTERNET" />` no manifest |
| `IllegalArgumentException: baseUrl must end in /` | O endereço não termina com barra | Escreva `.../` no fim da `baseUrl` |
| A lista vem vazia, mas sem erro | Os nomes dos campos da `data class` não batem com o JSON | Confira letra por letra: `title` ≠ `titulo` |
| App fecha ao abrir | Chamada de rede fora do `launch`, ou faltou o `try/catch` | Verifique se está tudo dentro de `viewModelScope.launch` |

---

## Resumo

- **Retrofit** busca dados na internet; **Gson** transforma o JSON em objetos Kotlin.
- Você precisa de: a permissão no manifest, uma `data class`, uma `interface` com `@GET` e o `object Rede`.
- Rede sempre pode falhar → sempre use `try / catch` e mostre uma mensagem ao usuário.
- Estados da tela: **carregando**, **erro**, **conteúdo**.

👉 Próxima aula: [Deixando o app com cara de app](03_app_com_cara_de_app.md)
