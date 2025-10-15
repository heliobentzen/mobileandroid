# Navegação com Navigation Component

O `Navigation Component` é uma biblioteca do Android Jetpack que simplifica a implementação da navegação em seu aplicativo. Ele fornece um framework robusto para construir a interface do usuário, desde simples cliques em botões até padrões mais complexos como `Bottom Navigation` e `Navigation Drawers`.

A abordagem principal é ter uma única `Activity` que hospeda vários destinos (`Fragments`), promovendo uma arquitetura de *Single-Activity*.

## Benefícios

*   **Visualização Gráfica:** Permite visualizar e editar o fluxo de navegação do app em um editor visual.
*   **Gerenciamento de Transações de Fragmentos:** Automatiza as complexas e propensas a erros `FragmentTransaction`.
*   **Type Safety com Safe Args:** Garante a segurança de tipos ao passar dados entre destinos, evitando `ClassCastException` em tempo de execução.
*   **Deep Links:** Simplifica a criação e o gerenciamento de links diretos para telas específicas do seu app.
*   **Testabilidade:** Facilita o teste isolado dos fluxos de navegação.

---

## Abordagem Prática: `Navigation Component` + `Safe Args`

Vamos criar um fluxo de navegação simples com duas telas:
1.  `HomeFragment`: Exibe uma lista de itens.
2.  `DetailFragment`: Exibe os detalhes de um item selecionado na `HomeFragment`.

### 1. Configuração do Ambiente

Adicione as dependências necessárias e habilite o plugin `Safe Args`.

**`build.gradle.kts` (nível do projeto)**
```kotlin
// Adicione o classpath do plugin Safe Args
plugins {
    // ...
    id("androidx.navigation.safeargs.kotlin") version "2.7.7" apply false
}
```

**`build.gradle.kts` (nível do módulo)**
```kotlin
plugins {
    // ...
    id("androidx.navigation.safeargs.kotlin")
}

android {
    // ...
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // ...
    // Navigation Component
    val navVersion = "2.7.7"
    implementation("androidx.navigation:navigation-fragment-ktx:$navVersion")
    implementation("androidx.navigation:navigation-ui-ktx:$navVersion")
}
```

### 2. Criando o Grafo de Navegação

O grafo de navegação é um arquivo XML que centraliza todo o fluxo de navegação do seu app.

1.  Clique com o botão direito na pasta `res` -> **New** -> **Android Resource File**.
2.  Nome do arquivo: `nav_graph`.
3.  Tipo de recurso: **Navigation**.

**`res/navigation/nav_graph.xml`**
```xml
<?xml version="1.0" encoding="utf-8"?>
<navigation xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:id="@+id/nav_graph"
    app:startDestination="@id/homeFragment">

    <!-- Destino 1: Home -->
    <fragment
        android:id="@+id/homeFragment"
        android:name="com.example.myapp.HomeFragment"
        android:label="Home"
        tools:layout="@layout/fragment_home">
        <!-- Ação para navegar de Home para Detail -->
        <action
            android:id="@+id/action_homeFragment_to_detailFragment"
            app:destination="@id/detailFragment" />
    </fragment>

    <!-- Destino 2: Detail -->
    <fragment
        android:id="@+id/detailFragment"
        android:name="com.example.myapp.DetailFragment"
        android:label="Detalhes"
        tools:layout="@layout/fragment_detail">
        <!-- Argumento que este destino espera receber -->
        <argument
            android:name="itemId"
            app:argType="string" />
    </fragment>

</navigation>
```
*   **`<navigation>`**: O elemento raiz. `app:startDestination` define a tela inicial.
*   **`<fragment>`**: Representa um destino (uma tela).
*   **`<action>`**: Define uma rota de navegação entre dois destinos.
*   **`<argument>`**: Define um parâmetro que um destino pode receber.

### 3. Configurando o `NavHost`

O `NavHost` é um contêiner vazio onde os destinos do seu grafo de navegação são exibidos. Adicione o `NavHostFragment` ao layout da sua `Activity` principal.

**`res/layout/activity_main.xml`**
```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <androidx.fragment.app.FragmentContainerView
        android:id="@+id/nav_host_fragment"
        android:name="androidx.navigation.fragment.NavHostFragment"
        android:layout_width="0dp"
        android:layout_height="0dp"
        app:layout_constraintLeft_toLeftOf="parent"
        app:layout_constraintRight_toRightOf="parent"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintBottom_toBottomOf="parent"
        app:defaultNavHost="true"
        app:navGraph="@navigation/nav_graph" />

</androidx.constraintlayout.widget.ConstraintLayout>
```
*   `android:name`: Especifica a implementação do `NavHost`.
*   `app:navGraph`: Conecta este `NavHost` ao seu grafo de navegação.
*   `app:defaultNavHost="true"`: Garante que seu `NavHost` intercepte o botão "Voltar" do sistema.

### 4. Navegando e Passando Dados com `Safe Args`

O plugin `Safe Args` gera classes que permitem navegar e passar argumentos de forma segura.

**`HomeFragment.kt` (Enviando dados)**
```kotlin
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.myapp.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonGoToDetails.setOnClickListener {
            // ID do item que queremos passar
            val itemId = "123-ABC"

            // Use a classe Directions gerada pelo Safe Args
            val action = HomeFragmentDirections.actionHomeFragmentToDetailFragment(itemId)

            // Navegue usando a ação
            findNavController().navigate(action)
        }
    }
    // ... Boilerplate de ViewBinding
}
```
*   `HomeFragmentDirections`: Classe gerada pelo Safe Args a partir do `nav_graph.xml`.
*   `actionHomeFragmentToDetailFragment(itemId)`: Método gerado que representa a `<action>` e aceita os argumentos definidos no destino.

**`DetailFragment.kt` (Recebendo dados)**
```kotlin
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.example.myapp.databinding.FragmentDetailBinding

class DetailFragment : Fragment() {

    private var _binding: FragmentDetailBinding? = null
    private val binding get() = _binding!!

    // Delegado de propriedade para obter os argumentos de forma segura
    private val args: DetailFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Acesse o argumento de forma segura
        val receivedItemId = args.itemId
        binding.textViewDetail.text = "Detalhes do item: $receivedItemId"
    }
    // ... Boilerplate de ViewBinding
}
```
*   `DetailFragmentArgs`: Classe gerada pelo Safe Args.
*   `by navArgs()`: Um delegado de propriedade Kotlin que extrai os argumentos do `Bundle` do fragmento de forma preguiçosa e segura.

### Conclusão

Usar o `Navigation Component` com `Safe Args` torna a navegação no Android mais declarativa, visual e segura. Ele elimina a necessidade de gerenciar manualmente as transações de fragmentos e de passar dados através de `Bundles` genéricos, resultando em um código mais limpo, testável e menos propenso a erros em tempo de execução.

---

## Exercício Prático: Tela de Login

**Objetivo:** Aplicar os conceitos de `Navigation Component` e `Safe Args` para criar um fluxo de login simples. O usuário inserirá um nome em uma tela e será navegado para uma tela de boas-vindas que exibe esse nome.

### Passos

1.  **Configuração do Projeto:** Crie um novo projeto Android (Empty Activity) e configure as dependências do `Navigation Component`, `Safe Args` e `viewBinding` conforme mostrado no material.

2.  **Criar Fragments e Layouts:**
    *   Crie `LoginFragment` e seu layout `fragment_login.xml`. Adicione um `EditText` para o nome e um `Button` para entrar.
    *   Crie `WelcomeFragment` e seu layout `fragment_welcome.xml`. Adicione um `TextView` para a mensagem de boas-vindas.

3.  **Criar o Grafo de Navegação (`nav_main.xml`):**
    *   Crie um novo arquivo de recurso de navegação em `res/navigation/`.
    *   Defina `LoginFragment` como o destino inicial (`startDestination`).
    *   Adicione `WelcomeFragment` como um segundo destino.
    *   Crie uma `<action>` para navegar de `LoginFragment` para `WelcomeFragment`.
    *   Adicione um `<argument>` em `WelcomeFragment` chamado `username` do tipo `string`.

    ```xml
    <!-- res/navigation/nav_main.xml -->
    <navigation ... app:startDestination="@id/loginFragment">
        <fragment
            android:id="@+id/loginFragment"
            android:name="com.example.yourapp.LoginFragment"
            android:label="Login"
            tools:layout="@layout/fragment_login">
            <action
                android:id="@+id/action_loginFragment_to_welcomeFragment"
                app:destination="@id/welcomeFragment" />
        </fragment>
        <fragment
            android:id="@+id/welcomeFragment"
            android:name="com.example.yourapp.WelcomeFragment"
            android:label="Welcome"
            tools:layout="@layout/fragment_welcome">
            <argument
                android:name="username"
                app:argType="string" />
        </fragment>
    </navigation>
    ```

4.  **Configurar `MainActivity`:**
    *   Atualize `activity_main.xml` para conter apenas o `NavHostFragment`, apontando para `@navigation/nav_main`.

5.  **Implementar a Lógica de Navegação:**
    *   **`LoginFragment.kt`**: No `onClickListener` do botão, pegue o texto do `EditText`. Use a classe `LoginFragmentDirections` gerada pelo Safe Args para criar a ação, passando o nome do usuário. Navegue usando `findNavController().navigate(action)`.

    <details>
      <summary>Código de Exemplo: LoginFragment.kt</summary>

      ```kotlin
      // ... imports
      import androidx.navigation.fragment.findNavController

      class LoginFragment : Fragment(R.layout.fragment_login) {
          private var _binding: FragmentLoginBinding? = null
          private val binding get() = _binding!!

          override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
              super.onViewCreated(view, savedInstanceState)
              _binding = FragmentLoginBinding.bind(view)

              binding.buttonLogin.setOnClickListener {
                  val username = binding.editTextUsername.text.toString()
                  if (username.isNotBlank()) {
                      val action = LoginFragmentDirections.actionLoginFragmentToWelcomeFragment(username)
                      findNavController().navigate(action)
                  }
              }
          }

          override fun onDestroyView() {
              super.onDestroyView()
              _binding = null
          }
      }
      ```
    </details>

6.  **Implementar o Recebimento de Dados:**
    *   **`WelcomeFragment.kt`**: Use o delegate `by navArgs<WelcomeFragmentArgs>()` para obter os argumentos. No `onViewCreated`, acesse `args.username` e defina o texto do `TextView` para exibir a mensagem de boas-vindas.

    <details>
      <summary>Código de Exemplo: WelcomeFragment.kt</summary>

      ```kotlin
      // ... imports
      import androidx.navigation.fragment.navArgs

      class WelcomeFragment : Fragment(R.layout.fragment_welcome) {
          private var _binding: FragmentWelcomeBinding? = null
          private val binding get() = _binding!!
          private val args: WelcomeFragmentArgs by navArgs()

          override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
              super.onViewCreated(view, savedInstanceState)
              _binding = FragmentWelcomeBinding.bind(view)

              val username = args.username
              binding.textViewWelcome.text = "Bem-vindo(a), $username!"
          }

          override fun onDestroyView() {
              super.onDestroyView()
              _binding = null
          }
      }
      ```
    </details>

7.  **Testar:** Execute o aplicativo. Digite seu nome, clique no botão e verifique se a tela de boas-vindas exibe a mensagem correta.