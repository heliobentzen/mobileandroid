# Módulo 4: CI/CD para Projetos Android

**Objetivo**: automatizar o processo de build, testes e entrega do app Android usando pipelines de Integração Contínua e Entrega Contínua (CI/CD), para não depender de rodar tudo manualmente a cada mudança.

**Pré-requisito**: ter concluído o [Módulo 4.01 — Testes](./01_testes.md) e o [Módulo 4.02 — Publicação](./02_publicacao.md). Você precisará entender testes unitários e assinatura de apps antes de automatizá-los — este módulo assume que você já sabe o que são esses dois conceitos.

---

## 1. O que é CI/CD?

### O que é

**CI/CD** é a sigla para **Integração Contínua** (Continuous Integration) e **Entrega/Implantação Contínua** (Continuous Delivery/Deployment). É a prática de deixar um robô (o servidor de CI/CD) fazer, automaticamente, tarefas repetitivas que você faria manualmente: compilar o código, rodar os testes, gerar o arquivo final do app.

Pense assim: em vez de você mesmo compilar o projeto e rodar os testes toda vez que altera uma linha de código, você configura um "robô" que faz isso sozinho, sempre que você envia código novo para o repositório.

| Conceito | Significado | Benefício |
|----------|-------------|-----------|
| **CI — Integração Contínua** | Cada push (envio de código para o repositório) dispara build e testes automaticamente | Detecta erros cedo, antes de chegar ao usuário |
| **CD — Entrega Contínua** | Após a CI passar, o artefato (o arquivo final, como o AAB) é gerado e pode ser publicado | Reduz trabalho manual e acelera releases |

Um **pipeline** é a sequência de etapas automatizadas que o robô executa, uma depois da outra (por exemplo: baixar o código → compilar → rodar testes → gerar o AAB). Um **artefato** é qualquer arquivo produzido por essas etapas e guardado para consulta posterior — como o APK gerado, ou o relatório de testes.

### Por que automatizar?

- **Consistência**: o pipeline roda sempre os mesmos passos, na mesma ordem, eliminando erros humanos como "esqueci de rodar os testes antes de mandar essa versão".
- **Velocidade**: feedback em minutos — não é preciso esperar alguém (ou lembrar de) rodar os testes manualmente antes de saber se o código quebrou algo.
- **Confiança**: toda mudança na branch principal é validada automaticamente antes do merge (a junção do código novo com o código principal do projeto).
- **Rastreabilidade**: cada execução fica registrada com logs, artefatos e status — se algo falhar, dá para abrir o histórico e ver exatamente o que aconteceu.

### Por que isso importa (o problema sem CI/CD)

Sem automação, cada desenvolvedor decide na hora se vai rodar os testes antes de enviar o código. É comum alguém esquecer, ou rodar só parte dos testes por pressa. O resultado: código quebrado chega à branch principal, e só é descoberto quando outra pessoa também sofre com o problema — ou pior, quando já está em produção. Com CI, essa checagem deixa de depender da disciplina individual e passa a ser garantida pelo processo.

### Erros comuns / Pegadinhas

- Achar que CI/CD substitui os testes: o pipeline só roda os testes que **você** escreveu. Sem testes (Módulo 4.01), o CI apenas garante que o código compila, mas não garante que ele funciona corretamente.
- Confundir CI com CD: CI é sobre *validar* o código (build + testes) a cada mudança; CD é sobre *entregar* o resultado (gerar/publicar o artefato). Um projeto pode ter só CI, sem chegar a automatizar a entrega.

---

## 2. GitHub Actions para Android

### O que é

O **GitHub Actions** é a ferramenta de CI/CD integrada ao GitHub — ou seja, já vem disponível em qualquer repositório hospedado lá, sem precisar contratar um serviço externo. Os workflows (as receitas de automação) são definidos em arquivos **YAML** dentro da pasta `.github/workflows/`.

**YAML** é um formato de texto usado para escrever configurações de forma legível, organizado por indentação (espaços no início da linha definem hierarquia, como tópicos e subtópicos). Se você já viu um arquivo `.gradle.kts`, o YAML é mais simples — não tem chaves `{}` nem parênteses, só texto organizado por indentação.

Um **workflow** é o arquivo YAML completo que descreve a automação. Dentro dele, existem **jobs** (tarefas maiores, que rodam em uma máquina virtual própria) e, dentro de cada job, **steps** (passos individuais, executados em sequência).

### Exemplo comentado: construindo o pipeline em passos

#### Passo 1 — a versão mais simples: build e testes

```yaml
# Nome exibido na aba "Actions" do repositório
name: Android CI

# Evento que dispara o workflow — o "gatilho" da automação
on:
  push:
    branches: [ main ] # executa ao fazer push na main
  pull_request:
    branches: [ main ] # executa ao abrir PR para a main

jobs:
  build: # nome do job (você escolhe)
    runs-on: ubuntu-latest # máquina virtual utilizada (Linux, sempre a versão mais recente)

    steps:
      # Baixar o código do repositório para dentro da máquina virtual
      - uses: actions/checkout@v4

      # Configurar o JDK necessário para o Gradle funcionar
      - name: Configurar JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin' # distribuição do JDK (implementação usada)
          java-version: '17'

      # Compilar o projeto
      - name: Build do projeto
        run: ./gradlew assembleDebug # gera o APK de debug

      # Rodar os testes unitários
      - name: Executar testes unitários
        run: ./gradlew test # executa todos os testes da pasta test/
```

Alguns termos novos nesse exemplo:

- **`uses:`**: reaproveita uma "ação" pronta, feita por outra pessoa (ou pelo próprio GitHub), em vez de escrever o passo do zero. `actions/checkout@v4`, por exemplo, é uma ação oficial do GitHub que baixa o código do repositório.
- **`run:`**: executa um comando de terminal diretamente, como você faria na sua própria máquina.
- **`with:`**: passa parâmetros extras para uma ação (`uses:`), como a versão do Java que você quer instalar.

Salve esse arquivo como `.github/workflows/android-ci.yml`. Isso já cobre o cenário mais comum: a cada push ou PR (pull request — uma proposta de mudança de código que ainda vai ser revisada) para a `main`, o robô builda o projeto e roda os testes.

#### Passo 2 — adicionando lint

**Lint** é uma ferramenta que analisa o código em busca de problemas comuns (variáveis não usadas, práticas desaconselhadas, possíveis bugs) sem precisar executar o app. É rápido e pega erros antes mesmo do build:

```yaml
      - name: Executar Lint
        run: ./gradlew lint # analisa o código em busca de problemas comuns
```

Adicione esse passo logo depois do `setup-java`, antes do build.

#### Passo 3 — acelerando com cache

Cada execução do workflow começa numa máquina virtual "zerada", sem as dependências do Gradle já baixadas — o que pode levar minutos toda vez. O **cache** guarda essas dependências entre uma execução e outra:

```yaml
      - name: Cache do Gradle
        uses: actions/cache@v4
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
          # Chave baseada nos arquivos de configuração do Gradle: se eles não mudarem, reaproveita o cache salvo
          key: gradle-${{ runner.os }}-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
          restore-keys: |
            gradle-${{ runner.os }}-

      - name: Permissão do Gradle Wrapper
        run: chmod +x ./gradlew # necessário em ambientes Linux/macOS
```

Adicione logo depois do `checkout`, antes de qualquer comando `./gradlew`.

#### Juntando os três passos

```yaml
name: Android CI

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  build-and-test:
    runs-on: ubuntu-latest
    timeout-minutes: 30 # cancela o job se ele travar

    steps:
      - uses: actions/checkout@v4

      - name: Configurar JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'

      - name: Cache do Gradle
        uses: actions/cache@v4
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
          key: gradle-${{ runner.os }}-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
          restore-keys: |
            gradle-${{ runner.os }}-

      - name: Permissão do Gradle Wrapper
        run: chmod +x ./gradlew

      - name: Executar Lint
        run: ./gradlew lint

      - name: Build Debug
        run: ./gradlew assembleDebug

      - name: Testes Unitários
        run: ./gradlew test

      # Guarda o relatório de testes como artefato do workflow, mesmo se algum teste falhar
      - name: Upload do Relatório de Testes
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: relatorio-testes
          path: app/build/reports/tests/
```

Salve como `.github/workflows/android-ci.yml`. Esse é o pipeline padrão que a maioria dos times Android usa no dia a dia: lint + testes + build a cada push/PR.

### Erros comuns / Pegadinhas

- Esquecer a indentação correta no YAML: diferente de Kotlin, o YAML usa espaços (não tabs) para definir a hierarquia. Um espaço a mais ou a menos muda o significado do arquivo, ou quebra o workflow.
- Colocar o arquivo fora da pasta `.github/workflows/`: o GitHub só reconhece workflows dentro exatamente desse caminho.
- Esquecer o `chmod +x ./gradlew`: em máquinas Linux (como o `ubuntu-latest`), o Gradle Wrapper pode não ter permissão de execução por padrão, e o workflow falha com "permission denied".
- Definir uma chave de cache (`key:`) genérica demais: se ela nunca mudar, o cache pode ficar desatualizado e esconder problemas reais de dependências.

---

## 3. Gerando um Build Assinado no CI (visão geral)

### O que é

Além de build e testes, o pipeline também pode gerar o **AAB assinado** (visto no [Módulo 4.02](./02_publicacao.md)) automaticamente. Para isso, a keystore e as senhas precisam estar disponíveis dentro do pipeline — mas nunca escritas diretamente no arquivo YAML, porque esse arquivo fica no repositório, visível para qualquer pessoa com acesso a ele.

A solução é usar **secrets**: um cofre de variáveis do próprio GitHub (**Settings → Secrets and variables → Actions**), onde você guarda informações sensíveis de forma criptografada. O valor de um secret nunca aparece nos logs do workflow, mesmo que alguém tente imprimi-lo por engano. Um passo de build assinado, usando secrets já cadastrados, é só mais um `step` com variáveis de ambiente:

```yaml
      - name: Gerar AAB assinado
        run: ./gradlew bundleRelease
        env:
          KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
          KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
```

`${{ secrets.KEYSTORE_PASSWORD }}` é a sintaxe do GitHub Actions para "ler o valor de um secret" dentro do YAML.

### Por que isso importa

Se a keystore ou as senhas fossem coladas diretamente no arquivo do workflow, qualquer pessoa com acesso de leitura ao código poderia assinar uma versão falsa do app usando sua identidade. Secrets resolvem exatamente esse risco.

> Este é só o essencial para você reconhecer o padrão. Um pipeline de release completo — keystore inteira codificada em Base64, workflow disparado por tag de versão (`v1.0.0`), upload automático para a Play Store — é comum em times que já têm CI consolidado no dia a dia, mas foge do escopo essencial deste curso. Vale se aprofundar na documentação de secrets do GitHub Actions quando essa necessidade aparecer no seu time.

### Erros comuns / Pegadinhas

- Colar valores sensíveis (senha, keystore) diretamente no YAML em vez de em um secret: isso derrota completamente o propósito de segurança, já que o YAML fica visível no histórico do repositório.
- Nome da variável de ambiente (`env:`) não bater com o que o `build.gradle.kts` espera ler (via `System.getenv(...)`, como visto no Módulo 4.02).

---

## 4. Boas Práticas

### Cache de dependências Gradle

Usar `actions/cache` (como no Passo 3 da Seção 2) evita baixar as mesmas dependências em toda execução, reduzindo o tempo do pipeline significativamente — em projetos grandes, a diferença pode ser de vários minutos por execução.

### Secrets para credenciais

Nunca versione arquivos `.jks`/`.keystore` (adicione-os ao `.gitignore`) e mantenha senhas apenas em secrets do GitHub. Rotacione (troque periodicamente) as senhas, especialmente se alguém que tinha acesso a elas sair da equipe.

### Branch Protection Rules

**Branch protection rules** (regras de proteção de branch) são configurações do GitHub que impedem mudanças diretas e sem revisão na branch principal. Configure-as na branch `main`:

1. **Require status checks** — exigir que o workflow de CI passe antes do merge (ou seja, o botão de merge fica bloqueado até os testes passarem).
2. **Require pull request reviews** — exigir aprovação de pelo menos um revisor humano antes do merge.
3. **Require branches to be up to date** — garantir que o PR está atualizado com a main antes de poder ser mesclado, evitando integrar código testado contra uma versão antiga do projeto.

Essas regras garantem que código quebrado nunca chegue à branch principal, mesmo que alguém esqueça de rodar os testes localmente.

---

## 6. Fastlane (Visão Geral)

### O que é

O [Fastlane](https://fastlane.tools/) é uma ferramenta open-source (código aberto) que automatiza o processo de build e deploy (publicação) de apps mobile, indo além do que o workflow de CI/CD sozinho normalmente faz. Ele é especialmente útil para:

- **Upload automático** do AAB para o Google Play Console — sem precisar acessar o site manualmente para cada release.
- **Gerenciamento de screenshots** e metadados da loja (título, descrição) direto por linha de comando.
- **Distribuição de builds de teste** para equipes internas, sem passar pela Play Store.

Um arquivo chamado `Fastfile` (escrito em Ruby, a linguagem do Fastlane) define **lanes** — sequências de tarefas nomeadas, parecidas com os `jobs` do GitHub Actions, mas focadas especificamente em build e publicação de apps mobile.

### Exemplo comentado: Fastfile básico

```ruby
# Arquivo Fastfile para automação de deploy Android
default_platform(:android) # define que este arquivo trata de automações Android

platform :android do
  # Lane para distribuição interna — uma "receita" nomeada que pode ser chamada por "fastlane deploy_interno"
  desc "Enviar build para o Google Play (faixa interna)"
  lane :deploy_interno do
    gradle(
      task: "bundle",            # gerar o AAB
      build_type: "Release"      # tipo de build
    )
    upload_to_play_store(
      track: "internal",         # faixa (trilha) de distribuição interna
      aab: "app/build/outputs/bundle/release/app-release.aab" # caminho do AAB
    )
  end
end
```

> O Fastlane pode ser integrado ao GitHub Actions como um passo adicional no workflow de release — ou seja, um dos `steps` do YAML pode simplesmente chamar `fastlane deploy_interno` em vez de comandos Gradle isolados.

### Erros comuns / Pegadinhas

- Introduzir o Fastlane antes de dominar o workflow básico de CI/CD: ele adiciona uma camada extra de configuração (Ruby, `Fastfile`) que só vale a pena quando você já automatiza builds regularmente e sente a dor de fazer o upload manual toda vez.
- Achar que o Fastlane substitui os secrets do GitHub Actions: ele também precisa de credenciais (uma chave de API do Google Play, por exemplo) configuradas com segurança, seguindo o mesmo cuidado visto na Seção 4.

---

## Resumo

- **CI/CD** automatiza tarefas repetitivas (build, testes, geração de artefatos) que, feitas manualmente, dependem da disciplina de cada desenvolvedor e são fáceis de esquecer.
- O **GitHub Actions** define workflows em arquivos **YAML** dentro de `.github/workflows/`, organizados em `jobs` e `steps`.
- **Secrets** guardam informações sensíveis (como a keystore em Base64) de forma segura, para que o pipeline consiga gerar um AAB assinado sem expor credenciais no repositório.
- **Cache** de dependências do Gradle acelera builds repetidos; **branch protection rules** impedem que código sem CI aprovada chegue à branch principal.
- O **Fastlane** é uma ferramenta opcional para automatizar o deploy completo até a Play Store, útil quando o fluxo manual de publicação já incomoda.

**Próximo passo**: este é o último módulo do curso. A melhor forma de consolidar tudo que você aprendeu — testes, publicação e CI/CD — é praticando em um app real, ainda que simples. Escolha um projeto (pode ser um dos exercícios anteriores), escreva alguns testes para ele, configure um workflow de CI no GitHub Actions e publique-o de verdade na trilha interna do Google Play Console. Passar pelo fluxo completo, do código até um app instalável, é o que transforma a teoria deste curso em experiência prática.

---

## 7. Exercícios Práticos

1. **Workflow de CI básico**: adicione o workflow da Seção 2 (`.github/workflows/android-ci.yml`) ao seu projeto e confirme que ele roda automaticamente ao abrir um pull request.
   - Checkpoint: veja a execução na aba "Actions" do GitHub e confira se build e testes passaram.

2. **Workflow completo**: evolua para o workflow da Seção 3, adicionando lint e cache do Gradle.
   - Checkpoint: compare o tempo de execução da primeira vez (sem cache) com a segunda vez (com cache já salvo).

3. **Secrets e AAB assinado**: configure os quatro secrets necessários (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`) e adicione o workflow de release da Seção 4.
   - Checkpoint: crie uma tag (`git tag v1.0.0 && git push --tags`) e confirme que o AAB assinado aparece como artefato do workflow.

4. **Branch protection**: configure as regras de proteção da Seção 5 na branch `main` do seu repositório.
   - Checkpoint: tente mesclar um PR com testes falhando de propósito, e confirme que o GitHub bloqueia o merge.

5. **Desafio final**: monte o fluxo completo, de ponta a ponta — código com testes, workflow de CI validando cada PR, workflow de release gerando o AAB assinado, e publicação manual (ou via Fastlane, se quiser ir além) na trilha interna do Google Play Console.
