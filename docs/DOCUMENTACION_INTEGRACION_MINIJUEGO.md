# Documentación de Integración del Minijuego «¿Regar o Esperar?»

**Proyecto:** Jardín Inteligente (`com.example.appjardin`)  
**Fecha:** Octubre 2026  
**Módulo:** `com.example.appjardin.ui.minijuego` / `com.example.appjardin.data.minijuego`  

---

## 1. Ubicación y Carga de Assets
* **Directorio:** `app/src/main/assets/MiniJuego/`
* **Entrada HTML:** `file:///android_asset/MiniJuego/index.html`
* **Assets Relativos:** `css/style.css`, `js/game.js`, `imagenes/plantas/`, `imagenes/mascotas/`, `imagenes/branding/`, `imagenes/fondos/`, `imagenes/ui/`.
* **Portada:** `app/src/main/assets/img/fondoJuego.png` ($941 \times 1672\text{ px}$).

---

## 2. Flujo de Pantallas e Integración de Usuario

```
[Pestaña Jugar (PlayScreen)]
 └── Muestra fondoJuego.png + Botón "Iniciar" (sin WebView ni carga de juego)
      └── Tap "Iniciar" -> Lanza MinijuegoActivity (Horizontal Pantalla Completa)
           └── [MinijuegoActivity]
                ├── WebView inmersivo con safe-areas y sin barras del sistema/app
                ├── Botón flotante nativo "Salir" (Top-End)
                └── Tap "Salir" / Atrás -> Finaliza Activity y regresa a PlayScreen
```

---

## 3. Decisión de Pantalla Completa Horizontal
* **Opción:** `MinijuegoActivity` dedicada con `screenOrientation="sensorLandscape"` y `configChanges="orientation|screenSize|smallestScreenSize|screenLayout|keyboard|keyboardHidden|uiMode"`.
* **Justificación:** Aislar la rotación landscape y el modo inmersivo dentro de una Activity propia evita alterar el ciclo de vida, orientación y barras de `MainActivity`. Al minimizar la app o pausar el juego, la conexión BLE y la telemetría en segundo plano de `GardenViewModel` permanecen vivas sin disparar alertas falsas de conexión perdida.

---

## 4. Contrato de Comunicación JavaScript ↔ Kotlin (AndroidBridge)

### Kotlin $\rightarrow$ JavaScript (JS Functions)
1. `setAndroidState(jsonString)`
   * Enviado por Android al recibir `requestInitState()`.
   * Payload: `{"coins": 12000, "xp": 10000, "dailyLimitReached": false}`.
2. `onAnswerProcessed(jsonString)`
   * Enviado por Android tras procesar una respuesta `ANSWER_SUBMITTED`.
   * Payload: `{"isCorrect": true, "correctAnswer": "REGAR", "explanation": "...", "awardedCoins": 20, "awardedXp": 30, "totalCoins": 12020, "totalXp": 10030, "streak": 1, "dailyLimitReached": false}`.

### JavaScript $\rightarrow$ Kotlin (AndroidBridge Interface)
1. `AndroidBridge.requestInitState()`: Carga y envía los saldos autoritativos del usuario.
2. `AndroidBridge.submitAnswer(jsonString)`:
   * Payload JS: `{"sessionId": "session_123", "caseId": "s1", "round": 1, "selectedAnswer": "REGAR"}`.
3. `AndroidBridge.exitGame()`: Cierra la Activity desde JS.

---

## 5. Reglas de Recompensa y Límite Diario

* **Lugar en Kotlin:** `MinijuegoRepository.kt` y `MinijuegoDataStore.kt`.
* **Reglas de Recompensa Base:**
  * Racha 1–2 aciertos: **+20 Monedas / +30 XP**.
  * Racha $\ge$ 3 aciertos: **+30 Monedas / +40 XP**.
  * Fallo: **+0 Monedas / +0 XP**, racha se reinicia a 0.
* **Límite Diario Separado:**
  * Máximo **5.000 Monedas** y **5.000 XP** acumulados por día desde el minijuego.
  * Los límites se aplican por separado (`minOf(recompensa, restante)`).
  * Si se alcanza el tope, las respuestas continúan validándose normalmente pero se acreditan $0$ recursos y el juego muestra un aviso no bloqueante *"Límite diario alcanzado"*.
* **Resistencia a Manipulación de Fecha:** `MinijuegoDataStore` rastrea la fecha máxima registrada (`LAST_MAX_DATE`). Si la fecha del sistema se cambia hacia atrás, los contadores diarios no se reinician.

---

## 6. Aislamiento e Idempotencia
* **Idempotencia:** La clave `$sessionId:$caseId` se registra en `MinijuegoDataStore`. Reenviar un mismo evento no vuelve a descontar del límite ni accredita doble saldo.
* **Aislamiento de Errores:** Errores en el puente JS, JSON malformados o fallos en el WebView son capturados en try-catch y presentados en un estado de error nativo con opciones *"Reintentar"* y *"Salir"*, garantizando que ningún fallo en el juego pueda tumbar la aplicación principal.
