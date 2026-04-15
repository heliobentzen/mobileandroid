package com.app.primeiraapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.primeiraapp.model.User
import com.app.primeiraapp.ui.EventoUi
import com.app.primeiraapp.ui.UiStateUser
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    // _uiState é MutableStateFlow (mutável) e privado — somente o ViewModel pode
    // alterar o estado. Esse padrão de encapsulamento impede que a View modifique
    // o estado diretamente, respeitando o fluxo unidirecional de dados do MVVM.
    private val _uiState = MutableStateFlow<UiStateUser>(UiStateUser.Loading)

    // asStateFlow() cria uma versão somente-leitura (StateFlow) do _uiState.
    // A View coleta este Flow sem conseguir emitir novos valores.
    val uiState: StateFlow<UiStateUser> = _uiState.asStateFlow()

    // SharedFlow para eventos pontuais (one-shot). Diferente do StateFlow, o
    // SharedFlow NÃO mantém o último valor — ideal para ações que não devem
    // ser re-executadas ao recriar a Activity (ex.: navegação, Toast).
    private val _evento = MutableSharedFlow<EventoUi>()
    val evento: SharedFlow<EventoUi> = _evento

    fun carregarUsuario() {
        // Simula carregamento de dados
        val usuario = User("Helio Pessoa", 35)
        _uiState.value = UiStateUser.Success(usuario)
    }

    fun navegarParaDetalhe(id: Int) {
        // viewModelScope — CoroutineScope fornecido pelo ViewModel que segue o
        // princípio de concorrência estruturada: todas as corrotinas lançadas aqui
        // são canceladas automaticamente quando o ViewModel é destruído (onCleared).
        viewModelScope.launch {
            _evento.emit(EventoUi.NavegarParaDetalhe(id))
        }
    }

    fun mostrarToast(mensagem: String) {
        viewModelScope.launch {
            _evento.emit(EventoUi.MostrarToast(mensagem))
        }
    }

}