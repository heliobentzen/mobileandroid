# Intents no Android (Uso Moderno)

`Intents` são objetos de mensagem usados para solicitar uma ação de outro componente de aplicativo. Eles são um dos conceitos fundamentais do Android, servindo como o "cimento" que liga Activities, Services e Broadcast Receivers. Você já viu uma introdução rápida a Intents no arquivo 05 — aqui vamos aprofundar tipos, segurança e boas práticas.

## Por que Intents existem?

**O que é.** No Android, cada aplicativo roda em seu próprio processo isolado por segurança — ou seja, um app não consegue simplesmente "chamar uma função" de outro app diretamente, como faria dentro do mesmo programa. Os componentes não se comunicam diretamente — o sistema operacional atua como intermediário, e as **Intents são o mecanismo de mensageria** que conecta esses componentes.

Pense em uma Intent como um **envelope de carta**: você descreve *o que* deseja fazer (a ação, como "ver isto" ou "enviar isto") e, opcionalmente, *com quais dados* (o conteúdo da carta). Você não precisa saber exatamente quem vai receber — o Android (como um carteiro) encontra o destinatário correto e entrega a mensagem.

**Por que isso importa.** Sem Intents, seria impossível, por exemplo, abrir a câmera de dentro do seu app, compartilhar uma foto com o WhatsApp, ou simplesmente navegar de uma tela para outra dentro do próprio app (nas versões mais antigas do Android, antes do Navigation Compose). Intents são o mecanismo universal de comunicação entre partes do sistema.

## Quando usar cada abordagem

- **Navegação interna (telas do próprio app):** use **Navigation Compose**.
- **Integração com sistema/outros apps:** use **Intents** (implícitas na maioria dos casos).
- **Intent explícita interna:** mantenha para casos pontuais de interoperabilidade (módulos legados, entrada específica por Activity).

`Navigation Compose` é a biblioteca Jetpack moderna para navegação entre telas do próprio app — você vai vê-la em detalhe no arquivo 07. Aqui usamos apenas a regra prática de quando escolher cada abordagem.

## Tipos de Intents

1. **Intents Explícitas**: Especificam o componente exato a ser iniciado (pelo nome da classe). Usadas para iniciar componentes **dentro do seu próprio aplicativo**. É como escrever o nome completo do destinatário no envelope.
2. **Intents Implícitas**: Declaram uma ação geral sem especificar o componente. O sistema encontra um componente capaz de realizá-la — que pode ser de **outro aplicativo**. Ex.: abrir uma URL no navegador. É como escrever "para quem entrega pizza" no envelope, sem saber qual pizzaria vai atender.

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

## Boas Práticas (e Erros Comuns a Evitar)

### 1. Verificar se há aplicativos disponíveis
Use `resolveActivity()` antes de disparar uma intent implícita. Esse método retorna o `ComponentName` do app que responderia ou `null` se nenhum existir. Sem essa verificação, o app lança `ActivityNotFoundException` — um crash que acontece, por exemplo, se o usuário não tiver nenhum navegador instalado no dispositivo.

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

## Resumo

- Intents são o mecanismo de mensageria que conecta componentes Android (Activities, Services, Broadcast Receivers), inclusive entre apps diferentes.
- Intent explícita: você conhece o destino exato (uso interno pontual). Intent implícita: você descreve uma ação e deixa o sistema encontrar o destino (uso para sair do app).
- Para navegação **entre telas do seu próprio app**, prefira Navigation Compose em vez de Intents explícitas.
- Sempre verifique `resolveActivity()` antes de disparar uma intent implícita, para evitar `ActivityNotFoundException`.
- Nunca envie dados sensíveis (senhas, tokens) por intents implícitas — elas podem ser interceptadas por outros apps.
- Use constantes para chaves de `putExtra`/`getX`, evitando erros de digitação silenciosos.

**Próximo passo:** no arquivo 07, você vai aprender Jetpack Compose em profundidade — como construir interfaces declarativas, gerenciar estado com `remember` e aplicar o Material Design 3.

## Exercícios Práticos

Resolva na ordem — cada exercício constrói sobre o anterior.

### 1. Navegação interna (moderno) — checkpoint: 15 min

Crie duas telas em Compose (`ListaScreen` e `DetalheScreen`) e navegue entre elas com `Navigation Compose`.
💡 **Dica**: passar IDs e tipos simples na rota (`detalhe/{id}`) é esperado. Evite passar objetos complexos na rota; prefira carregá-los no destino a partir do ID.

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

### 2. Intent Implícita — checkpoint: 10 min

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

### 3. Desafio — checkpoint: 30 min

Crie um aplicativo com duas telas em Compose. Na primeira, o usuário insere um texto. Na segunda, o texto é exibido e pode ser compartilhado com outros aplicativos.
💡 **Dica (base):** Use `Navigation Compose` para navegar entre telas internas e passe apenas um `id` na rota. Para este exercício, mantenha uma estrutura em memória (ex.: `val mensagens = hashMapOf("1" to "Olá")`) e busque o conteúdo por `id` antes de compartilhar com `ACTION_SEND`.
💡 **Opcional (avançado):** quando chegar ao módulo de MVVM, substitua a estrutura local por `ViewModel` + repositório.

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
