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

    private val _uiState = MutableStateFlow<UiStateUser>(UiStateUser.Loading)
    val uiState: StateFlow<UiStateUser> = _uiState.asStateFlow()

    private val _evento = MutableSharedFlow<EventoUi>()
    val evento: SharedFlow<EventoUi> = _evento


    fun carregarUsuario() {
        // Simula carregamento de dados
        val usuario = User("Helio Pessoa", 35)
        _uiState.value = UiStateUser.Success(usuario)
    }
    fun navegarParaDetalhe(id: Int) {
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