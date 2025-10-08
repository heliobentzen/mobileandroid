# Acessibilidade no Desenvolvimento Android

Acessibilidade (muitas vezes abreviada como "a11y") é a prática de projetar e desenvolver aplicativos que possam ser usados por todos, incluindo pessoas com deficiências visuais, auditivas, motoras ou cognitivas. Garantir que seu aplicativo seja acessível não apenas amplia seu público, mas também é um aspecto fundamental do desenvolvimento de software inclusivo e de alta qualidade.

## 1. `contentDescription`

Elementos visuais que não possuem texto, como `ImageView` e `ImageButton`, são invisíveis para leitores de tela como o TalkBack. O atributo `android:contentDescription` fornece uma descrição textual para esses componentes.

**Quando usar:**
*   `ImageView`: Descreva o que a imagem representa.
*   `ImageButton`: Descreva a ação que o botão executa (ex: "Adicionar aos favoritos", "Fechar").

**Exemplo em XML:**

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
*   Se uma imagem for puramente decorativa e não transmitir informação, defina `android:contentDescription="@null"` ou `android:importantForAccessibility="no"`.

---

## 2. Navegação por Foco

Usuários de leitores de tela navegam pela interface "focando" em um elemento por vez, geralmente deslizando o dedo para a direita ou para a esquerda. A ordem do foco por padrão segue a disposição dos elementos no layout XML.

**Agrupando Elementos:**
Às vezes, faz sentido que vários elementos sejam lidos como uma única unidade. Por exemplo, um item de lista com um ícone e dois textos. Você pode agrupar esses elementos em um `ViewGroup` (como `LinearLayout` ou `ConstraintLayout`) e torná-lo focável.

```xml
<!-- O leitor de tela focará no ícone, depois no título e depois no subtítulo (3 paradas) -->
<LinearLayout ... >
    <ImageView android:src="@drawable/ic_person" ... />
    <TextView android:text="Nome do Contato" ... />
    <TextView android:text="Telefone" ... />
</LinearLayout>

<!-- O leitor de tela focará no LinearLayout como um todo e lerá as descrições em ordem (1 parada) -->
<LinearLayout
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:focusable="true">

    <ImageView
        android:src="@drawable/ic_person"
        android:importantForAccessibility="no" /> <!-- Ignorado, pois o pai tem o foco -->

    <TextView
        android:text="Nome do Contato" />

    <TextView
        android:text="Telefone" />

</LinearLayout>
```

---

## 3. Rótulos para Campos de Entrada (`Labels`)

Campos de entrada, como `EditText`, precisam de um rótulo (`label`) que descreva qual informação deve ser inserida.

*   **`android:hint`**: Fornece um texto de exemplo dentro do campo, que desaparece quando o usuário começa a digitar. É útil, mas não substitui um rótulo permanente.
*   **`android:labelFor`**: Associa um `TextView` (que funciona como rótulo) a um campo de entrada. Quando o usuário foca no `TextView`, o foco é automaticamente movido para o campo associado.

**Exemplo com `labelFor`:**

```xml
<TextView
    android:id="@+id/label_email"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="@string/label_email"
    android:labelFor="@+id/input_email" />

<EditText
    android:id="@+id/input_email"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:hint="@string/hint_email"
    android:inputType="textEmailAddress" />
```

---

## 4. Externalização de Strings

"Hardcoding" (escrever texto diretamente no arquivo de layout XML ou no código Kotlin/Java) é uma má prática por vários motivos, incluindo acessibilidade e internacionalização (i18n).

Todos os textos visíveis para o usuário devem ser definidos no arquivo `res/values/strings.xml`.

**Por que externalizar strings é importante para a acessibilidade?**
1.  **Tradução:** Permite que você forneça traduções para seus textos. Um usuário cujo dispositivo está configurado para outro idioma poderá usar seu aplicativo mais facilmente.
2.  **Manutenção:** Centraliza todos os textos em um único lugar, facilitando a revisão e a correção de descrições e rótulos.

**Como fazer:**

1.  **Defina a string em `res/values/strings.xml`:**
    ```xml
    <resources>
        <string name="app_name">Meu App</string>
        <string name="desc_add_item">Adicionar novo item</string>
        <string name="label_email">Endereço de e-mail</string>
    </resources>
    ```

2.  **Use a string no seu layout XML:**
    ```xml
    <TextView
        android:text="@string/label_email" />

    <ImageButton
        android:contentDescription="@string/desc_add_item" />
    ```

### Resumo de Boas Práticas

*   **Teste com o TalkBack:** Ative o leitor de tela do Android e tente navegar pelo seu aplicativo sem olhar para a tela.
*   **Forneça `contentDescription`** para todos os elementos não textuais que transmitem informação.
*   **Garanta uma ordem de foco lógica.** Agrupe elementos relacionados quando fizer sentido.
*   **Use `labelFor`** para associar rótulos a campos de entrada.
*   **Sempre externalize suas strings** para o arquivo `strings.xml`.