# Formulários e Validação de Entrada no Jetpack Compose

Formulários são a principal forma de coletar dados do usuário em aplicativos móveis — telas de login, cadastro, endereço e pagamento dependem de campos bem construídos. Nesta aula, veremos como criar formulários em Compose, aplicar validações reativas com ViewModel e melhorar a experiência com máscaras, teclados e erros acessíveis.

> **Pré-requisito:** Módulo 2, Aula 01 — MVVM (para gerência de estado com ViewModel) e Módulo 1, Aula 07 — Jetpack Compose (para conceitos de estado e recomposição).

---

## 1. TextField e OutlinedTextField

O Compose oferece `TextField` (preenchido) e `OutlinedTextField` (com borda). Ambos funcionam da mesma forma — a diferença é apenas visual. Use `OutlinedTextField` em formulários com vários campos e `TextField` em barras de busca ou campos isolados.

```kotlin
@Composable
fun CampoNome() {
    // Estado local que armazena o texto digitado pelo usuário
    var nome by remember { mutableStateOf("") }

    OutlinedTextField(
        value = nome, // valor atual exibido no campo
        onValueChange = { nome = it }, // callback chamado a cada caractere digitado
        label = { Text("Nome completo") }, // rótulo flutuante do campo
        placeholder = { Text("Ex: Maria Silva") }, // texto de exemplo quando vazio
        singleLine = true // impede quebra de linha, mantém campo em uma linha
    )
}
```

---

## 2. Validação de Entrada

Validar a entrada é essencial para garantir dados corretos. O padrão é criar funções que retornam `String?` — `null` significa campo válido.

```kotlin
// Funções auxiliares de validação — retornam mensagem de erro ou null se válido
fun validarEmail(email: String): String? {
    if (email.isBlank()) return "E-mail é obrigatório" // verifica campo vazio
    // Verifica formato usando regex do Android
    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) return "E-mail inválido"
    return null // null significa que não há erro
}

fun validarSenha(senha: String): String? {
    if (senha.length < 8) return "Senha deve ter pelo menos 8 caracteres"
    if (!senha.any { it.isDigit() }) return "Senha deve conter ao menos um número"
    return null
}

fun validarCpf(cpf: String): String? {
    val digitos = cpf.replace("[^0-9]".toRegex(), "") // remove pontos e traço
    if (digitos.length != 11) return "CPF deve ter 11 dígitos"
    return null
}
```

Para exibir o erro no campo, usamos `isError` e `supportingText`:

```kotlin
@Composable
fun CampoEmail(email: String, erro: String?, onChanged: (String) -> Unit) {
    OutlinedTextField(
        value = email,
        onValueChange = onChanged, // delega a mudança ao pai (ViewModel)
        label = { Text("E-mail") },
        isError = erro != null, // destaca o campo em vermelho se houver erro
        supportingText = { if (erro != null) Text(erro) }, // mensagem abaixo do campo
        singleLine = true
    )
}
```

---

## 3. Estado do Formulário no ViewModel

Centralizar o estado no ViewModel garante que os dados sobrevivam a mudanças de configuração (rotação de tela) e mantém a validação fora da UI.

```kotlin
// Data class que representa o estado completo do formulário
data class CadastroUiState(
    val nome: String = "",         // valor do campo nome
    val email: String = "",        // valor do campo email
    val senha: String = "",        // valor do campo senha
    val erroNome: String? = null,  // mensagem de erro (null = sem erro)
    val erroEmail: String? = null,
    val erroSenha: String? = null,
    val enviando: Boolean = false  // indica se o formulário está sendo enviado
)

class CadastroViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(CadastroUiState()) // estado mutável interno
    val uiState: StateFlow<CadastroUiState> = _uiState // estado público imutável

    // Cada campo atualiza seu valor e limpa o erro ao digitar
    fun onNomeChanged(valor: String) { _uiState.update { it.copy(nome = valor, erroNome = null) } }
    fun onEmailChanged(valor: String) { _uiState.update { it.copy(email = valor, erroEmail = null) } }
    fun onSenhaChanged(valor: String) { _uiState.update { it.copy(senha = valor, erroSenha = null) } }

    // Valida todos os campos e retorna true se o formulário é válido
    fun validarFormulario(): Boolean {
        val state = _uiState.value
        val erroNome = if (state.nome.isBlank()) "Nome é obrigatório" else null
        val erroEmail = validarEmail(state.email) // reutiliza funções da seção 2
        val erroSenha = validarSenha(state.senha)
        _uiState.update { // atualiza o estado com todos os erros encontrados
            it.copy(erroNome = erroNome, erroEmail = erroEmail, erroSenha = erroSenha)
        }
        return listOf(erroNome, erroEmail, erroSenha).all { it == null }
    }
}
```

---

## 4. Formulário Completo

Exemplo de tela de cadastro conectando ViewModel, validação e campos visuais:
```kotlin
@Composable
fun TelaCadastro(viewModel: CadastroViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState() // recompõe quando qualquer campo muda
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp)
            .verticalScroll(rememberScrollState()), // permite scroll em telas pequenas
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Criar Conta", style = MaterialTheme.typography.headlineMedium)

        // Campo nome — delega mudanças ao ViewModel
        OutlinedTextField(
            value = state.nome, onValueChange = viewModel::onNomeChanged,
            label = { Text("Nome completo") },
            isError = state.erroNome != null, // vermelho se houver erro
            supportingText = { state.erroNome?.let { Text(it) } },
            singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        // Campo email — KeyboardType.Email exibe teclado com @
        OutlinedTextField(
            value = state.email, onValueChange = viewModel::onEmailChanged,
            label = { Text("E-mail") },
            isError = state.erroEmail != null,
            supportingText = { state.erroEmail?.let { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        // Campo senha — PasswordVisualTransformation oculta caracteres com ••••
        OutlinedTextField(
            value = state.senha, onValueChange = viewModel::onSenhaChanged,
            label = { Text("Senha") },
            isError = state.erroSenha != null,
            supportingText = { state.erroSenha?.let { Text(it) } },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        // Botão de envio — desabilitado enquanto envia
        Button(
            onClick = { if (viewModel.validarFormulario()) { /* enviar ao servidor */ } },
            enabled = !state.enviando, modifier = Modifier.fillMaxWidth()
        ) {
            if (state.enviando) CircularProgressIndicator(Modifier.size(20.dp))
            else Text("Cadastrar")
        }
    }
}
```

---

## 5. KeyboardOptions e KeyboardActions

`KeyboardOptions` configura o tipo de teclado. `KeyboardActions` define ações ao pressionar Done, Next ou Search. Use `FocusRequester` para navegar entre campos.

```kotlin
@Composable
fun CamposComNavegacao() {
    val focusSenha = remember { FocusRequester() } // controla o foco programaticamente

    OutlinedTextField( // campo email — "Next" move foco para o campo senha
        value = "", onValueChange = {},
        label = { Text("E-mail") },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email, // teclado com @ e .com
            imeAction = ImeAction.Next         // botão "Próximo" no teclado
        ),
        keyboardActions = KeyboardActions(
            onNext = { focusSenha.requestFocus() } // move foco ao próximo campo
        )
    )
    OutlinedTextField( // campo senha — "Done" executa a ação de login
        value = "", onValueChange = {},
        label = { Text("Senha") },
        modifier = Modifier.focusRequester(focusSenha), // recebe foco do campo anterior
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password, // oculta sugestões do teclado
            imeAction = ImeAction.Done             // botão "Concluído" no teclado
        ),
        keyboardActions = KeyboardActions(
            onDone = { /* executar login */ } // ação ao pressionar "Done"
        ),
        visualTransformation = PasswordVisualTransformation()
    )
}
```

**Tipos de teclado úteis:** `Email` (com `@`), `Number` (somente números), `Phone` (telefone), `Password` (oculta sugestões).

---

## 6. Máscara de Entrada

`VisualTransformation` exibe texto formatado sem alterar o valor armazenado. Ideal para CPF, telefone e cartão.

```kotlin
// Máscara de CPF: transforma "12345678900" em "123.456.789-00"
class CpfVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digitos = text.text // texto original sem formatação
        val formatado = buildString {
            digitos.forEachIndexed { i, c ->
                append(c)
                if (i == 2 || i == 5) append('.') // ponto após 3º e 6º dígito
                if (i == 8) append('-')            // traço após 9º dígito
            }
        }
        // OffsetMapping traduz posições entre texto original e formatado
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = when {
                offset <= 3 -> offset; offset <= 6 -> offset + 1
                offset <= 9 -> offset + 2; else -> offset + 3
            }
            override fun transformedToOriginal(offset: Int): Int = when {
                offset <= 3 -> offset; offset <= 7 -> offset - 1
                offset <= 11 -> offset - 2; else -> offset - 3
            }
        }
        return TransformedText(AnnotatedString(formatado), mapping)
    }
}

// Uso da máscara no campo de CPF
@Composable
fun CampoCpf(cpf: String, onChanged: (String) -> Unit) {
    OutlinedTextField(
        value = cpf,
        onValueChange = { novo ->
            val apenasDigitos = novo.filter { it.isDigit() }.take(11) // só dígitos, máx 11
            onChanged(apenasDigitos)
        },
        label = { Text("CPF") },
        visualTransformation = CpfVisualTransformation(), // aplica a máscara visual
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true
    )
}
```

---

## 7. Boas Práticas

- **Debounce em validação:** use `snapshotFlow` com `debounce` para aguardar o usuário parar de digitar antes de validar campos complexos (como verificar e-mail no servidor).
- **Acessibilidade de erros:** combine `isError` com `supportingText` para que o TalkBack anuncie erros. Use `liveRegion` para erros dinâmicos.
- **Scrollar para o primeiro erro:** use `BringIntoViewRequester` para levar o usuário ao primeiro campo com erro.
- **Limpar erros ao digitar:** remova a mensagem de erro assim que o usuário começa a corrigir o campo.
- **Desabilitar botão de envio:** enquanto o formulário estiver enviando, desabilite o botão para evitar envios duplicados.
- **Testar validações isoladamente:** funções de validação são Kotlin puro — teste com JUnit sem depender do Android.

---

## Resumo

`OutlinedTextField`/`TextField` são a base para entrada de texto. `isError` e `supportingText` exibem validação. O ViewModel centraliza estado via `StateFlow<UiState>`. `KeyboardOptions` configura teclado; `KeyboardActions` define ações (Next, Done). `VisualTransformation` formata exibição sem alterar o dado (máscaras). Validações devem ser reativas, acessíveis e testáveis.

---

## Próximos Passos

Com formulários e validação dominados, você tem as ferramentas para construir qualquer tela de entrada de dados. No **Módulo 3**, veremos como enviar esses dados para um servidor usando **Coroutines** e **Retrofit**, além de persistir informações localmente com **Room**.
