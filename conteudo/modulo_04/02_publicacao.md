# Módulo 4: Publicação no Google Play

**Objetivo**: preparar o app para distribuição, gerando um Android App Bundle (AAB) assinado, e conhecer o fluxo básico do Google Play Console — o painel onde você gerencia e publica apps na Play Store.

Se você nunca publicou um app antes, este módulo vai te guiar por cada etapa, explicando os termos técnicos (AAB, keystore, trilha, rollout) conforme eles aparecem.

---

## 1. Preparação Antes de Publicar

### O que é

Antes de gerar o build de release (a versão final do app, pronta para os usuários), existe uma lista de itens que precisam estar corretos. Pense nisso como uma checklist de voo antes da decolagem: cada item evita um problema que só apareceria depois de o app já estar no ar.

### Por que isso importa

Publicar um app com um problema nessa lista pode significar: usuários não conseguem instalar a atualização, o app fica gigante e lento para baixar, ou pior — o app vaza dados por causa de uma permissão desnecessária. Corrigir isso *depois* de publicado é bem mais trabalhoso do que checar *antes*.

| Item | Detalhes |
|------|----------|
| **Versão** | `versionCode` incrementado e `versionName` atualizado em `build.gradle.kts` |
| **Ícone** | Ícone adaptativo configurado (`mipmap-anydpi-v26`) |
| **ProGuard/R8** | `isMinifyEnabled = true` no build type `release` para reduzir tamanho e ofuscar |
| **Permissões** | Apenas as necessárias declaradas no `AndroidManifest.xml` |
| **Testes** | Testes unitários e de UI passando (veja o [Módulo 4.01 — Testes](./01_testes.md)) |
| **README** | Instruções de setup e build documentadas |

Alguns termos dessa tabela merecem explicação:

- **`versionCode`**: um número inteiro interno (ex.: `12`) que o Android usa para saber se uma versão é mais nova que outra. Ele **precisa** aumentar a cada release, ou a Play Store rejeita o upload.
- **`versionName`**: o texto que o usuário vê (ex.: `"1.2.0"`). Não afeta a lógica do Android, é só informativo.
- **ProGuard/R8**: ferramentas que reduzem o tamanho do app removendo código não usado e "ofuscam" o código (trocam nomes de classes/métodos por nomes curtos e sem sentido), dificultando engenharia reversa.
- **Ofuscar**: tornar o código mais difícil de ler/entender para quem tentar abrir o APK e examinar seu funcionamento interno.

### Erros comuns / Pegadinhas

- Esquecer de incrementar o `versionCode`: a Play Store rejeita o upload com um erro dizendo que já existe uma versão igual ou mais nova publicada.
- Deixar permissões de teste ou de desenvolvimento (ex.: acesso a localização "só para debugar") declaradas no manifest de produção — isso preocupa usuários e pode reprovar a revisão da Play Store.

---

## 2. Configurando a Assinatura

### O que é

**Assinar um app** é aplicar uma "impressão digital" criptográfica única ao arquivo do app, usando um arquivo especial chamado **keystore** (um "cofre" digital que guarda uma chave privada). É parecido com assinar um contrato: a assinatura prova quem criou aquele documento e garante que ninguém alterou o conteúdo depois.

### Por que assinar o app?

A assinatura digital é obrigatória por três motivos principais:

- **Verificação de identidade**: garante que o app foi publicado por você (ou pela sua organização), e não por um terceiro mal-intencionado.
- **Proteção contra adulteração**: se alguém modificar o APK/AAB depois da assinatura, o sistema detecta que o conteúdo foi alterado e impede a instalação.
- **Autenticidade de atualizações**: o Android só permite atualizar um app instalado se a nova versão tiver a mesma assinatura — isso impede que outra pessoa publique uma atualização falsa do seu app.

Em resumo: sem assinatura, o Google Play rejeita o upload e o Android se recusa a instalar o app. Não existe app publicado sem assinatura — a única escolha é *como* você gerencia essa chave (veja as duas opções abaixo).

### Erros comuns / Pegadinhas

- **Perder a keystore sem ter um backup**: se você usar assinatura local (Opção A) e perder o arquivo `.jks`, você não conseguirá mais publicar atualizações do mesmo app — a única saída seria publicar como um app novo, perdendo avaliações e usuários. Guarde a keystore em um local seguro (gerenciador de senhas, backup em nuvem privado), nunca só no seu computador.
- **Commitar a keystore ou as senhas no Git**: qualquer pessoa com acesso ao repositório poderia assinar uma versão falsa do seu app.

---

O Google Play exige que todo app seja assinado digitalmente. Há dois cenários:

### Opção A: Assinatura Local (Keystore)

#### Passo 1 — gerar a keystore

Gere o arquivo uma única vez e guarde-o com cuidado (perder essa keystore sem backup significa não conseguir mais publicar atualizações do app — veja "Erros comuns" acima):

```bash
keytool -genkeypair \
  -alias meu-app \
  -keyalg RSA -keysize 2048 \
  -validity 10000 \
  -keystore meu-app-release.jks
```

O comando usa o `keytool` (ferramenta que já vem com o JDK) para gerar um par de chaves criptográficas (`RSA`, `2048` bits) válidas por `10000` dias, guardadas em `meu-app-release.jks`. O `-alias` é o "apelido" dessa chave — um keystore pode guardar mais de uma.

#### Passo 2 — a versão mais simples de configuração

O jeito mais direto de ligar a keystore ao build é apontar direto para o arquivo no `build.gradle.kts`, lendo a senha de uma variável de ambiente:

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("meu-app-release.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
            keyAlias = "meu-app"
            keyPassword = System.getenv("KEY_PASSWORD") ?: ""
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

**Limitação**: essa abordagem funciona bem em CI (onde as variáveis de ambiente já existem), mas é incômoda no dia a dia local — cada desenvolvedor precisaria exportar `KEYSTORE_PASSWORD` e `KEY_PASSWORD` na própria máquina toda vez que abrir um terminal novo, e é fácil esquecer.

#### Passo 3 — o padrão de mercado: `keystore.properties`

Na prática, a forma mais comum de guardar essas credenciais em projetos Android é um arquivo `keystore.properties` na raiz do projeto, que cada desenvolvedor cria localmente e que nunca é commitado:

```properties
# keystore.properties — NÃO commitar este arquivo!
storeFile=meu-app-release.jks
storePassword=minhaSenhaSegura
keyAlias=meu-app
keyPassword=minhaSenhaDeChave
```

Adicione a entrada no `.gitignore`:

```gitignore
# Credenciais da keystore
keystore.properties
```

E leia o arquivo no `build.gradle.kts` com a classe `Properties` (padrão do Kotlin/Java):

```kotlin
import java.util.Properties

val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) load(file.inputStream())
}

android {
    signingConfigs {
        create("release") {
            storeFile = file(keystoreProperties.getProperty("storeFile"))
            storePassword = keystoreProperties.getProperty("storePassword")
            keyAlias = keystoreProperties.getProperty("keyAlias")
            keyPassword = keystoreProperties.getProperty("keyPassword")
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

Assim, o repositório permanece livre de credenciais e cada máquina (local ou CI) resolve suas próprias senhas.

### Opção B: Play App Signing (Recomendado)

O Google gerencia a chave de assinatura de produção. O fluxo funciona assim:

1. Você assina o AAB com a sua **chave de upload** (a keystore que você gerou localmente).
2. Faz o upload do AAB assinado para o Google Play Console.
3. O Google **re-assina** o app com a **chave de produção** gerenciada por ele.
4. O usuário final recebe o app assinado com a chave de produção do Google.

**Benefício prático**: se você perder a sua chave de upload (por exemplo, um HD corrompido), o Google pode gerar uma nova chave de upload para você — sem isso, você perderia o app e teria que publicá-lo como um app completamente novo. Com o Play App Signing, a chave de produção fica segura nos servidores do Google e nunca é exposta.

Para ativar, basta marcar a opção **"Permitir que o Google gerencie e proteja sua chave de assinatura do app"** ao criar a primeira release no Play Console. Essa é a opção recomendada para praticamente todos os projetos novos, justamente por reduzir o risco de perda irreversível da chave.

> Este módulo cobre o fluxo padrão de assinatura e publicação. Cenários mais avançados — múltiplos flavors de produção, políticas de compliance detalhadas por região, ou nuances de rotação de chave de upload no Play App Signing — existem para quando o projeto crescer, mas fogem do essencial para publicar seu primeiro app.

---

## 3. Gerando o AAB (Android App Bundle)

### O que é

**AAB (Android App Bundle)** é o formato de arquivo que você envia para a Play Store — ele substitui o antigo APK único como formato de publicação. A diferença principal: em vez de gerar um único arquivo com tudo (todas as imagens, todos os idiomas, todo o código para todas as arquiteturas de processador), o AAB contém "peças" separadas, e a própria Play Store monta um APK otimizado e enxuto para cada dispositivo que for instalar o app.

### Por que isso importa

Sem o AAB, o usuário baixaria um APK "genérico" contendo recursos que o dispositivo dele nem usa (por exemplo, imagens em altíssima resolução para telas que ele não tem, ou código para arquiteturas de processador diferentes da dele). Isso deixa o download maior e mais lento do que precisa ser. O AAB resolve esse desperdício.

### Via Android Studio

1. Menu: **Build → Generate Signed Bundle / APK...**
2. Selecione **Android App Bundle**
3. Escolha a keystore e insira as credenciais
4. Selecione o build variant `release`
5. Clique em **Create**
6. O arquivo `.aab` será gerado em `app/build/outputs/bundle/release/`

### Via linha de comando

```bash
./gradlew bundleRelease
```

O comando acima chama a tarefa `bundleRelease` do Gradle (o sistema de build do Android), que compila o projeto no modo release, aplica a assinatura configurada e empacota tudo no formato AAB. O arquivo final será gerado em `app/build/outputs/bundle/release/app-release.aab`.

### Erros comuns / Pegadinhas

- Confundir AAB com APK: você **não** consegue instalar um `.aab` diretamente em um celular como faria com um `.apk`. O AAB é um formato de "envio" para a Play Store; ela é quem gera o APK final para cada aparelho.
- Rodar `./gradlew assembleRelease` (que gera um APK) quando o objetivo era gerar o AAB para a Play Store — o comando certo para a Play Store é `bundleRelease`.

---

## 4. Checklist do Google Play Console

### O que é

O **Google Play Console** é o painel web onde você cria, configura e publica apps na Play Store. É de lá que você controla a ficha da loja, envia builds, gerencia testadores e acompanha estatísticas.

Para publicar na Play Store (mesmo na trilha interna de testes, a mais simples), siga os passos abaixo.

### 4.1 Criar conta de desenvolvedor
- Acesse [play.google.com/console](https://play.google.com/console)
- Taxa única de US$ 25 (paga uma vez, vale para todos os apps que você publicar depois)

### 4.2 Criar o app no Console
1. Clique em **Criar app**
2. Preencha: nome, idioma padrão, tipo (app/jogo), gratuito/pago

### 4.3 Ficha da loja (Store Listing)

A **ficha da loja** é a página pública do seu app na Play Store — título, descrição, imagens. É o que convence (ou não) alguém a instalar o app.

| Campo | Requisito | Dicas e Exemplos |
|-------|-----------|------------------|
| Título | Até 30 caracteres | Claro e memorável. Ex: *"Meu Diário de Notas"*. Evite palavras genéricas como "app" ou "melhor". |
| Descrição curta | Até 80 caracteres | Destaque o benefício principal. Ex: *"Organize suas anotações de aula de forma simples e rápida."* |
| Descrição completa | Até 4000 caracteres | Comece com as funcionalidades mais importantes. Use listas e emojis com moderação para facilitar a leitura. |
| Ícone | 512×512 px, PNG | Fundo simples, sem texto pequeno. Teste como fica em tamanho reduzido (48×48 px) — precisa ser reconhecível. |
| Feature graphic | 1024×500 px | Imagem de destaque na Play Store. Use uma arte limpa com o nome do app e uma captura de tela ou ilustração. |
| Screenshots | Mínimo 2, tamanho recomendado pelo console | Mostre as telas principais em ordem de importância. Adicione textos curtos explicando cada funcionalidade. |

### 4.4 Classificação de conteúdo
- Preencha o questionário de classificação do IARC (obrigatório) — um formulário padronizado usado internacionalmente para definir a faixa etária recomendada do app (ex.: Livre, 10+, 16+), com base nas respostas sobre violência, conteúdo sensível, etc.

### 4.5 Enviar o AAB
1. Vá em **Release → Testing → Internal testing**
2. Clique em **Create new release**
3. Faça upload do arquivo `.aab`
4. Adicione notas da versão (um texto curto contando o que mudou nessa versão)
5. Revise e publique na trilha interna

### 4.6 Adicionar testadores
- Na trilha interna, crie uma lista de e-mails de testadores
- Compartilhe o link de opt-in para que eles instalem o app (o **opt-in** é a página onde o testador confirma que aceita participar do teste antes de conseguir instalar)

### Erros comuns / Pegadinhas

- Preencher a ficha da loja com dados de teste ("lorem ipsum") e esquecer de trocar antes da publicação em produção.
- Pular a classificação de conteúdo: sem preenchê-la, a Play Store bloqueia o avanço da publicação.
- Esquecer de adicionar o próprio e-mail como testador antes de tentar instalar via link de opt-in.

---

## 5. Trilhas de Distribuição

### O que é

Uma **trilha (track)** é um "canal" separado de distribuição do seu app, com seu próprio grupo de usuários e seu próprio ritmo de lançamento. Pense nas trilhas como portões de embarque diferentes — cada grupo de passageiros (usuários) embarca por um portão diferente, dependendo de quão "pronto para o público" o app está.

| Trilha | Finalidade | Público |
|--------|-----------|---------|
| **Interna** | Testes rápidos da equipe | Até 100 testadores |
| **Fechada (Alpha)** | Teste com grupo maior | Lista de e-mails ou grupo do Google |
| **Aberta (Beta)** | Teste público antes do lançamento | Qualquer usuário pode participar |
| **Produção** | Lançamento oficial | Todos os usuários da Play Store |

### Por que isso importa

Publicar direto em produção, sem passar por nenhuma trilha de teste, significa que qualquer bug crítico é descoberto pelos seus usuários reais — o pior lugar possível para descobrir um bug. As trilhas de teste existem para você (e um grupo controlado de pessoas) encontrar problemas antes que eles cheguem a todo mundo.

Recomendação: comece sempre pela trilha interna, valide, e depois promova gradualmente para as próximas.

---

## 6. Boas Práticas de Release

1. **Versionamento semântico**: use `versionName` como `1.0.0` (major.minor.patch — respectivamente: mudança grande/quebra compatibilidade, nova funcionalidade, correção de bug) e incremente `versionCode` a cada release.
2. **Notas de versão**: descreva o que mudou para os usuários, em linguagem simples (não é o lugar para detalhes técnicos internos).
3. **Rollout gradual**: na produção, use rollout (lançamento) de 10% → 50% → 100% para detectar problemas cedo — ou seja, a atualização é liberada primeiro para uma fração pequena dos usuários, e só avança para o resto se nenhum problema grave aparecer.
4. **Monitoramento**: ative o Firebase Crashlytics ou ferramenta similar para acompanhar crashes em produção — assim você descobre problemas por relatórios automáticos, não por avaliações negativas na loja.
5. **README do projeto**: mantenha instruções claras de como gerar o build de release, para que qualquer pessoa da equipe consiga publicar, não só quem configurou originalmente.

---

## Resumo

- Antes de publicar, revise a checklist de preparação: versão, ícone, minificação, permissões, testes e documentação.
- **Assinar o app** é obrigatório — prova a identidade do desenvolvedor e protege contra adulteração. Prefira o **Play App Signing**, que protege você de perder a chave de produção.
- O **AAB** é o formato de envio para a Play Store; ela gera o APK otimizado para cada dispositivo a partir dele.
- O **Google Play Console** é onde você cria a ficha da loja, envia o AAB e gerencia as **trilhas de distribuição** (interna → fechada → aberta → produção).
- Um **rollout gradual** em produção reduz o impacto de bugs não detectados nas trilhas de teste.

**Próximo passo**: publicar manualmente é ótimo para aprender o fluxo, mas repetir esses passos a cada nova versão é trabalhoso e sujeito a erro humano. O próximo módulo, [CI/CD para Projetos Android](./03_ci_cd.md), ensina como automatizar build, testes e geração do AAB a cada mudança no código.

---

## 7. Exercícios Práticos

1. **Gerar AAB**: configure a assinatura (Opção A ou B) e gere um AAB assinado do seu projeto via linha de comando (`./gradlew bundleRelease`).
   - Checkpoint: confirme que o arquivo `.aab` foi criado em `app/build/outputs/bundle/release/`.

2. **Checklist de release**: preencha todos os campos obrigatórios do Play Console para uma trilha interna (pode ser com dados fictícios para prática).
   - Checkpoint: adicione seu próprio e-mail como testador e confirme que consegue acessar o link de opt-in.

3. **README de release**: escreva uma seção no README do seu projeto com instruções de:
   - Como gerar o build de release
   - Onde encontrar o arquivo `.aab`
   - Como incrementar a versão

4. **Desafio**: configure um workflow no GitHub Actions que execute os testes e gere o AAB automaticamente a cada push na branch `main`. (Você vai aprender exatamente como fazer isso no próximo módulo — pode voltar a este exercício depois de lê-lo.)

---

## Entrega Final

Ao concluir este módulo, você deve ter:

- [ ] Tela principal funcional com Jetpack Compose
- [ ] ViewModel testado (pelo menos 2 testes unitários)
- [ ] Build AAB assinado e pronto para upload
- [ ] README com instruções de setup e release

---
