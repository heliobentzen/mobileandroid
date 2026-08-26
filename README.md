# Desenvolvimento Mobile Android

Trilha de estudos para desenvolvimento Android — do Kotlin básico até a publicação na Play Store.

Stack principal: Kotlin, Jetpack Compose, MVVM, Room e Retrofit.

São três módulos: fundamentos, arquitetura e, no final, um app funcional publicado. O conteúdo avançado fica no [apêndice](conteudo/apendice/), separado do caminho principal.

## Módulos

### [Módulo 1 — Fundamentos Kotlin e Android](conteudo/modulo_01/)

| Aula | Tema |
|------|------|
| [01 — Introdução](conteudo/modulo_01/01_intro.md) | Visão geral do curso |
| [02 — Kotlin Essencial](conteudo/modulo_01/02_kotlin_essencial.md) | Tipos, null safety, data classes, lambdas |
| [03 — Estrutura de Projeto](conteudo/modulo_01/03_estrutura_projeto.md) | Gradle, namespaces, build variants |
| [04 — Activity](conteudo/modulo_01/04_activity.md) | Ciclo de vida |
| [05 — Componentes Android](conteudo/modulo_01/05_components_android.md) | Activity, ciclo de vida Compose e referência rápida de Fragment |
| [06 — Intents](conteudo/modulo_01/06_intents.md) | Integração com sistema e outros apps |
| [07 — Jetpack Compose](conteudo/modulo_01/07_jetpackcompose.md) | `@Composable`, state, theming |
| [08 — Kotlin Intermediário](conteudo/modulo_01/08_kotlin_intermediario.md) | Scope functions, sealed classes, generics |

Entrega parcial: tela estática com navegação básica entre Composables.

### [Módulo 2 — Arquitetura MVVM e UI Dinâmica](conteudo/modulo_02/)

| Aula | Tema |
|------|------|
| [01 — MVVM](conteudo/modulo_02/01_mvvm.md) | ViewModel + StateFlow, UiState |
| [02 — Eventos One-Shot](conteudo/modulo_02/02_eventos_oneshot.md) | SharedFlow para navegação/toasts |
| [03 — Listas](conteudo/modulo_02/03_listas.md) | LazyColumn com chaves estáveis |
| [04 — Navegação](conteudo/modulo_02/04_navegacao.md) | Navigation Component + Safe Args |
| [05 — Acessibilidade](conteudo/modulo_02/05_acessibilidade.md) | contentDescription, foco, labels |
| [06 — Formulários e Validação](conteudo/modulo_02/06_formularios_validacao.md) | TextField, validação reativa, ViewModel |

Entrega parcial: app com lista (mock) e tela de detalhes usando MVVM.

### [Módulo 3 — App Funcional e Publicação](conteudo/modulo_03/)

| Aula | Tema |
|------|------|
| [01 — Salvando dados no celular](conteudo/modulo_03/01_salvar_dados.md) | Room mínimo: lista que não some ao fechar o app |
| [02 — Buscando dados da internet](conteudo/modulo_03/02_dados_da_internet.md) | Retrofit em um arquivo, com tratamento de erro |
| [03 — Deixando o app com cara de app](conteudo/modulo_03/03_app_com_cara_de_app.md) | Nome, ícone, cor, versão e permissões |
| [04 — Gerando o arquivo do app](conteudo/modulo_03/04_gerar_apk_e_aab.md) | Assinatura, keystore, APK e AAB |
| [05 — Publicando na Play Store](conteudo/modulo_03/05_publicar_na_play_store.md) | Play Console, ficha da loja, trilhas de teste |
| [06 — Projeto final](conteudo/modulo_03/06_projeto_final.md) | Checklist de entrega e critérios de avaliação |

Entrega final: app simples e estável, com ícone e nome próprios, APK assinado instalado em celular real e AAB pronto para envio.

### [Apêndice — Conteúdo avançado (opcional)](conteudo/apendice/)

Não é necessário para concluir o curso. É para quem quiser ir além depois de publicar.

| Assunto | Arquivo |
|---------|---------|
| Coroutines a fundo, Flow avançado | [01](conteudo/apendice/01_coroutines.md) · [06](conteudo/apendice/06_flow_avancado.md) |
| Retrofit em camadas, Repository, Room avançado | [03](conteudo/apendice/03_retrofit_camadas.md) · [05](conteudo/apendice/05_repository.md) · [04](conteudo/apendice/04_room_avancado.md) |
| DataStore, Hilt | [07](conteudo/apendice/07_datastore.md) · [08](conteudo/apendice/08_hilt.md) |
| Testes, CI/CD, publicação detalhada | [09](conteudo/apendice/09_testes.md) · [10](conteudo/apendice/10_ci_cd.md) · [11](conteudo/apendice/11_publicacao_detalhada.md) |

## Práticas

Exercícios guiados passo a passo, ordenados do fundamental ao avançado.
Recomendamos seguir na ordem abaixo para uma evolução progressiva:

| # | Prática | Pré-requisito |
|---|---------|---------------|
| 01 | [Kotlin Básico](conteudo/praticas/02_kotlin_basico.md) | Nenhum — comece aqui |
| 02 | [Jetpack Compose Básico](conteudo/praticas/03_jetpack_compose_basico.md) | Kotlin Básico |
| 03 | [Compose + Navigation](conteudo/praticas/01_compose_navigation.md) | Jetpack Compose Básico |
| 04 | [MVVM + StateFlow](conteudo/praticas/04_mvvm_stateflow.md) | Compose + Navigation |
| 05 | [Listas com LazyColumn](conteudo/praticas/05_listas_lazy_column.md) | MVVM + StateFlow |
| 06 | [Coroutines](conteudo/praticas/06_coroutines.md) | MVVM + StateFlow |
| 07 | [Room — Persistência](conteudo/praticas/07_room_persistencia.md) | Coroutines |
| 08 | [Retrofit — API](conteudo/praticas/08_retrofit_api.md) | Coroutines |
| 09 | [Hilt — Injeção de Dependências](conteudo/praticas/09_hilt_di.md) *(opcional)* | Retrofit — API |
| 10 | [DataStore — Preferências](conteudo/praticas/10_datastore.md) *(opcional)* | Hilt — Injeção de Dependências |

As práticas 01 a 08 acompanham os três módulos. As 09 e 10 são extras, ligadas ao [apêndice](conteudo/apendice/).

## Projetos de exemplo

| Projeto | Descrição |
|---------|-----------|
| [projeto-flow](src/projeto-flow/) | App com ViewModel, StateFlow e navegação entre Activities |
| [projeto-room](src/projeto-room/) | Persistência local com Room (Entity, DAO, Database) |
| [NavHost.kt](src/NavHost.kt) | Exemplo de navegação com Compose |

## Stack

| Tecnologia | Uso |
|------------|-----|
| Kotlin | Linguagem |
| Jetpack Compose | UI |
| Material Components | Estilos e componentes |
| Navigation Component | Navegação entre telas |
| Coroutines + Flow | Concorrência e reatividade |
| Room | Persistência local |
| Retrofit + Gson | APIs REST |
| DataStore | Preferências *(apêndice)* |
| Hilt | Injeção de dependências *(apêndice)* |
| Coil | Imagens *(opcional)* |
| JUnit / Mockito / Espresso | Testes *(apêndice)* |

## Estrutura sugerida de projeto

Para os apps do curso, poucas pastas bastam:

```
app/
 ├── data/       # Entity, DAO, Database, Api
 ├── ui/         # telas Compose e ViewModels
 └── build.gradle.kts
```

Projetos maiores costumam separar mais camadas (`domain/`, `di/`, `core/`) — isso está no [apêndice](conteudo/apendice/).

## Critérios de conclusão

- O app abre, funciona e não fecha sozinho
- Os dados continuam salvos depois de fechar o app, ou os erros de internet são tratados na tela
- Nome, ícone, cor e versão próprios
- APK assinado instalado em um celular real
- AAB assinado gerado e material da ficha da loja pronto
- README com instruções de setup e de release

## Extensões opcionais

Paging 3 · WorkManager · Firebase (Auth, Firestore) · Crashlytics · Analytics · Compose Multiplatform
