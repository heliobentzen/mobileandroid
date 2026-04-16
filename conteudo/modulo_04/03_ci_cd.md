# Módulo 4: CI/CD para Projetos Android

Objetivo: Automatizar o processo de build, testes e entrega do app Android usando pipelines de Integração Contínua e Entrega Contínua (CI/CD).

**Pré-requisito**: ter concluído o [Módulo 4.01 — Testes](./01_testes.md) e o [Módulo 4.02 — Publicação](./02_publicacao.md). Você precisará entender testes unitários e assinatura de apps antes de automatizá-los.

---

## 1. O que é CI/CD?

| Conceito | Significado | Benefício |
|----------|-------------|-----------|
| **CI — Integração Contínua** | Cada push dispara build e testes automaticamente | Detecta erros cedo, antes de chegar ao usuário |
| **CD — Entrega Contínua** | Após a CI passar, o artefato é gerado e pode ser publicado | Reduz trabalho manual e acelera releases |

### Por que automatizar?

- **Consistência**: o pipeline roda sempre os mesmos passos, eliminando erros humanos.
- **Velocidade**: feedback em minutos — não é preciso esperar alguém rodar os testes manualmente.
- **Confiança**: toda mudança na branch principal é validada antes do merge.
- **Rastreabilidade**: cada build fica registrado com logs, artefatos e status.

---

## 2. GitHub Actions para Android

O GitHub Actions é a ferramenta de CI/CD integrada ao GitHub. Os workflows são definidos em arquivos YAML dentro da pasta `.github/workflows/`.

### Estrutura básica de um workflow

```yaml
# Nome exibido na aba "Actions" do repositório
name: Android CI

# Evento que dispara o workflow
on:
  push:
    branches: [ main ] # executa ao fazer push na main
  pull_request:
    branches: [ main ] # executa ao abrir PR para a main

# Tarefas que serão executadas
jobs:
  build:
    runs-on: ubuntu-latest # máquina virtual utilizada

    steps:
      # Passo 1: baixar o código do repositório
      - uses: actions/checkout@v4

      # Passo 2: configurar o JDK necessário para o Gradle
      - name: Configurar JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin' # distribuição do JDK
          java-version: '17'

      # Passo 3: compilar o projeto
      - name: Build do projeto
        run: ./gradlew assembleDebug # gera o APK de debug

      # Passo 4: rodar os testes unitários
      - name: Executar testes unitários
        run: ./gradlew test # executa todos os testes da pasta test/
```

> **Dica**: o workflow acima já cobre os cenários mais comuns — build e testes a cada push ou PR.

---

## 3. Exemplo de Workflow Completo

O arquivo abaixo combina build, testes unitários e lint em um único pipeline:

```yaml
# Workflow completo: build + testes + lint
name: Android CI Completo

on:
  push:
    branches: [ main, develop ] # dispara nas branches principais
  pull_request:
    branches: [ main ]

jobs:
  build-and-test:
    runs-on: ubuntu-latest
    timeout-minutes: 30 # limite de tempo para evitar builds travados

    steps:
      # Baixar o código-fonte
      - uses: actions/checkout@v4

      # Configurar o JDK
      - name: Configurar JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'

      # Cachear dependências do Gradle para acelerar builds futuros
      - name: Cache do Gradle
        uses: actions/cache@v4
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
          # Chave baseada nos arquivos de configuração do Gradle
          key: gradle-${{ runner.os }}-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
          restore-keys: |
            gradle-${{ runner.os }}-

      # Dar permissão de execução ao Gradle Wrapper
      - name: Permissão do Gradle Wrapper
        run: chmod +x ./gradlew

      # Verificar problemas de código com o Lint
      - name: Executar Lint
        run: ./gradlew lint # analisa o código em busca de problemas comuns

      # Compilar o projeto em modo debug
      - name: Build Debug
        run: ./gradlew assembleDebug # gera o APK de debug

      # Executar todos os testes unitários
      - name: Testes Unitários
        run: ./gradlew test # roda testes da pasta test/

      # Fazer upload do relatório de testes como artefato
      - name: Upload do Relatório de Testes
        if: always() # executa mesmo se o passo anterior falhar
        uses: actions/upload-artifact@v4
        with:
          name: relatorio-testes # nome do artefato no GitHub
          path: app/build/reports/tests/ # caminho dos relatórios gerados
```

Salve este arquivo como `.github/workflows/android-ci.yml` no seu repositório.

---

## 4. Gerando o AAB Automaticamente

Para gerar um Android App Bundle (AAB) assinado no pipeline, é necessário armazenar a keystore e as senhas como **secrets** do repositório.

### Configurando os secrets

No GitHub, acesse **Settings → Secrets and variables → Actions** e adicione:

| Secret | Descrição |
|--------|-----------|
| `KEYSTORE_BASE64` | Conteúdo da keystore codificado em Base64 |
| `KEYSTORE_PASSWORD` | Senha da keystore |
| `KEY_ALIAS` | Alias da chave de assinatura |
| `KEY_PASSWORD` | Senha da chave |

Para codificar a keystore em Base64:

```bash
# Codificar a keystore para armazenar como secret no GitHub
base64 -w 0 minha-keystore.jks > keystore_base64.txt
```

### Workflow de release

```yaml
# Workflow para gerar o AAB assinado
name: Android Release

on:
  push:
    tags:
      - 'v*' # dispara quando uma tag de versão é criada (ex: v1.0.0)

jobs:
  release:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Configurar JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'

      # Decodificar a keystore a partir do secret
      - name: Decodificar Keystore
        run: echo "${{ secrets.KEYSTORE_BASE64 }}" | base64 -d > app/keystore.jks

      # Gerar o AAB assinado usando variáveis de ambiente dos secrets
      - name: Gerar AAB assinado
        run: ./gradlew bundleRelease # gera o AAB de release
        env:
          KEYSTORE_FILE: app/keystore.jks # caminho da keystore decodificada
          KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
          KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
          KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}

      # Fazer upload do AAB como artefato do workflow
      - name: Upload do AAB
        uses: actions/upload-artifact@v4
        with:
          name: app-release # nome do artefato
          path: app/build/outputs/bundle/release/*.aab # caminho do AAB gerado
```

> **Importante**: nunca faça commit da keystore ou das senhas diretamente no repositório. Use sempre secrets.

---

## 5. Boas Práticas

### Cache de dependências Gradle

Usar `actions/cache` (como no exemplo da Seção 3) evita baixar as mesmas dependências em toda execução, reduzindo o tempo do pipeline significativamente.

### Secrets para keystore

- Armazene a keystore codificada em Base64 como secret do repositório.
- Nunca versione arquivos `.jks` ou `.keystore` — adicione-os ao `.gitignore`.
- Rotacione as senhas periodicamente.

### Branch Protection Rules

Configure regras de proteção na branch `main`:

1. **Require status checks** — exigir que o workflow de CI passe antes do merge.
2. **Require pull request reviews** — exigir aprovação de pelo menos um revisor.
3. **Require branches to be up to date** — garantir que o PR está atualizado com a main.

Essas regras garantem que código quebrado nunca chegue à branch principal.

---

## 6. Fastlane (Visão Geral)

O [Fastlane](https://fastlane.tools/) é uma ferramenta open-source que automatiza o processo de build e deploy de apps mobile. Ele é especialmente útil para:

- **Upload automático** do AAB para o Google Play Console.
- **Gerenciamento de screenshots** e metadados da loja.
- **Distribuição de builds de teste** para equipes internas.

### Exemplo básico de Fastfile

```ruby
# Arquivo Fastfile para automação de deploy Android
default_platform(:android)

platform :android do
  # Lane para distribuição interna
  desc "Enviar build para o Google Play (faixa interna)"
  lane :deploy_interno do
    gradle(
      task: "bundle",            # gerar o AAB
      build_type: "Release"      # tipo de build
    )
    upload_to_play_store(
      track: "internal",         # faixa de distribuição interna
      aab: "app/build/outputs/bundle/release/app-release.aab" # caminho do AAB
    )
  end
end
```

> O Fastlane pode ser integrado ao GitHub Actions como um passo adicional no workflow de release.

---

## 7. Resumo

| Tópico | O que aprendemos |
|--------|------------------|
| **CI/CD** | Automatizar build e testes a cada push garante qualidade contínua |
| **GitHub Actions** | Workflows YAML definem os passos de build, teste e lint |
| **AAB assinado** | Secrets armazenam a keystore com segurança para gerar releases no pipeline |
| **Cache** | Cachear o Gradle reduz significativamente o tempo de execução |
| **Branch Protection** | Regras de proteção impedem merge de código sem CI aprovada |
| **Fastlane** | Alternativa para automatizar o deploy completo até a loja |

---

## Próximos Passos

- Configure o workflow de CI no seu projeto seguindo o exemplo da Seção 3.
- Adicione os secrets de assinatura e teste o workflow de release com uma tag.
- Explore o Fastlane para automatizar o upload para o Google Play.
- Revise os módulos anteriores para consolidar seu conhecimento em [Testes](./01_testes.md) e [Publicação](./02_publicacao.md).
