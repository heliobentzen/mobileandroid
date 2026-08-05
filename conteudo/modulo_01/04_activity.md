# Activities no Android Moderno

## 1. O que é e por que existe

**O que é.** Uma **Activity** é um dos blocos fundamentais de um app Android: é a classe responsável por hospedar a tela com a qual o usuário interage diretamente. Você já usou uma no arquivo 01 (`MainActivity`) sem entrar em detalhes — agora vamos abrir essa caixa.

Historicamente, cada tela de um app era uma Activity separada. Hoje, adotamos uma arquitetura chamada **Single-Activity + Jetpack Compose + Navigation**: o app tem **uma única Activity** (geralmente `MainActivity`), que serve como um "contêiner" ou "host", enquanto a troca entre telas internas é feita por Composables e pelo Navigation Component (você vai estudar isso no arquivo 07 e com mais profundidade no Módulo 2).

Você ainda vai encontrar **Fragments** (outro componente, mais antigo, para representar partes de tela) em projetos legados — veja a referência rápida em [05 — Componentes Android](05_components_android.md). Mas em projetos novos com Compose, eles raramente são necessários.

**Por que isso importa.** Mesmo em uma arquitetura Single-Activity, entender Activities continua essencial porque:
- O sistema operacional (Android OS) gerencia o **ciclo de vida** da Activity de forma agressiva, para liberar memória e bateria — e isso afeta diretamente o que acontece com a tela do seu app quando o usuário sai dele, recebe uma ligação, ou gira o celular.
- Interações externas ao app (Intents, deep links, compartilhamento de conteúdo, permissões, retorno de outras telas do sistema) passam necessariamente por uma Activity.
- Muitos componentes do Android (câmera, notificações, App Links, atalhos/Shortcuts, Wear, Auto) envolvem *callbacks* (funções chamadas automaticamente pelo sistema em resposta a um evento) ligados à Activity.

---

## 2. Conceitos-chave modernos

Antes de ver o ciclo de vida em detalhe, vale conhecer os termos que você vai encontrar recorrentemente neste e nos próximos arquivos:

- **Single-Activity Architecture**: uma única Activity (geralmente `MainActivity`) hospeda todos os destinos (telas) do app, usando o **Navigation Component** para gerenciar a navegação entre eles em Compose.
- **ViewModel**: uma classe do Jetpack que guarda o estado da tela de um jeito que **sobrevive a recriações** da Activity (como quando o usuário gira o celular). Você viu uma introdução a isso no arquivo 02 — vamos revisitar aqui no contexto do ciclo de vida.
- **State hoisting** ("elevação de estado"): uma técnica do Compose onde o estado de um componente visual é movido para um nível acima na hierarquia, deixando os componentes visuais mais simples e reutilizáveis. Detalhado no arquivo 07.
- **`remember` / `rememberSaveable`**: formas de guardar um valor entre recomposições do Compose. `remember` perde o valor se a Activity for recriada (por exemplo, ao girar a tela); `rememberSaveable` sobrevive a esse tipo de recriação.
- **Activity Result APIs** (`registerForActivityResult`): a forma moderna de pedir um resultado a outra tela ou app (por exemplo, escolher uma foto da galeria), substituindo o antigo `onActivityResult`.
- **Lifecycle-aware components**: componentes (como observadores) que sabem automaticamente em qual estado do ciclo de vida a Activity está, e podem, por exemplo, parar de fazer um trabalho quando a tela não está mais visível — evitando **vazamentos de memória** (quando um recurso continua "vivo" na memória do dispositivo mesmo depois que não é mais necessário).
- **Back dispatch unificado**: o Android moderno usa um único mecanismo (`OnBackPressedDispatcher`, exposto no Compose através de `BackHandler`) para lidar com o botão/gesto de "voltar", em vez do antigo método `onBackPressed()` sobrescrito diretamente na Activity.

Não se preocupe em memorizar tudo isso agora — cada termo será revisitado com exemplos práticos nos próximos arquivos.

---

## 3. Ciclo de vida (essência prática)

**O que é.** O **ciclo de vida** (lifecycle) de uma Activity é a sequência de estados pelos quais ela passa, desde o momento em que é criada até o momento em que é destruída. O Android chama automaticamente métodos específicos (chamados **callbacks de ciclo de vida**) em cada transição de estado.

**Por que isso importa.** Se você não entender o ciclo de vida, corre o risco de: (1) vazar memória mantendo recursos abertos quando a tela não está mais visível (como a câmera ligada em segundo plano, gastando bateria); (2) perder dados do usuário quando a tela é recriada (por exemplo, ao girar o celular); ou (3) tentar acessar a UI depois que ela já foi destruída, causando um crash.

### Diagrama do Ciclo de Vida

```plaintext
onCreate -> onStart -> onResume -> (foreground)
  ^                                   |
  |                                   v
onRestart <- onStop <- onPause <- onDestroy
```

> **Como ler este diagrama:** a Activity nasce em `onCreate`, passa por `onStart` e chega a `onResume`, onde fica em primeiro plano (foreground) e totalmente interativa. Quando o usuário sai da tela (mas ela ainda pode voltar a ficar visível, como ao abrir outro app por cima), ela passa por `onPause` e `onStop`. Se o usuário voltar depois disso, o ciclo passa por `onRestart` antes de `onStart` novamente. Se a Activity for realmente encerrada, o último callback chamado é `onDestroy`.

### Estados principais (callbacks):
1. **onCreate**: chamado uma vez, na criação da Activity. É onde você faz inicializações (configurar a injeção de dependência, montar a UI com `setContent`, registrar observadores).
2. **onStart**: a UI se torna visível para o usuário — mas ainda não necessariamente interativa (por exemplo, pode estar parcialmente coberta por outra janela).
3. **onResume**: a Activity está em primeiro plano (foreground) e totalmente interativa — o usuário pode tocar nela.
4. **onPause**: a Activity perde o foco parcialmente (por exemplo, uma caixa de diálogo apareceu por cima). É o momento certo para salvar estado transitório leve, rapidamente, sem operações demoradas.
5. **onStop**: a Activity não está mais visível (por exemplo, o usuário abriu outro app). É o momento certo para liberar recursos que consomem bateria, como sensores e câmera.
6. **onDestroy**: limpeza final da Activity — exceto quando a Activity está sendo recriada por rotação de tela, caso em que o `ViewModel` (se usado corretamente) permanece vivo e não precisa ser recriado.

Em Compose, boa parte da lógica de estado é movida para o `ViewModel`, justamente para evitar que o `setContent` precise recriar do zero uma lógica pesada toda vez que a Activity passa por uma recriação.

### Erros comuns / Pegadinhas

- **Fazer chamadas de rede ou trabalho pesado direto em `onCreate`:** isso pode travar a interface enquanto a tela está sendo montada (lembra do "app não está respondendo" que vimos no arquivo 02?). Mova esse trabalho para o `ViewModel`, usando coroutines com `viewModelScope`.
- **Não liberar recursos em `onStop`:** esquecer de desligar a câmera ou parar sensores quando a tela não está mais visível continua consumindo bateria mesmo com o app em segundo plano.
- **Confundir "Activity destruída por rotação" com "Activity encerrada pelo usuário":** nos dois casos `onDestroy` é chamado, mas o motivo é diferente. Usar `ViewModel` para o estado da tela resolve o primeiro caso automaticamente — o estado sobrevive à rotação, mesmo com `onDestroy` sendo chamado.

---

## 4. Boas Práticas

- **Evite lógica pesada em `onCreate`**: use o `ViewModel` para inicializações demoradas ou que envolvam chamadas de rede/banco de dados.
- **Gerencie recursos corretamente**: libere sensores e câmeras em `onStop`, para não desperdiçar bateria enquanto o app está em segundo plano.
- **Salve estado essencial**: use `onSaveInstanceState` (ou `rememberSaveable` em Compose) para dados pequenos que precisam sobreviver a uma recriação inesperada da Activity, e o `ViewModel` para o restante do estado da tela.
- **Use APIs modernas**: prefira as Activity Result APIs (`registerForActivityResult`) e componentes cientes de ciclo de vida (`LifecycleObserver`), em vez das abordagens antigas equivalentes.

---

## 5. Exemplos

### Exemplo 1: Ciclo de Vida com Logs

Este exemplo é uma ótima forma de **ver o ciclo de vida acontecendo de verdade**: rode este código, abra o app, gire o celular, pressione o botão Home e volte — e observe no Logcat (o painel de logs do Android Studio) a ordem em que os callbacks são chamados.

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("CicloDeVida", "onCreate chamado")
        // Log.d registra uma mensagem de nível "debug" com a tag "CicloDeVida",
        // visível no painel Logcat do Android Studio (filtre por essa tag).
    }

    override fun onStart() {
        super.onStart()
        Log.d("CicloDeVida", "onStart chamado")
    }

    override fun onResume() {
        super.onResume()
        Log.d("CicloDeVida", "onResume chamado")
    }

    override fun onPause() {
        super.onPause()
        Log.d("CicloDeVida", "onPause chamado")
    }

    override fun onStop() {
        super.onStop()
        Log.d("CicloDeVida", "onStop chamado")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("CicloDeVida", "onDestroy chamado")
    }
}
```

> **Dica prática:** sempre chame `super.onX()` como a **primeira linha** de cada callback sobrescrito (exceto em alguns poucos casos documentados na API). Esquecer isso pode causar comportamento inesperado ou até crash, porque a classe-mãe (`ComponentActivity`) também precisa executar sua própria lógica interna nesse momento.

### Exemplo 2: Navegação com Compose

```kotlin
@Composable
fun MainScreen(navController: NavController) {
    // NavController é o objeto que comanda a navegação entre telas
    // dentro da mesma Activity — você vai estudar isso com profundidade
    // no arquivo 07 (Jetpack Compose) e no Módulo 2.
    Button(onClick = { navController.navigate("detalhes") }) {
        Text("Ir para Detalhes")
    }
}
```

---

## Resumo

- Uma Activity hospeda a interface com a qual o usuário interage; em projetos Compose modernos, geralmente existe apenas uma (`MainActivity`), e as telas internas são gerenciadas por Composables e Navigation.
- O ciclo de vida segue a sequência `onCreate → onStart → onResume → (foreground) → onPause → onStop → onDestroy`, com `onRestart` no retorno após `onStop`.
- Use `onCreate` para inicializações leves, `onStop` para liberar recursos pesados (câmera, sensores), e o `ViewModel` para guardar estado que precisa sobreviver a recriações da Activity.
- Prefira APIs modernas: Activity Result APIs em vez de `onActivityResult`, e componentes cientes de ciclo de vida em vez de checagens manuais.

**Próximo passo:** no arquivo 05, você vai ver uma referência rápida de outros componentes do Android (Fragment, Service, Broadcast Receiver) e como eles se relacionam com a Activity e com o ciclo de vida que você acabou de aprender.
