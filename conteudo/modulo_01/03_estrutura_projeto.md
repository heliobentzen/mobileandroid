# Estrutura de Projeto Android

Todo projeto Android de qualidade precisa de uma organização clara de arquivos e pastas. Sem uma estrutura bem definida, à medida que o projeto cresce fica cada vez mais difícil entender onde cada coisa está, escrever testes e adicionar novas funcionalidades sem quebrar o que já existe.

Nesta seção, você vai aprender como estruturar um projeto Android de forma modular e escalável, seguindo as boas práticas recomendadas pelo Google. Se você seguiu o arquivo 01 e já criou seu primeiro "Hello World", este arquivo aprofunda o que você viu de forma superficial ali.

---

## 1. Por que organizar?

**O que é.** "Organizar um projeto" significa decidir, de forma consciente, onde cada tipo de código vai morar — em vez de deixar tudo em um único arquivo gigante ou em pastas sem critério. No Android, essa organização normalmente é feita através de **módulos**: pedaços independentes do projeto que podem ser compilados separadamente.

**Por que isso importa.** Quando um projeto nasce sem organização, alguns problemas aparecem rapidamente, especialmente conforme mais pessoas trabalham nele ou o app cresce:

- **Encontrar bugs fica difícil** — sem separação clara, você não sabe se o problema está na tela, na regra de negócio ou na chamada de rede. Se tudo está misturado em um só arquivo, até um bug simples exige ler centenas de linhas.
- **Conflitos entre desenvolvedores** — se toda a lógica vive em poucos arquivos grandes, duas pessoas editando ao mesmo tempo vão gerar conflitos constantes no Git (o sistema de controle de versão usado para colaborar em código).
- **Testes isolados são impossíveis** — testar a lógica de login sem carregar o banco de dados inteiro exige que essas partes estejam em módulos separados, com pouca dependência entre si.
- **Builds lentos** — o Gradle (a ferramenta de compilação que você viu no arquivo 01) só recompila módulos que mudaram. Em um projeto com um único módulo gigante, qualquer alteração pequena obriga a recompilar tudo, deixando o desenvolvimento mais lento.

A regra geral é: **separe por responsabilidade**. Cada módulo faz uma coisa, e faz bem.

> Como iniciante, você provavelmente vai começar seus primeiros projetos com um único módulo (`app`) — e está tudo bem. Esta seção existe para você já reconhecer os nomes e conceitos quando vir projetos maiores, e para você entender o "porquê" por trás da modularização quando chegar a hora de aplicá-la.

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

> **Dica:** comece com `app/` + um ou dois módulos `core-`. Adicione módulos de `feature-` e `design-system` conforme o projeto crescer. Não tente modularizar tudo desde o primeiro dia — isso adiciona complexidade que você ainda não precisa como iniciante.

### Erros comuns / Pegadinhas

- **Modularizar cedo demais:** iniciantes às vezes tentam criar 10 módulos em um projeto pequeno de aprendizado. Isso só adiciona complexidade sem benefício real. Modularize quando sentir a dor descrita acima (builds lentos, conflitos de Git, dificuldade de testar).
- **Colocar código Android em `core-domain`:** esse módulo deveria ser Kotlin puro (sem `import android.*`), justamente para compilar rápido e ser fácil de testar sem simular o Android inteiro.

---

## 2. Gradle básico

**O que é.** Como você já viu no arquivo 01, o **Gradle** é a ferramenta que compila e monta seu app. Aqui vamos ver como ele se aplica quando o projeto tem vários módulos.

Use **Kotlin DSL** (arquivos `build.gradle.kts`, escritos em Kotlin) e centralize as versões das bibliotecas no **Version Catalog** (`libs.versions.toml`) — um arquivo único que lista todas as versões usadas no projeto, evitando duplicação.

### Registrando os módulos — `settings.gradle.kts`

**Por que isso importa.** O Gradle só compila os módulos que estiverem explicitamente listados neste arquivo. Se você criar uma pasta de módulo nova mas esquecer de declará-la aqui com `include(...)`, o Android Studio simplesmente vai ignorá-la.

```kotlin
// settings.gradle.kts (na raiz do projeto)
pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
// pluginManagement define ONDE o Gradle procura os plugins usados no build.

dependencyResolutionManagement { repositories { google(); mavenCentral() } }
// Define onde o Gradle procura as DEPENDÊNCIAS (bibliotecas) do projeto.
// google() e mavenCentral() são os dois repositórios públicos mais usados
// no ecossistema Android/Kotlin.

rootProject.name = "MeuApp" // nome do projeto (aparece no Android Studio)

// Cada 'include' registra um módulo que o Gradle deve compilar
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
    alias(libs.plugins.android.library) // "android.library" (não "application"!)
    // — módulos que não são o app principal usam o plugin de biblioteca,
    // pois não geram um app instalável sozinhos, apenas código reutilizável.
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.exemplo.core.network" // pacote de recursos deste módulo
    compileSdk = 35
    defaultConfig { minSdk = 24 }
    // repare: NÃO há applicationId aqui — só o módulo 'app' define isso.
}

dependencies {
    implementation(libs.retrofit)
    implementation(libs.okhttp.logging)
    implementation(project(":core-common"))
    // 'project(":core-common")' é como um módulo depende de outro módulo
    // do MESMO projeto (em vez de uma biblioteca externa).
}
```

Outras boas práticas do Gradle:

- **Ative cache e paralelismo** em `gradle.properties`: `org.gradle.caching=true` e `org.gradle.parallel=true`. Isso acelera builds reaproveitando resultados de compilações anteriores e compilando módulos independentes ao mesmo tempo.
- **Declare dependências de forma explícita** — cada módulo lista apenas o que ele próprio usa. Evite depender de bibliotecas "porque outro módulo já usa" — isso cria acoplamento invisível.

### Erros comuns / Pegadinhas

- **Esquecer o `include(":nome-do-modulo")` depois de criar a pasta:** o módulo simplesmente não aparece no projeto até você adicionar essa linha e sincronizar o Gradle.
- **Colocar `applicationId` em um módulo library:** isso gera erro de build — `applicationId` só é válido no plugin `com.android.application` (usado pelo módulo `app`), não em `com.android.library`.

---

## 3. Namespace vs applicationId

Esses dois conceitos causam confusão, mas a diferença é simples:

- **`namespace`** — identifica o **pacote R** (o conjunto de recursos gerados automaticamente, como IDs de layouts e strings) de cada módulo. **Todo** módulo Android precisa de um `namespace`, mesmo os que não são o app principal.
- **`applicationId`** — identifica o app de forma única nas lojas (como a Google Play). **Só** o módulo `app` define isso, porque é ele quem é publicado.

**Por que isso importa.** Sem entender essa diferença, é fácil ficar confuso ao ver um erro de build dizendo "applicationId não encontrado" em um módulo `feature-` ou `core-` — a resposta é que esses módulos não deveriam ter esse campo mesmo.

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

### Erros comuns / Pegadinhas

- **Achar que `namespace` e `applicationId` sempre precisam ser iguais:** no início do projeto geralmente coincidem (como você viu no arquivo 01), mas em projetos com múltiplos módulos, cada módulo tem seu próprio `namespace`, todos diferentes do `applicationId` único do app.
- **Trocar o `applicationId` depois de já ter publicado o app:** isso faz a loja tratar como um app **completamente novo** (perdendo avaliações, instalações, etc.). Escolha esse identificador com cuidado desde o início.

---

## 4. Variantes (quando preciso)

**O que é.** O Android permite criar diferentes "versões" do mesmo app usando **Build Types** e **Product Flavors** — dois eixos independentes que se combinam para gerar variantes finais do app.

- **Build Types** controlam *como* o app é compilado (voltado para desenvolvimento vs voltado para publicação).
  - `debug` = com logs habilitados, sem ofuscação de código, assinado com uma chave de teste.
  - `release` = sem logs de debug, com ofuscação (usando ferramentas como R8/ProGuard, que tornam o código mais difícil de ler caso alguém tente descompilar o app), assinado com a chave de produção.
- **Product Flavors** controlam *o que* o app contém (edições diferentes do mesmo produto).
  - Exemplo: versão `free` (com anúncios) vs versão `pro` (sem anúncios, com mais funcionalidades).

> **Analogia:** Build Type é como escolher entre *rascunho* e *versão final* de um documento — o conteúdo é parecido, mas o tratamento é diferente. Flavor é como criar edições diferentes do mesmo livro — *edição estudante* vs *edição profissional* — com conteúdo real distinto.

**Por que isso importa.** Sem Build Types, você teria que trocar manualmente configurações de debug/produção toda vez que fosse testar ou publicar — um processo arriscado e sujeito a erro humano (esquecer de desligar logs de debug na versão publicada, por exemplo).

**Use Flavors apenas quando houver diferença real** no comportamento ou conteúdo do app (ex: URL de API diferente, funcionalidades habilitadas/desabilitadas). Se o app é único, não crie flavors — eles adicionam complexidade desnecessária.

#### Passo 1 — declarando os flavors

```kotlin
android {
    flavorDimensions += "tier"
    // 'flavorDimensions' agrupa os flavors em uma categoria — aqui, "tier"
    // representa o "nível" do produto (free vs pro).
    productFlavors {
        create("free") { dimension = "tier"; applicationIdSuffix = ".free" }
        // applicationIdSuffix ".free" gera um applicationId final como
        // "com.exemplo.app.free" — permite instalar free e pro no mesmo device.
        create("pro")  { dimension = "tier" }
    }
}
```

Isso já gera duas variantes: `freeDebug` e `proDebug` (o `debug` é o build type padrão). Mas ainda não configuramos como o app se comporta ao ser publicado.

#### Passo 2 — combinando com os build types

```kotlin
android {
    // ...flavors do passo 1 continuam aqui...
    buildTypes {
        getByName("debug") { isMinifyEnabled = false }
        getByName("release") { isMinifyEnabled = true }
    }
}
```

Agora o Gradle combina os dois eixos (flavor × build type) e gera 4 variantes: `freeDebug`, `freeRelease`, `proDebug` e `proRelease`. Evite criar muitas variantes no início — cada combinação pode precisar de diretórios próprios de código-fonte (`src/freeDebug/`), o que complica o projeto rapidamente.

### Erros comuns / Pegadinhas

- **Criar flavors "só porque parece profissional":** cada flavor multiplica o tempo de build e a complexidade do projeto. Só crie quando o app realmente precisar de conteúdo/comportamento diferente entre versões.
- **Esquecer o `applicationIdSuffix`:** sem ele, `free` e `pro` teriam o mesmo `applicationId`, e você não conseguiria instalar as duas versões no mesmo dispositivo para testar lado a lado.

---

## 5. Boas práticas iniciais

- **Extraia código sem dependência do Android** para módulos Kotlin puros (como `core-domain`) — eles compilam mais rápido (não dependem do SDK Android) e são mais fáceis de testar, porque não exigem simular componentes do Android nos testes.
- **Menos flavors = build mais rápido** — cada flavor multiplica o número de variantes que o Gradle precisa gerenciar.
- **Use `implementation` como padrão** ao declarar dependências — só troque para `api` quando um módulo precisar expor uma dependência para quem o consome. A diferença: `implementation` mantém a dependência "privada" ao módulo (compila mais rápido, pois mudanças nela não forçam recompilar quem depende do seu módulo); `api` "vaza" a dependência para fora.
- **Documente no README** o que cada módulo faz e quais são suas dependências diretas — isso ajuda quem chega depois (inclusive você mesmo, daqui a alguns meses).

### Checklist rápido

- [ ] Namespace definido em cada módulo?
- [ ] Version Catalog (`libs.versions.toml`) ativo?
- [ ] Flavors são realmente necessários?
- [ ] Cache e paralelismo do Gradle ligados?
- [ ] Diferença entre `applicationId` e `namespace` está clara para o time?

## Resumo

- Organizar um projeto em módulos com responsabilidades claras facilita achar bugs, evitar conflitos de Git, testar isoladamente e acelerar builds.
- `settings.gradle.kts` registra quais módulos existem (via `include(...)`); cada módulo tem seu próprio `build.gradle.kts`.
- `namespace` é obrigatório em todo módulo (identifica recursos); `applicationId` só existe no módulo `app` (identifica o app publicado).
- Build Types (`debug`/`release`) mudam *como* o app é compilado; Product Flavors (`free`/`pro`) mudam *o que* o app contém — use ambos com moderação.
- Prefira `implementation` a `api` nas dependências, e extraia código sem dependência do Android para módulos Kotlin puros quando possível.

## Próximos passos

Os tópicos de camadas (data/domain/presentation), rede (Retrofit), persistência (Room), Repository, ViewModel e DI (Hilt) serão aprofundados nos módulos seguintes:

- **Módulo 2**: Arquitetura MVVM, ViewModel e fluxo de dados.
- **Módulo 3**: Coroutines, Retrofit, Room e Repository Pattern.

**Próximo passo imediato:** no arquivo 04, você vai estudar a Activity — o componente que serve de porta de entrada para a interface do seu app — e seu ciclo de vida.
