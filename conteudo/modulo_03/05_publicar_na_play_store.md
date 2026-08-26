# Aula 5 — Publicando na Play Store

**Objetivo:** entender e percorrer o caminho que leva o seu app até a loja.

Aqui não tem código. Tem formulário, imagem e paciência. É a parte que quase ninguém ensina — e é ela que separa "fiz um app na aula" de "meu app está no ar".

---

## 1. Antes de tudo: a conta de desenvolvedor

Para publicar na Play Store você precisa de uma **conta de desenvolvedor Google Play**:

- Endereço: [play.google.com/console](https://play.google.com/console)
- Custo: **US$ 25**, pagos **uma única vez** (não é mensalidade) — vale para todos os apps que você publicar depois
- Precisa de: conta Google, cartão internacional e um documento de identidade

> **Não tem como pagar? Sem problema para este curso.** Você não precisa de conta para concluir o módulo. O APK assinado da Aula 4 já é um app completo e instalável — é ele que você entrega. Leia esta aula mesmo assim: saber como a publicação funciona vale tanto quanto fazê-la, e um dia você vai precisar.
>
> Alternativas gratuitas para distribuir: mandar o APK direto, ou publicar em lojas sem taxa como a [F-Droid](https://f-droid.org/) (para apps de código aberto) ou a [Amazon Appstore](https://developer.amazon.com/apps-and-games).

---

## 2. Criar o app no Play Console

Dentro do Console: **Criar app**. Preencha:

- Nome do app (o que aparece na loja, até 30 caracteres)
- Idioma padrão: Português (Brasil)
- App ou jogo
- Gratuito ou pago — **atenção: se marcar gratuito, não dá para mudar para pago depois**
- Aceite das declarações

---

## 3. A ficha da loja

A **ficha da loja** é a página do seu app na Play Store. É ela que decide se alguém instala ou passa direto.

| Item | Limite | Como fazer bem |
|------|--------|----------------|
| Título | 30 caracteres | Nome claro. "Minhas Tarefas", não "App Tarefas 2024 Grátis" |
| Descrição curta | 80 caracteres | Uma frase dizendo o que o app resolve: *"Anote suas tarefas da escola e não esqueça mais nada."* |
| Descrição completa | 4000 caracteres | Comece pelo mais importante. Liste as funções em tópicos. Escreva para uma pessoa, não para o Google. |
| Ícone | 512×512 px, PNG | Aquele que você guardou na Aula 3 |
| Imagem de destaque | 1024×500 px | Uma arte com o nome do app. Dá para fazer no Canva em 10 minutos. |
| Screenshots | Mínimo 2, celular | Prints das telas principais do app rodando |

**Como tirar bons screenshots:** rode o app no emulador, use o botão de câmera na barra lateral do emulador. Mostre o app **com conteúdo** (lista cheia de tarefas), nunca com a tela vazia.

---

## 4. Os formulários obrigatórios

Esta é a parte que trava a maioria das pessoas. Nenhum deles pode ser pulado.

| Formulário | O que é | O que responder |
|------------|---------|-----------------|
| **Classificação de conteúdo** | Questionário sobre violência, conteúdo sensível etc. | Responda com sinceridade. Um app de tarefas recebe "Livre" |
| **Público-alvo e conteúdo** | Faixa etária a que o app se destina | Se marcar que atinge crianças, entram regras bem mais rígidas |
| **Segurança dos dados** | O que o app coleta e envia | Se o app só salva no próprio celular (Aula 1): **não coleta nem compartilha dados** |
| **Política de privacidade** | Um link para uma página explicando o uso dos dados | Obrigatório na maioria dos casos |
| **Anúncios** | Se o app tem propaganda | "Não" para o app do curso |

### Sobre a política de privacidade

É uma página pública dizendo quais dados o app usa. Para um app simples que não coleta nada, ela cabe em um parágrafo. Você pode:

- Escrever você mesmo e publicar de graça no GitHub Pages, Google Sites ou num Gist público
- Usar um gerador gratuito (procure por *privacy policy generator*)

Depois é só colar o link no Console.

---

## 5. Enviando o app

1. Menu lateral: **Testar e lançar → Teste interno**
2. **Criar nova versão**
3. Aceite o **Play App Signing** quando aparecer (recomendado — o Google guarda uma cópia segura da sua chave de produção; se você perder a sua, dá para recuperar)
4. Faça upload do arquivo **`app-release.aab`** da Aula 4
5. Escreva as **notas da versão**: o que tem nesta versão, em linguagem simples
6. **Revisar → Iniciar lançamento**
7. Em **Testadores**, crie uma lista com e-mails (o seu e o dos colegas) e copie o **link de aceitação**

Quem abrir o link, aceitar e instalar recebe o app pela própria Play Store. Já é publicação de verdade — só que com público restrito.

> **Atenção:** o e-mail do testador precisa ser o mesmo da conta Google usada na Play Store do celular dele, senão o link não funciona.

---

## 6. Do teste até a loja pública

O app não vai direto do seu computador para o mundo. Ele passa por **trilhas**:

| Trilha | Quem vê |
|--------|---------|
| **Teste interno** | Até 100 pessoas que você listar. Liberação em minutos. |
| **Teste fechado** | Grupo maior, ainda convidado |
| **Teste aberto** | Qualquer pessoa que quiser entrar no teste |
| **Produção** | Todo mundo, na busca da Play Store |

Comece **sempre** pelo teste interno. É rápido e é onde você descobre os problemas com custo zero.

> **Regra importante:** contas de desenvolvedor **pessoais** criadas nos últimos anos precisam de um teste fechado com **pelo menos 12 testadores por 14 dias seguidos** antes de liberar para produção. As regras mudam de tempos em tempos — confira sempre o que o próprio Play Console está pedindo na sua conta.

Ao enviar para produção, o app entra em **revisão** do Google. Costuma levar de algumas horas a alguns dias.

---

## 7. Depois de publicado

- Toda nova versão precisa de `versionCode` **maior** que a anterior (Aula 3)
- Em produção, use **lançamento gradual**: libere para 20% dos usuários, veja se aparece problema, depois vá para 100%
- Acompanhe a aba **Qualidade** no Console: ela mostra travamentos e erros dos usuários reais

---

## Erros comuns

| Problema | Causa | Solução |
|----------|-------|---------|
| "Você já usou o código de versão 1" | Enviou o mesmo `versionCode` duas vezes | Aumente o `versionCode`, gere o AAB de novo |
| Não consigo avançar para lançamento | Algum formulário obrigatório em aberto | O painel do Console lista o que falta — resolva um por um |
| O testador não consegue instalar | E-mail diferente do da conta Google do celular | Adicione o e-mail correto na lista de testadores |
| App reprovado na revisão | Permissão sem justificativa, ficha incompleta ou política de privacidade ausente | Leia o e-mail do Google — ele diz exatamente o motivo — corrija e reenvie |
| Subi o APK e deu erro | A Play Store quer AAB | Envie o `app-release.aab` |

---

## Resumo

- Conta de desenvolvedor: **US$ 25, pagos uma vez**. Sem ela, o APK assinado ainda é uma entrega completa.
- A **ficha da loja** (título, descrições, ícone, imagem de destaque, screenshots) é a vitrine do app.
- Os formulários obrigatórios — classificação, público-alvo, segurança dos dados, política de privacidade — travam a publicação se ficarem em branco.
- Envie o **AAB** primeiro para o **teste interno**, depois avance para produção.
- Cada nova versão precisa de um `versionCode` maior.

👉 Próxima e última aula: [Projeto final](06_projeto_final.md)
