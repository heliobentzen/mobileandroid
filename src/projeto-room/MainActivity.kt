package com.example.projetoroom

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.projetoroom.database.UserDatabase
import com.example.projetoroom.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val db = UserDatabase.getDatabase(applicationContext)

        val novoUsuario = User(name = "Rute", email ="rute@gmail.com")
        val userDao = db.userDao()

        CoroutineScope(Dispatchers.IO).launch {
            userDao.insert(novoUsuario)
            val users = userDao.getAllUsers()
            for(user in users){
                Log.d("User", "${user.id}: ${user.name} - ${user.email}")
            }
        }



    }
}
