# Formulários e Validação de Entrada no Jetpack Compose

Formulários são a principal forma de coletar dados do usuário em aplicativos móveis — telas de login, cadastro, endereço e pagamento dependem de campos bem construídos. Nesta aula, veremos como criar formulários em Compose, aplicar validações reativas com ViewModel e melhorar a experiência com máscaras, teclados e erros acessíveis.

> **Pré-requisito:** Módulo 2, Aula 01 — MVVM (para gerência de estado com ViewModel) e Módulo 1, Aula 07 — Jetpack Compose (para conceitos de estado e recomposição).

## O que é um formulário "bem construído"?

Um formulário bem construído não é só "campos de texto na tela". Ele precisa: mostrar claramente o que cada campo espera (rótulo), avisar o usuário assim que algo está errado (validação), abrir o teclado certo para cada tipo de dado (numérico, e-mail, senha), e comunicar erros de um jeito que também funcione para quem usa leitor de tela. Nesta aula juntamos tudo isso.

## Por que isso importa

Um formulário mal feito é uma das maiores fontes de frustração em apps: usuário não sabe por que o botão "Enviar" está desabilitado, digita a senha errada sem saber o motivo, ou perde os dados digitados ao girar a tela porque o estado não estava centralizado no ViewModel. Validação clara e estado bem gerenciado evitam que o usuário desista do cadastro no meio do caminho — e evitam bugs difíceis de reproduzir.

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

**Por que `value` + `onValueChange` juntos?** Esse par forma o que se chama de "campo controlado": o Compose não guarda o texto digitado sozinho — cada tecla digitada dispara `onValueChange`, que você usa para atualizar uma variável de estado (`nome`), e é essa variável que volta a ser mostrada em `value`. Esse ciclo garante que a UI sempre reflita exatamente o estado atual, sem duplicidade de fontes de verdade.

### Erros comuns / Pegadinhas

- **Usar `var nome = ""` (sem `remember`/`mutableStateOf`)**: sem isso, o Compose não sabe que precisa recompor quando `nome` mudar, e o campo parece "travado", sem atualizar o que é exibido.
- **Esquecer `singleLine = true` em campos de uma linha só**: sem isso, apertar Enter no teclado pode quebrar linha dentro do campo em vez de submeter o formulário.

---

## 2. Validação de Entrada

Validar a entrada é essencial para garantir dados corretos. O padrão é criar funções que retornam `String?` (uma `String` que também pode ser `null`) — `null` significa campo válido, e qualquer texto retornado é a mensagem de erro a ser exibida.

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

**Por que essas funções não recebem nada do Compose (nenhum `@Composable`, nenhum `Context`)?** Elas são Kotlin puro — recebem uma `String` e devolvem uma `String?`. Isso é proposital: assim dá para testá-las com JUnit comum, sem precisar rodar um emulador Android. É uma boa prática separar regras de negócio (validação) de código de UI.

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

### Erros comuns / Pegadinhas

- **Validar só no clique do botão "Enviar" e nunca antes**: o usuário só descobre o erro depois de preencher tudo e tentar enviar, o que é frustrante. Prefira limpar o erro assim que o usuário começa a corrigir o campo (como fazemos no ViewModel, seção 3) e, quando fizer sentido, validar também ao sair do campo (`onFocusChanged`).
- **Misturar a regex de validação direto no Composable**: mantenha funções de validação fora da UI (como no exemplo acima) para poder reutilizá-las e testá-las isoladamente.
- **Confundir `isError` com desabilitar o campo**: `isError = true` só muda a aparência (cor vermelha) — o campo continua editável, o que é o comportamento correto (o usuário precisa poder corrigir).

---

## 3. Estado do Formulário no ViewModel

Centralizar o estado no `ViewModel` (a classe de lógica de apresentação vista na aula `01_mvvm.md`) garante que os dados sobrevivam a mudanças de configuração (rotação de tela) e mantém a validação fora da UI. Em vez de já mostrar o `ViewModel` com todos os campos do formulário, vamos construí-lo em etapas.

#### Passo 1 — estado com um único campo

Comece com o mínimo: um campo (`email`), seu erro e a função que atualiza os dois.

```kotlin
data class CadastroUiState(
    val email: String = "",       // valor do campo email
    val erroEmail: String? = null // mensagem de erro (null = sem erro)
)

class CadastroViewModel : ViewModel() {
    // MutableStateFlow: uma "caixa observável" que guarda o valor atual do
    // estado e notifica automaticamente a tela sempre que ele muda
    // (explicado em detalhe na aula 01_mvvm.md).
    private val _uiState = MutableStateFlow(CadastroUiState()) // estado mutável interno
    val uiState: StateFlow<CadastroUiState> = _uiState // estado público imutável

    // Atualiza o valor e limpa o erro anterior ao digitar
    fun onEmailChanged(valor: String) {
        _uiState.update { it.copy(email = valor, erroEmail = null) }
    }
}
```

**O que faz `_uiState.update { ... }`?** É uma forma segura de alterar o valor de um `MutableStateFlow` a partir do valor atual: a função recebe o estado antigo (`it`) e você devolve o novo estado (geralmente com `.copy(...)`, já que `CadastroUiState` é uma `data class` imutável). Isso evita problemas de concorrência que poderiam ocorrer se duas atualizações tentassem escrever `.value` ao mesmo tempo.

Essa versão já é reativa — o campo atualiza e limpa seu próprio erro ao digitar. Mas falta o essencial: **nada aqui chama a validação de verdade**. `erroEmail` nunca é preenchido com uma mensagem, então `isError` nunca fica `true`, não importa o que o usuário digite.

#### Passo 2 — validando ao enviar

Adicione uma função que roda a validação (reutilizando `validarEmail`, da seção 2) e grava o resultado no estado.

```kotlin
fun validarFormulario(): Boolean {
    val state = _uiState.value
    val erroEmail = validarEmail(state.email) // reutiliza a função da seção 2
    _uiState.update { it.copy(erroEmail = erroEmail) }
    return erroEmail == null
}
```

Agora, ao chamar `validarFormulario()` (por exemplo, no clique do botão "Cadastrar"), o campo passa a mostrar o erro de verdade quando o e-mail é inválido.

#### Passo 3 — repetindo o padrão para os demais campos

O `CadastroUiState` de um formulário real tem mais de um campo. A boa notícia é que o padrão dos Passos 1 e 2 apenas se repete — cada campo ganha seu par (valor + erro) e sua função `onXChanged`, e `validarFormulario()` passa a validar todos de uma vez:

```kotlin
data class CadastroUiState(
    val nome: String = "",
    val email: String = "",
    val senha: String = "",
    val erroNome: String? = null,
    val erroEmail: String? = null,
    val erroSenha: String? = null,
    val enviando: Boolean = false // indica se o formulário está sendo enviado
)

class CadastroViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(CadastroUiState())
    val uiState: StateFlow<CadastroUiState> = _uiState

    fun onNomeChanged(valor: String) { _uiState.update { it.copy(nome = valor, erroNome = null) } }
    fun onEmailChanged(valor: String) { _uiState.update { it.copy(email = valor, erroEmail = null) } }
    fun onSenhaChanged(valor: String) { _uiState.update { it.copy(senha = valor, erroSenha = null) } }

    // Valida todos os campos e retorna true se o formulário é válido
    fun validarFormulario(): Boolean {
        val state = _uiState.value
        val erroNome = if (state.nome.isBlank()) "Nome é obrigatório" else null
        val erroEmail = validarEmail(state.email)
        val erroSenha = validarSenha(state.senha)
        _uiState.update { // atualiza o estado com todos os erros encontrados
            it.copy(erroNome = erroNome, erroEmail = erroEmail, erroSenha = erroSenha)
        }
        return listOf(erroNome, erroEmail, erroSenha).all { it == null }
    }
}
```

Essa é a versão que usamos no restante da aula (o campo `enviando` entra em cena na próxima seção, para desabilitar o botão durante o envio).

### Erros comuns / Pegadinhas

- **Alterar campos individuais do estado em vez de usar `.copy()`**: como `CadastroUiState` é imutável (todos os campos são `val`), você não pode fazer `state.nome = valor`. Sempre crie uma cópia com o campo alterado, como no exemplo.
- **Esquecer de limpar o erro ao digitar (`erroNome = null` dentro de `onNomeChanged`)**: sem isso, a mensagem de erro fica presa na tela mesmo depois do usuário já ter corrigido o campo, o que é confuso.
- **Validar campo por campo em funções separadas, sem uma função central (`validarFormulario`)**: isso dificulta garantir que *todos* os campos foram checados antes de enviar o formulário ao servidor.

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

**Por que `viewModel::onNomeChanged` em vez de `{ viewModel.onNomeChanged(it) }`?** As duas formas fazem a mesma coisa — `::` é uma **referência de função** do Kotlin, uma forma mais curta de dizer "chame esta função passando o argumento recebido". É só um atalho de sintaxe, sem diferença de comportamento aqui.

### Erros comuns / Pegadinhas

- **Esquecer `verticalScroll(rememberScrollState())` em formulários longos**: sem isso, em telas pequenas ou com o teclado aberto, os campos de baixo (e o botão de envio) podem ficar inacessíveis, cortados fora da tela.
- **Não desabilitar o botão durante o envio (`enabled = !state.enviando`)**: sem isso, o usuário pode clicar em "Cadastrar" várias vezes seguidas, disparando múltiplos envios do mesmo formulário ao servidor.

---

## 5. KeyboardOptions e KeyboardActions

`KeyboardOptions` configura o tipo de teclado que aparece para o usuário (numérico, e-mail, etc). `KeyboardActions` define ações ao pressionar botões especiais do teclado, como "Done" (concluir), "Next" (próximo) ou "Search" (buscar). Use `FocusRequester` — um objeto que permite mover o foco de teclado programaticamente — para navegar entre campos sem o usuário precisar tocar manualmente no próximo campo.

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

### Erros comuns / Pegadinhas

- **Deixar o `imeAction` padrão em todos os campos**: sem configurar `ImeAction.Next`/`Done`, o teclado mostra sempre a tecla genérica de quebra de linha, obrigando o usuário a tocar manualmente em cada campo — pior experiência de digitação.
- **Esquecer `Modifier.focusRequester(focusSenha)` no campo de destino**: sem essa linha, o `FocusRequester` não sabe para qual campo mover o foco, e `requestFocus()` não tem efeito (ou lança erro).

---

## 6. Máscara de Entrada

`VisualTransformation` é uma interface do Compose que exibe o texto **formatado** na tela sem alterar o valor real armazenado no estado. Ideal para CPF, telefone e número de cartão, onde o usuário vê pontos e traços, mas o app guarda só os dígitos.

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

**Por que precisamos de `OffsetMapping`?** Quando o texto exibido (`"123.456.789-00"`) tem caracteres a mais que o texto real (`"12345678900"`), o Compose precisa saber como traduzir a posição do cursor entre os dois formatos — por exemplo, se o cursor está na posição 5 do texto formatado (`"123.4|56..."`), ele precisa saber que isso corresponde à posição 4 do texto original (sem os pontos). O `OffsetMapping` faz essa tradução nos dois sentidos.

### Erros comuns / Pegadinhas

- **Guardar o texto já formatado no estado (`"123.456.789-00"`) em vez de só os dígitos**: isso complica validações e envio ao servidor, que normalmente esperam o CPF "limpo". Sempre guarde o valor cru e use `VisualTransformation` só para exibição.
- **Esquecer o `.take(11)`**: sem limitar a quantidade de dígitos, o usuário pode digitar mais números do que o CPF permite, e a máscara customizada pode quebrar ou se comportar de forma inesperada.

---

## 7. Boas Práticas

- **Debounce em validação:** use `snapshotFlow` com `debounce` para aguardar o usuário parar de digitar antes de validar campos complexos (como verificar e-mail no servidor). "Debounce" significa "esperar um tempinho sem novas mudanças antes de agir" — evita disparar uma validação (ou requisição de rede) a cada tecla digitada.
- **Acessibilidade de erros:** combine `isError` com `supportingText` para que o TalkBack anuncie erros (veja a aula `05_acessibilidade.md`). Use `liveRegion` para erros dinâmicos que aparecem sem o usuário focar no campo.
- **Scrollar para o primeiro erro:** use `BringIntoViewRequester` para levar o usuário ao primeiro campo com erro, especialmente útil em formulários longos.
- **Limpar erros ao digitar:** remova a mensagem de erro assim que o usuário começa a corrigir o campo.
- **Desabilitar botão de envio:** enquanto o formulário estiver enviando, desabilite o botão para evitar envios duplicados.
- **Testar validações isoladamente:** funções de validação são Kotlin puro — teste com JUnit sem depender do Android (nem de emulador).

---

## Exercícios Práticos

1. **Adicionar validação de CPF ao formulário**
   - Checkpoint 1: adicione `cpf` e `erroCpf` ao `CadastroUiState`.
   - Checkpoint 2: crie `onCpfChanged` no `CadastroViewModel`, seguindo o mesmo padrão de `onNomeChanged`.
   - Checkpoint 3: inclua a validação de CPF (`validarCpf`, já definida na seção 2) dentro de `validarFormulario()`.
   - Checkpoint 4: adicione o campo `CampoCpf` (seção 6) na `TelaCadastro`.

2. **Confirmar senha**
   - Adicione um segundo campo "Confirmar senha" e valide que ele é igual ao campo "Senha" — mostre um erro específico ("As senhas não coincidem") se forem diferentes.

3. **Desafio**: implemente debounce na validação de e-mail, simulando uma checagem "no servidor" que só dispara 500ms depois que o usuário parar de digitar. Dica: pesquise sobre `snapshotFlow { state.email }.debounce(500)` dentro de um `LaunchedEffect`.

---

## Resumo

- `TextField`/`OutlinedTextField` são a base para entrada de texto controlada (`value` + `onValueChange`).
- Funções de validação em Kotlin puro (`String -> String?`) mantêm a lógica testável e fora da UI.
- `isError` e `supportingText` exibem o erro visualmente e de forma acessível ao TalkBack.
- O `ViewModel` centraliza o estado do formulário via `StateFlow<UiState>`, sobrevivendo a rotações de tela.
- `KeyboardOptions` configura o tipo de teclado; `KeyboardActions` define o que acontece ao pressionar Next/Done.
- `VisualTransformation` formata a exibição (máscaras de CPF, telefone) sem alterar o dado real armazenado.

**Próximo passo:** com formulários e validação dominados, você tem as ferramentas para construir qualquer tela de entrada de dados. No **Módulo 3**, veremos como enviar esses dados para um servidor usando **Coroutines** e **Retrofit**, além de persistir informações localmente com **Room**.
