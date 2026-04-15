package com.app.primeiraapp.ui

/**
 * Sealed class para eventos da UI (one-shot).
 *
 * Diferença entre EVENTO e ESTADO:
 *   - **Estado** (UiStateUser / StateFlow) é persistente: sempre mantém o último
 *     valor e re-emite para novos coletores. Ideal para representar "como a tela
 *     deve estar agora".
 *   - **Evento** (EventoUi / SharedFlow) é pontual: deve ser consumido uma única
 *     vez e não re-emitido. Ideal para ações como navegar para outra tela ou
 *     exibir um Toast, que não devem se repetir ao recriar a Activity.
 */
sealed class EventoUi {
    data class NavegarParaDetalhe(val id: Int) : EventoUi()
    data class MostrarToast(val mensagem: String) : EventoUi()
}