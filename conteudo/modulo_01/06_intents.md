# Intents no Android

`Intents` são objetos de mensagem que você pode usar para solicitar uma ação de outro componente de aplicativo. Eles são um dos conceitos fundamentais do Android e servem como o "cimento" que liga os componentes (como Activities, Services e Broadcast Receivers) entre si.

Existem dois tipos principais de intents:

1.  **Intents Explícitas**: Especificam o componente exato a ser iniciado (pelo nome da classe). São usadas principalmente para iniciar componentes dentro do seu próprio aplicativo.
2.  **Intents Implícitas**: Não especificam o componente, mas declaram uma ação geral a ser executada. Isso permite que um componente de outro aplicativo a manipule. Por exemplo, se você quiser mostrar ao usuário uma localização em um mapa, pode usar uma intent implícita para solicitar que outro aplicativo capaz exiba a localização.

---

## Exemplo 1: Intent Explícita Simples

A forma mais comum de usar uma `Intent` é para iniciar uma nova `Activity`, mas com o advento do Jetpack Compose, a navegação interna pode ser feita de forma mais direta e eficiente.

**Cenário**: Navegar da `MainActivity` para uma `DetalhesActivity`.

**1. Código na `MainActivity.kt`**

```kotlin
val intent = Intent(this, DetalhesActivity::class.java)
intent.putExtra("chave", "valor")
startActivity(intent)
```

**2. Código na `DetalhesActivity.kt`**

```kotlin
val valor = intent.getStringExtra("chave")
```

---

## Exemplo 2: Intent Implícita

**Cenário**: Abrir um navegador para uma URL.

```kotlin
val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))
startActivity(intent)
```

**Cenário**: Compartilhar texto com outros aplicativos.

```kotlin
val intent = Intent(Intent.ACTION_SEND)
intent.type = "text/plain"
intent.putExtra(Intent.EXTRA_TEXT, "Texto para compartilhar")
startActivity(Intent.createChooser(intent, "Compartilhar via"))
```

---

## Boas Práticas

1. **Verificar se há aplicativos disponíveis**:
   - Antes de usar intents implícitas, verifique se há aplicativos capazes de lidar com a ação.

```kotlin
val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))
if (intent.resolveActivity(packageManager) != null) {
    startActivity(intent)
} else {
    Log.e("Intent", "Nenhum aplicativo disponível para abrir a URL")
}
```

2. **Evitar vazamento de dados sensíveis**:
   - Ao usar intents, tenha cuidado ao compartilhar dados sensíveis.

3. **Usar constantes para chaves**:
   - Defina constantes para as chaves usadas em `putExtra` para evitar erros de digitação.

```kotlin
companion object {
    const val EXTRA_CHAVE = "chave"
}
```

---

## Exercícios Práticos

1. **Intent Explícita**:
   - Crie uma `Activity` que receba um nome via `Intent` e exiba uma saudação personalizada.

2. **Intent Implícita**:
   - Implemente um botão que abra o aplicativo de e-mail para enviar uma mensagem.

3. **Desafio**:
   - Crie um aplicativo com duas telas. Na primeira, o usuário insere um texto. Na segunda, o texto é exibido e pode ser compartilhado com outros aplicativos.

---
