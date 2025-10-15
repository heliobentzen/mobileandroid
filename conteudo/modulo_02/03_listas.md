# Listas com RecyclerView, ListAdapter e DiffUtil

Criar listas em Android é uma tarefa fundamental. A forma moderna e eficiente de fazer isso é usando `RecyclerView` em conjunto com `ListAdapter` e `DiffUtil`. Essa combinação simplifica o código, melhora a performance e fornece animações de atualização de lista gratuitamente.

Vamos aprender passo a passo.

## Por que `ListAdapter` e `DiffUtil`?

-   **`RecyclerView`**: É o componente principal para exibir listas longas de forma eficiente, reciclando as visualizações dos itens que saem da tela.
-   **`ListAdapter`**: É uma especialização do `RecyclerView.Adapter` que já vem preparada para lidar com atualizações de lista de forma assíncrona.
-   **`DiffUtil`**: É uma classe utilitária que calcula a diferença entre duas listas. Ele descobre quais itens foram adicionados, removidos ou alterados, permitindo que o `RecyclerView` anime apenas os itens necessários, em vez de redesenhar a lista inteira.

Juntos, eles automatizam o processo de atualização da lista, tornando seu código mais limpo e performático.

---

## Passo 1: O Modelo de Dados (Data Class)

Primeiro, precisamos de um modelo para os dados que vamos exibir. Vamos criar uma classe simples para representar um usuário.

**`User.kt`**
```kotlin
// Define uma classe de dados para representar um usuário.
// O 'data class' em Kotlin gera automaticamente métodos como equals(), hashCode() e toString().
// 'id' é um identificador único para cada usuário.
// 'name' é o nome do usuário.
// 'avatarUrl' é a URL para a imagem do avatar do usuário.
data class User(
    val id: Int,
    val name: String,
    val avatarUrl: String
)
```

---

## Passo 2: O Layout do Item da Lista

Agora, vamos criar o layout XML para um único item da nossa lista.

**`item_user.xml`**
```xml
<?xml version="1.0" encoding="utf-8"?>
<!-- Usamos um LinearLayout horizontal para alinhar a imagem e o texto lado a lado. -->
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="horizontal"
    android:padding="16dp"
    android:gravity="center_vertical">

    <!-- ImageView para exibir o avatar do usuário. -->
    <ImageView
        android:id="@+id/ivAvatar"
        android:layout_width="48dp"
        android:layout_height="48dp"
        tools:src="@mipmap/ic_launcher_round" />

    <!-- TextView para exibir o nome do usuário. -->
    <TextView
        android:id="@+id/tvName"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginStart="16dp"
        android:textSize="16sp"
        android:textColor="@android:color/black"
        tools:text="Nome do Usuário" />

</LinearLayout>
```

---

## Passo 3: Implementando o `DiffUtil.ItemCallback`

O `DiffUtil` precisa saber como comparar nossos objetos `User`. Para isso, criamos uma classe que estende `DiffUtil.ItemCallback`.

**`UserDiffCallback.kt`**
```kotlin
import androidx.recyclerview.widget.DiffUtil
import com.example.myapp.User // Importe sua classe User

// Esta classe ajuda o ListAdapter a entender como verificar as diferenças na lista.
class UserDiffCallback : DiffUtil.ItemCallback<User>() {

    // Este método verifica se dois itens representam o mesmo objeto.
    // Geralmente, comparamos os IDs únicos dos itens.
    override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
        return oldItem.id == newItem.id
    }

    // Este método verifica se o conteúdo de dois itens é o mesmo.
    // É chamado apenas se areItemsTheSame() retornar true.
    // O Kotlin data class gera o método equals() que compara todas as propriedades,
    // então podemos simplesmente usar '==' aqui.
    override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
        return oldItem == newItem
    }
}
```

---

## Passo 4: Criando o `ListAdapter`

Agora, vamos criar nosso Adapter. Ele herdará de `ListAdapter` em vez do tradicional `RecyclerView.Adapter`.

**`UserAdapter.kt`**
```kotlin
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapp.R // Importe seus recursos
import com.example.myapp.User // Importe sua classe User

// O Adapter herda de ListAdapter, passando nosso modelo (User) e o ViewHolder.
// O construtor do ListAdapter requer uma instância do nosso DiffUtil.ItemCallback.
class UserAdapter : ListAdapter<User, UserAdapter.UserViewHolder>(UserDiffCallback()) {

    // Este método é chamado para criar uma nova ViewHolder.
    // Ele "infla" (cria) a view do item a partir do XML.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        // Cria a view a partir do layout item_user.xml.
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    // Este método é chamado para vincular (associar) os dados de um item à sua view.
    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        // Pega o item da lista na posição atual.
        val user = getItem(position)
        // Chama o método bind do ViewHolder para configurar a view com os dados do usuário.
        holder.bind(user)
    }

    // ViewHolder é uma classe interna que armazena as referências das views de um item.
    // Isso evita chamadas repetidas a findViewById(), melhorando a performance.
    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Referências para as views dentro do item_user.xml.
        private val tvName: TextView = itemView.findViewById(R.id.tvName)
        private val ivAvatar: ImageView = itemView.findViewById(R.id.ivAvatar)

        // Método para vincular os dados do objeto User às views.
        fun bind(user: User) {
            tvName.text = user.name
            // Em um app real, você usaria uma biblioteca como Glide ou Coil para carregar a imagem da URL.
            // Ex: Glide.with(itemView.context).load(user.avatarUrl).into(ivAvatar)
            // Para simplificar, vamos apenas usar um placeholder.
            ivAvatar.setImageResource(R.mipmap.ic_launcher_round)
        }
    }
}
```

---

## Passo 5: Configurando o `RecyclerView` na Activity/Fragment

Finalmente, vamos juntar tudo na nossa Activity ou Fragment.

**`activity_main.xml`**
```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context=".MainActivity">

    <!-- O componente RecyclerView que ocupará a tela inteira. -->
    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/recyclerView"
        android:layout_width="0dp"
        android:layout_height="0dp"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        tools:listitem="@layout/item_user" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

**`MainActivity.kt`**
```kotlin
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapp.R
import com.example.myapp.User
import com.example.myapp.UserAdapter

class MainActivity : AppCompatActivity() {

    // Declaração tardia do adapter. Ele será inicializado em onCreate.
    private lateinit var userAdapter: UserAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Encontra o RecyclerView no layout.
        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)

        // Instancia o nosso adapter.
        userAdapter = UserAdapter()

        // Configura o RecyclerView.
        recyclerView.apply {
            // Define o LayoutManager, que posiciona os itens (vertical, horizontal, grid).
            layoutManager = LinearLayoutManager(this@MainActivity)
            // Define o adapter para o RecyclerView.
            adapter = userAdapter
        }

        // Cria uma lista inicial de usuários.
        val initialUsers = listOf(
            User(1, "Ana", "url_avatar_1"),
            User(2, "Bruno", "url_avatar_2"),
            User(3, "Carlos", "url_avatar_3")
        )

        // Envia a lista inicial para o adapter.
        // O ListAdapter calculará as diferenças (nenhuma, neste caso) e exibirá a lista.
        userAdapter.submitList(initialUsers)

        // Exemplo de como atualizar a lista (ex: após 3 segundos).
        // Em um app real, isso viria de uma API, banco de dados, etc.
        recyclerView.postDelayed({
            val updatedUsers = listOf(
                User(1, "Ana Silva", "url_avatar_1"), // Conteúdo alterado
                User(3, "Carlos", "url_avatar_3"),    // Posição alterada
                User(4, "Daniela", "url_avatar_4")    // Novo item
                // O item com id 2 (Bruno) foi removido.
            )
            // Envia a nova lista. O ListAdapter e o DiffUtil farão todo o trabalho pesado!
            // Eles vão detectar as mudanças e animar apenas o que for necessário.
            userAdapter.submitList(updatedUsers)
        }, 3000)
    }
}
```

## Resumo das Vantagens

1.  **Código Simples**: Você não precisa mais chamar `notifyDataSetChanged()`, `notifyItemInserted()`, etc. Apenas envie a nova lista com `submitList()`.
2.  **Performance**: O `DiffUtil` executa os cálculos de diferença em uma thread de segundo plano, evitando que a UI trave, mesmo com listas grandes.
3.  **Animações Automáticas**: O `RecyclerView` recebe as operações exatas de mudança (adição, remoção, alteração) e aplica as animações padrão de forma suave e bonita.
4.  **Menos Bugs**: Automatizar as atualizações da lista reduz a chance de erros comuns, como `IndexOutOfBoundsException`, que podem ocorrer com a manipulação manual das notificações do adapter.

---

## Exercício Prático: Lista de Tarefas Interativa

**Objetivo:** Aplicar os conceitos de `ListAdapter` e `DiffUtil` para criar uma lista de tarefas simples onde o usuário pode adicionar, remover e marcar tarefas como concluídas.

**Passos:**

1.  **Modelo de Dados:**
    *   Crie uma `data class` chamada `Task` com os seguintes campos:
        *   `id: Long` (use `System.currentTimeMillis()` para um ID simples e único)
        *   `title: String`
        *   `isCompleted: Boolean`

2.  **Layout do Item (`item_task.xml`):**
    *   Crie um layout para o item da lista que contenha:
        *   Um `CheckBox` para marcar a tarefa como concluída.
        *   Um `TextView` para exibir o título da tarefa.
        *   Um `ImageButton` com um ícone de lixeira para remover a tarefa.

3.  **DiffUtil Callback:**
    *   Implemente um `DiffUtil.ItemCallback<Task>` para comparar os itens da tarefa.

4.  **Adapter (`TaskAdapter.kt`):**
    *   Crie um `TaskAdapter` que herde de `ListAdapter<Task, ...>`.
    *   No `ViewHolder`, configure os listeners de clique para o `CheckBox` e o `ImageButton`.
    *   Para comunicar os cliques de volta para a Activity/Fragment, defina funções lambda no construtor do adapter. Por exemplo: `class TaskAdapter(val onTaskClicked: (Task) -> Unit, val onDeleteClicked: (Task) -> Unit)`.
    *   Chame essas lambdas de dentro dos listeners no `ViewHolder`.

5.  **Interface do Usuário (`activity_main.xml`):**
    *   Além do `RecyclerView`, adicione um `EditText` e um `Button` ("Adicionar") ao layout da sua activity para que o usuário possa inserir novas tarefas.

6.  **Lógica na Activity/Fragment:**
    *   Mantenha uma lista mutável de tarefas (ex: `private var taskList = mutableListOf<Task>()`).
    *   Configure o `RecyclerView` e instancie seu `TaskAdapter`, passando as implementações das lambdas de clique.
    *   **Adicionar Tarefa:** No listener de clique do botão "Adicionar", crie um novo objeto `Task`, adicione-o à `taskList`, e então chame `adapter.submitList(taskList.toList())`. Usar `.toList()` cria uma nova lista, o que é crucial para que o `DiffUtil` detecte a mudança.
    *   **Marcar como Concluída:** Na lambda `onTaskClicked`, encontre a tarefa na `taskList`, crie uma cópia dela com o status `isCompleted` invertido, substitua a tarefa antiga pela nova na lista e chame `submitList`.
    *   **Remover Tarefa:** Na lambda `onDeleteClicked`, remova a tarefa da `taskList` e chame `submitList`.

Ao final, você terá uma aplicação funcional que demonstra o poder e a simplicidade do `ListAdapter`, com animações automáticas para todas as operações na lista.