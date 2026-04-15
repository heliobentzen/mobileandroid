# Estrutura de Projeto Android

Todo projeto Android de qualidade precisa de uma organização clara de arquivos e pastas. Sem uma estrutura bem definida, à medida que o projeto cresce fica cada vez mais difícil entender onde cada coisa está, escrever testes e adicionar novas funcionalidades sem quebrar o que já existe.

Nesta seção, você vai aprender como estruturar um projeto Android de forma modular e escalável, seguindo as boas práticas recomendadas pelo Google.

---

## 1. Por que organizar?
Facilita manutenção, testes e crescimento. Separe por responsabilidade.

Sugestão inicial:
```
app/              (launcher, DI, navigation)
core-common/      (utilidades gerais)
core-network/     (Retrofit + interceptors)
core-database/    (Room)
core-domain/      (use cases + modelos)
feature-login/    (tela e lógica da feature)
design-system/    (componentes UI)
```

Cada módulo tem uma responsabilidade única, o que facilita trocá-lo ou testá-lo de forma independente.

## 2. Gradle básico
- Use Kotlin DSL (`build.gradle.kts`).
- Centralize versões em `libs.versions.toml`.
- Ative cache e paralelismo.
- Declare dependências de forma explícita.

## 3. Namespace vs applicationId
- Cada módulo Android: `android { namespace = "com.exemplo.feature.login" }`
- Só o módulo `app` tem `applicationId`.
- Padrão simples: `com.empresa.(core|feature|design).nome`.

## 4. Variantes (quando preciso)
- Build Types: `debug`, `release`.
- Flavors só se houver diferença real (ex: URL de API).
Exemplo mínimo:
```
android {
    flavorDimensions += "tier"
    productFlavors {
        create("free") { dimension = "tier"; applicationIdSuffix = ".free" }
        create("pro")  { dimension = "tier" }
    }
    buildTypes {
        getByName("debug") { isMinifyEnabled = false }
        getByName("release") { isMinifyEnabled = true }
    }
}
```
Evite muitos diretórios (`src/freeDebug/`) no início.

## 5. Boas práticas iniciais
- Extraia código comum para módulos sem Android.
- Menos flavors = build mais rápido.
- Use `implementation` como padrão (evite `api` cedo).
- Documente no README o que cada módulo faz.

Checklist rápido:
- Namespace definido?
- Version Catalog ativo?
- Flavors realmente úteis?
- Cache Gradle ligado?
- Diferença clara entre `applicationId` e `namespace`?

## Resumo

- Separe responsabilidades cedo.
- Mantenha variantes e módulos no mínimo.
- Centralize dependências.
- Adicione complexidade só quando necessário.

## Próximos passos

Os tópicos de camadas (data/domain/presentation), rede (Retrofit), persistência (Room), Repository, ViewModel e DI (Hilt) serão aprofundados nos módulos seguintes:

- **Módulo 2**: Arquitetura MVVM, ViewModel e fluxo de dados.
- **Módulo 3**: Coroutines, Retrofit, Room e Repository Pattern.

