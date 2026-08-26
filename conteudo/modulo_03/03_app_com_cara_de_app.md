# Aula 3 — Deixando o app com cara de app

**Objetivo:** transformar o seu projeto de exercício em algo que dá orgulho de mostrar.

Nenhuma linha difícil de código aqui. Só ajustes que fazem toda a diferença: nome, ícone, cor e versão. É a parte que faz o professor, o colega e a Play Store olharem o seu app como um app de verdade.

---

## 1. O nome que aparece no celular

Abra `app/src/main/res/values/strings.xml`:

```xml
<resources>
    <string name="app_name">Minhas Tarefas</string>
</resources>
```

Troque `app_name` pelo nome do seu app. É esse texto que aparece embaixo do ícone na tela inicial do celular.

**Dicas de nome:**

- Curto: cabe até uns 12 caracteres embaixo do ícone. Mais que isso, o Android corta com "…".
- Sem "App" no nome: "App de Tarefas" fica pior que "Minhas Tarefas".
- Na Play Store, o título pode ter até 30 caracteres — mas o nome no celular deve ser curto mesmo assim.

---

## 2. O ícone

O Android Studio gera o ícone para você, em todos os tamanhos necessários.

1. Clique com o botão direito na pasta `res`
2. **New → Image Asset**
3. Em **Icon Type**, deixe *Launcher Icons (Adaptive and Legacy)*
4. Em **Foreground Layer**, escolha:
   - **Clip Art** — um desenho pronto do Android (mais rápido, e já fica bom)
   - **Image** — uma imagem PNG sua (fundo transparente, quadrada)
   - **Text** — a inicial do nome do app
5. Em **Background Layer**, escolha uma cor sólida
6. **Next → Finish**

**Regras de ícone que funciona:**

- Uma coisa só no ícone. Desenho simples, sem detalhes pequenos.
- Nada de texto (fora uma letra grande). No tamanho real ele fica com meio centímetro.
- Bom contraste entre desenho e fundo.
- Olhe a pré-visualização pequena do próprio Image Asset: se você não reconhece ali, ninguém reconhece.

> Guarde também uma versão **512×512 px em PNG** do seu ícone. A Play Store vai pedir esse arquivo na Aula 5.

---

## 3. Cores do app

Em `app/src/main/java/.../ui/theme/Color.kt` você encontra as cores do tema. Trocar a cor principal já muda a cara do app inteiro (botões, campos, barras):

```kotlin
val Purple40 = Color(0xFF6650a4)   // troque por uma cor sua, ex: Color(0xFF0B7A4B)
```

O formato é `0xFF` + o código hexadecimal da cor (o mesmo do Paint, do Figma, de qualquer site de cores).

---

## 4. Versão e identidade do app

Abra o `build.gradle.kts` do módulo `app`. Procure o bloco `defaultConfig`:

```kotlin
android {
    namespace = "com.seunome.minhastarefas"

    defaultConfig {
        applicationId = "com.seunome.minhastarefas"   // a "identidade" do app
        minSdk = 24                                   // Android mais antigo aceito
        targetSdk = 35
        versionCode = 1                               // número interno, sempre aumenta
        versionName = "1.0"                           // versão que o usuário vê
    }
}
```

| Campo | O que é | Cuidado |
|-------|---------|---------|
| `applicationId` | O "CPF" do app. Dois apps no mundo não podem ter o mesmo. | **Nunca mude depois de publicar.** Se mudar, a Play Store trata como um app novo. |
| `versionCode` | Número inteiro: 1, 2, 3... | Precisa **aumentar** a cada envio, ou a Play Store recusa. |
| `versionName` | Texto que o usuário lê: "1.0", "1.1", "2.0" | Só informativo, use como quiser. |
| `minSdk` | Versão mínima do Android | `24` (Android 7) cobre praticamente todos os celulares em uso. |

**Como escolher o `applicationId`:** use algo único e sem acentos, no formato `com.seunome.nomedoapp`. Exemplo: `com.anasilva.minhastarefas`.

---

## 5. Só as permissões necessárias

Abra o `AndroidManifest.xml` e olhe as linhas `<uses-permission ...>`.

Deixe **apenas** as que o app realmente usa. Se o seu app é o da Aula 1 (só banco de dados), ele não precisa de permissão nenhuma. Se é o da Aula 2 (internet), precisa só da `INTERNET`.

> Pedir permissões que o app não usa assusta o usuário ("por que um app de tarefas quer minha localização?") e pode fazer a Play Store reprovar a publicação.

---

## 6. Teste em um celular de verdade

Emulador é bom, mas celular de verdade mostra coisas que o emulador esconde.

1. No celular: **Configurações → Sobre o telefone** → toque 7 vezes em **Número da versão**
2. Volte: aparece **Opções do desenvolvedor** → ative **Depuração USB**
3. Ligue o celular no computador pelo cabo, aceite o aviso na tela do celular
4. No Android Studio, o celular aparece na lista de dispositivos. Aperte ▶

**Confira no celular:**

- [ ] O nome embaixo do ícone está certo
- [ ] O ícone é o seu, não o robozinho verde padrão
- [ ] O app abre sem fechar sozinho
- [ ] Girei o celular e o app continuou funcionando
- [ ] Testei no modo avião (se o app usa internet)

---

## Erros comuns

| Problema | Causa | Solução |
|----------|-------|---------|
| O nome no celular continua o antigo | O `android:label` do manifest está com texto fixo | No manifest, use `android:label="@string/app_name"` |
| O ícone continua o robozinho | Cache antigo | Desinstale o app do celular e instale de novo |
| O celular não aparece no Android Studio | Depuração USB desligada, ou cabo só de carga | Reative a depuração e teste outro cabo |
| Cor mudou só em uma tela | Você mudou a cor solta no Composable, não no tema | Mude em `Color.kt` |

---

## Resumo

- `strings.xml` → nome do app no celular.
- **New → Image Asset** → ícone em todos os tamanhos. Guarde um PNG 512×512.
- `Color.kt` → cor principal do app inteiro.
- `build.gradle.kts` → `applicationId` (nunca muda), `versionCode` (sempre aumenta), `versionName`.
- Só as permissões necessárias no manifest.
- Sempre teste em um celular real antes de publicar.

👉 Próxima aula: [Gerando o arquivo do app (APK e AAB)](04_gerar_apk_e_aab.md)
