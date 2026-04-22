# Intents no Android (Uso Moderno)

`Intents` são objetos de mensagem usados para solicitar uma ação de outro componente de aplicativo. Eles são um dos conceitos fundamentais do Android, servindo como o "cimento" que liga Activities, Services e Broadcast Receivers.

## Por que Intents existem?

No Android, cada aplicativo roda em seu próprio processo isolado por segurança. Os componentes não se comunicam diretamente — o sistema operacional atua como intermediário, e as **Intents são o mecanismo de mensageria** que conecta esses componentes. Pense em uma Intent como um **envelope**: você descreve *o que* deseja fazer (a ação) e, opcionalmente, *com quais dados*. O Android encontra o destinatário correto e entrega a mensagem.

## Quando usar cada abordagem

- **Navegação interna (telas do próprio app):** use **Navigation Compose**.
- **Integração com sistema/outros apps:** use **Intents** (implícitas na maioria dos casos).
- **Intent explícita interna:** mantenha para casos pontuais de interoperabilidade (módulos legados, entrada específica por Activity).

## Tipos de Intents

1. **Intents Explícitas**: Especificam o componente exato a ser iniciado (pelo nome da classe). Usadas para iniciar componentes **dentro do seu próprio aplicativo**.
2. **Intents Implícitas**: Declaram uma ação geral sem especificar o componente. O sistema encontra um componente capaz de realizá-la — que pode ser de **outro aplicativo**. Ex.: abrir uma URL no navegador.

---

## Exemplo 1: Intent Explícita (caso pontual)

**Cenário**: Abrir uma `SupportActivity` interna para um fluxo legado específico.

**1. Na `MainActivity.kt`**

```kotlin
// Intent explícita para um componente específico do próprio app
val intent = Intent(this, SupportActivity::class.java)
// Dados de contexto para o fluxo legado
intent.putExtra("source", "help_center")
startActivity(intent)
```

**2. Na `SupportActivity.kt`**

```kotlin
// Recupera o valor enviado pela Activity anterior usando a mesma chave
val source = intent.getStringExtra("source") // retorna null se a chave não existir
```
---

## Exemplo 2: Intent Implícita

**Cenário**: Abrir um navegador para uma URL.

```kotlin
// ACTION_VIEW pede ao sistema para "visualizar" o recurso indicado pela Uri
val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))
startActivity(intent)
```

**Cenário**: Compartilhar texto com outros aplicativos.
```kotlin
// ACTION_SEND indica que queremos enviar dados para outro app
val intent = Intent(Intent.ACTION_SEND)
intent.type = "text/plain" // define o tipo MIME dos dados
intent.putExtra(Intent.EXTRA_TEXT, "Texto para compartilhar")
// createChooser exibe um seletor para o usuário escolher o app de destino
startActivity(Intent.createChooser(intent, "Compartilhar via"))
```

---

## Intents vs Navigation Compose

| Situação | Solução recomendada |
|---|---|
| Navegar entre telas **dentro** do seu app | **Navigation Compose** — mais leve, sem criar novas Activities |
| Abrir outro aplicativo (navegador, e-mail, mapa) | **Intent Implícita** — o sistema encontra o app adequado |
| Iniciar uma Activity de outro módulo ou app | **Intent Explícita** — quando você conhece o destino exato |

> **Regra prática**: destino interno → Navigation Compose. Destino externo ou componentes Android (Services, Broadcast Receivers) → Intents.

---

## Boas Práticas

### 1. Verificar se há aplicativos disponíveis
Use `resolveActivity()` antes de disparar uma intent implícita. Esse método retorna o `ComponentName` do app que responderia ou `null` se nenhum existir. Sem essa verificação, o app lança `ActivityNotFoundException`.

```kotlin
val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))
// resolveActivity consulta o PackageManager e retorna null se nenhum app atender
if (intent.resolveActivity(packageManager) != null) {
    startActivity(intent)
} else {
    Log.e("Intent", "Nenhum aplicativo disponível para abrir a URL")
}
```

### 2. Evitar vazamento de dados sensíveis
Intents implícitas podem ser interceptadas por qualquer app que declare o filtro correspondente. Nunca envie senhas, tokens ou dados pessoais por esse canal.

```kotlin
// ❌ Evite — qualquer app com filtro ACTION_SEND pode ler o token
val intentInsegura = Intent(Intent.ACTION_SEND)
intentInsegura.putExtra(Intent.EXTRA_TEXT, "token=abc123secreto")
// ✅ Prefira — intent explícita garante que só o seu componente recebe os dados
val intentSegura = Intent(this, MinhaActivityInterna::class.java)
intentSegura.putExtra("token", "abc123secreto")
```

### 3. Usar constantes para chaves
Definir constantes para as chaves de `putExtra` evita erros de digitação e centraliza a manutenção.

```kotlin
class DetalhesActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_NOME = "extra_nome" // constante compartilhada
    }
}
// Na Activity de origem, use a constante em vez de uma string literal
val intent = Intent(this, DetalhesActivity::class.java)
intent.putExtra(DetalhesActivity.EXTRA_NOME, "Maria")
```

---

## Exercícios Práticos

### 1. Navegação interna (moderno)

Crie duas telas em Compose (`ListaScreen` e `DetalheScreen`) e navegue entre elas com `Navigation Compose`.
💡 **Dica**: passe apenas o ID na rota (`detalhe/{id}`) e carregue os dados completos no destino. Isso evita rotas longas/frágeis e mantém responsabilidades melhor separadas.

```kotlin
// No NavHost:
// composable("lista") { ListaScreen(onOpen = { id -> navController.navigate("detalhe/$id") }) }
// composable(
//     route = "detalhe/{id}",
//     arguments = listOf(navArgument("id") { type = NavType.StringType })
// ) { backStackEntry ->
//     val id = requireNotNull(backStackEntry.arguments?.getString("id"))
//     DetalheScreen(id)
// }
```

### 2. Intent Implícita

Implemente um botão que abra o aplicativo de e-mail para enviar uma mensagem.
💡 **Dica**: Use `Intent.ACTION_SENDTO` com uma Uri `mailto:` para direcionar apenas apps de e-mail.

```kotlin
val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
    data = Uri.parse("mailto:") // garante que só apps de e-mail respondam
    putExtra(Intent.EXTRA_EMAIL, arrayOf("destino@email.com"))
    putExtra(Intent.EXTRA_SUBJECT, "Assunto do e-mail")
}
// Lembre-se de verificar com resolveActivity() antes de chamar startActivity()
```

### 3. Desafio

Crie um aplicativo com duas telas em Compose. Na primeira, o usuário insere um texto. Na segunda, o texto é exibido e pode ser compartilhado com outros aplicativos.
💡 **Dica**: Use `Navigation Compose` para navegar entre telas internas e passe apenas um `id` na rota. Em arquitetura MVVM, recupere o texto via `ViewModel`/repositório; em versão simples de estudo, mantenha uma estrutura em memória (ex.: `val mensagens = hashMapOf("1" to "Olá")`) e busque o conteúdo por `id` antes de compartilhar com `ACTION_SEND`.

```kotlin
// Tela 1 — navegar para a rota da tela 2 passando apenas um id:
// navController.navigate("exibicao/$mensagemId")
// Tela 2 — botão de compartilhar com outro app:
// val compartilharIntent = Intent(Intent.ACTION_SEND).apply {
//     type = "text/plain"
//     putExtra(Intent.EXTRA_TEXT, textoCarregadoPeloId)
// }
// startActivity(Intent.createChooser(compartilharIntent, "Compartilhar via"))
```

---
