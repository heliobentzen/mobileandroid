# Introdução ao Android

O Android é um sistema operacional móvel desenvolvido pelo Google, amplamente utilizado em smartphones e tablets. Ele oferece uma plataforma aberta para desenvolvedores criarem aplicativos inovadores, utilizando a linguagem de programação Kotlin ou Java.

## Desenvolvimento no Android Studio

O Android Studio é o ambiente de desenvolvimento oficial para criar aplicativos Android. Ele fornece ferramentas integradas para design de interface, depuração, testes e publicação de apps.

### Principais recursos do Android Studio:
- Editor de código inteligente
- Emulador de dispositivos Android
- Gerenciamento de dependências
- Ferramentas de análise de desempenho

---

## 1. Visão Rápida da Estrutura de um Projeto
Quando você cria um projeto (Empty Activity), o Android Studio gera pastas e arquivos. Entenda apenas o essencial agora:

| Caminho / Arquivo | Para que serve (versão simples) |
|-------------------|---------------------------------|
| `settings.gradle[.kts]` | Lista módulos do projeto (`:app`) |
| `gradle/libs.versions.toml` | Onde ficam versões centralizadas (Gradle moderno) |
| `app/build.gradle.kts` | Configura app: id, sdk, dependências |
| `app/src/main/AndroidManifest.xml` | Declara Activity inicial, permissões |
| `app/src/main/res/layout/` | Layouts XML das telas |
| `app/src/main/res/values/` | Strings, cores, dimens, temas |
| `app/src/main/java/` ou `kotlin/` | Código Kotlin (Activities, etc.) |

Memorize: Manifest descreve, `res/` guarda recursos, `build.gradle.kts` configura, código fica em `java/` ou `kotlin/`.

---

## 2. Gradle Moderno
Você verá dois lugares principais de configuração:
- Catálogo de versões: `gradle/libs.versions.toml` (facilita atualizar tudo num só ponto)
- Arquivo do módulo: `app/build.gradle.kts`

Exemplo mínimo de dependências usando catálogo:
```toml
[versions]
agp = "8.5.1"
kotlin = "2.0.0"
appcompat = "1.7.0"
material = "1.12.0"

[libraries]
androidx-appcompat = { group = "androidx.appcompat", name = "appcompat", version.ref = "appcompat" }
google-material = { group = "com.google.android.material", name = "material", version.ref = "material" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
```

E no `app/build.gradle.kts` você usa aliases:
```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.exemplo.app"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.exemplo.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
    buildTypes {
        release { isMinifyEnabled = false }
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.google.material)
}
```

Anote: `namespace` (interno) e `applicationId` (publicado) normalmente iguais no início.

---

## 3. Passo a Passo do Primeiro Projeto
Ordem recomendada:
1. Criar projeto (Empty Activity)
2. Rodar sem alterações para validar ambiente
3. Olhar Manifest e layout gerado
4. Alterar texto no XML
5. Adicionar um segundo componente (ex: Button)
6. Testar emulador + dispositivo físico

Depois disso você já pode estudar a parte detalhada da estrutura (seção avançada mais adiante) e ir refinando.

---

## 4. Prática: Criando um Hello World

Vamos criar um aplicativo simples que exibe "Hello World" na tela para validar toda a cadeia (IDE → build → emulador).

### Passos Detalhados:

1. **Abra o Android Studio** e crie um novo projeto.
2. Escolha a opção **Empty Activity**.
3. Abra o arquivo `app/src/main/res/layout/activity_main.xml` e substitua o conteúdo por:

    ```xml
    <TextView
         android:layout_width="wrap_content"
         android:layout_height="wrap_content"
         android:text="Hello World!"
         android:layout_gravity="center"/>
    ```

4. Execute o aplicativo (botão Run ▶). Se abrir no emulador, você concluiu o ciclo básico.

### Explicando o Layout
Esse `TextView` é um widget simples. A Activity usa `setContentView(R.layout.activity_main)` (gerado automaticamente no template) para inflar esse XML.

Próximo passo: adicionar um `Button` e capturar clique no código Kotlin.

---

Pronto! Você criou seu primeiro app Android com uma mensagem de "Hello World".

---

## 5. Estrutura Avançada (Aprofundando um Pouco Mais)
Após o primeiro contato, aprofunde:
1. `settings.gradle.kts` define módulos e repositórios.
2. Product Flavors criam variantes (ex: free vs pro) – adie até precisar.
3. Build Types combinam com flavors gerando múltiplos APKs.
4. `kotlin { jvmToolchain(17) }` garante compilação consistente em Java 17.
5. Separar versões no catálogo evita “caça a número” quando atualizar libs.

Snippet com flavors (apenas quando dominar o básico):
```kotlin
android {
    productFlavors {
        create("free") { applicationIdSuffix = ".free"; versionNameSuffix = "-free" }
        create("pro") { applicationIdSuffix = ".pro" }
    }
}
```

Checklist mental antes de seguir para arquitetura:
- Sei onde editar dependências
- Sei diferença namespace vs applicationId
- Consigo criar e abrir um layout XML
- Rodar app em emulador e device físico

---