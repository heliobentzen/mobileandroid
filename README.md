# 📱 Desenvolvimento Mobile Android

Este documento é o guia completo para a trilha de Desenvolvimento Mobile Android. Ele detalha o currículo, as tecnologias e os projetos que transformarão você em um desenvolvedor Android capaz de criar aplicativos profissionais, desde o conceito até a publicação.

---

## 🎯 Objetivo Geral
Formar o aluno para entregar um app Android profissional usando a stack estável mais difundida: Activities/Fragments + XML, MVVM + Repository, Room, Retrofit, Hilt, Coroutines/Flow, testes fundamentais e pipeline de release.

---

## 🧩 Módulos

### Módulo 1 - Fundamentos Kotlin e Android
- **Kotlin Essencial**: Tipos, null safety, data classes, coleções, funções de extensão e lambdas.
- **Estrutura de Projeto**: Gradle moderno, namespaces e build variants.
- **Componentes Android**: Ciclo de vida de Activity/Fragment e uso de Intents.
- **UI Clássica (XML)**: Layouts com ConstraintLayout e Material Components.
- **Acesso a Views**: Adoção de ViewBinding em vez de `findViewById`.

**Entrega parcial**: Tela estática simples com navegação básica entre Fragments.

### Módulo 2 - Arquitetura MVVM e UI Dinâmica
- **MVVM**: `ViewModel` + `StateFlow` (`UiState` com `sealed class`) e fluxo unidirecional de dados.
- **Eventos One-Shot**: `SharedFlow` para navegação/toasts.
- **Listas**: `RecyclerView` com `ListAdapter` + `DiffUtil`.
- **Navegação**: `Navigation Component` + `Safe Args` para navegação segura e testável.
- **Acessibilidade**: `contentDescription`, foco, labels e strings externalizadas.


**Entrega parcial**: App com lista de dados locais (mock) e tela de detalhes, usando a arquitetura MVVM.

### Módulo 3 - Persistência e Networking
- **Coroutines Avançados**: `Dispatchers`, concorrência estruturada e cancelamento.
- **Networking (Retrofit + OkHttp)**: `suspend functions` e tratamento de erros/timeouts.
- **Persistência**: `Room` (Entity, DAO, migrations básicas) e `DataStore` para preferências.
- **Repository Pattern**: Combinar fontes de dados local e remota (padrão `NetworkBoundResource` simplificado).


**Entrega parcial**: App consumindo uma API real, com cache local em Room para funcionamento offline.

### Módulo 4 - Jetpack Compose, Testes e Publicação
- **Jetpack Compose Básico**: `@Composable`, state (`remember`, state hoisting), recomposition e theming mínimo (Material 3 básico).
- **Integração Compose ↔ ViewModel**: Conectar `StateFlow` do ViewModel ao estado do Compose.
- **Testes Essenciais**: Testes de unidade para `ViewModel`/`UseCases` (MockK/Turbine) e 1–2 testes de UI com Compose.
- **Publicação Mínima**: Gerar AAB assinado localmente e checklist do Play Console (trilha interna).

**Entrega final**: Tela principal refatorada para Compose (pelo menos a lista), ViewModel testado, build AAB pronto e README com instruções de release.

## 🛠️ Stack de Tecnologias
| Tecnologia | Uso Principal |
|------------|---------------|
| Kotlin | Linguagem |
| XML + ViewBinding | Camada de UI clássica |
| Material Components | Estilos e componentes visuais |
| RecyclerView / ListAdapter | Listas performáticas |
| Navigation Component | Navegação declarativa entre telas |
| Coroutines + Flow | Concorrência e reatividade |
| Room | Persistência local estruturada |
| DataStore | Armazenamento leve de preferências |
| Retrofit + OkHttp + Moshi | Consumo de APIs REST |
| Hilt | Injeção de dependências |
| Coil | Carregamento de imagens |
| JUnit / Mockito / Espresso | Testes |
| Git + GitHub | Versionamento e colaboração |


---

## 📦 Estrutura (sugerida para projetos de exemplo)
```
app/
 ┣ data/        (datasources remotos, locais, dtos, repos)
 ┣ domain/      (models de negócio, use cases - opcional se simplificar)
 ┣ ui/          (activities, fragments, adapters, viewmodels)
 ┣ di/          (módulos Hilt)
 ┣ core/        (utils, Result wrappers, extensions)
 ┗ build.gradle.kts
```

---

## 🧪 Critérios de Conclusão
- App final roda offline com cache coerente
- Tratamento de erros visível ao usuário
- Fluxo de login ou equivalente simples (mock ou real)
- Pipeline automatizado (build + testes) configurado
- Documentação curta de setup no README do projeto final

---

Opcional / Extensões: Jetpack Compose, Paging 3, WorkManager, Firebase (Auth, Firestore), Crashlytics, Analytics, Compose Multiplatform.