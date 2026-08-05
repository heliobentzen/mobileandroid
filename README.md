# Desenvolvimento Mobile Android

Trilha de estudos para desenvolvimento Android — do Kotlin básico até a publicação na Play Store.

Stack principal: Jetpack Compose, MVVM, Room, Retrofit, Hilt, Coroutines/Flow.

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

### [Módulo 3 — Persistência e Networking](conteudo/modulo_03/)

| Aula | Tema |
|------|------|
| [01 — Coroutines](conteudo/modulo_03/01_coroutines.md) | Dispatchers, concorrência estruturada |
| [02 — Retrofit](conteudo/modulo_03/02_retrofit.md) | suspend functions, tratamento de erros |
| [03 — Room](conteudo/modulo_03/03_persistencia_room.md) | Entity, DAO, migrations |
| [04 — Repository](conteudo/modulo_03/04_repository.md) | Fontes local + remota combinadas |
| [05 — Flow Avançado](conteudo/modulo_03/05_flow_avancado.md) | Operadores, combinação e boas práticas |
| [06 — DataStore](conteudo/modulo_03/06_datastore.md) | Preferências modernas com Flow |
| [07 — Hilt](conteudo/modulo_03/07_hilt.md) | Injeção de dependências com Hilt |

Entrega parcial: app consumindo API real com cache local em Room.

### [Módulo 4 — Testes e Publicação](conteudo/modulo_04/)

| Aula | Tema |
|------|------|
| [01 — Testes](conteudo/modulo_04/01_testes.md) | Unitários (MockK/Turbine) e UI (Compose) |
| [02 — Publicação](conteudo/modulo_04/02_publicacao.md) | AAB assinado, checklist Play Console |
| [03 — CI/CD](conteudo/modulo_04/03_ci_cd.md) | Pipeline de build, testes e entrega automatizados |
| [04 — Vibe Coding](conteudo/modulo_04/04_vibe_coding.md) | Desenvolvimento assistido por IA: quando e como usar bem |

Entrega final: app com Compose, ViewModel testado, AAB pronto, desenvolvido com apoio consciente de ferramentas de IA (vibe coding).

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
| 09 | [Hilt — Injeção de Dependências](conteudo/praticas/09_hilt_di.md) | Retrofit — API |
| 10 | [DataStore — Preferências](conteudo/praticas/10_datastore.md) | Hilt — Injeção de Dependências |

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
| DataStore | Preferências |
| Retrofit + OkHttp + Gson | APIs REST |
| Hilt | Injeção de dependências |
| Coil | Imagens |
| JUnit / Mockito / Espresso | Testes |

## Estrutura sugerida de projeto

```
app/
 ├── data/       # datasources, dtos, repos
 ├── domain/     # models, use cases
 ├── ui/         # activities, composables, viewmodels
 ├── di/         # módulos Hilt
 ├── core/       # utils, extensions
 └── build.gradle.kts
```

## Critérios de conclusão

- App roda offline com cache coerente
- Erros tratados e visíveis ao usuário
- Fluxo de login (mock ou real)
- Pipeline de build + testes configurado
- README com instruções de setup

## Extensões opcionais

Paging 3 · WorkManager · Firebase (Auth, Firestore) · Crashlytics · Analytics · Compose Multiplatform
