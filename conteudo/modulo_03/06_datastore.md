# Jetpack DataStore: Preferências Modernas no Android

Objetivo: substituir o SharedPreferences pelo Jetpack DataStore — armazenamento assíncrono, seguro para tipos e baseado em Kotlin Flow para preferências do usuário.

**Pré-requisito:** Módulo 3.01 (Coroutines) e Módulo 3.05 (Flow Avançado).

---

## 1. Por que DataStore?

O `SharedPreferences` foi a solução padrão por anos, mas apresenta problemas sérios em apps modernos:

| Problema do SharedPreferences       | Como o DataStore resolve                        |
|--------------------------------------|-------------------------------------------------|
| Leitura síncrona bloqueia a UI thread | Totalmente assíncrono com Coroutines e Flow     |
| `apply()` pode perder dados em crash | Escrita transacional e atômica                  |
| Não sinaliza erros de forma clara    | Propaga exceções via Flow                       |
| Sem segurança de tipos               | Preferences DataStore usa chaves tipadas        |
| Não é seguro para múltiplas threads  | Garante consistência com Coroutines             |

O DataStore possui duas variantes:

- **Preferences DataStore** — pares chave-valor tipados (foco desta aula).
- **Proto DataStore** — objetos tipados com Protocol Buffers (mais avançado).

---

## 2. Preferences DataStore

### Dependências (build.gradle app)

```kotlin
dependencies {
    // DataStore para preferências chave-valor
    implementation("androidx.datastore:datastore-preferences:1.1.4")

    // Coroutines (necessário para operações assíncronas)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Lifecycle (para coletar Flow no ViewModel)
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
}
```

### Criando a instância (singleton)

```kotlin
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

// Extensão no Context — cria um único DataStore chamado "configuracoes"
// IMPORTANTE: declarar no nível do arquivo (top-level) garante singleton
val Context.dataStore by preferencesDataStore(name = "configuracoes")
```

### Definindo chaves tipadas

```kotlin
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey

object PreferencesKeys {
    // Cada chave define o tipo do valor armazenado
    val TEMA_ESCURO = booleanPreferencesKey("tema_escuro")
    val IDIOMA = stringPreferencesKey("idioma")
    val TAMANHO_FONTE = intPreferencesKey("tamanho_fonte")
    val NOTIFICACOES = booleanPreferencesKey("notificacoes_ativas")
}
```

### Leitura com Flow

```kotlin
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Lê o valor de tema escuro como Flow reativo
val temaEscuroFlow: Flow<Boolean> = context.dataStore.data
    .map { preferencias: Preferences ->
        // Retorna o valor salvo ou false como padrão
        preferencias[PreferencesKeys.TEMA_ESCURO] ?: false
    }
```

### Escrita com edit

```kotlin
import androidx.datastore.preferences.core.edit

// Função suspensa — deve ser chamada dentro de uma coroutine
suspend fun salvarTemaEscuro(context: Context, ativado: Boolean) {
    context.dataStore.edit { preferencias ->
        // Atualiza o valor de forma transacional e segura
        preferencias[PreferencesKeys.TEMA_ESCURO] = ativado
    }
}

suspend fun salvarIdioma(context: Context, idioma: String) {
    context.dataStore.edit { preferencias ->
        // Salva o idioma escolhido pelo usuário
        preferencias[PreferencesKeys.IDIOMA] = idioma
    }
}
```

---

## 3. Integração com ViewModel

O ViewModel expõe as preferências como `StateFlow` para a UI coletar de forma segura com o ciclo de vida. Vamos começar com um único valor e depois evoluir para várias preferências combinadas.

#### Passo 1 — expondo um único valor

```kotlin
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ConfiguracoesViewModel(
    private val dataStore: androidx.datastore.core.DataStore<Preferences>
) : ViewModel() {

    val temaEscuro: StateFlow<Boolean> = dataStore.data
        .map { prefs -> prefs[PreferencesKeys.TEMA_ESCURO] ?: false }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false
        )

    fun alternarTema(ativado: Boolean) {
        viewModelScope.launch {
            dataStore.edit { prefs -> prefs[PreferencesKeys.TEMA_ESCURO] = ativado }
        }
    }
}
```

**A limitação:** funciona bem para um único campo, mas uma tela de configurações real costuma ter vários (idioma, notificações, tamanho de fonte...). Expor um `StateFlow` separado para cada campo obrigaria a UI a coletar vários fluxos ao mesmo tempo — repetitivo e difícil de manter.

#### Passo 2 — agrupando várias preferências em um único estado

Em vez de um `StateFlow<Boolean>` por campo, criamos uma `data class` que representa o estado completo da tela, e um único `map` que lê todos os campos de uma vez:

```kotlin
// Estado da tela de configurações
data class ConfiguracoesUiState(
    val temaEscuro: Boolean = false,
    val idioma: String = "pt-BR",
    val notificacoesAtivas: Boolean = true,
    val tamanhoFonte: Int = 16
)

class ConfiguracoesViewModel(
    private val dataStore: androidx.datastore.core.DataStore<Preferences>
) : ViewModel() {

    // Combina todas as preferências em um único estado da tela
    val uiState: StateFlow<ConfiguracoesUiState> = dataStore.data
        .map { prefs ->
            ConfiguracoesUiState(
                temaEscuro = prefs[PreferencesKeys.TEMA_ESCURO] ?: false,
                idioma = prefs[PreferencesKeys.IDIOMA] ?: "pt-BR",
                notificacoesAtivas = prefs[PreferencesKeys.NOTIFICACOES] ?: true,
                tamanhoFonte = prefs[PreferencesKeys.TAMANHO_FONTE] ?: 16
            )
        }
        .stateIn(
            scope = viewModelScope,
            // Mantém ativo por 5s após o último coletor (sobrevive a rotação)
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ConfiguracoesUiState()
        )

    // Alterna o tema escuro
    fun alternarTema(ativado: Boolean) {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[PreferencesKeys.TEMA_ESCURO] = ativado
            }
        }
    }

    // Atualiza o idioma selecionado
    fun atualizarIdioma(idioma: String) {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[PreferencesKeys.IDIOMA] = idioma
            }
        }
    }

    // Ativa ou desativa notificações
    fun alternarNotificacoes(ativas: Boolean) {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[PreferencesKeys.NOTIFICACOES] = ativas
            }
        }
    }
}
```

---

## 4. Exemplo Prático: Tela de Configurações

```kotlin
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ConfiguracoesScreen(viewModel: ConfiguracoesViewModel) {
    // Coleta o estado respeitando o ciclo de vida
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(Modifier.padding(16.dp)) {
        Text("Configurações", style = MaterialTheme.typography.headlineMedium)

        Spacer(Modifier.height(24.dp))

        // Toggle de tema escuro
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Tema escuro")
            Switch(
                checked = state.temaEscuro,
                // Chama o ViewModel ao alternar — DataStore salva automaticamente
                onCheckedChange = { viewModel.alternarTema(it) }
            )
        }

        Spacer(Modifier.height(16.dp))

        // Toggle de notificações
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Notificações")
            Switch(
                checked = state.notificacoesAtivas,
                onCheckedChange = { viewModel.alternarNotificacoes(it) }
            )
        }

        Spacer(Modifier.height(16.dp))

        // Seleção de idioma com menu suspenso
        Text("Idioma: ${state.idioma}")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Botões para cada idioma disponível
            listOf("pt-BR", "en-US", "es-ES").forEach { opcao ->
                FilterChip(
                    selected = state.idioma == opcao,
                    onClick = { viewModel.atualizarIdioma(opcao) },
                    label = { Text(opcao) }
                )
            }
        }
    }
}
```

---

## 5. Migrando de SharedPreferences

O DataStore oferece suporte nativo para migração gradual. Os dados são copiados automaticamente na primeira leitura e o SharedPreferences original é removido.

```kotlin
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.preferencesDataStore

// Declara o DataStore com migração automática do SharedPreferences antigo
val Context.dataStore by preferencesDataStore(
    name = "configuracoes",
    produceMigrations = { contexto ->
        listOf(
            // Migra todos os pares do SharedPreferences "prefs_antigas"
            SharedPreferencesMigration(contexto, "prefs_antigas")
        )
    }
)
```

> **Dica:** a migração acontece uma única vez. Após a cópia, o arquivo XML do SharedPreferences é excluído. Não é necessário manter código legado de leitura.

---

## 6. Boas Práticas

### Singleton obrigatório

Nunca crie mais de uma instância de DataStore para o mesmo arquivo — isso causa `IllegalStateException`. A declaração `by preferencesDataStore()` no nível do arquivo já garante isso.

### Injeção de dependência

```kotlin
// Com Hilt: fornece o DataStore como dependência injetável
@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun provideDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> {
        // Reutiliza a propriedade de extensão singleton
        return context.dataStore
    }
}
```

### Tratamento de erros

```kotlin
val temaEscuroFlow: Flow<Boolean> = context.dataStore.data
    .catch { excecao ->
        // IOException indica arquivo corrompido — emite valores padrão
        if (excecao is IOException) {
            emit(emptyPreferences())
        } else {
            // Outros erros devem ser propagados para não mascarar bugs
            throw excecao
        }
    }
    .map { prefs ->
        // Retorna o valor salvo ou o padrão seguro
        prefs[PreferencesKeys.TEMA_ESCURO] ?: false
    }
```

### Regras gerais

1. Declare `preferencesDataStore` como propriedade de extensão no nível do arquivo.
2. Use `catch` antes de `map` para tratar `IOException` de leitura.
3. Injete `DataStore<Preferences>` no ViewModel em vez de `Context`.
4. Prefira `collectAsStateWithLifecycle` em Compose para coleta segura.
5. Não use DataStore para grandes volumes de dados — prefira Room.

---

## Erros Comuns / Pegadinhas

1. **Criar mais de uma instância de DataStore para o mesmo nome de arquivo.** Se você declarar `preferencesDataStore(name = "configuracoes")` em mais de um lugar (por exemplo, dentro de uma função em vez de como propriedade de extensão no nível do arquivo), o Android lança `IllegalStateException: There are multiple DataStores active for the same file`. A solução é sempre ter uma única declaração `by preferencesDataStore(...)` por nome de arquivo, no nível do arquivo (top-level), como mostrado na seção 2.

2. **Chamar `dataStore.edit { }` fora de uma coroutine.** `edit` é uma função `suspend` — ela só pode ser chamada de dentro de `viewModelScope.launch { }` ou outra coroutine (veja Módulo 3.01). Tentar chamá-la direto de um `onClick`, sem uma coroutine, não compila.

3. **Não tratar `IOException` na leitura do `dataStore.data`.** Se o arquivo de preferências for corrompido (raro, mas possível), a leitura lança uma exceção que, sem tratamento, derruba o app. Sempre use `.catch { }` antes do `.map { }`, como mostrado na seção "Tratamento de erros".

4. **Usar DataStore para guardar listas grandes ou dados estruturados complexos.** DataStore (Preferences) foi feito para configurações simples — um tema, um idioma, um número de tentativas. Para volumes maiores de dados (uma lista de produtos, histórico de pedidos), use o Room (Módulo 3.03), que foi desenhado para consultas e grandes volumes.

---

## 7. Resumo

| Conceito                    | Para que serve                                         |
|-----------------------------|--------------------------------------------------------|
| `preferencesDataStore`      | Cria instância singleton do DataStore                  |
| `booleanPreferencesKey`     | Define chave tipada para Boolean                       |
| `dataStore.data`            | Flow reativo com todas as preferências                 |
| `dataStore.edit { }`        | Escrita transacional e assíncrona                      |
| `stateIn`                   | Converte Flow frio em StateFlow no ViewModel           |
| `SharedPreferencesMigration`| Migra dados do SharedPreferences automaticamente       |
| `catch` + `IOException`     | Trata erros de leitura do arquivo de preferências      |

---

## Próximos Passos

- **Módulo 4.01 (Testes):** testar ViewModels que usam DataStore com fakes e `TestDispatcher`.
- Explorar **Proto DataStore** para objetos complexos com segurança de tipos via Protocol Buffers.
- Integrar DataStore com **Hilt** para injeção em projetos maiores.
