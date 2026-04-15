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

    // "by viewModels()" é um delegate que cria o ViewModel de forma consciente do
    // ciclo de vida. O ViewModel sobrevive a mudanças de configuração (ex.: rotação
    // de tela) e é destruído apenas quando a Activity é finalizada de verdade.
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // lifecycleScope — CoroutineScope vinculado ao ciclo de vida desta Activity.
        // Todas as corrotinas lançadas aqui são canceladas automaticamente quando
        // a Activity é destruída (onDestroy), evitando vazamentos de memória.
        lifecycleScope.launch {
            // collectLatest cancela o bloco anterior se um novo valor chegar antes
            // de o bloco terminar. Diferente de collect (que espera o bloco completar),
            // collectLatest é ideal quando só nos importamos com o estado mais recente.
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

            // ATENÇÃO: esta chamada está DENTRO do bloco lifecycleScope.launch, porém
            // APÓS o collectLatest. Como collectLatest é uma função suspensa que nunca
            // retorna (fica coletando indefinidamente), esta linha jamais será executada.
            // Solução: mover para ANTES do collectLatest, para um launch separado, ou
            // para o init {} do próprio ViewModel — que é o padrão recomendado.
            viewModel.carregarUsuario()

        }

        // Segundo launch para coletar eventos one-shot (SharedFlow).
        // Eventos como navegação e Toast devem ser tratados separadamente do estado.
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