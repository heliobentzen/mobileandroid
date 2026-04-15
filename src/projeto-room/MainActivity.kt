package com.example.projetoroom

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.projetoroom.database.UserDatabase
import com.example.projetoroom.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Em um app real, a lógica de acesso ao banco de dados deveria estar em um
// ViewModel (ou Repository), e não diretamente na Activity. Isso facilita
// testes, respeita a separação de responsabilidades e sobrevive a mudanças
// de configuração (rotação de tela, etc.).
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val db = UserDatabase.getDatabase(applicationContext)

        val novoUsuario = User(name = "Rute", email = "rute@gmail.com")
        val userDao = db.userDao()

        // Usamos lifecycleScope em vez de CoroutineScope(Dispatchers.IO) avulso.
        // O CoroutineScope avulso NÃO é cancelado quando a Activity é destruída,
        // o que pode causar vazamento de memória e crashes ao acessar a Activity
        // após ela ter sido finalizada.
        // lifecycleScope é cancelado automaticamente em onDestroy.
        lifecycleScope.launch {
            // withContext(Dispatchers.IO) troca para a thread de IO apenas durante
            // as operações de banco, e retorna ao dispatcher principal ao terminar.
            withContext(Dispatchers.IO) {
                userDao.insert(novoUsuario)
                val users = userDao.getAllUsers()
                for (user in users) {
                    Log.d("User", "${user.id}: ${user.name} - ${user.email}")
                }
            }
        }

    }
}
