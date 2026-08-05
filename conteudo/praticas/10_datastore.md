# Prática: Preferências com Jetpack DataStore

Pré-requisito: Módulo 3, Aula 06 — DataStore

Este guia apresenta exercícios práticos para salvar e ler preferências do usuário usando o **Jetpack DataStore**, a solução moderna do Android para armazenamento de pares chave-valor.

---

## O que é o DataStore?

O DataStore substitui o `SharedPreferences` com diversas vantagens:

- Leitura e escrita **assíncronas** usando Kotlin Coroutines e Flow.
- Operações **seguras para threads** (thread-safe) por padrão.
- Detecção de **erros em tempo de compilação** (com Proto DataStore).

Nesta prática usaremos o **Preferences DataStore**, que armazena pares chave-valor sem necessidade de schema.

---

## Configuração

Adicione ao `app/build.gradle.kts`:

```kotlin
dependencies {
    // DataStore para preferências
    implementation("androidx.datastore:datastore-preferences:1.1.4")

    // ViewModel e integração com Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
}
```

---

## Exercício 1: Preferences DataStore Básico

### Objetivo
Criar um DataStore simples para salvar e ler o nome do usuário e a preferência de tema escuro.

Toda vez que um app "lembra" de uma escolha do usuário — um tema, um idioma, se o tutorial já foi visto — algum tipo de armazenamento local de preferências está por trás disso. O DataStore é a ferramenta oficial recomendada pelo Google para esse tipo de dado simples, pequeno e que precisa sobreviver entre uma sessão de uso e outra. Sem ele, o app "esqueceria" tudo toda vez que fosse fechado, obrigando o usuário a reconfigurar tudo de novo.

### Passo a Passo

Vamos guardar uma preferência (o nome do usuário) de ponta a ponta primeiro, e só depois somar uma segunda (o tema escuro).

**1. Criando o DataStore — só a preferência de nome** (`PreferenciasManager.kt`):

```kotlin
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Extensão que cria uma única instância do DataStore no contexto da aplicação
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "configuracoes")

class PreferenciasManager(private val context: Context) {

    // Chave tipada para a preferência armazenada
    companion object {
        val NOME_USUARIO = stringPreferencesKey("nome_usuario")   // Chave do tipo String
    }

    // Flow que emite o nome do usuário salvo, ou string vazia como padrão
    val nomeUsuario: Flow<String> = context.dataStore.data.map { preferencias ->
        preferencias[NOME_USUARIO] ?: ""
    }

    // Função suspensa que salva o nome do usuário no DataStore
    suspend fun salvarNomeUsuario(nome: String) {
        context.dataStore.edit { preferencias ->
            preferencias[NOME_USUARIO] = nome
        }
    }
}
```

**2. Tela simples para testar** (`PerfilScreen.kt`):

```kotlin
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun PerfilScreen() {
    // Obtém o contexto e cria o gerenciador de preferências
    val contexto = LocalContext.current
    val preferencias = remember { PreferenciasManager(contexto) }
    val escopo = rememberCoroutineScope()

    // Coleta o valor salvo no DataStore como estado do Compose
    val nomeSalvo by preferencias.nomeUsuario.collectAsState(initial = "")

    // Estado local do campo de texto
    var nomeDigitado by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Perfil do Usuário", style = MaterialTheme.typography.headlineMedium)

        // Campo para digitar o nome do usuário
        OutlinedTextField(
            value = nomeDigitado,
            onValueChange = { nomeDigitado = it },
            label = { Text("Nome") },
            modifier = Modifier.fillMaxWidth()
        )

        // Botão que salva o nome no DataStore
        Button(onClick = {
            escopo.launch { preferencias.salvarNomeUsuario(nomeDigitado) }
        }) {
            Text("Salvar Nome")
        }

        // Exibe o nome que está salvo no DataStore
        if (nomeSalvo.isNotBlank()) {
            Text("Nome salvo: $nomeSalvo", style = MaterialTheme.typography.bodyLarge)
        }
    }
}
```

Neste ponto já dá para testar: digite um nome, toque em salvar, feche e reabra o app — o nome continua lá.

**3. Adicione uma segunda preferência: tema escuro.** No `PreferenciasManager`, acrescente a chave, o `Flow` e a função de salvar:

```kotlin
import androidx.datastore.preferences.core.booleanPreferencesKey

// Dentro do companion object:
val TEMA_ESCURO = booleanPreferencesKey("tema_escuro")    // Chave do tipo Boolean

// Flow que emite a preferência de tema escuro, falso como padrão
val temaEscuro: Flow<Boolean> = context.dataStore.data.map { preferencias ->
    preferencias[TEMA_ESCURO] ?: false
}

// Função suspensa que salva a preferência de tema escuro
suspend fun salvarTemaEscuro(ativado: Boolean) {
    context.dataStore.edit { preferencias ->
        preferencias[TEMA_ESCURO] = ativado
    }
}
```

**4. Adicione o switch de tema na tela.** Colete o novo `Flow` e adicione um `Switch` abaixo do nome salvo:

```kotlin
import androidx.compose.ui.Alignment

// Dentro de PerfilScreen(), junto com nomeSalvo:
val temaEscuro by preferencias.temaEscuro.collectAsState(initial = false)

// Adicione ao final da Column, depois do Text de "Nome salvo":
HorizontalDivider()

Row(verticalAlignment = Alignment.CenterVertically) {
    Text("Tema Escuro", modifier = Modifier.weight(1f))
    Switch(
        checked = temaEscuro,
        onCheckedChange = { ativado ->
            // Salva a preferência ao alternar o switch
            escopo.launch { preferencias.salvarTemaEscuro(ativado) }
        }
    )
}
```

### Comportamento Esperado
- O nome digitado é persistido e exibido mesmo após reiniciar o app.
- O switch de tema escuro mantém seu estado entre sessões.

> **💡 Por trás dos panos**
> `val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "configuracoes")` cria uma **extension property** no `Context` — ou seja, adiciona uma propriedade nova a uma classe que você não escreveu. O `by preferencesDataStore(...)` garante que só existe uma única instância do DataStore para todo o app, mesmo que essa propriedade seja acessada de vários lugares diferentes. Isso é importante: ter duas instâncias do mesmo DataStore ao mesmo tempo pode causar inconsistência entre leituras e escritas.

### Exercícios

1. Adicione uma terceira preferência: `idadeUsuario` (do tipo `Int`), usando `intPreferencesKey`. Salve e exiba na mesma tela.
   - *Dica se travar*: siga exatamente o padrão de `NOME_USUARIO` — troque só o tipo da chave e o tipo do valor.
2. Adicione um botão "Limpar preferências" que apaga todos os dados salvos no DataStore de uma vez.
   - *Dica se travar*: pesquise sobre `context.dataStore.edit { it.clear() }`.

---

## Exercício 2: Tela de Configurações

### Objetivo
Criar uma tela de configurações completa com múltiplas opções salvas no DataStore.

Uma tela de configurações raramente tem só uma opção — geralmente é um conjunto de preferências relacionadas (aparência, notificações, idioma, privacidade). Esta prática mostra como agrupar várias chaves do DataStore em um único objeto (`Configuracoes`) e expô-las como um só `Flow`, em vez de ficar observando cada preferência separadamente — um padrão que deixa a tela mais simples de programar e mais fácil de expandir com novas opções no futuro.

### Passo a Passo

**1. Ampliando o gerenciador** (`ConfiguracoesDataStore.kt`):

```kotlin
import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Modelo que agrupa todas as configurações do app
data class Configuracoes(
    val temaEscuro: Boolean = false,        // Tema escuro ativado ou não
    val notificacoesAtivas: Boolean = true,  // Notificações habilitadas por padrão
    val idioma: String = "pt-BR"             // Idioma padrão do app
)

class ConfiguracoesDataStore(private val context: Context) {

    // Chaves para cada configuração no DataStore
    companion object {
        val TEMA_ESCURO = booleanPreferencesKey("tema_escuro")
        val NOTIFICACOES = booleanPreferencesKey("notificacoes_ativas")
        val IDIOMA = stringPreferencesKey("idioma")
    }

    // Flow único que emite o objeto Configuracoes completo
    val configuracoes: Flow<Configuracoes> = context.dataStore.data.map { prefs ->
        Configuracoes(
            temaEscuro = prefs[TEMA_ESCURO] ?: false,
            notificacoesAtivas = prefs[NOTIFICACOES] ?: true,
            idioma = prefs[IDIOMA] ?: "pt-BR"
        )
    }

    // Salva a preferência de tema escuro
    suspend fun atualizarTemaEscuro(ativado: Boolean) {
        context.dataStore.edit { it[TEMA_ESCURO] = ativado }
    }

    // Salva a preferência de notificações
    suspend fun atualizarNotificacoes(ativas: Boolean) {
        context.dataStore.edit { it[NOTIFICACOES] = ativas }
    }

    // Salva o idioma escolhido pelo usuário
    suspend fun atualizarIdioma(idioma: String) {
        context.dataStore.edit { it[IDIOMA] = idioma }
    }
}
```

**2. Tela de configurações com Compose** (`ConfiguracoesScreen.kt`):

```kotlin
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfiguracoesScreen() {
    val contexto = LocalContext.current
    val dataStore = remember { ConfiguracoesDataStore(contexto) }
    val escopo = rememberCoroutineScope()

    // Coleta todas as configurações como um único estado
    val config by dataStore.configuracoes.collectAsState(initial = Configuracoes())

    Scaffold(
        topBar = { TopAppBar(title = { Text("Configurações") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Seção: Aparência
            Text("Aparência", style = MaterialTheme.typography.titleSmall)

            // Item de configuração para tema escuro
            ItemConfiguracao(
                icone = { Icon(Icons.Default.DarkMode, contentDescription = null) },
                titulo = "Tema Escuro",
                descricao = "Ativar modo escuro no aplicativo",
                ativado = config.temaEscuro,
                aoAlterar = { escopo.launch { dataStore.atualizarTemaEscuro(it) } }
            )

            Divider()

            // Seção: Notificações
            Text("Notificações", style = MaterialTheme.typography.titleSmall)

            // Item de configuração para notificações
            ItemConfiguracao(
                icone = { Icon(Icons.Default.Notifications, contentDescription = null) },
                titulo = "Notificações",
                descricao = "Receber alertas e avisos do app",
                ativado = config.notificacoesAtivas,
                aoAlterar = { escopo.launch { dataStore.atualizarNotificacoes(it) } }
            )

            Divider()

            // Exibe o idioma atual selecionado
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Language, contentDescription = null)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Idioma", style = MaterialTheme.typography.bodyLarge)
                    Text("Atual: ${config.idioma}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

// Componente reutilizável para cada linha de configuração com Switch
@Composable
fun ItemConfiguracao(
    icone: @Composable () -> Unit,
    titulo: String,
    descricao: String,
    ativado: Boolean,
    aoAlterar: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icone()
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.bodyLarge)
            Text(descricao, style = MaterialTheme.typography.bodySmall)
        }
        // Switch que altera e salva a preferência automaticamente
        Switch(checked = ativado, onCheckedChange = aoAlterar)
    }
}
```

### Comportamento Esperado
- Cada switch salva a preferência imediatamente ao ser alternado.
- Ao reabrir o app, todas as configurações permanecem no estado salvo.
- O componente `ItemConfiguracao` é reutilizável para novas opções.

> **💡 Por trás dos panos**
> O `Flow<Configuracoes>` combina três chaves diferentes do DataStore (`TEMA_ESCURO`, `NOTIFICACOES`, `IDIOMA`) em um único objeto por meio de `.map { prefs -> Configuracoes(...) }`. Toda vez que **qualquer uma** dessas chaves muda, o DataStore emite o mapa de preferências inteiro de novo, o `.map` reconstrói o objeto `Configuracoes` completo, e a tela observa só esse objeto único — em vez de precisar coletar três `Flow`s separados e sincronizá-los manualmente.

### Exercícios

1. Adicione uma opção de configuração "Tamanho da fonte" com três valores possíveis: Pequena, Média, Grande. Use `stringPreferencesKey` para guardar a escolha.
   - Primeiro, adicione o campo `tamanhoFonte: String = "Média"` à data class `Configuracoes`.
   - Depois, crie a chave e a função `atualizarTamanhoFonte(tamanho: String)` no `ConfiguracoesDataStore`, seguindo o padrão das outras preferências.
   - Por fim, adicione um `Row` com três botões (ou um `DropdownMenu`) na tela para escolher o tamanho.
   - *Dica se travar*: copie o padrão exato de `IDIOMA`, que também é uma `String` — só muda o nome da chave e da função.
2. Adicione uma seção "Sobre" na tela, mostrando um texto fixo com a versão do app (pode ser um valor fixo no código, sem persistir no DataStore).

---

## Exercício 3: Integração com ViewModel

### Objetivo
Mover a lógica do DataStore para um `ViewModel`, expondo as preferências como `StateFlow` para uma UI totalmente reativa.

Nos exercícios anteriores, a tela acessava o DataStore diretamente — funcional para um exemplo simples, mas não é a estrutura recomendada em apps reais, porque mistura lógica de dados com a interface. Aqui você aplica o mesmo padrão MVVM que já viu no guia `04_mvvm_stateflow.md`, desta vez com o DataStore como fonte de dados no lugar de um `StateFlow` criado manualmente. É a forma correta e testável de expor preferências para a UI.

### Passo a Passo

**1. ViewModel de configurações** (`ConfiguracoesViewModel.kt`):

```kotlin
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ConfiguracoesViewModel(
    private val dataStore: ConfiguracoesDataStore
) : ViewModel() {

    // Converte o Flow do DataStore em StateFlow para a UI observar
    val configuracoes: StateFlow<Configuracoes> = dataStore.configuracoes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000), // Mantém ativo por 5s após último observador
        initialValue = Configuracoes()                    // Valor padrão enquanto carrega
    )

    // Alterna o tema escuro e salva no DataStore
    fun alternarTemaEscuro(ativado: Boolean) {
        viewModelScope.launch {
            dataStore.atualizarTemaEscuro(ativado)
        }
    }

    // Alterna notificações e salva no DataStore
    fun alternarNotificacoes(ativas: Boolean) {
        viewModelScope.launch {
            dataStore.atualizarNotificacoes(ativas)
        }
    }

    // Atualiza o idioma escolhido pelo usuário
    fun alterarIdioma(idioma: String) {
        viewModelScope.launch {
            dataStore.atualizarIdioma(idioma)
        }
    }
}

// Factory para injetar o DataStore no ViewModel
class ConfiguracoesViewModelFactory(
    private val dataStore: ConfiguracoesDataStore
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // Verifica se o ViewModel solicitado é do tipo correto
        if (modelClass.isAssignableFrom(ConfiguracoesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ConfiguracoesViewModel(dataStore) as T
        }
        throw IllegalArgumentException("ViewModel desconhecido")
    }
}
```

**2. Tela com ViewModel** (`ConfiguracoesComViewModelScreen.kt`):

```kotlin
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfiguracoesComViewModelScreen(viewModel: ConfiguracoesViewModel) {
    // collectAsStateWithLifecycle respeita o ciclo de vida da Activity
    val config by viewModel.configuracoes.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Configurações") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Switch de tema escuro controlado pelo ViewModel
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Tema Escuro", modifier = Modifier.weight(1f))
                Switch(
                    checked = config.temaEscuro,
                    onCheckedChange = { viewModel.alternarTemaEscuro(it) }
                )
            }

            // Switch de notificações controlado pelo ViewModel
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Notificações", modifier = Modifier.weight(1f))
                Switch(
                    checked = config.notificacoesAtivas,
                    onCheckedChange = { viewModel.alternarNotificacoes(it) }
                )
            }

            Divider()

            // Exibe o idioma atual salvo nas preferências
            Text(
                text = "Idioma: ${config.idioma}",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
```

**3. Conectando na MainActivity**:

```kotlin
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Cria o DataStore e o ViewModel usando a Factory
        val dataStore = ConfiguracoesDataStore(applicationContext)
        val viewModel: ConfiguracoesViewModel by viewModels {
            ConfiguracoesViewModelFactory(dataStore)
        }

        setContent {
            MaterialTheme {
                ConfiguracoesComViewModelScreen(viewModel = viewModel)
            }
        }
    }
}
```

### Comportamento Esperado
- O `ViewModel` centraliza toda a lógica de leitura e escrita do DataStore.
- A UI reage automaticamente a qualquer mudança via `StateFlow`.
- O `collectAsStateWithLifecycle` pausa a coleta quando o app vai para segundo plano.

> **💡 Por trás dos panos**
> A `ConfiguracoesViewModelFactory` existe porque `ConfiguracoesViewModel` não tem um construtor vazio — ele precisa receber um `ConfiguracoesDataStore` para funcionar, e o Android não sabe de onde tirar esse valor sozinho. A `Factory` é o "manual de instruções" que ensina o sistema a montar esse ViewModel corretamente. Esse mesmo problema (fornecer dependências para um ViewModel) é resolvido de forma muito mais automática com o Hilt, que você viu no guia `09_hilt_di.md` — vale a pena comparar as duas abordagens lado a lado.

### Exercícios

1. Reescreva o `ConfiguracoesViewModel` para usar Hilt em vez da `Factory` manual (`@HiltViewModel` + `@Inject constructor`). Compare a quantidade de código necessária nas duas abordagens.
   - *Dica se travar*: reveja o Exercício 1 do guia `09_hilt_di.md` para relembrar a sintaxe de `@HiltViewModel`.
2. Adicione uma função `resetarConfiguracoes()` ao ViewModel que restaura todas as preferências para os valores padrão.

---

## Conceitos Chave

```
Usuário altera switch → ViewModel chama DataStore → DataStore persiste → Flow emite novo valor → UI atualiza
```

| Conceito | Descrição |
|----------|-----------|
| `preferencesDataStore` | Cria instância singleton do DataStore |
| `stringPreferencesKey` | Chave tipada para valores String |
| `booleanPreferencesKey` | Chave tipada para valores Boolean |
| `dataStore.edit {}` | Bloco para escrever preferências de forma atômica |
| `dataStore.data` | Flow que emite as preferências atuais |
| `StateFlow` | Versão do Flow com valor atual, ideal para UI |
| `collectAsStateWithLifecycle` | Coleta Flow respeitando o ciclo de vida |

---

## Próximos Passos

- Explore o **Proto DataStore** para armazenar objetos tipados com Protocol Buffers.
- Combine DataStore com **Hilt** (veja `09_hilt_di.md`) para injetar o `ConfiguracoesDataStore` automaticamente.
- Use as preferências do DataStore para controlar o tema da aplicação com `darkColorScheme()` e `lightColorScheme()`.
