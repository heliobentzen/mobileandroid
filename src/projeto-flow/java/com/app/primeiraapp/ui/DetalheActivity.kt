package com.app.primeiraapp.ui

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.app.primeiraapp.R

/**
 * Activity de detalhes do usuário.
 *
 * Esta tela é aberta via Intent a partir da MainActivity quando o evento
 * EventoUi.NavegarParaDetalhe é emitido pelo ViewModel. O id do usuário
 * é passado como extra na Intent e pode ser recuperado aqui para carregar
 * os dados completos.
 */
class DetalheActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_detalhe)

        // Recupera o id do usuário passado pela Intent.
        // Em um app real, esse id seria usado para buscar os dados completos
        // do usuário no ViewModel/Repository.
        val idUsuario = intent.getIntExtra("idUsuario", -1)
    }
}