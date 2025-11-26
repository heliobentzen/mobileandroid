# Activities no Android Moderno

## 1. Por que existem e o que são

Uma Activity é um dos blocos fundamentais de um app Android: é a porta de entrada de uma UI com a qual o usuário interage diretamente. Historicamente cada tela era uma Activity. Hoje, adotamos (quando possível) uma arquitetura de Single-Activity + Navigation (Fragments ou, mais moderno, Jetpack Compose + Navigation), deixando a Activity como host do fluxo. Ainda assim, entender Activities continua essencial porque:
- O sistema (Android OS) gerencia seu ciclo de vida agressivamente para liberar recursos.
- Interações externas (intents, deep links, compartilhamento, permissões, resultado de outras telas) passam por Activities.
- Muitos componentes (câmera, notificações, App Links, Shortcuts, Wear, Auto) envolvem callbacks ligados à Activity.

---

## 2. Conceitos-chave modernos

- **Single-Activity Architecture**: 1 Activity (geralmente MainActivity) + Navigation Component para destinos (destinations) em Compose.
- **ViewModel** para desacoplar lógica de ciclo de vida (sobrevive a recriações).
- **State hoisting** + Compose para UI declarativa reativa.
- **remember / rememberSaveable** para estado efêmero vs persistente à recriação.
- **Activity Result APIs** (registerForActivityResult) substituem onActivityResult legado.
- **Lifecycle-aware components** (LifecycleOwner, repeatOnLifecycle) evitam vazamentos.
- **Back dispatch unificado** (OnBackPressedDispatcher + Compose BackHandler).

---

## 3. Ciclo de vida (essência prática)

### Diagrama do Ciclo de Vida

```plaintext
onCreate -> onStart -> onResume -> (foreground)
  ^                                   |
  |                                   v
onRestart <- onStop <- onPause <- onDestroy
```

### Estados principais (callbacks):
1. **onCreate**: inicialização (injeção, composição de UI, registrar observers).
2. **onStart**: UI visível (não ainda interativa necessariamente).
3. **onResume**: foreground interativo.
4. **onPause**: perder foco parcial (salvar estado transitório leve).
5. **onStop**: não visível (liberar sensores, câmera).
6. **onDestroy**: limpeza final (exceto quando rotação: ViewModel permanece).

Em Compose, muita lógica de estado vai para ViewModel; evita setContent recriar lógica pesada.

---

## 4. Boas Práticas

- **Evite lógica pesada em onCreate**: Use ViewModel para inicializações demoradas.
- **Gerencie recursos corretamente**: Libere sensores e câmeras em onStop.
- **Salve estado essencial**: Use onSaveInstanceState para pequenos dados e ViewModel para persistência.
- **Use APIs modernas**: Prefira Activity Result APIs e Lifecycle-aware components.

---

## 5. Exemplos

### Exemplo 1: Ciclo de Vida com Logs

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("CicloDeVida", "onCreate chamado")
    }

    override fun onStart() {
        super.onStart()
        Log.d("CicloDeVida", "onStart chamado")
    }

    override fun onResume() {
        super.onResume()
        Log.d("CicloDeVida", "onResume chamado")
    }

    override fun onPause() {
        super.onPause()
        Log.d("CicloDeVida", "onPause chamado")
    }

    override fun onStop() {
        super.onStop()
        Log.d("CicloDeVida", "onStop chamado")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("CicloDeVida", "onDestroy chamado")
    }
}
```

### Exemplo 2: Navegação com Compose

```kotlin
@Composable
fun MainScreen(navController: NavController) {
    Button(onClick = { navController.navigate("detalhes") }) {
        Text("Ir para Detalhes")
    }
}
```

---
