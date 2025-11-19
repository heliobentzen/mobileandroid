# Introdução ao Android com Jetpack Compose

O Android é um sistema operacional móvel desenvolvido pelo Google, amplamente utilizado em smartphones e tablets. Ele oferece uma plataforma aberta para desenvolvedores criarem aplicativos inovadores, utilizando a linguagem de programação Kotlin.

## Desenvolvimento no Android Studio com Jetpack Compose

O Android Studio é o ambiente de desenvolvimento oficial para criar aplicativos Android. Com a introdução do Jetpack Compose, a criação de interfaces se tornou mais intuitiva e declarativa.

### Principais recursos do Jetpack Compose:
- UI declarativa
- Reutilização de componentes
- Integração com Material Design
- Ferramentas de pré-visualização

---

## 1. Visão Rápida da Estrutura de um Projeto
Quando você cria um projeto (Empty Compose Activity), o Android Studio gera pastas e arquivos. Entenda apenas o essencial agora:

| Caminho / Arquivo | Para que serve (versão simples) |
|-------------------|---------------------------------|
| `settings.gradle[.kts]` | Lista módulos do projeto (`:app`) |
| `gradle/libs.versions.toml` | Onde ficam versões centralizadas (Gradle moderno) |
| `app/build.gradle.kts` | Configura app: id, sdk, dependências |
| `app/src/main/AndroidManifest.xml` | Declara Activity inicial, permissões |
| `app/src/main/res/` | Recursos como imagens e strings |
| `app/src/main/java/` ou `kotlin/` | Código Kotlin (Composables, etc.) |

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
compose = "1.4.0"

[libraries]
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui", version.ref = "compose" }
androidx-compose-material = { group = "androidx.compose.material", name = "material", version.ref = "compose" }

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
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material)
}
```

Anote: `namespace` (interno) e `applicationId` (publicado) normalmente iguais no início.

---

## 3. Passo a Passo do Primeiro Projeto
Ordem recomendada:
1. Criar projeto (Empty Compose Activity)
2. Rodar sem alterações para validar ambiente
3. Olhar Manifest e layout gerado
4. Alterar texto no Composable
5. Adicionar um segundo componente (ex: Button)
6. Testar emulador + dispositivo físico

Depois disso você já pode estudar a parte detalhada da estrutura (seção avançada mais adiante) e ir refinando.

---

## 4. Prática: Criando um Hello World

Vamos criar um aplicativo simples que exibe "Hello World" na tela para validar toda a cadeia (IDE → build → emulador).

### Passos Detalhados:

1. **Abra o Android Studio** e crie um novo projeto.
2. Escolha a opção **Empty Compose Activity**.
3. Abra o arquivo `app/src/main/java/com/exemplo/app/MainActivity.kt` e substitua o conteúdo por:

    ```kotlin
    setContent {
        MaterialTheme {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
                Text("Hello World!", modifier = Modifier.align(Alignment.Center))
            }
        }
    }
    ```

4. Execute o aplicativo (botão Run ▶). Se abrir no emulador, você concluiu o ciclo básico.

### Explicando o Layout
Esse `Text` é um componente simples. A Activity usa `setContent { ... }` para inflar esse Composable.

Próximo passo: adicionar um `Button` e capturar clique no código Kotlin.

---

Pronto! Você criou seu primeiro app Android com uma mensagem de "Hello World" usando Jetpack Compose.

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
- Consigo criar e abrir um layout Composable
- Rodar app em emulador e device físico

---