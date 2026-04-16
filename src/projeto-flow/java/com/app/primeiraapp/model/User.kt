package com.app.primeiraapp.model

/**
 * Data class que representa um usuário no domínio do aplicativo.
 *
 * Em Kotlin, data classes geram automaticamente equals(), hashCode(), toString()
 * e copy(), evitando boilerplate. Usamos val (imutável) para garantir que os
 * objetos não sejam alterados após a criação — isso é fundamental para trabalhar
 * de forma segura com StateFlow e Compose, que dependem de comparações por
 * igualdade para decidir quando a UI deve ser atualizada.
 */
data class User(
    val nome: String,
    val idade: Int
)