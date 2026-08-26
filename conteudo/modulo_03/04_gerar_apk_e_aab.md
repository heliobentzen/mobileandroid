# Aula 4 — Gerando o arquivo do app (APK e AAB)

**Objetivo:** gerar o arquivo final do seu app — o mesmo tipo de arquivo que está por trás de qualquer app instalado no seu celular.

Até agora o app só rodava quando ligado no Android Studio. Nesta aula ele vira um arquivo independente, que você pode mandar para um colega no WhatsApp ou enviar para a Play Store.

---

## 1. Dois formatos: APK e AAB

| Formato | Para que serve | Dá para instalar direto? |
|---------|----------------|--------------------------|
| **APK** | Mandar para amigos, professor, testar | ✅ Sim |
| **AAB** | Enviar para a Play Store | ❌ Não |

O **AAB** é uma "caixa de peças". A Play Store recebe essa caixa e monta um APK sob medida para cada celular — só com as imagens e o código que aquele aparelho usa. Por isso o download fica menor.

**Você vai gerar os dois:** o APK para testar e mostrar, o AAB para publicar.

---

## 2. Assinar o app

Todo app Android precisa ser **assinado**. Assinar é carimbar o arquivo com uma chave secreta que só você tem.

**Para que serve:**

- Prova que o app é seu, e não de alguém se passando por você.
- Se alguém alterar o arquivo depois de assinado, o celular percebe e recusa a instalação.
- Só uma atualização assinada com a **mesma chave** substitui o app instalado.

Essa chave fica guardada em um arquivo chamado **keystore** (extensão `.jks`). Você cria uma vez e usa para sempre.

> ⚠️ **O aviso mais importante do módulo:** se você perder o arquivo `.jks` ou esquecer a senha, **nunca mais** conseguirá atualizar esse app na Play Store. Teria que publicar tudo de novo, do zero, como um app diferente. Faça backup do arquivo e anote as senhas.

---

## 3. Criando a keystore e gerando o app

Tudo pelo Android Studio, sem terminal.

1. Menu **Build → Generate Signed App Bundle / APK...**
2. Escolha **Android App Bundle** e clique em **Next**
3. Clique em **Create new...** (só na primeira vez que você faz isso)
4. Preencha:

| Campo | O que colocar |
|-------|---------------|
| Key store path | Onde salvar. Escolha uma pasta que você **não** vá apagar. Ex.: `Documentos/chaves/meu-app.jks` |
| Password | Senha do arquivo. **Anote.** |
| Alias | Um apelido para a chave: `minhas-tarefas` |
| Password (da chave) | Pode ser a mesma. **Anote também.** |
| Validity | Deixe 25 anos (o padrão) |
| First and Last Name / Organization | Seu nome e sua escola. Não precisa ser formal. |

5. **OK → Next**
6. Em **Build Variants**, escolha **release**
7. **Create**

Espere terminar (a primeira vez demora um pouco). No canto inferior aparece um aviso com o link **locate** — clique nele para abrir a pasta do arquivo.

O `.aab` fica em:

```
app/build/outputs/bundle/release/app-release.aab
```

### Agora gere também o APK

Repita o processo, mas no passo 2 escolha **APK**. O arquivo fica em:

```
app/build/outputs/apk/release/app-release.apk
```

Esse é o arquivo que você manda para alguém instalar.

---

## 4. Guardando a keystore com segurança

Faça isso **hoje**, não depois:

- [ ] Copiei o arquivo `.jks` para o Google Drive (ou pen drive, ou e-mail para mim mesmo)
- [ ] Anotei as duas senhas e o alias em um lugar que eu vou lembrar
- [ ] **Não** coloquei o `.jks` nem as senhas no GitHub

Se o projeto está no GitHub, garanta que o `.gitignore` tenha:

```gitignore
*.jks
keystore.properties
```

> Chave no GitHub = qualquer pessoa pode assinar um app falso com o seu nome.

---

## 5. Instalando o APK em um celular

1. Mande o `app-release.apk` para o celular (WhatsApp, Drive, cabo USB)
2. No celular, toque no arquivo
3. Vai aparecer um aviso de "fontes desconhecidas" — é normal, porque o app não veio da Play Store. Permita para o app que está instalando (Arquivos, WhatsApp...)
4. **Instalar**

Peça para dois ou três colegas instalarem e usarem. Isso é um teste de verdade: quase sempre alguém encontra algo que você não tinha visto.

---

## Erros comuns

| Erro | Causa | Solução |
|------|-------|---------|
| `Keystore was tampered with, or password was incorrect` | Senha errada | Confira a senha; não há como recuperar se foi perdida |
| `App not installed` no celular | Já existe uma versão do app assinada com outra chave (a de teste do Android Studio) | Desinstale a versão antiga do celular e instale de novo |
| Não acho o arquivo gerado | Procurou na pasta errada | Use o link **locate** do aviso, ou veja os caminhos acima |
| Tentei instalar o `.aab` e não abriu | AAB não é instalável | Gere e use o `.apk` |
| Versão de release fecha, mas a de debug funciona | O R8 removeu código usado pelo Gson/Retrofit | Se ativou `isMinifyEnabled = true`, desative por enquanto; para o curso, deixe `false` |

---

## Resumo

- **APK** = instalar e compartilhar. **AAB** = enviar para a Play Store.
- Todo app precisa de assinatura; a chave mora no arquivo `.jks` (keystore).
- **Perdeu a keystore, perdeu o app.** Backup e senhas anotadas, hoje.
- Tudo pelo menu **Build → Generate Signed App Bundle / APK**.
- Instale o APK em celulares de colegas antes de publicar.

👉 Próxima aula: [Publicando na Play Store](05_publicar_na_play_store.md)
