# Recursos no Desenvolvimento Android

Recursos são arquivos e valores estáticos que seu código utiliza, como bitmaps, definições de layout, strings da interface do usuário, instruções de animação, cores e muito mais.

Centralizar os recursos do aplicativo facilita a manutenção e, mais importante, permite que o Android selecione os recursos apropriados para diferentes configurações de dispositivo (como idiomas ou tamanhos de tela diferentes).

Todos os recursos ficam localizados no diretório `res/` do seu projeto.

## 1. Strings (`strings.xml`)

Centralizar as strings do seu aplicativo em um único local facilita a tradução (localização) e a manutenção de textos.

**Localização:** `res/values/strings.xml`

### Exemplo

```xml
<!-- res/values/strings.xml -->
<resources>
    <string name="app_name">IFMaker</string>
    <string name="welcome_message">Bem-vindo ao nosso App!</string>
    <string name="button_save">Salvar</string>
</resources>
```

### Como Usar

**No Layout XML:**

```xml
<TextView
    android:id="@+id/welcomeTextView"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="@string/welcome_message" />

<Button
    android:id="@+id/saveButton"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="@string/button_save" />
```

**No Código Kotlin:**

```kotlin
val welcomeText = getString(R.string.welcome_message)
welcomeTextView.text = welcomeText
```

## 2. Dimensões (`dimens.xml`)

Define tamanhos reutilizáveis para margens, preenchimentos, tamanhos de texto, etc. Isso ajuda a manter a consistência visual e facilita a adaptação para diferentes tamanhos de tela.

**Localização:** `res/values/dimens.xml`

### Exemplo

```xml
<!-- res/values/dimens.xml -->
<resources>
    <!-- Margens e Paddings -->
    <dimen name="margin_small">8dp</dimen>
    <dimen name="margin_medium">16dp</dimen>
    <dimen name="margin_large">24dp</dimen>

    <!-- Tamanhos de Texto -->
    <dimen name="text_size_body">16sp</dimen>
    <dimen name="text_size_title">22sp</dimen>
</resources>
```

> **Nota:** Use `dp` (density-independent pixels) para tamanhos de layout e `sp` (scale-independent pixels) para tamanhos de texto, para garantir a acessibilidade e a correta adaptação à densidade da tela e às preferências de fonte do usuário.

### Como Usar

```xml
<TextView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:textSize="@dimen/text_size_title"
    android:padding="@dimen/margin_medium"
    android:layout_margin="@dimen/margin_small"
    android:text="@string/welcome_message" />
```

## 3. Cores (`colors.xml`)

Define a paleta de cores do seu aplicativo. Com o Material 3, as cores são frequentemente definidas dentro do arquivo de tema, mas você ainda pode definir cores personalizadas em `colors.xml`.

**Localização:** `res/values/colors.xml`

### Exemplo

```xml
<!-- res/values/colors.xml -->
<resources>
    <color name="brand_primary">#6200EE</color>
    <color name="brand_secondary">#03DAC5</color>
    <color name="white">#FFFFFFFF</color>
    <color name="black">#FF000000</color>
</resources>
```

### Como Usar

```xml
<TextView
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="Título Colorido"
    android:textColor="@color/brand_primary"
    android:background="@color/white" />
```

## 4. Drawables

Drawables são recursos gráficos que podem ser desenhados na tela. Eles podem ser bitmaps (imagens .png, .jpg), vetores (XML) ou formas (XML).

**Localização:** `res/drawable/`

### Vetores (Vector Drawables)

São definidos em XML e são ideais para ícones, pois podem ser redimensionados sem perda de qualidade.

**Exemplo:** `res/drawable/ic_home.xml`

```xml
<!-- res/drawable/ic_home.xml -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24"
    android:tint="?attr/colorControlNormal">
  <path
      android:fillColor="@android:color/white"
      android:pathData="M10,20v-6h4v6h5v-8h3L12,3 2,12h3v8z"/>
</vector>
```

### Formas (Shape Drawables)

Permitem criar formas geométricas simples, com cores sólidas, gradientes e bordas, diretamente em XML. São ótimos para fundos de botões e layouts.

**Exemplo:** `res/drawable/button_background.xml`

```xml
<!-- res/drawable/button_background.xml -->
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <!-- Cor de fundo -->
    <solid android:color="@color/brand_primary" />
    <!-- Cantos arredondados -->
    <corners android:radius="8dp" />
    <!-- Borda (opcional) -->
    <stroke
        android:width="2dp"
        android:color="@color/brand_secondary" />
</shape>
```

### Como Usar Drawables

```xml
<!-- Usando um ícone vetorial em um ImageView -->
<ImageView
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:src="@drawable/ic_home"
    android:contentDescription="Ícone de Início" />

<!-- Usando uma forma como fundo de um botão -->
<Button
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:text="Clique Aqui"
    android:background="@drawable/button_background"
    android:textColor="@color/white" />
```

## 5. Estilos (`styles.xml`)

Um estilo é uma coleção de atributos que especificam a aparência de uma única `View`. É como uma "classe CSS" para seus componentes de UI, permitindo reutilizar um conjunto de propriedades.

**Localização:** `res/values/styles.xml` (ou `themes.xml`)

### Exemplo

```xml
<!-- res/values/styles.xml -->
<resources>
    <!-- Estilo para um título principal -->
    <style name="TitleText">
        <item name="android:textSize">@dimen/text_size_title</item>
        <item name="android:textColor">?attr/colorPrimary</item>
        <item name="android:textStyle">bold</item>
    </style>

    <!-- Estilo para um botão primário -->
    <style name="PrimaryButton" parent="Widget.Material3.Button">
        <item name="android:backgroundTint">?attr/colorPrimary</item>
        <item name="android:textColor">?attr/colorOnPrimary</item>
        <item name="android:layout_margin">@dimen/margin_medium</item>
    </style>
</resources>
```

### Como Usar

```xml
<TextView
    style="@style/TitleText"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="Meu Título com Estilo" />

<Button
    style="@style/PrimaryButton"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:text="Botão com Estilo" />
```

## 6. Temas (`themes.xml`)

Um tema é um estilo aplicado a uma `Activity` ou a um aplicativo inteiro, em vez de a uma `View` individual. Ele define a aparência padrão de todos os componentes, como a cor da barra de aplicativos e a cor de fundo. O Material 3 fornece temas robustos como base.

**Localização:** `res/values/themes.xml` e `res/values-night/themes.xml` (para o modo noturno)

### Exemplo

```xml
<!-- res/values/themes.xml -->
<resources xmlns:tools="http://schemas.android.com/tools">
    <!-- Tema base do aplicativo (modo claro) -->
    <style name="Theme.PensaCare" parent="Theme.Material3.DayNight.NoActionBar">
        <!-- Cores primárias da marca -->
        <item name="colorPrimary">@color/brand_primary</item>
        <item name="colorPrimaryVariant">@color/brand_primary</item>
        <item name="colorOnPrimary">@color/white</item>
        <!-- Cores secundárias da marca -->
        <item name="colorSecondary">@color/brand_secondary</item>
        <item name="colorSecondaryVariant">@color/brand_secondary</item>
        <item name="colorOnSecondary">@color/black</item>
        <!-- Cor da barra de status -->
        <item name="android:statusBarColor">?attr/colorPrimaryVariant</item>
    </style>
</resources>
```

### Como Aplicar o Tema

O tema é aplicado a todo o aplicativo ou a uma atividade específica no arquivo `AndroidManifest.xml`.

```xml
<!-- AndroidManifest.xml -->
<application
    android:allowBackup="true"
    android:icon="@mipmap/ic_launcher"
    android:label="@string/app_name"
    android:roundIcon="@mipmap/ic_launcher_round"
    android:supportsRtl="true"
    android:theme="@style/Theme.PensaCare"> <!-- Tema aplicado aqui -->

    <activity
        android:name=".MainActivity"
        android:exported="true">
        <!-- ... -->
    </activity>

</application>
```