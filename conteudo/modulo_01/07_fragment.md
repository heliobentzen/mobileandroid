# Fragments no Android (práticas modernas)

Observação: Para novas UIs, prefira Jetpack Compose e Navigation Compose. Este guia cobre Fragments com Views (AndroidX) para manutenção de projetos existentes.

## O que é um Fragment?

Um Fragment é uma parte reutilizável da interface e da lógica de uma tela, sempre hospedado por uma Activity. Tem ciclo de vida próprio, pode ser combinado e reutilizado para layouts adaptáveis (telefones e tablets).

Por que usar:
- Modularidade e reutilização de UI/lógica
- Layouts responsivos (multi-pane em tablets)
- Integração com Navigation Component (recomendado)

---

## 1) Criando um Fragment (com View Binding, conciso)

Layout do fragment:

res/layout/fragment_exemplo.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <TextView
        android:id="@+id/texto_fragment"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Olá, eu sou um Fragment!"
        android:textSize="20sp"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent"/>
</androidx.constraintlayout.widget.ConstraintLayout>
```

Habilite View Binding no módulo app (build.gradle):
```groovy
android {
  buildFeatures { viewBinding true }
}
```
(No Kotlin DSL: buildFeatures { viewBinding = true })

Classe do fragment (inflando via construtor + binding seguro no ciclo da view):

ExemploFragment.kt
```kotlin
package com.exemplo.app

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.exemplo.app.databinding.FragmentExemploBinding

class ExemploFragment : Fragment(R.layout.fragment_exemplo) {

    private var _binding: FragmentExemploBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentExemploBinding.bind(view)

        val mensagem = arguments?.getString(ARG_MENSAGEM) ?: "Mensagem Padrão"
        binding.textoFragment.text = mensagem
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_MENSAGEM = "argumento_mensagem"

        fun newInstance(mensagem: String) = ExemploFragment().apply {
            arguments = bundleOf(ARG_MENSAGEM to mensagem)
        }
    }
}
```

Dicas:
- Prefira Fragment(R.layout.seu_layout) para inflar sem onCreateView.
- Use FragmentExemploBinding.bind(view) em onViewCreated e zere no onDestroyView para evitar leaks.

---

## 2) Adicionando um Fragment a uma Activity

Recomendado: Navigation Component. Alternativa: transações manuais.

### Opção A — Navigation Component (recomendado)

Activity com NavHost:

res/layout/activity_main.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.fragment.app.FragmentContainerView
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/nav_host"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:name="androidx.navigation.fragment.NavHostFragment"
    app:defaultNavHost="true"
    app:navGraph="@navigation/nav_graph"/>
```

Gráfico de navegação:

res/navigation/nav_graph.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<navigation xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/nav_graph"
    app:startDestination="@id/exemploFragment">

    <fragment
        android:id="@+id/exemploFragment"
        android:name="com.exemplo.app.ExemploFragment">
        <argument
            android:name="argumento_mensagem"
            app:argType="string"
            android:defaultValue="Mensagem Padrão" />
    </fragment>
</navigation>
```

Navegar para o fragment (evite navegar novamente em recriações):

MainActivity.kt
```kotlin
package com.exemplo.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.navigation.findNavController

class MainActivity : AppCompatActivity(R.layout.activity_main) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            findNavController(R.id.nav_host).navigate(
                R.id.exemploFragment,
                bundleOf("argumento_mensagem" to "Dados vindos da Activity!")
            )
        }
    }
}
```

Observação: Para argumentos type-safe, use Safe Args (plugin Gradle) e Directions geradas.

### Opção B — Transação manual com FragmentManager

Layout com container:

res/layout/activity_main.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.fragment.app.FragmentContainerView
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/fragment_container"
    android:layout_width="match_parent"
    android:layout_height="match_parent"/>
```

Transação:

MainActivity.kt
```kotlin
package com.exemplo.app

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.commit
import androidx.fragment.app.replace

class MainActivity : AppCompatActivity(R.layout.activity_main) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                replace<ExemploFragment>(
                    R.id.fragment_container,
                    args = bundleOf("argumento_mensagem" to "Dados vindos da Activity!")
                )
                addToBackStack(null)
            }
        }
    }
}
```

Notas:
- Prefira replace<T>() de fragment-ktx com args.
- addToBackStack permite voltar; o Navigation Component gerencia isso automaticamente.

---

## 3) Passando dados para um Fragment

- Navigation Component: defina argumentos no nav_graph (preferível). Com Safe Args, você obtém segurança de tipos.
- Sem Navigation: use Bundle via newInstance + bundleOf, como no exemplo acima. Evite construtores com parâmetros.

---

## 4) Observando dados com ciclo de vida correto

- LiveData:
```kotlin
viewModel.texto.observe(viewLifecycleOwner) { valor ->
    binding.textoFragment.text = valor
}
```

- Flow/Coroutines (recomendado):
```kotlin
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch

viewLifecycleOwner.lifecycleScope.launch {
    viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.textoFlow.collect { valor ->
            binding.textoFragment.text = valor
        }
    }
}
```

---

## 5) Boas práticas

- Evite memory leaks: zere o binding em onDestroyView.
- Use FragmentContainerView em vez de FrameLayout quando possível (melhor integração com FragmentManager/NavHost).
- Prefira Navigation Component para back stack, transições e argumentos.
- Para menus, use MenuHost/MenuProvider com viewLifecycleOwner.
- Para multijanela/tablets, use múltiplos fragments e layouts com qualifiers sw600dp.
- Se estiver em Compose, prefira Navigation Compose e evite Fragments quando viável.

Referências:
- Guia Fragments (AndroidX)
- Jetpack Navigation
- View Binding
- Lifecycle + coroutines (repeatOnLifecycle)
- Navigation Safe Args
- Compose + Navigation Compose
