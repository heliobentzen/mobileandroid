# Módulo 4: Vibe Coding — Desenvolvimento Assistido por IA

**Objetivo**: entender o que é "vibe coding", quando ele acelera de verdade o seu trabalho como dev Android e quando ele vira um risco — e sair deste módulo (e do curso) sabendo usar assistentes de IA como ferramenta, sem depender cegamente deles.

**Pré-requisito**: este é o último arquivo do curso. Os exemplos aqui reaproveitam conceitos que você já viu — `ViewModel`, `StateFlow` e `UiState` (Módulo 2), Compose (Módulo 1) e testes (Módulo 4.01) — então vale ter esses módulos frescos na memória.

---

## 1. O que é Vibe Coding?

### O que é

**Vibe coding** é um termo popularizado em 2025 para descrever uma forma de programar em que você descreve, em linguagem natural, *o que* quer que o código faça — e um assistente de IA (Claude Code, GitHub Copilot, Cursor, Gemini no Android Studio, entre outros) gera o código, que você então roda, observa e ajusta iterativamente. Em vez de escrever cada linha manualmente, você guia o resultado por meio de prompts e do comportamento observado do app.

É útil pensar nisso como uma evolução natural de algo que você provavelmente já usa: o autocomplete com IA (que sugere a próxima linha enquanto você digita). O vibe coding vai um passo além — em vez de completar o que você já começou a escrever, você descreve a *intenção* ("crie uma tela que lista as tarefas pendentes, ordenadas por prazo") e deixa a IA propor o *como*.

### Por que isso importa

No mercado hoje, assistentes de IA já fazem parte do fluxo de trabalho da maioria dos times de desenvolvimento — não como substitutos do dev, mas como acelerador de tarefas específicas:

- **Prototipagem rápida**: validar uma ideia de tela ou fluxo em minutos, antes de investir tempo "produzindo" o código final.
- **Menos tempo em boilerplate**: telas Compose simples, testes repetitivos (Módulo 4.01), scripts de automação (Módulo 4.03) — tarefas mecânicas que consomem tempo sem exigir muita decisão de arquitetura.
- **Redução de fricção para aprender**: pedir para a IA explicar um trecho de código desconhecido é, muitas vezes, mais rápido do que vasculhar documentação.

Ignorar essa ferramenta por princípio é abrir mão de produtividade real. Mas usá-la sem critério — aceitando qualquer código gerado sem entender o que ele faz — é abrir mão de qualidade e de controle sobre o próprio projeto. O equilíbrio entre os dois é a habilidade que este módulo tenta ensinar.

### Erros comuns / Pegadinhas

- Tratar vibe coding como "mágica que substitui aprender a programar": ele funciona melhor justamente quando quem está pilotando entende o suficiente para avaliar o resultado.
- Achar que é uma prática exclusiva de iniciantes: devs sêniores também usam IA para acelerar tarefas mecânicas — a diferença é que eles sabem exatamente o que revisar antes de aceitar.

---

## 2. Quando Usar (e Quando Não Usar)

### O que é

Vibe coding não é uma técnica que se aplica igualmente a qualquer parte do código. Existe um espectro: em uma ponta, tarefas onde o risco de um erro é baixo e fácil de perceber; na outra, tarefas onde um erro pode ser sutil, caro ou perigoso.

| Bom uso | Uso arriscado |
|---------|----------------|
| Prototipar uma tela Compose simples | Lógica de negócio crítica (cálculo de preços, regras de permissão) |
| Gerar boilerplate (data classes, mappers simples) | Código que lida com segurança (autenticação, criptografia, tokens) |
| Escrever testes unitários (Módulo 4.01) para código já existente | Qualquer trecho que você não conseguiria debugar sozinho depois |
| Scripts de automação e configuração de CI (Módulo 4.03) | Migrações de banco de dados em produção |
| Explorar uma API ou biblioteca nova | Decisões de arquitetura que vão moldar o projeto por meses |

### Por que isso importa

A régua não é "IA sim ou não" — é "o que acontece se esse código gerado estiver sutilmente errado?". Uma tela de prototipagem com um bug visual você percebe olhando para ela. Uma regra de negócio com um bug sutil (por exemplo, um cálculo de desconto errado) pode passar despercebida por semanas e custar dinheiro real ou dados de usuários.

Regra prática: quanto mais crítico, mais difícil de detectar visualmente, ou mais sensível o código, menos você deveria delegar a decisão para a IA — mesmo que ela escreva o código, a decisão sobre *como* resolver o problema continua sendo sua.

### Erros comuns / Pegadinhas

- Usar vibe coding em código de autenticação ou pagamento "porque foi mais rápido": o custo de um erro nessas áreas é desproporcional ao tempo economizado.
- Aceitar uma migration de banco gerada por IA sem revisar linha a linha: mudanças de schema em produção são difíceis de reverter sem perda de dados.

---

## 3. Boas Práticas Essenciais

### O que é

Um pequeno conjunto de hábitos separa o uso produtivo de vibe coding do uso irresponsável:

1. **Sempre leia e entenda o código antes de aceitar.** Não existe atalho aqui — se você não sabe o que uma linha faz, você não pode confiar nela.
2. **Dê contexto do projeto no prompt.** Em vez de "crie uma tela de perfil", prefira algo como "estamos usando MVVM com StateFlow neste projeto (como no restante do curso); crie um `ProfileScreen` que observe um `ProfileViewModel` já existente". Contexto ruim gera código genérico, que não segue os padrões do resto do app.
3. **Peça para a IA explicar o que gerou.** Um pedido simples como "explique por que você usou esse operador de Flow aqui" transforma a resposta em aprendizado, não só em código pronto.
4. **Rode e teste antes de confiar.** Código que "parece certo" pode falhar em tempo de execução, ou passar despercebido em um cenário que você não testou.
5. **Nunca aceite um trecho que você não conseguiria explicar para outra pessoa.** Se você não consegue explicar o "porquê" de uma linha em uma revisão de código, é sinal de que ainda não entendeu o que aceitou.
6. **Nunca cole dados sensíveis em prompts de ferramentas na nuvem.** Chaves de API, tokens, senhas e dados reais de usuários não devem ir para um prompt de uma ferramenta que roda fora da sua infraestrutura — trate um prompt como um lugar tão exposto quanto um log público.

### Por que isso importa

Essas práticas não são burocracia — elas existem porque cada uma previne um tipo específico de problema real: código que ninguém no time entende (dívida técnica silenciosa), bugs que só aparecem em produção (falta de teste), e vazamento de credenciais (dado sensível em um prompt, que pode ficar armazenado nos logs da ferramenta usada).

### Erros comuns / Pegadinhas

- Copiar e colar um trecho de log de erro real (com dados de usuário) em um prompt para "pedir ajuda para debugar": anonimize ou remova dados sensíveis antes.
- Prompts vagos ("melhore esse código") que geram sugestões genéricas, desconectadas do padrão real do projeto — quanto mais específico o pedido, melhor (e mais revisável) o resultado.

---

## 4. Exemplo Prático: Gerando uma Tela Compose com IA

### O que é

Vamos simular um cenário realista: você precisa de uma tela de perfil simples, que reaproveita o padrão MVVM + StateFlow já usado no restante do curso (Módulo 2).

**Prompt de exemplo** (com contexto do projeto, como recomendado na Boa Prática 2):

> "Estamos usando MVVM com StateFlow neste projeto Android, com Jetpack Compose. Já existe um `ProfileViewModel` que expõe `val uiState: StateFlow<ProfileUiState>`, onde `ProfileUiState` é uma sealed class com os estados `Loading`, `Success(name: String, email: String)` e `Error(message: String)` — o mesmo padrão usado no `TasksViewModel` do Módulo 4.01. Crie um Composable `ProfileScreen` que observe esse ViewModel e mostre um indicador de carregamento, os dados do usuário, ou uma mensagem de erro, conforme o estado."

### Exemplo comentado: resultado gerado (para revisar, não para aceitar de olhos fechados)

```kotlin
@Composable
fun ProfileScreen(viewModel: ProfileViewModel) {
    // collectAsStateWithLifecycle observa o StateFlow respeitando o ciclo de vida da tela
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is ProfileUiState.Loading -> CircularProgressIndicator()

        is ProfileUiState.Success -> Column {
            Text(text = state.name)
            Text(text = state.email)
        }

        is ProfileUiState.Error -> Text(text = state.message)
    }
}
```

### O que você deveria revisar antes de aceitar esse resultado

Mesmo um exemplo simples como esse merece uma checagem ativa — é exatamente aqui que a Boa Prática 1 ("leia e entenda antes de aceitar") entra em ação:

- **Falta `Modifier`?** O Composable não recebe nem repassa um `Modifier`, o que dificulta reutilizá-lo dentro de outras telas (algo visto no Módulo 1). Vale pedir para a IA ajustar, ou ajustar você mesmo.
- **Acessibilidade** (Módulo 2.05): o `CircularProgressIndicator` deveria ter uma `contentDescription` acessível, e os textos deveriam ter tamanho e contraste adequados. A IA raramente pensa nisso por padrão — é sua responsabilidade cobrar.
- **Strings hardcoded**: em um projeto real, `state.message` vindo direto de uma exceção pode não ser um texto amigável para o usuário final. Vale revisar se essa é realmente a mensagem que deveria aparecer na tela.
- **O `ProfileViewModel` realmente existe com essa assinatura?** Se a IA "inventou" um formato de `ProfileUiState` diferente do que já existe no seu projeto, o código não vai nem compilar — sempre confira contra o código real, não contra o que parece plausível.
- **Você testaria isso como no Módulo 4.01?** Antes de aceitar como pronto, rode a tela (ou escreva um teste de UI simples com `onNodeWithText`) para confirmar que os três estados realmente aparecem como esperado.

Note que nenhum desses pontos é sobre "a IA errou" — o código gerado está sintaticamente correto. O ponto é que "compilar" e "estar pronto para o projeto" são coisas diferentes, e só quem entende o contexto todo consegue fechar essa lacuna.

---

## Erros Comuns

- **"Vibe coding irresponsável"**: aceitar código gerado sem ler, sem entender e sem testar — só porque "rodou sem erro". Isso é o oposto do que este módulo defende: a IA acelera a escrita, não substitui a revisão.
- **Não testar antes de considerar pronto**: um Composable que renderiza sem crash não significa que ele está correto para todos os estados (Loading, Success, Error, listas vazias etc.).
- **Prompts vagos**: pedir "crie uma tela de login" sem contexto do projeto tende a gerar um código genérico, com padrões diferentes do resto do app — que depois exige mais trabalho de adaptação do que escrever do zero.
- **Vazar dados sensíveis em prompts**: colar uma chave de API real, um token de autenticação ou dados reais de usuários em uma ferramenta de IA na nuvem é um risco de segurança concreto, não teórico — trate esses dados com o mesmo cuidado que teria ao postar algo publicamente.

---

## Resumo

- **Vibe coding** é desenvolver descrevendo a intenção em linguagem natural para um assistente de IA, e iterar observando o resultado — uma evolução do autocomplete com IA, focada no "o quê" em vez do "como".
- É uma ferramenta de produtividade real no mercado atual: ótima para prototipagem, boilerplate, testes e scripts; arriscada para lógica de negócio crítica, segurança e qualquer código que você não conseguiria manter sozinho depois.
- As boas práticas essenciais giram em torno de um princípio único: **você continua responsável pelo código, mesmo que não tenha digitado cada linha**. Ler, entender, dar contexto, testar e nunca vazar dados sensíveis são os hábitos que sustentam isso.
- O exemplo da tela `ProfileScreen` mostrou que mesmo um resultado simples e correto sintaticamente ainda exige revisão ativa — de acessibilidade, de fidelidade ao código real do projeto, e de comportamento testado.

---

## Você Concluiu a Trilha

Chegar até aqui significa que você percorreu o caminho completo: começou com Kotlin essencial e os fundamentos do Android (Módulo 1), estruturou apps reais com MVVM, `StateFlow` e Compose (Módulo 2), conectou esses apps a dados de verdade com coroutines, Retrofit, Room e Hilt (Módulo 3), e fechou com a parte que transforma um projeto pessoal em software profissional — testes automatizados, publicação na Play Store e automação com CI/CD (Módulo 4). Vibe coding é só mais uma ferramenta na caixa, e agora você sabe usá-la com critério, exatamente porque entende o que está por trás do código que ela gera.

Nenhum curso substitui a prática contínua: o próximo passo é pegar um projeto — seu ou de um exercício anterior — e levá-lo até o fim, com testes, pipeline e uma versão publicada de verdade. Cada bug que você resolver sozinho, cada decisão de arquitetura que você tomar com convicção, vai valer mais do que qualquer aula. Bom trabalho por ter chegado até aqui — e boas builds pela frente.
