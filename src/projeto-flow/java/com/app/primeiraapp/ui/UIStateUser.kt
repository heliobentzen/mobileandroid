package com.app.primeiraapp.ui

import com.app.primeiraapp.model.User

sealed class UiStateUser {
    object Loading : UiStateUser()
    data class Success(val usuario: User) : UiStateUser()
    data class Error(val mensagem: String) : UiStateUser()
}