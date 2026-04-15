# Projeto Flow — StateFlow + SharedFlow

Projeto de exemplo que demonstra o uso de **ViewModel**, **StateFlow** e **SharedFlow**
no Android com arquitetura **MVVM** (Model-View-ViewModel).

O app carrega dados de um usuário, exibe na tela e permite navegar para uma
tela de detalhes ou exibir um Toast — tudo coordenado por Kotlin Flows.

## Arquitetura (MVVM)

```
View (Activity) ──observa──▶ ViewModel ──manipula──▶ Model
       │                          │
       │  coleta StateFlow        │  emite SharedFlow
       │  (estado da UI)          │  (eventos pontuais)
       ▼                          ▼
  Atualiza a tela           Ações únicas (navegar, toast)
```

- **Model** — dados do domínio (`User`).
- **ViewModel** — mantém o estado da UI e processa ações do usuário.
- **View (Activity)** — observa os Flows e renderiza a interface.

## Arquivos principais

| Arquivo | Responsabilidade |
|---|---|
| `ui/MainActivity.kt` | Activity que coleta o `StateFlow` de estado e o `SharedFlow` de eventos, atualizando a UI conforme os valores emitidos. |
| `viewmodel/MainViewModel.kt` | ViewModel que expõe `uiState` (StateFlow) e `evento` (SharedFlow), além de funções para carregar dados e disparar eventos. |
| `ui/UIStateUser.kt` | Sealed class que modela os estados possíveis da UI: Loading, Success e Error. |
| `ui/EventoUI.kt` | Sealed class que modela eventos pontuais (one-shot): navegação e Toast. |
| `model/User.kt` | Data class simples representando um usuário (nome e idade). |

## Conceitos demonstrados

- **StateFlow para estado da UI** — mantém sempre o último valor e re-emite para
  novos coletores, ideal para representar o estado atual da tela.
- **SharedFlow para eventos one-shot** — diferente do StateFlow, não re-emite o
  último valor; perfeito para ações que devem acontecer apenas uma vez (ex.: navegar,
  mostrar Toast).
- **lifecycleScope para coleta** — garante que as corrotinas de coleta são
  canceladas automaticamente quando a Activity é destruída, evitando vazamentos
  de memória.
