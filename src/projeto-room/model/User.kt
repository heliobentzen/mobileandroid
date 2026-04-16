package com.example.projetoroom.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidade Room que representa a tabela "users" no banco de dados local.
 *
 * Cada propriedade da data class corresponde a uma coluna na tabela.
 * O Room usa reflexão em tempo de compilação (via annotation processor)
 * para gerar o código SQL automaticamente.
 */
@Entity(tableName = "users")
data class User(
    // autoGenerate = true faz o SQLite criar ids sequenciais automaticamente.
    // O valor padrão 0 indica ao Room que o id ainda não foi atribuído.
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val name: String,

    val email: String
)
