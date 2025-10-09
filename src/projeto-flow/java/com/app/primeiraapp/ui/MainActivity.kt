package com.app.primeiraapp.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.app.primeiraapp.R
import com.app.primeiraapp.databinding.ActivityMainBinding
import com.app.primeiraapp.viewmodel.MainViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    is UiStateUser.Loading -> {
                        binding.tvNome.text = "Carregando..."
                        binding.tvIdade.text = ""
                    }
                    is UiStateUser.Success -> {
                        binding.tvNome.text = getString(R.string.label_nome, state.usuario.nome)
                        binding.tvIdade.text = getString(R.string.label_idade, state.usuario.idade)
                    }
                    is UiStateUser.Error -> {
                        binding.tvNome.text = "Erro: ${state.mensagem}"
                        binding.tvIdade.text = ""
                    }
                }
            }

            viewModel.carregarUsuario();

        }

        lifecycleScope.launch {
            viewModel.evento.collectLatest { evento ->
                when (evento) {
                    is EventoUi.NavegarParaDetalhe -> {
                        val intent = Intent(this@MainActivity, DetalheActivity::class.java)
                        intent.putExtra("idUsuario", evento.id)
                        startActivity(intent)
                    }
                    is EventoUi.MostrarToast -> {
                        Toast.makeText(this@MainActivity, evento.mensagem, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        binding.btnDetalhar.setOnClickListener {
            viewModel.navegarParaDetalhe(1)
        }

        binding.btnToast.setOnClickListener {
            viewModel.mostrarToast("Usuário carregado com sucesso!")
        }

    }
}