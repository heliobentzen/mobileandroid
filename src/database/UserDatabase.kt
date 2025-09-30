package com.example.projetoroom.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.projetoroom.dao.UserDao
import com.example.projetoroom.model.User

@Database(entities = [User::class], version = 1, exportSchema = true)
abstract class UserDatabase : RoomDatabase() {

    /**
     * Método abstrato que retorna o DAO (Data Access Object) para a entidade User.
     * O Room gera automaticamente a implementação desse método.
     */
    abstract fun userDao(): UserDao

    /**
     * Companion object:
     * - Funciona como membros "estáticos" em Java.
     * - Aqui usamos para manter uma única instância (Singleton) do banco de dados.
     */
    companion object {

        /**
         * @Volatile:
         * - Garante que a variável seja sempre lida da memória principal
         *   e não de um cache de thread.
         * - Isso evita que threads diferentes usem versões desatualizadas da instância.
         */
        @Volatile
        private var INSTANCE: UserDatabase? = null

        /**
         * Função para obter a instância do banco de dados.
         * - Se já existir, retorna a existente.
         * - Se não existir, cria uma nova de forma segura para múltiplas threads.
         */
        fun getDatabase(context: Context): UserDatabase {
            // Operador Elvis (?:) verifica se INSTANCE não é nula.
            // Caso seja nula, entra no bloco synchronized para criar a instância.
            return INSTANCE ?: synchronized(this) {

                /**
                 * Cria o banco de dados usando o Room.
                 * - context.applicationContext: evita vazamento de memória.
                 * - UserDatabase::class.java: referência à classe do banco.
                 * - "user-database": nome do arquivo físico do banco no dispositivo.
                 */
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    UserDatabase::class.java,
                    "user-database"
                )
                    /**
                     * Por padrão, o Room executa operações de criação/abertura
                     * do banco em segundo plano, evitando travar a UI.
                     * Aqui não adicionamos migrations, então mudanças de versão
                     * exigirão recriação do banco.
                     */
                    .build()

                // Armazena a instância criada para reutilização futura.
                INSTANCE = instance

                // Retorna a instância recém-criada.
                instance
            }
        }
    }
}
