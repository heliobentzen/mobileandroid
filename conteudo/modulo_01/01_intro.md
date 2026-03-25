# Introdução ao Android com Jetpack Compose

O Android é um sistema operacional móvel desenvolvido pelo Google, amplamente utilizado em smartphones e tablets. Ele oferece uma plataforma aberta para desenvolvedores criarem aplicativos inovadores, utilizando a linguagem de programação Kotlin.

## Contexto Histórico

O Android foi lançado em 2008 e, desde então, evoluiu significativamente. Inicialmente, o desenvolvimento era focado em XML para layouts e Java como linguagem principal. Com o tempo, o Google introduziu o Kotlin como linguagem oficial e o Jetpack Compose como uma abordagem moderna para criar interfaces de usuário de forma declarativa.

### Por que escolher Android?
- **Ampla adoção**: Mais de 70% dos dispositivos móveis utilizam Android.
- **Flexibilidade**: Suporte para uma ampla gama de dispositivos, incluindo wearables, TVs e carros.
- **Comunidade ativa**: Uma das maiores comunidades de desenvolvedores do mundo.

### Comparação com outras plataformas
| Plataforma       | Linguagem Principal | UI Declarativa         | Ecossistema |
|------------------|---------------------|------------------------|-------------|
| Android          | Kotlin              | Jetpack Compose        | Aberto      |
| iOS              | Swift               | SwiftUI                | Fechado     |
| Flutter          | Dart                | Widgets (próprio)      | Multiplataforma |

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
agp = "8.8.0"
kotlin = "2.1.0"
composeBom = "2025.01.00"

[libraries]
# Usando o BOM do Compose para gerenciar versões automaticamente
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
```

E no `app/build.gradle.kts` você usa aliases:
```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose) // plugin obrigatório a partir do Kotlin 2.0
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
    // Use o BOM para gerenciar automaticamente as versões do Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
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
    import androidx.activity.ComponentActivity
    import androidx.activity.compose.setContent
    import androidx.compose.foundation.layout.Box
    import androidx.compose.foundation.layout.fillMaxSize
    import androidx.compose.material3.MaterialTheme
    import androidx.compose.material3.Surface
    import androidx.compose.material3.Text
    import androidx.compose.ui.Alignment
    import androidx.compose.ui.Modifier

    class MainActivity : ComponentActivity() {
        override fun onCreate(savedInstanceState: android.os.Bundle?) {
            super.onCreate(savedInstanceState)
            setContent {
                MaterialTheme {
                    // Surface preenche toda a tela com a cor de fundo do tema
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background // Material 3: use colorScheme
                    ) {
                        // Box permite centralizar o conteúdo
                        Box(contentAlignment = Alignment.Center) {
                            Text("Hello World!")
                        }
                    }
                }
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