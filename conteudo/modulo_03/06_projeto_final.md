# Aula 6 — Projeto final

**Objetivo:** juntar tudo em um app seu, do zero ao arquivo pronto para publicar.

Não é para inventar um app complicado. É para entregar um app **simples e que funciona de verdade**, com cara de app publicado. Isso vale muito mais do que um app cheio de telas pela metade.

---

## O que entregar

Escolha **uma** das opções:

| Opção | Base | Ideia |
|-------|------|-------|
| **A — App que salva** | Aula 1 (Room) | Lista de compras, agenda de provas, diário de treinos, controle de gastos |
| **B — App que busca** | Aula 2 (Retrofit) | Lista de notícias, catálogo de filmes, feed de posts |
| **C — Os dois juntos** *(desafio)* | Aulas 1 + 2 | Busca uma lista da internet e deixa salvar os favoritos no celular |

Não precisa ser original. Precisa funcionar.

---

## Passo a passo

### 1. Faça o app funcionar (Aulas 1 e 2)

- Uma tela principal que mostra a lista
- Pelo menos uma ação do usuário: adicionar, apagar, favoritar ou atualizar
- Se usa internet: `try / catch` com mensagem de erro e botão "Tentar de novo"

### 2. Dê identidade ao app (Aula 3)

- Nome em `strings.xml`
- Ícone próprio (**New → Image Asset**) + PNG 512×512 guardado
- Cor principal trocada em `Color.kt`
- `applicationId`, `versionCode = 1`, `versionName = "1.0"`
- Só as permissões necessárias no manifest

### 3. Gere e teste o arquivo (Aula 4)

- Keystore criada, **com backup e senhas anotadas**
- `app-release.apk` gerado e instalado em um celular real
- Pelo menos duas pessoas testaram e usaram o app

### 4. Prepare a publicação (Aula 5)

- `app-release.aab` gerado
- Textos da ficha: título (30), descrição curta (80), descrição completa
- Imagem de destaque 1024×500 e no mínimo 2 screenshots
- Rascunho da política de privacidade

### 5. Escreva o README do projeto

Um arquivo `README.md` na raiz do projeto com:

```markdown
# Nome do App

O que o app faz, em duas ou três linhas.

## Como rodar
1. Abrir o projeto no Android Studio
2. Sync Gradle
3. Rodar no emulador ou celular

## Tecnologias
Kotlin, Jetpack Compose, Room (ou Retrofit), ViewModel

## Como gerar a versão de release
Build → Generate Signed App Bundle / APK → release

## Versão
1.0 (versionCode 1)
```

---

## Checklist de entrega

Marque tudo antes de entregar:

**Funciona**
- [ ] O app abre e não fecha sozinho
- [ ] A ação principal funciona (adicionar / buscar / favoritar)
- [ ] Os dados continuam lá depois de fechar e reabrir o app *(opções A e C)*
- [ ] Sem internet, o app mostra mensagem em vez de fechar *(opções B e C)*
- [ ] Girei o celular e nada quebrou

**Tem cara de app**
- [ ] Nome próprio embaixo do ícone
- [ ] Ícone próprio, não o robozinho verde
- [ ] Cor principal escolhida por mim
- [ ] Nenhuma permissão sobrando no manifest

**Está pronto para publicar**
- [ ] APK assinado gerado e instalado em celular real
- [ ] AAB assinado gerado
- [ ] Keystore com backup e senhas anotadas
- [ ] Textos e imagens da ficha da loja prontos
- [ ] README preenchido

---

## Como este projeto é avaliado

| Peso | Critério |
|------|----------|
| 40% | O app funciona sem travar e faz o que promete |
| 20% | Os dados persistem, ou os erros de internet são tratados |
| 20% | Identidade: nome, ícone, cor, versão, permissões |
| 20% | Arquivo assinado gerado + material da ficha da loja + README |

Repare no que **não** está sendo avaliado: quantidade de telas, uso de bibliotecas avançadas, código "esperto". Um app pequeno, estável e bem acabado tira nota cheia.

---

## Se quiser ir além

Depois de entregar, o [apêndice](../apendice/) tem o conteúdo avançado do mesmo material: coroutines a fundo, Retrofit em camadas, Repository, Flow avançado, DataStore, Hilt, testes automatizados e CI/CD.

Nenhum deles é necessário para publicar um app. Todos aparecem no primeiro emprego.

---

## Resumo do módulo

Você começou o módulo com um app que perdia tudo ao fechar. Terminou com:

1. Um app que **salva dados** no celular (Room)
2. Um app que **busca dados** na internet (Retrofit), tratando falhas
3. Um app com **nome, ícone e versão** próprios
4. Um **arquivo assinado**, instalável em qualquer Android
5. O caminho completo até a **Play Store**

Isso é o ciclo inteiro do desenvolvimento Android. Da linha de código até o celular de outra pessoa. 🎉
