package com.example.projetoroom.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.projetoroom.model.User


@Dao
interface UserDao {

    // OnConflictStrategy.REPLACE — se já existir um registro com a mesma chave
    // primária (id), o Room apaga o registro antigo e insere o novo no lugar.
    // Outras estratégias comuns: IGNORE (descarta o novo) e ABORT (lança exceção).
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(user: User)

    @Query("SELECT * FROM users")
    fun getAllUsers(): List<User>

}
