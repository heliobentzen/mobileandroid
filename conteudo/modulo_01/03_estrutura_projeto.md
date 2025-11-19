### Estrutura de Projeto: visão simples

#### 1. Por que organizar?
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

#### 2. Gradle básico
- Use Kotlin DSL (`build.gradle.kts`).
- Centralize versões em `libs.versions.toml`.
- Ative cache e paralelismo.
- Declare dependências de forma explícita.

#### 3. Namespace vs applicationId
- Cada módulo Android: `android { namespace = "com.exemplo.feature.login" }`
- Só o módulo `app` tem `applicationId`.
- Padrão simples: `com.empresa.(core|feature|design).nome`.

#### 4. Variantes (quando preciso)
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

#### 5. Boas práticas iniciais
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

#### 6. Camadas simples
- data: fala com rede e banco.
- domain: regras e use cases.
- presentation: ViewModel + UI.

Fluxo:
UI -> ViewModel -> UseCase -> Repository -> (Network | DB)

#### 7. Network (Retrofit)
```
interface ApiService {
    @GET("users/{id}")
    suspend fun getUser(@Path("id") id: String): UserDto
}
```
Criar Retrofit com base URL do `BuildConfig`.

#### 8. Banco (Room)
Entidade + DAO + Database:
```
@Entity(tableName = "users")
data class UserEntity(@PrimaryKey val id: String, val name: String)
```

#### 9. Repository simples
- Tenta cache (DAO).
- Se não existe, busca na API e salva.

#### 10. Use Case
Encapsula ação: `GetUserUseCase(id)`.

#### 11. ViewModel
- Expõe estado com `StateFlow`.
- Carrega e atualiza UI.

Estado básico:
```
sealed interface UserUiState {
    data object Idle
    data object Loading
    data class Success(val user: User)
    data class Error(val error: Throwable)
}
```

#### 12. DI (ex: Hilt)
`@HiltAndroidApp` no `Application`.
Módulos para fornecer Retrofit, OkHttp, Database.

#### 13. Flavors para ambientes
Defina:
```
buildConfigField("String", "API_BASE_URL", "\"https://api.free.example.com\"")
```

#### 14. Modelos
- DTO (rede)
- Entity (banco)
- Domain (regra)
- UI (exibição)
Converter nas bordas com mappers simples.

#### 15. Testes iniciais
- Repository: fake API + Room em memória.
- ViewModel: testar `StateFlow`.

Resumo:
- Separe responsabilidades cedo.
- Mantenha variantes e módulos no mínimo.
- Centralize dependências.
- Use camadas claras: data -> domain -> presentation.
- Adicione complexidade só quando necessário.

