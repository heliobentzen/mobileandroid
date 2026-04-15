# Intents no Android

`Intents` são objetos de mensagem usados para solicitar uma ação de outro componente de aplicativo. Eles são um dos conceitos fundamentais do Android, servindo como o "cimento" que liga Activities, Services e Broadcast Receivers.

## Por que Intents existem?

No Android, cada aplicativo roda em seu próprio processo isolado por segurança. Os componentes não se comunicam diretamente — o sistema operacional atua como intermediário, e as **Intents são o mecanismo de mensageria** que conecta esses componentes. Pense em uma Intent como um **envelope**: você descreve *o que* deseja fazer (a ação) e, opcionalmente, *com quais dados*. O Android encontra o destinatário correto e entrega a mensagem.

## Tipos de Intents

1. **Intents Explícitas**: Especificam o componente exato a ser iniciado (pelo nome da classe). Usadas para iniciar componentes **dentro do seu próprio aplicativo**.
2. **Intents Implícitas**: Declaram uma ação geral sem especificar o componente. O sistema encontra um componente capaz de realizá-la — que pode ser de **outro aplicativo**. Ex.: abrir uma URL no navegador.

---

## Exemplo 1: Intent Explícita

**Cenário**: Navegar da `MainActivity` para uma `DetalhesActivity`, enviando dados.

**1. Na `MainActivity.kt`**

```kotlin
// Cria uma Intent explícita indicando a Activity de destino
val intent = Intent(this, DetalhesActivity::class.java)
// putExtra anexa dados ao "envelope" da Intent (chave → valor)
intent.putExtra("chave", "valor")
// Solicita ao sistema que inicie a DetalhesActivity
startActivity(intent)
```

**2. Na `DetalhesActivity.kt`**

```kotlin
// Recupera o valor enviado pela Activity anterior usando a mesma chave
val valor = intent.getStringExtra("chave") // retorna null se a chave não existir
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

### 1. Intent Explícita

Crie uma `Activity` que receba um nome via `Intent` e exiba uma saudação personalizada.
💡 **Dica**: Use `putExtra` para enviar o nome e `getStringExtra` para recuperá-lo.

```kotlin
// Na Activity de origem:
val intent = Intent(this, SaudacaoActivity::class.java)
intent.putExtra("nome", /* texto capturado do usuário */)
startActivity(intent)
// Na SaudacaoActivity — recupere o nome e monte a saudação:
// val nome = intent.getStringExtra("nome")
// Exiba: "Olá, $nome! Bem-vindo(a)!"
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

Crie um aplicativo com duas telas. Na primeira, o usuário insere um texto. Na segunda, o texto é exibido e pode ser compartilhado com outros aplicativos.
💡 **Dica**: Use uma intent explícita para navegar entre as telas e uma implícita (`ACTION_SEND`) para compartilhar.

```kotlin
// Tela 1 — enviar o texto digitado para a Tela 2:
// val intent = Intent(this, ExibicaoActivity::class.java)
// intent.putExtra(ExibicaoActivity.EXTRA_TEXTO, textoDigitado)
// Tela 2 — botão de compartilhar:
// val compartilharIntent = Intent(Intent.ACTION_SEND).apply {
//     type = "text/plain"
//     putExtra(Intent.EXTRA_TEXT, textoRecebido)
// }
// startActivity(Intent.createChooser(compartilharIntent, "Compartilhar via"))
```

---
