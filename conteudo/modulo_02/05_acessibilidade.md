# Acessibilidade no Desenvolvimento Android

Acessibilidade (muitas vezes abreviada como "a11y") é a prática de projetar e desenvolver aplicativos que possam ser usados por todos, incluindo pessoas com deficiências visuais, auditivas, motoras ou cognitivas. Garantir que seu aplicativo seja acessível não apenas amplia seu público, mas também é um aspecto fundamental do desenvolvimento de software inclusivo e de alta qualidade.

> **Pré-requisito:** Módulo 1, Aula 07 — Jetpack Compose (para os exemplos em Compose).

---

## 1. `contentDescription`

Elementos visuais que não possuem texto, como ícones e imagens, são invisíveis para leitores de tela como o TalkBack. Precisamos fornecer uma descrição textual para esses componentes.

**Quando usar:**
*   **Imagens informativas** — descreva o que a imagem representa.
*   **Botões com ícone** — descreva a ação que o botão executa (ex: "Adicionar aos favoritos", "Fechar").
*   **Imagens decorativas** — marque como decorativa para o leitor de tela ignorar.

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

---

## 2. Navegação por Foco e Agrupamento Semântico

Usuários de leitores de tela navegam pela interface "focando" em um elemento por vez. A ordem do foco por padrão segue a disposição dos elementos no layout.

### Jetpack Compose — `Modifier.semantics`

Em Compose, usamos `Modifier.semantics(mergeDescendants = true)` para agrupar elementos que devem ser lidos como uma única unidade pelo TalkBack.

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

---

## 3. Rótulos para Campos de Entrada (`Labels`)

Campos de entrada precisam de um rótulo que descreva qual informação deve ser inserida. Sem um rótulo, o TalkBack não consegue informar ao usuário o que digitar.

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

---

## 4. Anúncios e Ações Customizadas (Compose)

Além das semânticas padrão, Compose permite criar anúncios e ações personalizadas para o TalkBack.

### Anúncio de estado (Live Region)

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

---

## 5. Externalização de Strings

"Hardcoding" (escrever texto diretamente no código) é uma má prática por vários motivos, incluindo acessibilidade e internacionalização (i18n).

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
1.  **Tradução:** Permite fornecer traduções para todos os textos, incluindo descrições de acessibilidade.
2.  **Manutenção:** Centraliza todos os textos em um único lugar, facilitando revisão e correção.

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

*   **Teste com o TalkBack** — navegue pelo app inteiro sem olhar para a tela.
*   **`contentDescription`** — presente em todos os elementos não textuais informativos.
*   **Agrupamento semântico** — elementos relacionados são lidos como uma unidade.
*   **Rótulos em campos** — todos os campos de entrada têm `label` ou `contentDescription`.
*   **Strings externalizadas** — nenhum texto hardcoded no código.
*   **Contraste de cores** — texto com ratio mínimo de 4.5:1 (AA) ou 7:1 (AAA).
*   **Tamanho de toque** — áreas clicáveis com no mínimo 48dp × 48dp.