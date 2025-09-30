# Aula: Interface de Usuário (UI) no Android (Views + XML)

Objetivo: Entender o básico para montar telas usando XML e código Kotlin.

## 1. O Que É a UI Clássica
Você trabalha com:
- XML: descreve a tela (layout).
- Activity / Fragment: código que controla a tela.
- View: cada elemento (texto, botão, imagem).
- ViewGroup: agrupa outras Views.
- Pasta res/: onde ficam textos, cores, dimensões, imagens.

## 2. Peças Principais
- Activity: carrega um layout (setContentView(...)).
- Fragment: parte reutilizável de uma tela.
- View: TextView, Button, ImageView, EditText etc.
- ViewGroup: LinearLayout, ConstraintLayout, FrameLayout, ScrollView.

## 3. Primeiro Layout (res/layout/activity_main.xml)
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:orientation="vertical"
    android:padding="16dp"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <TextView
        android:id="@+id/tvTitle"
        android:text="Olá, Android!"
        android:textSize="20sp"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"/>

    <Button
        android:id="@+id/btnOk"
        android:text="OK"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"/>
</LinearLayout>
```

## 4. Acessando Views no Kotlin
```kotlin
class MainActivity : AppCompatActivity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        setContentView(R.layout.activity_main)

        val title = findViewById<TextView>(R.id.tvTitle)
        val btn = findViewById<Button>(R.id.btnOk)

        btn.setOnClickListener {
            title.text = "Botão clicado!"
        }
    }
}
```
Ideia: pegar a View pelo id e reagir a clique.

## 5. ViewGroups Mais Usados
- LinearLayout: empilha em coluna ou linha.
- ConstraintLayout: posiciona com “amarras” (flexível, recomendado).
- FrameLayout: uma View sobre a outra (simples).
- ScrollView: adiciona rolagem (apenas 1 filho).
- RecyclerView: listas grandes (melhor performance).

## 6. ConstraintLayout (exemplo simples)
```xml
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <TextView
        android:id="@+id/tvHello"
        android:text="Olá"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        android:padding="16dp"/>

    <Button
        android:id="@+id/btnAction"
        android:text="Continuar"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        app:layout_constraintTop_toBottomOf="@id/tvHello"
        app:layout_constraintStart_toStartOf="@id/tvHello"/>
</androidx.constraintlayout.widget.ConstraintLayout>
```
Cada app:layout_constraint... diz a que borda se liga.

## 7. Recursos (res/)
Separar para reutilizar:
- strings.xml: textos.
- colors.xml: cores.
- dimens.xml: tamanhos (dp / sp).
- drawable/: formas, ícones, fundos.

Exemplo shape (res/drawable/bg_button.xml):
```xml
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="@color/purple_500"/>
    <corners android:radius="8dp"/>
    <padding android:top="8dp" android:bottom="8dp"
        android:left="16dp" android:right="16dp"/>
</shape>
```
Usar em um botão:
```xml
<Button
    android:background="@drawable/bg_button"
    ... />
```

## 8. Estilos e Tema
styles.xml (estilo reaproveitável):
```xml
<style name="TitleText">
    <item name="android:textSize">22sp</item>
    <item name="android:textStyle">bold</item>
</style>
```
Aplicar:
```xml
<TextView
    style="@style/TitleText"
    ... />
```

## 9. Eventos Comuns
- setOnClickListener { }
- setOnLongClickListener { }
- TextWatcher (texto mudando)
Exemplo TextWatcher:
```kotlin
editText.addTextChangedListener(object: TextWatcher {
    override fun afterTextChanged(s: Editable?) {}
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        result.text = s
    }
})
```

## 10. Lista com RecyclerView (bem básico)
Layout:
```xml
<androidx.recyclerview.widget.RecyclerView
    android:id="@+id/rvItems"
    android:layout_width="match_parent"
    android:layout_height="match_parent"/>
```
Item (res/layout/item_row.xml):
```xml
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:padding="12dp"
    android:orientation="horizontal"
    android:layout_width="match_parent"
    android:layout_height="wrap_content">

    <TextView
        android:id="@+id/tvLabel"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"/>
</LinearLayout>
```
Adapter:
```kotlin
class RowAdapter(private val data: List<String>)
    : RecyclerView.Adapter<RowAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val label = v.findViewById<TextView>(R.id.tvLabel)
    }

    override fun onCreateViewHolder(p: ViewGroup, vt: Int): VH {
        val v = LayoutInflater.from(p.context)
            .inflate(R.layout.item_row, p, false)
        return VH(v)
    }

    override fun onBindViewHolder(h: VH, pos: Int) {
        h.label.text = data[pos]
    }

    override fun getItemCount() = data.size
}
```
Uso:
```kotlin
rvItems.layoutManager = LinearLayoutManager(this)
rvItems.adapter = RowAdapter(listOf("Um","Dois","Três"))
```

## 11. Boas Práticas Simples
- Nunca repetir texto: usar strings.xml.
- Usar dp (tamanho) e sp (texto).
- Evitar hierarquia profunda (preferir ConstraintLayout).
- Não travar a UI (tarefas pesadas fora da main thread).
- Acessibilidade: contentDescription em imagens.

## 12. Compose (quando olhar?)
Compose é a forma moderna (declarativa). Aprenda o básico de Views porque muitos apps ainda usam.

## 13. Erros Frequentes
- Hardcode de texto e cor.
- Usar wrap_content em algo que deve ocupar a tela.
- Fazer operação demorada dentro do onCreate diretamente.
- Ignorar diferentes tamanhos de tela.

## 14. Como a Tela Renderiza (resumo)
Medir -> posicionar -> desenhar. Pai pergunta “quanto você quer?”; filho responde; depois tudo é desenhado.

--------------------------------
# Exercícios (Prática)

1. ConstraintLayout Responsivo  
Título no topo centro, imagem abaixo, dois botões lado a lado embaixo. Testar em portrait e landscape.

2. Lista com Clique  
RecyclerView de produtos (nome + preço). Ao clicar: Toast mostrando preço formatado.

3. Formulário Simples  
Campos: nome, email, idade + botão Enviar. Validar: nome não vazio, email formato básico, idade >= 18. Mostrar mensagens de erro abaixo dos campos.

4. Acessibilidade  
Pegar um layout e: adicionar contentDescription em imagens, garantir contraste, aumentar área de toque de ícone pequeno (padding + fundo). Colocar comentários explicando.

(Entregar cada exercício com XML principal, código Kotlin essencial e breve explicação.)

Fim.
