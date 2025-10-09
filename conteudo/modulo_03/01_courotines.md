# Coroutines: Do Básico ao Avançado

Imagine que seu aplicativo é um restaurante. A interface do usuário (UI) é o garçom que anota os pedidos e serve os clientes na mesa. A cozinha é onde as tarefas demoradas, como cozinhar um prato complexo, acontecem.

## O Problema: Um Garçom Sobrecarregado (A Thread Principal)

Por padrão, todo aplicativo Android tem uma "thread principal" (ou "UI thread"). Pense nela como um único garçom para todo o restaurante. Este garçom é responsável por tudo que o usuário vê e com o que interage: desenhar botões, processar cliques, exibir animações, etc.

O que acontece se você pedir a este garçom para ir à cozinha e preparar um prato que leva 10 minutos? Durante esses 10 minutos, ele não poderá atender outros clientes, anotar novos pedidos ou sequer responder a uma pergunta. O restaurante inteiro "congela".

No Android, isso é o que acontece quando você executa uma tarefa longa (como baixar um arquivo, acessar um banco de dados ou processar uma imagem) na thread principal. O aplicativo para de responder, resultando em uma péssima experiência para o usuário e, eventualmente, no temido erro "Application Not Responding" (ANR).

## A Solução Clássica: Contratar Ajudantes (Threads)

A solução óbvia é contratar mais gente. Podemos ter um cozinheiro na cozinha (uma "background thread") para preparar os pratos demorados, enquanto o garçom (a "UI thread") continua atendendo os clientes.

Isso é **concorrência**: executar várias tarefas ao mesmo tempo. As Threads são a forma tradicional de se fazer isso.

**O problema com as Threads:**
1.  **São "caras"**: Criar e destruir threads consome bastante memória e recursos do sistema. É como contratar e demitir um funcionário toda vez que um novo prato é pedido.
2.  **Comunicação complexa**: A comunicação entre o garçom e o cozinheiro pode ser complicada. Como o cozinheiro avisa que o prato está pronto? E se o cliente cancelar o pedido no meio do preparo? Gerenciar essa comunicação entre threads é difícil e propenso a erros.

## A Solução Moderna: Coroutines (Um Garçom Multitarefa)

Coroutines são como um garçom super-eficiente que sabe gerenciar seu tempo. Em vez de ir para a cozinha e ficar parado esperando o prato ficar pronto, ele pode:

1.  Levar o pedido para a cozinha (`launch` uma coroutine).
2.  Deixar o prato cozinhando (`suspend` a tarefa).
3.  Enquanto o prato cozinha, ele volta ao salão para atender outros clientes (a UI thread continua livre).
4.  Quando o prato está pronto, ele é notificado e vai buscá-lo para servir (a coroutine `resume`).

Coroutines são "tarefas leves" que rodam em cima de threads existentes. Elas podem ser **suspensas** e **retomadas**. Suspender uma coroutine não bloqueia a thread; apenas libera a thread para fazer outras coisas. Isso nos permite executar muitas operações concorrentes com um número pequeno de threads, tornando-as muito mais eficientes.

Neste módulo, vamos explorar os pilares que tornam as coroutines tão poderosas: `Dispatchers` (onde a tarefa é executada), Concorrência Estruturada (como gerenciar o ciclo de vida das tarefas) e Cancelamento (como parar uma tarefa de forma segura).

---

## 1. Dispatchers: Onde a Tarefa Acontece (Cozinha, Balcão, etc.)

Um `Dispatcher` determina em qual thread (ou conjunto de threads) a coroutine será executada. É como decidir se uma tarefa deve ser feita pelo garçom no salão ou pelo cozinheiro na cozinha.

Os principais dispatchers são:

-   **`Dispatchers.Main`**: A "thread do garçom". Exclusiva para interagir com a UI. Usá-la para tarefas longas bloqueia o aplicativo.
-   **`Dispatchers.IO`**: A "cozinha". Otimizada para operações de entrada e saída (I/O), como chamadas de rede, acesso a banco de dados ou leitura/escrita de arquivos.
-   **`Dispatchers.Default`**: A "área de preparação". Otimizada para tarefas que usam intensivamente o processador (CPU), como ordenar uma lista gigante, cálculos matemáticos complexos ou aplicar um filtro em uma imagem.
-   **`Dispatchers.Unconfined`**: Um "ajudante coringa". Inicia na thread atual, mas pode pular para qualquer outra. Seu uso é raro e deve ser feito com cuidado.

### Trocando de Contexto com `withContext`

A função `withContext` permite trocar de dispatcher de forma segura. É como o garçom levando um pedido para a cozinha e esperando lá até que o cozinheiro termine, para então pegar o prato e voltar ao salão.

```kotlin
viewModelScope.launch { // Inicia no Dispatchers.Main (padrão do viewModelScope)
    showLoadingSpinner() // Tarefa de UI, ok aqui.

    // Muda para a "cozinha" (thread de I/O) para buscar dados da internet.
    val result = withContext(Dispatchers.IO) {
        api.fetchData() // Chamada de rede demorada.
    }

    // `withContext` terminou, estamos de volta ao Dispatchers.Main automaticamente.
    hideLoadingSpinner()
    updateUi(result) // Atualiza a UI com o resultado.
}
```

---

## 2. Concorrência Estruturada: Gerenciando a Equipe

Concorrência Estruturada garante que as coroutines sejam lançadas dentro de um escopo (`CoroutineScope`), criando uma relação de "pai e filho". Isso impõe ordem e previne vazamentos de recursos (coroutines "zumbis").

Pense no `CoroutineScope` como o gerente do turno do restaurante.

1.  **Ciclo de Vida Contido**: Se o gerente encerra o turno (`scope` é cancelado), todos os garçons e cozinheiros sob sua gerência param o que estão fazendo e vão para casa (todas as coroutines filhas são canceladas). Isso é crucial em Android: quando uma tela é destruída, o `viewModelScope` é cancelado, limpando todas as tarefas de rede ou banco de dados associadas a ela.
2.  **Propagação de Erros**: Se um cozinheiro causa um incêndio na cozinha (uma coroutine filha lança uma exceção), o gerente é notificado e imediatamente encerra o turno para todos, por segurança (a exceção sobe para o pai, cancelando o escopo e as outras coroutines).
3.  **Cancelamento Simplificado**: Para cancelar um grande pedido com vários pratos, basta falar com o gerente. Ele se encarrega de avisar todos os cozinheiros envolvidos.

```kotlin
// viewModelScope é o "gerente" do nosso ViewModel.
viewModelScope.launch { // Coroutine "pai"
    println("Gerente: Iniciando o turno.")

    val job1 = launch { // Coroutine "filha" 1
        delay(1000)
        println("Cozinheiro 1: Prato A pronto.")
    }

    val job2 = launch { // Coroutine "filha" 2
        delay(500)
        println("Cozinheiro 2: Prato B pronto.")
    }

    // O gerente (launch pai) só considera o turno encerrado
    // quando todos os seus cozinheiros terminarem suas tarefas.
}
```
Se o ViewModel for destruído, o `viewModelScope` é cancelado, e `job1` e `job2` são interrompidos automaticamente.

---

## 3. Cancelamento Cooperativo

O cancelamento em coroutines é **cooperativo**. Uma coroutine não pode ser forçada a parar abruptamente. Ela precisa "colaborar" com o pedido de cancelamento.

Imagine que você pede ao cozinheiro para parar de preparar um prato. Ele não vai simplesmente largar a faca no meio de um corte. Ele vai terminar o movimento atual, limpar a faca e então parar. Ele verifica periodicamente se o pedido foi cancelado.

Uma coroutine só pode ser cancelada em um **ponto de suspensão**. Todas as funções de suspensão da biblioteca `kotlinx.coroutines` (`delay()`, `yield()`, `withContext()`, etc.) são pontos de suspensão que verificam o estado de cancelamento e lançam uma `CancellationException` se a coroutine foi cancelada.

### Tornando uma Coroutine Cancelável

Se você tem um trabalho computacional longo sem pontos de suspensão, ele ignorará o cancelamento.

**Forma incorreta (não cancelável):**
```kotlin
val job = launch(Dispatchers.Default) {
    // Este cozinheiro está tão focado em picar cebolas que não ouve ninguém.
    var i = 0
    while (true) { // Loop infinito sem ponto de suspensão.
        i++
    }
}
delay(1000)
job.cancelAndJoin() // O pedido de cancelamento será ignorado. A coroutine nunca para.
```

**Forma correta (cancelável):**
```kotlin
val job = launch(Dispatchers.Default) {
    var i = 0
    // O cozinheiro agora verifica se o gerente pediu para parar (isActive).
    while (isActive) {
        i++
    }
    println("Loop encerrado por cancelamento.")
}
delay(1000)
println("Gerente: Cancelem o trabalho!")
job.cancelAndJoin() // Agora o cancelamento funciona.
println("Trabalho cancelado.")
```

Usar a propriedade `isActive` em loops computacionais garante que sua coroutine coopere com o cancelamento, tornando seu aplicativo mais seguro e previsível.