package com.app.primeiraapp.ui

sealed class EventoUi {
    data class NavegarParaDetalhe(val id: Int) : EventoUi()
    data class MostrarToast(val mensagem: String) : EventoUi()
}