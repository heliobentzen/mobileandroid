package com.app.primeiraapp.ui

import com.app.primeiraapp.model.User

/**
 * Sealed class que representa todos os estados possíveis da UI para o usuário.
 *
 * Usar sealed class traz duas vantagens principais:
 *   1. **Exaustividade no when** — o compilador garante que todos os estados sejam
 *      tratados; se um novo estado for adicionado, todo when sem o caso novo gera
 *      erro de compilação.
 *   2. **Segurança de tipos** — cada estado pode carregar dados diferentes
 *      (ex.: Success tem o usuário, Error tem a mensagem), sem precisar de casts
 *      ou campos anuláveis genéricos.
 */
sealed class UiStateUser {
    object Loading : UiStateUser()
    data class Success(val usuario: User) : UiStateUser()
    data class Error(val mensagem: String) : UiStateUser()
}