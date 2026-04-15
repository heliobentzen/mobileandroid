# Módulo 4: Publicação no Google Play

Objetivo: Preparar o app para distribuição gerando um Android App Bundle (AAB) assinado e conhecendo o fluxo básico do Google Play Console.

---

## 1. Preparação Antes de Publicar

Antes de gerar o build de release, verifique:

| Item | Detalhes |
|------|----------|
| **Versão** | `versionCode` incrementado e `versionName` atualizado em `build.gradle.kts` |
| **Ícone** | Ícone adaptativo configurado (`mipmap-anydpi-v26`) |
| **ProGuard/R8** | `isMinifyEnabled = true` no build type `release` para reduzir tamanho e ofuscar |
| **Permissões** | Apenas as necessárias declaradas no `AndroidManifest.xml` |
| **Testes** | Testes unitários e de UI passando (Módulo anterior) |
| **README** | Instruções de setup e build documentadas |

---

## 2. Configurando a Assinatura

O Google Play exige que todo app seja assinado digitalmente. Há dois cenários:

### Opção A: Assinatura Local (Keystore)

1. **Gerar a keystore** (uma única vez):

```bash
keytool -genkeypair \
  -alias meu-app \
  -keyalg RSA -keysize 2048 \
  -validity 10000 \
  -keystore meu-app-release.jks
```

2. **Configurar no `build.gradle.kts`**:

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("meu-app-release.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
            keyAlias = "meu-app"
            keyPassword = System.getenv("KEY_PASSWORD") ?: ""
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

> **Importante**: Nunca commite senhas diretamente no código. Use variáveis de ambiente ou um arquivo `local.properties` (listado no `.gitignore`).

### Opção B: Play App Signing (Recomendado)

O Google gerencia a chave de assinatura de produção. Você assina com uma chave de upload e o Play Console re-assina para distribuição. Vantagem: se perder a chave de upload, o Google pode gerar uma nova.

---

## 3. Gerando o AAB (Android App Bundle)

O AAB é o formato preferido pelo Google Play. Ele permite que o Play Store gere APKs otimizados para cada dispositivo.

### Via Android Studio

1. Menu: **Build → Generate Signed Bundle / APK...**
2. Selecione **Android App Bundle**
3. Escolha a keystore e insira as credenciais
4. Selecione o build variant `release`
5. Clique em **Create**
6. O arquivo `.aab` será gerado em `app/build/outputs/bundle/release/`

### Via linha de comando

```bash
./gradlew bundleRelease
```

O AAB será gerado em `app/build/outputs/bundle/release/app-release.aab`.

---

## 4. Checklist do Google Play Console

Para publicar na Play Store (mesmo na trilha interna de testes):

### 4.1 Criar conta de desenvolvedor
- Acesse [play.google.com/console](https://play.google.com/console)
- Taxa única de US$ 25

### 4.2 Criar o app no Console
1. Clique em **Criar app**
2. Preencha: nome, idioma padrão, tipo (app/jogo), gratuito/pago

### 4.3 Ficha da loja (Store Listing)
| Campo | Requisito |
|-------|-----------|
| Título | Até 30 caracteres |
| Descrição curta | Até 80 caracteres |
| Descrição completa | Até 4000 caracteres |
| Ícone | 512×512 px, PNG |
| Feature graphic | 1024×500 px |
| Screenshots | Mínimo 2, tamanho recomendado pelo console |

### 4.4 Classificação de conteúdo
- Preencha o questionário de classificação do IARC (obrigatório)

### 4.5 Enviar o AAB
1. Vá em **Release → Testing → Internal testing**
2. Clique em **Create new release**
3. Faça upload do arquivo `.aab`
4. Adicione notas da versão
5. Revise e publique na trilha interna

### 4.6 Adicionar testadores
- Na trilha interna, crie uma lista de e-mails de testadores
- Compartilhe o link de opt-in para que eles instalem o app

---

## 5. Trilhas de Distribuição

| Trilha | Finalidade | Público |
|--------|-----------|---------|
| **Interna** | Testes rápidos da equipe | Até 100 testadores |
| **Fechada (Alpha)** | Teste com grupo maior | Lista de e-mails ou grupo do Google |
| **Aberta (Beta)** | Teste público antes do lançamento | Qualquer usuário pode participar |
| **Produção** | Lançamento oficial | Todos os usuários da Play Store |

Recomendação: comece sempre pela trilha interna, valide, e depois promova para as próximas.

---

## 6. Boas Práticas de Release

1. **Versionamento semântico**: use `versionName` como `1.0.0` (major.minor.patch) e incremente `versionCode` a cada release.
2. **Notas de versão**: descreva o que mudou para os usuários.
3. **Rollout gradual**: na produção, use rollout de 10% → 50% → 100% para detectar problemas cedo.
4. **Monitoramento**: ative o Firebase Crashlytics ou ferramenta similar para acompanhar crashes em produção.
5. **README do projeto**: mantenha instruções claras de como gerar o build de release.

---

## 7. Exercícios Práticos

1. **Gerar AAB**: Configure a assinatura e gere um AAB assinado do seu projeto via linha de comando.

2. **Checklist de release**: Preencha todos os campos obrigatórios do Play Console para uma trilha interna (pode ser com dados fictícios para prática).

3. **README de release**: Escreva uma seção no README do seu projeto com instruções de:
   - Como gerar o build de release
   - Onde encontrar o arquivo `.aab`
   - Como incrementar a versão

4. **Desafio**: Configure um workflow no GitHub Actions que execute os testes e gere o AAB automaticamente a cada push na branch `main`.

---

## Entrega Final

Ao concluir este módulo, você deve ter:

- [ ] Tela principal funcional com Jetpack Compose
- [ ] ViewModel testado (pelo menos 2 testes unitários)
- [ ] Build AAB assinado e pronto para upload
- [ ] README com instruções de setup e release

---
