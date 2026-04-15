# Estrutura de Projeto Android

Todo projeto Android de qualidade precisa de uma organização clara de arquivos e pastas. Sem uma estrutura bem definida, à medida que o projeto cresce fica cada vez mais difícil entender onde cada coisa está, escrever testes e adicionar novas funcionalidades sem quebrar o que já existe.

Nesta seção, você vai aprender como estruturar um projeto Android de forma modular e escalável, seguindo as boas práticas recomendadas pelo Google.

---

## 1. Por que organizar?

Quando um projeto nasce sem organização, alguns problemas aparecem rapidamente:

- **Encontrar bugs fica difícil** — sem separação clara, você não sabe se o problema está na tela, na regra de negócio ou na chamada de rede.
- **Conflitos entre desenvolvedores** — se toda a lógica vive em poucos arquivos grandes, duas pessoas editando ao mesmo tempo vão gerar conflitos constantes no Git.
- **Testes isolados são impossíveis** — testar a lógica de login sem carregar o banco de dados inteiro exige que essas partes estejam em módulos separados.
- **Builds lentos** — o Gradle só recompila módulos que mudaram. Em um projeto com módulo único, qualquer alteração recompila tudo.

A regra geral é: **separe por responsabilidade**. Cada módulo faz uma coisa, e faz bem.

### Sugestão de estrutura modular

```
app/              ← OBRIGATÓRIO — ponto de entrada, DI raiz e navegação
core-common/      ← Recomendado — extensões, constantes e utilidades gerais
core-network/     ← Recomendado — configuração do Retrofit e interceptors
core-database/    ← Recomendado — entidades e DAOs do Room
core-domain/      ← Recomendado — use cases e modelos de domínio (sem Android)
feature-login/    ← Opcional — tela, ViewModel e lógica da feature de login
design-system/    ← Opcional — componentes visuais reutilizáveis (botões, temas)
```

| Módulo | Tipo | O que contém |
|---|---|---|
| `app/` | **Obrigatório** | `Application`, injeção de dependências raiz, navegação, `MainActivity`. |
| `core-common/` | Recomendado | Extensões, formatadores de data, constantes compartilhadas. |
| `core-network/` | Recomendado | Retrofit/OkHttp, interceptors de auth e logging. |
| `core-database/` | Recomendado | `RoomDatabase`, entidades `@Entity` e interfaces `@Dao`. |
| `core-domain/` | Recomendado | Use cases e modelos puros Kotlin (sem dependência do Android). |
| `feature-login/` | Opcional | Telas, ViewModel e repositório específicos do login. |
| `design-system/` | Opcional | Tema, componentes visuais customizados, tokens de cor e tipografia. |

> **Dica:** comece com `app/` + um ou dois módulos `core-`. Adicione módulos de `feature-` e `design-system` conforme o projeto crescer.

## 2. Gradle básico

Use **Kotlin DSL** (`build.gradle.kts`) e centralize versões no **Version Catalog** (`libs.versions.toml`).

### Registrando os módulos — `settings.gradle.kts`

```kotlin
// settings.gradle.kts (na raiz do projeto)
pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement { repositories { google(); mavenCentral() } }

rootProject.name = "MeuApp"

include(":app")
include(":core-common")
include(":core-network")
include(":core-domain")
include(":feature-login")
```

### Exemplo de `build.gradle.kts` de um módulo library

```kotlin
// core-network/build.gradle.kts
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.exemplo.core.network"
    compileSdk = 35
    defaultConfig { minSdk = 24 }
}

dependencies {
    implementation(libs.retrofit)
    implementation(libs.okhttp.logging)
    implementation(project(":core-common"))  // dependência entre módulos
}
```

Outras boas práticas do Gradle:

- **Ative cache e paralelismo** em `gradle.properties`: `org.gradle.caching=true` e `org.gradle.parallel=true`.
- **Declare dependências de forma explícita** — cada módulo lista apenas o que ele próprio usa.

## 3. Namespace vs applicationId

Esses dois conceitos causam confusão, mas a diferença é simples:

- **`namespace`** — identifica o pacote R (recursos) de cada módulo. Todo módulo Android precisa de um.
- **`applicationId`** — identifica o app nas lojas (Google Play). Só o módulo `app` define isso.

Veja lado a lado:

```kotlin
// app/build.gradle.kts — TEM applicationId
android {
    namespace = "com.exemplo.app"                    // pacote R do módulo
    defaultConfig { applicationId = "com.exemplo.app" }  // ID na Play Store
}

// feature-login/build.gradle.kts — NÃO tem applicationId
android {
    namespace = "com.exemplo.feature.login"  // pacote R deste módulo
    // ⚠️ Só o módulo app define applicationId
}
```

**Padrão sugerido para namespaces:** `com.empresa.<camada>.<nome>`

- `com.exemplo.core.network`
- `com.exemplo.feature.login`
- `com.exemplo.design.system`

## 4. Variantes (quando preciso)

O Android permite criar diferentes "versões" do mesmo app usando **Build Types** e **Product Flavors**:

- **Build Types** controlam *como* o app é compilado (desenvolvimento vs publicação).
  - `debug` = com logs, sem ofuscação, assinatura de debug.
  - `release` = sem logs, com ofuscação (R8/ProGuard), assinatura de produção.
- **Product Flavors** controlam *o que* o app contém (edições diferentes do produto).
  - Exemplo: versão `free` (com anúncios) vs versão `pro` (sem anúncios).

> **Analogia:** Build Type é como escolher entre *rascunho* e *versão final* de um documento. Flavor é como criar edições diferentes — *edição estudante* vs *edição profissional*.

**Use Flavors apenas quando houver diferença real** (ex: URL de API, features habilitadas). Se o app é único, não crie flavors.

Exemplo mínimo com ambos:

```kotlin
android {
    flavorDimensions += "tier"
    productFlavors {
        create("free") { dimension = "tier"; applicationIdSuffix = ".free" }
        create("pro")  { dimension = "tier" }
    }
    buildTypes {
        getByName("debug") { isMinifyEnabled = false }
        getByName("release") { isMinifyEnabled = true }
    }
}
```

Isso gera 4 variantes: `freeDebug`, `freeRelease`, `proDebug` e `proRelease`. Evite criar muitas variantes no início — cada combinação pode precisar de diretórios próprios (`src/freeDebug/`), o que complica o projeto.

## 5. Boas práticas iniciais

- **Extraia código sem dependência do Android** para módulos Kotlin puros — eles compilam mais rápido e são mais fáceis de testar.
- **Menos flavors = build mais rápido** — cada flavor multiplica o número de variantes.
- **Use `implementation` como padrão** — só troque para `api` quando um módulo precisar expor uma dependência para quem o consome.
- **Documente no README** o que cada módulo faz e quais são suas dependências diretas.

### Checklist rápido

- [ ] Namespace definido em cada módulo?
- [ ] Version Catalog (`libs.versions.toml`) ativo?
- [ ] Flavors são realmente necessários?
- [ ] Cache e paralelismo do Gradle ligados?
- [ ] Diferença entre `applicationId` e `namespace` está clara para o time?

## Resumo

- Separe responsabilidades cedo — bugs ficam mais fáceis de encontrar e corrigir.
- Mantenha variantes e módulos no mínimo necessário.
- Centralize dependências com Version Catalog.
- Adicione complexidade (flavors, módulos extras) só quando houver necessidade real.

## Próximos passos

Os tópicos de camadas (data/domain/presentation), rede (Retrofit), persistência (Room), Repository, ViewModel e DI (Hilt) serão aprofundados nos módulos seguintes:

- **Módulo 2**: Arquitetura MVVM, ViewModel e fluxo de dados.
- **Módulo 3**: Coroutines, Retrofit, Room e Repository Pattern.

