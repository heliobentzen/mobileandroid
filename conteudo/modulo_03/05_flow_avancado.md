# Flow Avançado: Operadores, Combinação e Boas Práticas

Objetivo: aprofundar o uso de Kotlin Flow em aplicações Android reais — operadores de transformação, combinação de múltiplos fluxos, integração com Room e tratamento robusto de erros.

**Pré-requisito:** Módulo 3.01 (Coroutines) e Módulo 2.01 (MVVM com StateFlow).

---

## 1. Recap: StateFlow vs SharedFlow

Antes de avançar, vale relembrar quando usar cada tipo de Flow quente:

```plaintext
Precisa de estado (valor atual)? ──► Sim ──► StateFlow
                                  └► Não ──► SharedFlow (eventos one-shot)
```

| Característica        | StateFlow              | SharedFlow                  |
|-----------------------|------------------------|-----------------------------|
| Valor inicial         | Obrigatório            | Não possui                  |
| Replay padrão         | 1 (último valor)       | 0 (configurável)            |
| Emissões duplicadas   | Ignora (distinctUntilChanged) | Entrega todas         |
| Uso típico            | Estado da tela          | Eventos one-shot            |

---

## 2. Operadores de Flow

Operadores transformam ou filtram dados emitidos por um Flow, formando uma cadeia de processamento.

### map, filter e distinctUntilChanged

```kotlin
val produtosBaratos: Flow<List<Produto>> = produtoDao
    .observarTodos() // Flow<List<Produto>> vindo do Room
    .map { lista ->
        // Transforma cada emissão: filtra apenas produtos abaixo de R$50
        lista.filter { it.preco < 50.0 }
    }
    .distinctUntilChanged() // Só emite se a lista resultante realmente mudou
```

### debounce e flatMapLatest

```kotlin
// Ideal para campo de busca: espera o usuário parar de digitar
val resultadosBusca: Flow<List<Produto>> = queryFlow
    .debounce(400L) // Aguarda 400ms sem novas emissões
    .filter { it.length >= 2 } // Ignora buscas com menos de 2 caracteres
    .flatMapLatest { termo ->
        // Cancela a busca anterior e inicia nova a cada termo
        repository.buscar(termo)
    }
```

### combine e zip

```kotlin
// combine: emite sempre que QUALQUER fonte muda
val tela: Flow<UiState> = combine(
    produtosFlow,   // Flow<List<Produto>>
    filtroFlow       // Flow<Filtro>
) { produtos, filtro ->
    // Combina os dois valores mais recentes
    UiState(produtos.aplicarFiltro(filtro))
}

// zip: emite apenas quando AMBAS as fontes têm valor novo (pareamento 1:1)
val pareado: Flow<Pair<Usuario, Perfil>> = usuarioFlow.zip(perfilFlow) { u, p ->
    // Cada emissão de usuário é pareada com uma emissão de perfil
    Pair(u, p)
}
```

---

## 3. stateIn e shareIn — Convertendo Flow Frio em Quente

Um Flow frio (como o retornado pelo Room) reinicia a coleta para cada coletor. Com `stateIn` e `shareIn`, convertemos para um Flow quente compartilhado.

```kotlin
class ProdutoViewModel(private val repository: ProdutoRepository) : ViewModel() {

    // Converte Flow frio do Room em StateFlow quente
    val produtos: StateFlow<List<Produto>> = repository
        .observarProdutos() // Flow<List<Produto>> frio
        .stateIn(
            scope = viewModelScope,               // Escopo de vida do Flow
            started = SharingStarted.WhileSubscribed(5_000), // Mantém ativo 5s após último coletor
            initialValue = emptyList()             // Valor antes da primeira emissão
        )

    // shareIn: útil quando não precisamos de valor inicial (ex.: eventos)
    val eventos: SharedFlow<Evento> = repository
        .observarEventos()
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(), // Para quando não há coletores
            replay = 0                              // Não repete eventos antigos
        )
}
```

> **Por que `WhileSubscribed(5_000)`?** Ao girar a tela, a Activity é destruída e recriada. O timeout de 5 segundos evita que o Flow seja cancelado e reiniciado durante essa transição rápida.

---

## 4. Combinando Múltiplos Flows

Cenário realista: tela de produtos com campo de busca e filtro de categoria.

```kotlin
class CatalogoViewModel(private val repository: ProdutoRepository) : ViewModel() {

    // Texto digitado no campo de busca
    private val _query = MutableStateFlow("")

    // Categoria selecionada pelo usuário
    private val _categoria = MutableStateFlow("Todas")

    // Estado combinado da tela: busca + filtro + dados do banco
    val uiState: StateFlow<CatalogoUiState> = combine(
        _query.debounce(400L),                  // Espera o usuário parar de digitar
        _categoria,                              // Emite ao trocar categoria
        repository.observarProdutos()            // Flow do Room (atualiza em tempo real)
    ) { query, categoria, produtos ->
        // Aplica filtros sobre a lista vinda do banco
        val filtrados = produtos
            .filter { categoria == "Todas" || it.categoria == categoria }
            .filter { query.isBlank() || it.nome.contains(query, ignoreCase = true) }

        CatalogoUiState(produtos = filtrados, query = query, categoria = categoria)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CatalogoUiState()
    )

    // Funções chamadas pela UI
    fun atualizarBusca(texto: String) { _query.value = texto }
    fun selecionarCategoria(cat: String) { _categoria.value = cat }
}
```

---

## 5. Flow com Room

O Room retorna `Flow` nativamente nas queries com `@Query`. Cada vez que os dados da tabela mudam, o Flow emite a lista atualizada — sem polling.

```kotlin
@Dao
interface ProdutoDao {
    // Room emite nova lista sempre que a tabela "produtos" é alterada
    @Query("SELECT * FROM produtos ORDER BY nome ASC")
    fun observarTodos(): Flow<List<ProdutoEntity>>

    // Consulta reativa com parâmetro
    @Query("SELECT * FROM produtos WHERE categoria = :cat")
    fun observarPorCategoria(cat: String): Flow<List<ProdutoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(produto: ProdutoEntity)
}
```

No Repository, basta expor o Flow do DAO com `map` para converter Entity → Model:

```kotlin
class ProdutoRepository(private val dao: ProdutoDao) {
    // Converte cada entidade do banco para o modelo de domínio
    fun observarProdutos(): Flow<List<Produto>> =
        dao.observarTodos().map { lista -> lista.map { it.toModel() } }
}
```

O ciclo: UI coleta o Flow → usuário dispara `sincronizar()` → Room insere dados → Flow emite nova lista → UI atualiza.

---

## 6. Tratamento de Erros em Flow

### catch

```kotlin
val produtos: Flow<List<Produto>> = repository
    .observarProdutos()
    .catch { erro ->
        // Captura exceções emitidas ANTES deste ponto na cadeia
        emit(emptyList()) // Emite valor padrão para a UI não quebrar
        // Opcional: registrar o erro em um logger
    }
```

> **Atenção:** `catch` só intercepta erros de operadores *upstream* (acima dele na cadeia). Erros dentro do `collect` não são capturados por `catch`.

### retry e retryWhen

```kotlin
// retry: tenta novamente um número fixo de vezes
val dadosComRetry: Flow<List<Produto>> = repository
    .buscarRemoto()
    .retry(retries = 3) { causa ->
        causa is IOException // Só retenta em erro de rede
    }

// retryWhen: controle total com espera exponencial
val dadosComBackoff: Flow<List<Produto>> = repository
    .buscarRemoto()
    .retryWhen { causa, tentativa ->
        if (causa is IOException && tentativa < 3) {
            delay(1000L * (1L shl tentativa.toInt())) // 1s, 2s, 4s...
            true
        } else false
    }
    .catch { emit(emptyList()) } // Fallback final
```

---

## 7. Boas Práticas

### SharingStarted — escolha certa

| Estratégia                          | Quando usar                                    |
|-------------------------------------|-------------------------------------------------|
| `WhileSubscribed(5_000)`            | Telas comuns — para ao sair, sobrevive a rotação |
| `Eagerly`                           | Dados que devem estar prontos antes da coleta    |
| `Lazily`                            | Início tardio, mas nunca para depois de começar  |

### Coleta segura com ciclo de vida

```kotlin
// Em Activity/Fragment: repeatOnLifecycle
lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect { state -> atualizarTela(state) }
    }
}

// Em Compose: collectAsStateWithLifecycle (lifecycle-runtime-compose)
@Composable
fun CatalogoScreen(viewModel: CatalogoViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LazyColumn {
        items(state.produtos) { produto -> Text(produto.nome) }
    }
}
```

### Regras gerais

1. Prefira `collectAsStateWithLifecycle` em Compose em vez de `collectAsState`.
2. Use `WhileSubscribed(5_000)` como padrão para `stateIn` em ViewModels.
3. Trate erros com `catch` antes de `stateIn`/`shareIn` para evitar crashes.
4. Não crie Flows dentro de funções Composable — exponha-os do ViewModel.
5. Use `distinctUntilChanged` para evitar recomposições desnecessárias.

---

## 8. Resumo

| Conceito                  | Para que serve                                          |
|---------------------------|---------------------------------------------------------|
| `map`, `filter`           | Transformar e filtrar emissões                          |
| `debounce`                | Esperar o usuário parar de digitar                      |
| `distinctUntilChanged`    | Ignorar emissões repetidas                              |
| `combine`                 | Unir múltiplos Flows (emite quando qualquer um muda)    |
| `zip`                     | Parear emissões de dois Flows (1:1)                     |
| `flatMapLatest`           | Cancelar Flow anterior ao receber novo valor            |
| `stateIn` / `shareIn`    | Converter Flow frio em quente no ViewModel              |
| `catch`                   | Interceptar erros na cadeia                             |
| `retry` / `retryWhen`    | Retentar operações que falharam                         |
| `WhileSubscribed`         | Controlar quando o Flow quente fica ativo               |

---

## Próximos Passos

- **Módulo 4.01 (Testes):** como testar ViewModels que expõem Flows com `Turbine` e `TestDispatcher`.
- Explorar `callbackFlow` para integrar APIs baseadas em listeners (ex.: sensores, Firebase).
- Estudar `flowOn` para mover operadores pesados para `Dispatchers.Default` sem trocar o coletor de thread.
