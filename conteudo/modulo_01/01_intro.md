# Introdução ao Android com Jetpack Compose

Bem-vindo ao primeiro contato com o desenvolvimento Android! Este arquivo é o ponto de partida do curso: aqui você vai entender o que é o Android, por que ele é tão usado, como o Android Studio organiza um projeto, e vai criar (e rodar) seu primeiro aplicativo. Não se preocupe se algum termo parecer estranho agora — vamos explicar cada um deles conforme aparecem, e você vai revisitar esses conceitos várias vezes ao longo do curso.

## O que é o Android

**O que é.** Android é um sistema operacional (o "software base" que controla o hardware) para celulares, tablets e outros dispositivos, criado pelo Google. Pense nele como o Windows do seu computador, mas feito para dispositivos móveis: é a camada que gerencia a tela, o toque, a câmera, as notificações e permite que outros aplicativos (como o seu!) rodem em cima dele.

**Por que isso importa.** Antes de escrever a primeira linha de código, é importante saber onde seu aplicativo vai "morar". O Android define regras (APIs, ciclo de vida, permissões) que todo app precisa seguir. Entender isso desde o início evita a sensação de "por que meu código precisa fazer isso?" mais adiante — várias exigências que parecem burocráticas existem porque o sistema operacional está gerenciando, ao mesmo tempo, dezenas de apps concorrendo por bateria, memória e atenção do usuário.

## Contexto Histórico

O Android foi lançado em 2008. Desde então, evoluiu bastante:

- No começo, os aplicativos eram escritos em **Java** (uma linguagem de programação mais antiga e verbosa) e as telas eram descritas em arquivos **XML** (um formato de marcação parecido com HTML, usado para descrever layouts "na mão").
- Em 2017, o Google adotou **Kotlin** como linguagem oficial recomendada — uma linguagem mais moderna, concisa e mais segura contra um tipo de erro muito comum chamado `NullPointerException` (vamos explicar isso em detalhe no próximo arquivo).
- Mais recentemente, o Google lançou o **Jetpack Compose**, uma forma nova de construir a interface visual do app (telas, botões, listas) escrevendo código Kotlin em vez de XML. É essa abordagem — Kotlin + Jetpack Compose — que este curso usa do início ao fim.

Você não precisa saber Java nem XML de layout para seguir este curso. Eles aparecem aqui só para contexto, porque é comum encontrar tutoriais e projetos antigos que ainda os usam.

### Por que escolher Android?

- **Ampla adoção**: mais de 70% dos smartphones do mundo rodam Android. Isso significa que o que você aprender aqui tem alcance real.
- **Flexibilidade**: o mesmo conhecimento de Android serve para criar apps para celulares, tablets, relógios (wearables), TVs e até carros.
- **Comunidade ativa**: é uma das maiores comunidades de desenvolvedores do mundo, o que significa mais tutoriais, mais respostas no Stack Overflow e mais bibliotecas prontas para usar.

### Comparação com outras plataformas

| Plataforma       | Linguagem Principal | UI Declarativa         | Ecossistema |
|------------------|---------------------|------------------------|-------------|
| Android          | Kotlin              | Jetpack Compose        | Aberto      |
| iOS              | Swift               | SwiftUI                | Fechado     |
| Flutter          | Dart                | Widgets (próprio)      | Multiplataforma |

> **O que significa "UI Declarativa"?** É um jeito de descrever a interface dizendo *"o que"* deve aparecer na tela dado um determinado estado (ex: "se `carregando` for verdadeiro, mostre um círculo de progresso"), em vez de dizer passo a passo *"como"* montar e atualizar a tela manualmente. Vamos ver isso na prática mais adiante — por enquanto, guarde que é uma forma mais simples de pensar sobre telas que mudam com o tempo.

## Desenvolvimento no Android Studio com Jetpack Compose

**O que é.** O **Android Studio** é o programa (chamado de IDE — *Integrated Development Environment*, ou "ambiente de desenvolvimento integrado") que você usa para escrever código, montar o layout, rodar o app em um emulador ou celular real, e depurar erros. É o "canivete suíço" oficial do desenvolvedor Android, mantido pelo próprio Google.

**Por que isso importa.** Sem uma IDE como essa, você teria que compilar código, montar o pacote do aplicativo e instalar no celular manualmente por linha de comando — um processo lento e propenso a erros. O Android Studio automatiza tudo isso com poucos cliques, além de te avisar sobre erros de código antes mesmo de você rodar o app.

### Principais recursos do Jetpack Compose:
- UI declarativa (você descreve o resultado desejado, não os passos manuais).
- Reutilização de componentes (crie uma vez, use em várias telas).
- Integração com Material Design (o guia visual do Google, com botões, cores e tipografia prontos).
- Ferramentas de pré-visualização (você vê a tela sem precisar rodar o app inteiro no emulador).

---

## 1. Visão Rápida da Estrutura de um Projeto

**O que é.** Quando você cria um projeto Android (usando o modelo "Empty Compose Activity"), o Android Studio gera automaticamente uma série de pastas e arquivos. Isso é chamado de **estrutura de projeto**. Por enquanto, você só precisa reconhecer o essencial — vamos nos aprofundar nisso no próximo arquivo do curso.

**Por que isso importa.** Se você não souber onde cada tipo de arquivo deve ficar, vai perder tempo procurando onde editar algo simples, como o nome do app ou uma dependência. Saber a estrutura básica desde já economiza muita frustração.

| Caminho / Arquivo | Para que serve (versão simples) |
|-------------------|---------------------------------|
| `settings.gradle[.kts]` | Lista os módulos do projeto (o principal é `:app`) |
| `gradle/libs.versions.toml` | Onde ficam as versões das bibliotecas, centralizadas em um só lugar |
| `app/build.gradle.kts` | Configura o app: identificador, versão do Android suportada, dependências |
| `app/src/main/AndroidManifest.xml` | Declara qual é a tela inicial do app (Activity) e quais permissões ele precisa |
| `app/src/main/res/` | Guarda recursos como imagens, ícones e textos (strings) |
| `app/src/main/java/` ou `kotlin/` | Onde fica o código Kotlin (telas, lógica, etc.) |

Memorize esta frase: **o Manifest descreve o app, `res/` guarda os recursos visuais, `build.gradle.kts` configura como o app é montado, e o código Kotlin fica em `java/` ou `kotlin/`.**

> **Erro comum:** iniciantes costumam editar o texto de um botão direto na pasta errada (por exemplo, tentando mudar o nome do app dentro do código Kotlin). O nome do app, por exemplo, geralmente fica em `res/values/strings.xml`. Se você não achar algo onde esperava, volte a esta tabela.

---

## 2. Gradle Moderno

**O que é.** O **Gradle** é a ferramenta que compila (transforma código em algo que o celular consegue executar) e monta o seu aplicativo Android. Ele lê arquivos de configuração escritos em Kotlin (por isso a extensão `.gradle.kts`) para saber quais bibliotecas baixar, qual versão do Android mirar, e como empacotar tudo no final.

**Por que isso importa.** Sem o Gradle, você teria que baixar manualmente cada biblioteca (por exemplo, a que desenha botões do Material Design) e configurar o compilador na mão. O Gradle faz isso automaticamente, mas exige que você entenda dois arquivos principais:

- O **catálogo de versões**: `gradle/libs.versions.toml` (facilita atualizar tudo em um só ponto — sem ele, você precisaria trocar o número da versão em vários arquivos diferentes toda vez que atualizasse uma biblioteca).
- O **arquivo do módulo**: `app/build.gradle.kts` (configura o módulo `app` especificamente).

### Exemplo comentado: catálogo de versões

```toml
[versions]
agp = "8.8.0"
kotlin = "2.1.0"
composeBom = "2025.01.00"

[libraries]
# Usando o BOM (Bill of Materials) do Compose para gerenciar versões automaticamente.
# BOM = uma "lista mestre" que garante que todas as bibliotecas do Compose usadas
# no projeto sejam compatíveis entre si, sem você precisar escolher cada versão manualmente.
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
```

### Exemplo comentado: `app/build.gradle.kts`

```kotlin
plugins {
    // "alias" busca o plugin pelo apelido definido no catálogo (libs.versions.toml)
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose) // plugin obrigatório a partir do Kotlin 2.0
}

android {
    namespace = "com.exemplo.app"       // identifica o pacote de recursos (R) deste módulo
    compileSdk = 35                     // versão do Android usada para compilar o app

    defaultConfig {
        applicationId = "com.exemplo.app" // identificador único do app nas lojas
        minSdk = 24                       // versão mínima do Android que o app suporta
        targetSdk = 35                    // versão do Android para a qual o app foi testado/otimizado
        versionCode = 1                   // número interno de versão (incrementa a cada publicação)
        versionName = "1.0"                // versão "visível" para o usuário, ex: "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false // se true, o código é compactado/ofuscado antes de publicar
        }
    }
}

dependencies {
    // Use o BOM para gerenciar automaticamente as versões do Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
}
```

**Anote:** `namespace` (uso interno do módulo) e `applicationId` (o identificador publicado na loja) costumam ser iguais no início do projeto, mas representam coisas diferentes — vamos detalhar essa diferença no próximo arquivo do curso.

### Erros comuns / Pegadinhas

- **Editar a versão em um lugar só e esquecer de outro:** se você não usar o catálogo de versões (`libs.versions.toml`), fica fácil ter duas bibliotecas com versões incompatíveis. Sempre que possível, centralize ali.
- **Confundir `minSdk` com `targetSdk`:** `minSdk` é a versão mínima do Android em que o app *roda*; `targetSdk` é a versão para a qual o app foi *testado e otimizado*. Um `minSdk` muito alto exclui usuários com celulares mais antigos.
- **Esquecer de sincronizar o Gradle:** depois de editar `build.gradle.kts`, o Android Studio precisa "sincronizar" (botão "Sync Now" que aparece no topo). Se você editar e nada acontecer, é provavelmente isso.

---

## 3. Passo a Passo do Primeiro Projeto

Ordem recomendada para criar e validar seu primeiro projeto:

1. Criar o projeto usando o modelo **Empty Compose Activity**.
2. Rodar o app sem alterar nada, só para confirmar que o ambiente (IDE + emulador) está funcionando.
3. Olhar o `AndroidManifest.xml` e o código do layout gerado, mesmo sem entender tudo ainda.
4. Alterar o texto que aparece no Composable (a função que desenha a tela).
5. Adicionar um segundo componente visual, como um `Button` (botão).
6. Testar tanto no emulador (um "celular virtual" simulado no computador) quanto em um dispositivo físico, se tiver um disponível.

Depois disso, você já pode estudar a parte mais detalhada da estrutura de projeto na seção 5 deste arquivo, ou seguir para o próximo arquivo do curso.

---

## 4. Prática: Criando um Hello World

**O que é.** "Hello World" é o nome tradicional dado ao primeiro programa que qualquer pessoa cria ao aprender uma nova linguagem ou plataforma — geralmente só exibe uma mensagem simples na tela. Vamos criar um aplicativo que exibe "Hello World" para validar toda a cadeia: IDE → compilação (build) → emulador.

**Por que isso importa.** Esse exercício simples serve para confirmar que todas as ferramentas estão instaladas e configuradas corretamente antes de você tentar algo mais complexo. Se algo der errado aqui, é melhor descobrir agora do que no meio de um projeto maior.

### Passos Detalhados:

1. **Abra o Android Studio** e crie um novo projeto.
2. Escolha a opção **Empty Compose Activity**.
3. Abra o arquivo `app/src/main/java/com/exemplo/app/MainActivity.kt`. Em vez de colar o código final pronto, vamos construir a Activity em três passos pequenos — cada um acrescenta uma única coisa nova.

#### Passo 1 — a versão mais simples possível

```kotlin
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text

// MainActivity é a "porta de entrada" do app — a primeira tela que o usuário vê.
// ComponentActivity é a classe base que dá suporte ao Jetpack Compose.
class MainActivity : ComponentActivity() {

    // onCreate é chamado automaticamente pelo Android quando a tela é criada.
    // Vamos explicar o ciclo de vida completo (onCreate, onStart, etc.) no arquivo 04.
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState) // sempre chame a versão da classe-mãe primeiro

        // setContent define QUAL interface Compose será exibida nesta Activity
        setContent {
            Text("Hello World!") // o texto que o usuário vai ver
        }
    }
}
```

Rode o app: ele já funciona! Mas repare que o texto aparece "cru" no canto superior esquerdo, sem cor de fundo nem estilo — essa versão ainda não usa o tema visual do Material Design.

#### Passo 2 — aplicando o tema do Material Design

Troque apenas o conteúdo de `setContent { }` por:

```kotlin
setContent {
    MaterialTheme {
        // Surface preenche o fundo com a cor do tema, em vez de deixar transparente
        Surface(color = MaterialTheme.colorScheme.background) {
            Text("Hello World!")
        }
    }
}
```

(Adicione os imports `androidx.compose.material3.MaterialTheme` e `androidx.compose.material3.Surface`.) Agora o texto já respeita as cores do tema — mas continua "colado" no canto, sem preencher a tela nem estar centralizado.

#### Passo 3 — preenchendo a tela e centralizando o conteúdo

```kotlin
setContent {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(), // ocupa toda a tela disponível
            color = MaterialTheme.colorScheme.background
        ) {
            Box(contentAlignment = Alignment.Center) { // centraliza o conteúdo dentro dele
                Text("Hello World!")
            }
        }
    }
}
```

Imports adicionais deste passo: `androidx.compose.foundation.layout.Box`, `androidx.compose.foundation.layout.fillMaxSize`, `androidx.compose.ui.Alignment`, `androidx.compose.ui.Modifier`. Essa é a versão final: `Surface` agora ocupa a tela inteira (`fillMaxSize`) e `Box` centraliza o texto dentro dela.

4. Execute o aplicativo clicando no botão **Run** (▶). Se o app abrir no emulador exibindo "Hello World!", você concluiu o ciclo básico com sucesso.

### Explicando o Layout

O `Text("Hello World!")` é um **Composable** — uma função Kotlin marcada com `@Composable` (você vai ver esse detalhe no arquivo 07) que descreve um pedaço da interface visual. A Activity usa `setContent { ... }` para dizer ao Android "desenhe esta árvore de Composables nesta tela".

### Erros comuns / Pegadinhas

- **Esquecer o `super.onCreate(savedInstanceState)`:** essa chamada é obrigatória — sem ela, o app trava (crash) logo ao abrir.
- **Colocar código fora do `setContent { }`:** qualquer Composable (como `Text`, `Button`) só pode ser chamado dentro de outro Composable ou dentro do bloco passado para `setContent`. Chamar um `Text(...)` direto em `onCreate`, fora do `setContent`, gera erro de compilação.
- **Não rodar o emulador antes:** se nenhum emulador estiver configurado e nenhum celular estiver conectado, o botão Run não terá onde instalar o app. Configure um emulador em *Tools > Device Manager* antes de rodar.

Próximo passo: adicionar um `Button` (botão) e capturar o clique dele no código Kotlin — você vai praticar isso nos próximos arquivos.

---

Pronto! Você criou seu primeiro app Android com uma mensagem de "Hello World" usando Jetpack Compose.

---

## 5. Estrutura Avançada (Aprofundando um Pouco Mais)

Esta seção é opcional na primeira leitura — volte a ela quando já tiver criado alguns projetos e quiser entender detalhes mais avançados da configuração.

1. `settings.gradle.kts` define quais módulos existem no projeto e de onde as bibliotecas são baixadas (repositórios).
2. **Product Flavors** criam variantes do mesmo app (por exemplo, uma versão gratuita e uma versão paga). Deixe esse conceito para depois — só é necessário quando o projeto realmente precisar de versões diferentes.
3. **Build Types** combinam com flavors para gerar múltiplos pacotes instaláveis (APKs) a partir do mesmo código-fonte.
4. `kotlin { jvmToolchain(17) }` garante que o código seja compilado de forma consistente usando a versão 17 do Java (o Kotlin roda sobre a máquina virtual Java).
5. Separar as versões no catálogo (`libs.versions.toml`) evita ter que caçar números de versão espalhados pelo projeto quando você for atualizar uma biblioteca.

Snippet com flavors (estude apenas quando já dominar o básico):
```kotlin
android {
    productFlavors {
        create("free") { applicationIdSuffix = ".free"; versionNameSuffix = "-free" }
        create("pro") { applicationIdSuffix = ".pro" }
    }
}
```

### Checklist mental antes de seguir para o próximo arquivo

- [ ] Sei onde editar as dependências do projeto.
- [ ] Sei a diferença entre `namespace` e `applicationId` (mesmo que só de forma geral — vamos aprofundar no arquivo 03).
- [ ] Consigo criar e abrir um arquivo com um Composable.
- [ ] Já rodei o app em um emulador (e, se possível, em um dispositivo físico).

---

## Resumo

- Android é o sistema operacional mais usado do mundo em dispositivos móveis; Kotlin é a linguagem oficial recomendada e Jetpack Compose é a forma moderna de construir telas.
- O Android Studio é a IDE oficial: cria projetos, compila código, mostra pré-visualizações e roda o app em emuladores ou dispositivos físicos.
- Todo projeto tem uma estrutura padrão: `AndroidManifest.xml` descreve o app, `res/` guarda recursos visuais, `build.gradle.kts` configura a compilação e o código Kotlin fica em `java/`/`kotlin/`.
- O Gradle é a ferramenta de build; o catálogo de versões (`libs.versions.toml`) centraliza as versões das bibliotecas usadas.
- Você já criou e rodou seu primeiro app "Hello World" usando `ComponentActivity` e `setContent { }`.

**Próximo passo:** no arquivo 02, você vai aprender a linguagem Kotlin do zero — variáveis, funções, null safety e outros fundamentos que vai usar em todo o restante do curso.
