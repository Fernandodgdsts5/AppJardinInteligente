# Guía de Reintegración del Minijuego Web «¿Regar o Esperar?»

**Propósito:** Este documento sirve como guía rápida y exacta cuando el dueño del proyecto reemplace la carpeta `app/src/main/assets/MiniJuego/` con una nueva versión del equipo web autónomo.

---

## Pasos para Conectar una Nueva Versión de la Carpeta

1. **Copiar la carpeta:**
   * Copiar los nuevos archivos a `app/src/main/assets/MiniJuego/` (asegurar el nombre exacto con `M` y `J` mayúsculas).

2. **Añadir `js/host-bridge.js`:**
   * Crear el archivo `app/src/main/assets/MiniJuego/js/host-bridge.js` con el siguiente contenido:
     ```javascript
     (function () {
         const isAndroid = typeof window.AndroidBridge !== "undefined";
         window.MinijuegoHost = {
             isAndroid: isAndroid,
             requestInitState: function () {
                 if (isAndroid && typeof window.AndroidBridge.requestInitState === "function") {
                     window.AndroidBridge.requestInitState();
                 }
             },
             submitAnswer: function (sessionId, caseId, round, selectedAnswer) {
                 if (isAndroid && typeof window.AndroidBridge.submitAnswer === "function") {
                     window.AndroidBridge.submitAnswer(JSON.stringify({ sessionId, caseId, round, selectedAnswer }));
                 }
             }
         };
     })();
     ```

3. **Insertar en `index.html`:**
   * Agregar `<link rel="stylesheet" href="css/host-overrides.css">` en `<head>`.
   * Agregar `<script src="js/host-bridge.js"></script>` inmediatamente antes de `<script src="js/game.js" defer></script>`.

4. **Añadir Ganchos Mínimos en `js/game.js`:**
   * Asignar `id: "s1"` a `"s6"` en los objetos de `SCENARIOS`.
   * En `loadProfile()` y `saveProfile()`: agregar al inicio `if (window.MinijuegoHost && window.MinijuegoHost.isAndroid) return;`.
   * En `submitAnswer()`: agregar la invocación `window.MinijuegoHost.submitAnswer(state.sessionId, scenario.id, state.roundIndex + 1, answer);`.
   * Agregar las funciones globales `window.setAndroidState` y `window.onAnswerProcessed`.
   * En `init()`: llamar a `if (window.MinijuegoHost) window.MinijuegoHost.requestInitState();`.

5. **Comprobación:**
   * Ejecutar `./gradlew assembleDebug testDebugUnitTest`.
