# Acessibilidade no Desenvolvimento Android

Acessibilidade (muitas vezes abreviada como "a11y" — o "11" representa as 11 letras entre o "a" e o "y" de *accessibility*) é a prática de projetar e desenvolver aplicativos que possam ser usados por todos, incluindo pessoas com deficiências visuais, auditivas, motoras ou cognitivas. Garantir que seu aplicativo seja acessível não apenas amplia seu público, mas também é um aspecto fundamental do desenvolvimento de software inclusivo e de alta qualidade.

> **Pré-requisito:** Módulo 1, Aula 07 — Jetpack Compose (para os exemplos em Compose).

## O que é acessibilidade, na prática?

Pense em um usuário cego usando o celular. Ele não vê a tela — em vez disso, um programa chamado **TalkBack** (o leitor de tela nativo do Android) lê em voz alta o que está na tela, e o usuário navega tocando e deslizando o dedo para ouvir cada elemento, um de cada vez. Se um botão só tem um ícone e nenhum texto, o TalkBack não tem o que ler — para esse usuário, o botão é **invisível**, mesmo estando fisicamente ali na tela.

Acessibilidade é sobre garantir que informações que uma pessoa vidente capta "de graça" (um ícone de coração significa "favoritar", uma cor vermelha significa "erro") também cheguem a quem não consegue ver a tela, não consegue ouvir sons, ou não consegue usar gestos de toque precisos.

## Por que isso importa

Ignorar acessibilidade tem custos concretos:

- **Parte real dos seus usuários fica excluída**: segundo a OMS, mais de 1 bilhão de pessoas no mundo vivem com algum tipo de deficiência. Um app inacessível simplesmente não funciona para essas pessoas.
- **Pode ser uma exigência legal**: em vários países (incluindo o Brasil, com a Lei Brasileira de Inclusão), acessibilidade digital é um requisito legal para determinados serviços.
- **Boas práticas de acessibilidade melhoram o app para todo mundo**: contraste de cor melhor ajuda em ambientes com muita luz solar; áreas de toque maiores ajudam qualquer pessoa com o dedo maior ou o celular em movimento (ex: dentro de um ônibus).

---

## 1. `contentDescription`

Elementos visuais que não possuem texto, como ícones e imagens, são invisíveis para leitores de tela como o TalkBack. Precisamos fornecer uma descrição textual para esses componentes através da propriedade **`contentDescription`** — o texto que o TalkBack vai ler em voz alta quando o usuário focar naquele elemento.

**Quando usar:**
*   **Imagens informativas**: descreva o que a imagem representa.
*   **Botões com ícone**: descreva a ação que o botão executa (ex: "Adicionar aos favoritos", "Fechar").
*   **Imagens decorativas**: marque como decorativa para o leitor de tela ignorar.

### Jetpack Compose

```kotlin
// BOM: O leitor de tela anunciará "Adicionar novo item" ao focar no botão
IconButton(onClick = { /* ação */ }) {
    Icon(
        imageVector = Icons.Default.Add,
        // contentDescription informa ao TalkBack o que este ícone representa
        contentDescription = "Adicionar novo item"
    )
}

// Imagem decorativa — passe null para contentDescription.
// O Compose automaticamente marca como "não importante para acessibilidade".
Image(
    painter = painterResource(R.drawable.bg_header),
    contentDescription = null // imagem puramente decorativa
)

// Imagem informativa — descreva o conteúdo
Image(
    painter = painterResource(R.drawable.foto_produto),
    contentDescription = "Foto do produto: Camiseta azul tamanho M"
)
```

### XML (referência)

```xml
<!-- RUIM: Sem descrição para o leitor de tela -->
<ImageButton
    android:id="@+id/button_add"
    android:src="@drawable/ic_add" />

<!-- BOM: O leitor de tela anunciará "Adicionar novo item" -->
<ImageButton
    android:id="@+id/button_add_accessible"
    android:src="@drawable/ic_add"
    android:contentDescription="@string/desc_add_item" />
```

**Boas Práticas:**
*   Seja conciso e descritivo.
*   Não inclua "imagem de" ou "botão para" na descrição. O leitor de tela já informa o tipo do componente.
*   Em Compose, passe `contentDescription = null` para imagens decorativas.
*   Em XML, defina `android:contentDescription="@null"` ou `android:importantForAccessibility="no"`.

### Erros comuns / Pegadinhas

- **Deixar `contentDescription` vazio ("") em vez de `null` para imagens decorativas**: uma string vazia ainda é lida como "presente" por algumas ferramentas de verificação e pode confundir o TalkBack; use `null` explicitamente.
- **Copiar e colar o mesmo texto do rótulo visível na descrição de um ícone ao lado dele**: isso faz o TalkBack ler a mesma informação duas vezes seguidas. Se já existe um `Text` visível ao lado do ícone, o ícone pode receber `contentDescription = null` (veja a seção 2, sobre agrupamento).
- **Descrever a aparência em vez da função**: prefira "Adicionar aos favoritos" a "Ícone de coração". O usuário quer saber o que o botão faz, não como ele se parece.

---

## 2. Navegação por Foco e Agrupamento Semântico

Usuários de leitores de tela navegam pela interface "focando" em um elemento por vez — cada deslizar de dedo move o foco para o próximo elemento, que é então lido em voz alta. A ordem do foco por padrão segue a disposição dos elementos no layout.

### Jetpack Compose — `Modifier.semantics`

Em Compose, o termo **semântica** se refere às informações que descrevem o *significado* de um componente para tecnologias assistivas (como o TalkBack), além da sua aparência visual. Usamos `Modifier.semantics(mergeDescendants = true)` para agrupar elementos que devem ser lidos como uma única unidade pelo TalkBack, em vez de vários elementos separados.

```kotlin
// SEM agrupamento: o TalkBack foca em cada Text separadamente (3 paradas)
Row {
    Icon(Icons.Default.Person, contentDescription = null)
    Text("Maria Silva")
    Text("(11) 99999-0000")
}

// COM agrupamento: o TalkBack lê tudo junto como "Maria Silva, (11) 99999-0000"
// mergeDescendants = true combina a semântica de todos os filhos em um único nó
Row(
    modifier = Modifier.semantics(mergeDescendants = true) {}
) {
    // O ícone não precisa de contentDescription porque está dentro do grupo
    Icon(Icons.Default.Person, contentDescription = null)
    Text("Maria Silva")
    Text("(11) 99999-0000")
}
```

### Compose — Ordem de foco customizada

```kotlin
// Customize a ordem de travessia do foco usando traversalIndex.
// Valores menores são focados primeiro pelo TalkBack.
Column {
    Text(
        text = "Segundo na leitura",
        modifier = Modifier.semantics { traversalIndex = 2f }
    )
    Text(
        text = "Primeiro na leitura",
        modifier = Modifier.semantics { traversalIndex = 1f }
    )
}
```

### XML (referência)

```xml
<!-- O leitor de tela focará no LinearLayout como um todo (1 parada) -->
<LinearLayout
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:focusable="true">

    <ImageView
        android:src="@drawable/ic_person"
        android:importantForAccessibility="no" /> <!-- Ignorado, pois o pai tem o foco -->

    <TextView android:text="Nome do Contato" />
    <TextView android:text="Telefone" />
</LinearLayout>
```

### Erros comuns / Pegadinhas

- **Agrupar elementos demais em um único `mergeDescendants = true`**: se você agrupar uma tela inteira, o TalkBack lê tudo de uma vez em um bloco gigante, o que é tão ruim quanto não agrupar nada. Agrupe apenas conjuntos pequenos e relacionados (como um cartão de contato).
- **Usar `traversalIndex` sem necessidade real**: a ordem padrão (de cima para baixo, esquerda para direita) já funciona na maioria dos casos. Só customize quando a ordem visual não corresponder à ordem lógica de leitura.

---

## 3. Rótulos para Campos de Entrada (`Labels`)

Campos de entrada precisam de um rótulo que descreva qual informação deve ser inserida. Sem um rótulo, o TalkBack não consegue informar ao usuário o que digitar — ele só saberia dizer "campo de texto", sem contexto nenhum.

### Jetpack Compose — TextField acessível

```kotlin
// O parâmetro "label" do TextField funciona como rótulo permanente para
// acessibilidade. O TalkBack lê "E-mail, campo de texto" ao focar.
var email by remember { mutableStateOf("") }

OutlinedTextField(
    value = email,
    onValueChange = { email = it },
    // label — exibido acima do campo e lido pelo TalkBack como rótulo
    label = { Text("E-mail") },
    // placeholder — texto de exemplo que aparece quando o campo está vazio
    placeholder = { Text("exemplo@email.com") },
    // keyboardOptions configura o tipo de teclado (impacta a acessibilidade)
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
)
```

### Compose — Campo com ação acessível

```kotlin
// Botão de limpar dentro do campo com descrição para o leitor de tela
OutlinedTextField(
    value = busca,
    onValueChange = { busca = it },
    label = { Text("Pesquisar") },
    trailingIcon = {
        if (busca.isNotEmpty()) {
            // O contentDescription do IconButton é anunciado pelo TalkBack
            IconButton(onClick = { busca = "" }) {
                Icon(
                    Icons.Default.Clear,
                    contentDescription = "Limpar campo de pesquisa"
                )
            }
        }
    }
)
```

### XML (referência)

```xml
<!-- labelFor associa o TextView ao EditText para leitores de tela -->
<TextView
    android:id="@+id/label_email"
    android:text="@string/label_email"
    android:labelFor="@+id/input_email" />

<EditText
    android:id="@+id/input_email"
    android:hint="@string/hint_email"
    android:inputType="textEmailAddress" />
```

### Erros comuns / Pegadinhas

- **Usar apenas `placeholder`/`hint` como rótulo**: o placeholder some assim que o usuário começa a digitar, então quem depende do TalkBack (ou até quem só olha a tela depois de preencher o campo) perde a referência do que aquele campo representa. Sempre use um `label` fixo além do placeholder.
- **Em XML, esquecer `labelFor`**: sem essa associação, o `TextView` e o `EditText` são lidos como elementos separados e desconectados pelo TalkBack.

---

## 4. Anúncios e Ações Customizadas (Compose)

Além das semânticas padrão, Compose permite criar anúncios e ações personalizadas para o TalkBack.

### Anúncio de estado (Live Region)

Uma **live region** (região ao vivo) é uma parte da tela que, ao mudar de conteúdo, é anunciada automaticamente pelo TalkBack — sem que o usuário precise mover o foco manualmente até ela. É essencial para conteúdo dinâmico, como contadores e status de carregamento.

```kotlin
// liveRegion faz o TalkBack anunciar mudanças automaticamente,
// sem que o usuário precise focar no elemento. Útil para contadores,
// status de carregamento e mensagens de erro.
Text(
    text = if (carregando) "Carregando..." else "Pronto",
    modifier = Modifier.semantics {
        // Polite: anuncia quando o TalkBack estiver ocioso
        // Assertive: interrompe qualquer leitura em andamento
        liveRegion = LiveRegionMode.Polite
    }
)
```

### Ações customizadas

```kotlin
// Adiciona ações acessíveis que aparecem no menu de ações do TalkBack
// (gesto: deslizar para cima/baixo). Útil para ações de swipe e gestos
// que não são acessíveis por padrão.
Card(
    modifier = Modifier.semantics {
        customActions = listOf(
            CustomAccessibilityAction("Favoritar") {
                // Lógica de favoritar
                true // retorne true se a ação foi executada com sucesso
            },
            CustomAccessibilityAction("Compartilhar") {
                // Lógica de compartilhar
                true
            }
        )
    }
) {
    Text("Item da lista")
}
```

### Erros comuns / Pegadinhas

- **Usar `LiveRegionMode.Assertive` para qualquer mudança**: como esse modo interrompe a leitura em andamento, usá-lo em excesso (ex: para cada pequena atualização) é intrusivo e frustrante. Reserve `Assertive` para informações realmente urgentes (ex: erros críticos); use `Polite` para o resto.
- **Depender só de gestos de swipe sem ação customizada equivalente**: um usuário do TalkBack pode não conseguir executar um gesto de swipe pensado para uso visual comum (como "arrastar para excluir"). Sempre ofereça uma `CustomAccessibilityAction` equivalente.

---

## 5. Externalização de Strings

"Hardcoding" (escrever texto diretamente no código, em vez de em um arquivo de recursos) é uma má prática por vários motivos, incluindo acessibilidade e internacionalização (i18n — preparar o app para múltiplos idiomas).

### Compose — usando `stringResource`

```kotlin
// Use stringResource() para acessar strings de res/values/strings.xml.
// Isso permite tradução automática e centraliza os textos do app.
Text(text = stringResource(R.string.label_email))

// Strings com parâmetros (formatação)
Text(text = stringResource(R.string.bem_vindo, nomeUsuario))

// contentDescription com string externalizada
Icon(
    imageVector = Icons.Default.Favorite,
    contentDescription = stringResource(R.string.desc_favoritar)
)
```

**Defina as strings em `res/values/strings.xml`:**

```xml
<resources>
    <string name="app_name">Meu App</string>
    <string name="label_email">Endereço de e-mail</string>
    <string name="bem_vindo">Bem-vindo, %1$s!</string>
    <string name="desc_favoritar">Adicionar aos favoritos</string>
</resources>
```

**Por que externalizar strings é importante para a acessibilidade?**
1.  **Tradução**: permite fornecer traduções para todos os textos, incluindo descrições de acessibilidade — um usuário que usa o TalkBack em outro idioma também precisa de descrições traduzidas.
2.  **Manutenção**: centraliza todos os textos em um único lugar, facilitando revisão e correção (por exemplo, um revisor pode conferir todas as descrições de acessibilidade de uma vez, sem precisar vasculhar o código Kotlin).

### Erros comuns / Pegadinhas

- **Concatenar strings manualmente (`"Bem-vindo, " + nome + "!"`)**: em vez de usar `%1$s` como no exemplo, isso quebra a ordem correta em idiomas onde a gramática é diferente (algumas línguas colocam o nome em outra posição na frase).

---

## 6. Testando a Acessibilidade

### Teste manual com TalkBack

1. Ative o TalkBack: **Configurações → Acessibilidade → TalkBack**.
2. Navegue pelo app deslizando o dedo para a direita (próximo) e esquerda (anterior).
3. Verifique se todos os elementos interativos são anunciados corretamente.

### Teste automatizado em Compose

```kotlin
@Test
fun botaoFavoritar_deveSerAcessivel() {
    composeTestRule.setContent {
        BotaoFavoritar(onClick = {})
    }

    // Verifica se o botão tem contentDescription configurado
    composeTestRule
        .onNodeWithContentDescription("Adicionar aos favoritos")
        .assertExists()            // o nó com essa descrição existe
        .assertHasClickAction()    // é clicável (interativo)
}

@Test
fun campoEmail_deveSerRotulado() {
    composeTestRule.setContent {
        FormularioContato()
    }

    // Verifica se o campo tem um rótulo associado
    composeTestRule
        .onNodeWithText("E-mail")
        .assertExists()
}
```

### Checklist de acessibilidade

*   **Teste com o TalkBack**: navegue pelo app inteiro sem olhar para a tela.
*   **`contentDescription`**: presente em todos os elementos não textuais informativos.
*   **Agrupamento semântico**: elementos relacionados são lidos como uma unidade.
*   **Rótulos em campos**: todos os campos de entrada têm `label` ou `contentDescription`.
*   **Strings externalizadas**: nenhum texto hardcoded no código.
*   **Contraste de cores**: texto com ratio mínimo de 4.5:1 (AA) ou 7:1 (AAA).
*   **Tamanho de toque**: áreas clicáveis com no mínimo 48dp × 48dp.

---

## Resumo

- **Acessibilidade** garante que o app funcione para pessoas com deficiências visuais, auditivas, motoras ou cognitivas — e melhora a experiência para todo mundo.
- **`contentDescription`** é a descrição textual que o TalkBack lê para elementos sem texto visível (ícones, imagens). Use `null` para conteúdo puramente decorativo.
- **`Modifier.semantics`** controla como o TalkBack agrupa (`mergeDescendants`), ordena (`traversalIndex`) e anuncia (`liveRegion`) elementos.
- **Rótulos (`label`)** em campos de formulário são obrigatórios — placeholder sozinho não é suficiente.
- **Strings externalizadas** (`stringResource`) viabilizam tradução e mantêm as descrições de acessibilidade centralizadas.
- Teste sempre com o **TalkBack** ligado, além de testes automatizados que verificam `contentDescription` e rótulos.

**Próximo passo**: na próxima aula (`06_formularios_validacao.md`) você vai construir formulários completos, aplicando inclusive os conceitos de acessibilidade vistos aqui (rótulos, `isError`, mensagens de erro anunciadas).

---

## Exercícios Práticos

1. **Auditoria de um componente existente**
   - Pegue o `TaskItem` construído na aula de MVVM (`01_mvvm.md`) e identifique: falta algum `contentDescription`? O `Checkbox` sozinho é compreensível para quem usa o TalkBack, ou seria melhor agrupar o `Row` inteiro com `mergeDescendants = true`?

2. **Corrigir um formulário inacessível**
   - Dado um `OutlinedTextField` que só tem `placeholder` e nenhum `label`, corrija-o adicionando um `label` adequado.

3. **Desafio**: adicione uma `liveRegion` a um contador de itens no carrinho de compras (ex: "3 itens no carrinho"), de forma que, ao adicionar um novo item, o TalkBack anuncie automaticamente a mudança sem o usuário precisar focar no contador manualmente.
