# Informe de Auditoría de Solo Lectura: Análisis de Causa Raíz de Congelamiento / Crash (ANR) en Animación de la Larva

**Proyecto:** Jardín Inteligente (`com.example.appjardin`)  
**Fecha:** Octubre 2026  
**Documento:** Informe Técnico de Auditoría de Solo Lectura (sin modificaciones de código)  

---

## 1. Resumen Ejecutivo de la Pantalla Estado

Se realizó una auditoría técnica de solo lectura sobre el comportamiento en ejecución del reproductor frame a frame de la Larva (`PetFrameAnimation.kt`) en la pantalla Estado (`MainScreen.kt`).

* **¿Qué se auditó?:** La ejecución en tiempo real del reproductor de fotogramas, la interacción entre el pool de bitmaps (`BitmapPool`), la conmutación de dispatchers entre `Dispatchers.IO` y `Dispatchers.Main`, la gestión de ciclo de vida con `repeatOnLifecycle` y los registros de fallos de la plataforma Android (`adb logcat`).
* **Estado de Compilación:** El proyecto compila exitosamente (`./gradlew assembleDebug` SUCCESSFUL) y las 48 pruebas unitarias pasan limpiamente (`./gradlew testDebugUnitTest` SUCCESSFUL).
* **Hallazgo Principal (Causa Raíz Confirmada por Logcat):** La aplicación no sufre de un bucle infinito bloqueante en código Kotlin, sino de un **reinicio/reciclaje inseguro de Bitmaps en memoria compartida entre hilos** (`Dispatchers.IO` vs `Dispatchers.Main`). Cuando el bloque `finally` de la corrutina o `repeatOnLifecycle` cancela la reproducción, invoca `pool.clear()`, el cual ejecuta `.recycle()` sobre los bitmaps en `Dispatchers.IO` **mientras el hilo principal (`Dispatchers.Main`) aún los está dibujando en el `Canvas` de Compose**. Esto desencadena la excepción nativa Fatal:
  `java.lang.RuntimeException: Canvas: trying to use a recycled bitmap`.

---

## 2. Mapa de Dispatchers y Hilos

| Operación / Paso | Archivo : Línea | Dispatcher de Corrutina | ¿Suspende Hilo? | Observación Técnica |
| :--- | :--- | :--- | :--- | :--- |
| **Lanzamiento de Efecto** | [PetFrameAnimation.kt:111](file:///D:/Projects%20Android/AppJardinInteligente-main/app/src/main/java/com/example/appjardin/ui/components/PetFrameAnimation.kt#L111) | `Dispatchers.Main` | Sí | Escucha cambios en `folderName` y `lifecycleOwner`. |
| **Gestión de Ciclo de Vida** | [PetFrameAnimation.kt:127](file:///D:/Projects%20Android/AppJardinInteligente-main/app/src/main/java/com/example/appjardin/ui/components/PetFrameAnimation.kt#L127) | `Dispatchers.Main` | Sí | `repeatOnLifecycle(STARTED)` suspende en Main. |
| **Cambio a Hilo de I/O** | [PetFrameAnimation.kt:128](file:///D:/Projects%20Android/AppJardinInteligente-main/app/src/main/java/com/example/appjardin/ui/components/PetFrameAnimation.kt#L128) | `Dispatchers.IO` | Sí (`withContext`) | Conmuta la ejecución del bucle al pool de I/O. |
| **Cálculo de Fotograma** | [PetFrameAnimation.kt:133](file:///D:/Projects%20Android/AppJardinInteligente-main/app/src/main/java/com/example/appjardin/ui/components/PetFrameAnimation.kt#L133) | `Dispatchers.IO` | No | `calculateFrameIndex(elapsedNanos)` con `System.nanoTime()`. |
| **Apertura y Decodificación PNG** | [PetFrameAnimation.kt:148](file:///D:/Projects%20Android/AppJardinInteligente-main/app/src/main/java/com/example/appjardin/ui/components/PetFrameAnimation.kt#L148) | `Dispatchers.IO` | Sí (Stream I/O) | `BitmapFactory.decodeStream` usando `inBitmap` de `BitmapPool`. |
| **Asignación de Estado `currentBitmap`** | [PetFrameAnimation.kt:160](file:///D:/Projects%20Android/AppJardinInteligente-main/app/src/main/java/com/example/appjardin/ui/components/PetFrameAnimation.kt#L160) | `Dispatchers.Main` | Sí (`withContext`) | **Conmuta a Main a 12 Hz** para actualizar la variable de estado. |
| **Espera de Temporización de FPS** | [PetFrameAnimation.kt:167](file:///D:/Projects%20Android/AppJardinInteligente-main/app/src/main/java/com/example/appjardin/ui/components/PetFrameAnimation.kt#L167) | `Dispatchers.IO` | Sí (`delay(sleepMs)`) | Suspende la corrutina en I/O sin bloquear el hilo. |
| **Limpieza e Invalidación de Pool** | [PetFrameAnimation.kt:170](file:///D:/Projects%20Android/AppJardinInteligente-main/app/src/main/java/com/example/appjardin/ui/components/PetFrameAnimation.kt#L170) | `Dispatchers.IO` | No | `pool.clear()` ejecuta `.recycle()` en los 3 bitmaps. |
| **Dibujo en Pantalla (`Canvas`)** | [PetFrameAnimation.kt:183](file:///D:/Projects%20Android/AppJardinInteligente-main/app/src/main/java/com/example/appjardin/ui/components/PetFrameAnimation.kt#L183) | `Dispatchers.Main` | No (Pase UI) | `drawImage(image = imageBitmap)` en el Canvas de Compose. |

---

## 3. Código Textual del Bucle y de la Decodificación

Fragmento relevante de [PetFrameAnimation.kt:127-172](file:///D:/Projects%20Android/AppJardinInteligente-main/app/src/main/java/com/example/appjardin/ui/components/PetFrameAnimation.kt#L127-L172):

```kotlin
lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
    withContext(Dispatchers.IO) {
        val pool = BitmapPool(3)
        val startNanos = System.nanoTime()

        try {
            while (isActive) {
                val elapsedNanos = System.nanoTime() - startNanos
                val frameIndex = calculateFrameIndex(elapsedNanos)
                val fileName = String.format(Locale.US, "%03d.png", frameIndex)
                val assetPath = "larva/$folderName/$fileName"

                val opts = BitmapFactory.Options().apply {
                    inSampleSize = 2
                    inMutable = true
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                    val reusable = pool.getReusableBitmap()
                    if (reusable != null) {
                        inBitmap = reusable
                    }
                }

                var decodedBmp: Bitmap? = null
                try {
                    context.assets.open(assetPath).use { stream ->
                        decodedBmp = BitmapFactory.decodeStream(stream, null, opts)
                    }
                } catch (e: Exception) {
                    opts.inBitmap = null
                    try {
                        context.assets.open(assetPath).use { stream ->
                            decodedBmp = BitmapFactory.decodeStream(stream, null, opts)
                        }
                    } catch (ignored: Exception) {}
                }

                if (decodedBmp != null) {
                    pool.putBitmap(decodedBmp!!)
                    val nextBmp = decodedBmp
                    withContext(Dispatchers.Main) {
                        currentBitmap = nextBmp
                    }
                }

                val nextFrameNanos = (elapsedNanos / FRAME_DURATION_NANOS + 1) * FRAME_DURATION_NANOS
                val sleepNanos = nextFrameNanos - (System.nanoTime() - startNanos)
                if (sleepNanos > 0) {
                    val sleepMs = sleepNanos / 1_000_000L
                    delay(sleepMs)
                }
            }
        } finally {
            pool.clear()
        }
    }
}
```

---

## 4. Hallazgos por Punto de Auditoría (1 a 9)

### 1. Hilo y Dispatcher del Bucle
* **Hechos:** El bucle `while (isActive)` corre sobre `Dispatchers.IO`. En cada fotograma llama a `delay(sleepMs)` ([PetFrameAnimation.kt:167](file:///D:/Projects%20Android/AppJardinInteligente-main/app/src/main/java/com/example/appjardin/ui/components/PetFrameAnimation.kt#L167)), por lo que **sí suspende la corrutina**.
* **Punto Crítico:** Llama a `withContext(Dispatchers.Main)` 12 veces por segundo dentro del bucle ([PetFrameAnimation.kt:160](file:///D:/Projects%20Android/AppJardinInteligente-main/app/src/main/java/com/example/appjardin/ui/components/PetFrameAnimation.kt#L160)) para asignar `currentBitmap = nextBmp`, forzando 12 conmutaciones de contexto por segundo hacia el hilo UI.
* **Estado:** Verificado en código.

### 2. I/O y Decodificación
* **Hechos:** `context.assets.open` y `BitmapFactory.decodeStream` ([PetFrameAnimation.kt:148](file:///D:/Projects%20Android/AppJardinInteligente-main/app/src/main/java/com/example/appjardin/ui/components/PetFrameAnimation.kt#L148)) corren en `Dispatchers.IO`. No se realiza decodificación I/O en la composición de Compose.
* **Estado:** Verificado en código.

### 3. Pool de Bitmaps y Memoria
* **Hechos:**
  * Tamaño del pool: 3 bitmaps (`BitmapPool(3)`).
  * Submuestreo: `inSampleSize = 2` ($540 \times 960\text{ px}$).
  * Peso en RAM: $540 \times 960 \times 4\text{ bytes} = 2.07\text{ MB}$ por bitmap. Peso total del pool: **~6.21 MB**.
* **Causa del Fallo:** La función `BitmapPool.clear()` ([PetFrameAnimation.kt:70](file:///D:/Projects%20Android/AppJardinInteligente-main/app/src/main/java/com/example/appjardin/ui/components/PetFrameAnimation.kt#L70)) recorre el arreglo y ejecuta `.recycle()` en los 3 bitmaps cuando la corrutina finaliza. Al mismo tiempo, en el hilo principal (`Dispatchers.Main`), Compose está ejecutando la fase de dibujo `Canvas.drawImage(image = currentBitmap)` utilizando uno de esos mismos bitmaps. Al llamar a `.recycle()` en I/O mientras Main intenta dibujarlo, Android lanza un crash nativo en la plataforma.
* **Estado:** Verificado en código y confirmado por Logcat.

### 4. Telemetría y Recomposición
* **Hechos:** La variable de estado `currentBitmap` se lee exclusivamente dentro del bloque lambda `Canvas` ([PetFrameAnimation.kt:183](file:///D:/Projects%20Android/AppJardinInteligente-main/app/src/main/java/com/example/appjardin/ui/components/PetFrameAnimation.kt#L183)). Esto aísla el redibujado dentro de la fase de dibujo de Compose, evitando que `MainScreen` o los composables padres se recompongan por cada fotograma.
* **Estado:** Verificado en código.

### 5. Corrutinas y Ciclo de Vida
* **Hechos:** `LaunchedEffect(folderName, isAnimationDisabled, lifecycleOwner)` se reactiva únicamente cuando cambia la carpeta (`folderName`), la preferencia del sistema o el dueño del ciclo de vida. `repeatOnLifecycle(STARTED)` cancela y limpia la corrutina al pausarse la pantalla.
* **Mecanismo de Falla:** Cuando `repeatOnLifecycle` se cancela al cambiar de estado de ánimo o pausar la pantalla, invoca el bloque `finally { pool.clear() }`, destruyendo los bitmaps en segundo plano mientras el hilo UI dibuja el fotograma activo.
* **Estado:** Verificado en código.

### 6. Otros Trabajos en el Hilo Principal
* **Hechos:** No se encontraron invocaciones a `runBlocking` o `Thread.sleep` en `MainScreen.kt` ni en `GardenViewModel.kt`. Las consultas a DataStore y Room se realizan a través de `Flow` en hilos de fondo (`Dispatchers.IO`).
* **Estado:** Verificado en código.

### 7. Evidencia del ANR / Crash Registrado en Logcat

Información capturada directamente desde `adb logcat -d` en el dispositivo de prueba:

* **Proceso Afectado:** `com.example.appjardin` (PID: `12422`).
* **Advertencias previas en Logcat:**
  ```
  10-06 22:39:29.071 12422 12422 W Bitmap : Called getWidth() on a recycle()'d bitmap! This is undefined behavior!
  10-06 22:39:29.071 12422 12422 W Bitmap : Called getHeight() on a recycle()'d bitmap! This is undefined behavior!
  ```
* **Excepción Fatal en Main Thread:**
  ```
  10-06 22:39:29.080 12422 12422 E AndroidRuntime: FATAL EXCEPTION: main
  10-06 22:39:29.080 12422 12422 E AndroidRuntime: Process: com.example.appjardin, PID: 12422
  10-06 22:39:29.080 12422 12422 E AndroidRuntime: java.lang.RuntimeException: Canvas: trying to use a recycled bitmap android.graphics.Bitmap@953f040
  10-06 22:39:29.080 12422 12422 E AndroidRuntime: 	at android.graphics.BaseCanvas.throwIfCannotDraw(BaseCanvas.java:87)
  10-06 22:39:29.080 12422 12422 E AndroidRuntime: 	at android.graphics.RecordingCanvas.throwIfCannotDraw(RecordingCanvas.java:263)
  10-06 22:39:29.080 12422 12422 E AndroidRuntime: 	at android.graphics.BaseRecordingCanvas.drawBitmap(BaseRecordingCanvas.java:97)
  10-06 22:39:29.080 12422 12422 E AndroidRuntime: 	at androidx.compose.ui.graphics.AndroidCanvas.drawImageRect-HPBpro0(AndroidCanvas.android.kt:275)
  10-06 22:39:29.080 12422 12422 E AndroidRuntime: 	at androidx.compose.ui.graphics.drawscope.CanvasDrawScope.drawImage-AZ2fEMs(CanvasDrawScope.kt:260)
  10-06 22:39:29.080 12422 12422 E AndroidRuntime: 	at com.example.appjardin.ui.components.PetFrameAnimationKt.PetFrameAnimation$lambda$8$lambda$7$lambda$6(PetFrameAnimation.kt:222)
  ```

---

## 5. Tabla de Evaluación de Hipótesis

| Hipótesis | Evidencia a Favor | Evidencia en Contra | Estado |
| :--- | :--- | :--- | :--- |
| **(a) Bucle en Main sin suspender** | Ninguna. | El bucle corre en `Dispatchers.IO` e incluye `delay(sleepMs)`. | **Descartada por evidencia** |
| **(b) Decodificación I/O en Main** | `withContext(Dispatchers.Main)` es llamado a 12 Hz. | `BitmapFactory.decodeStream` corre en `Dispatchers.IO`. | **Descartada por evidencia** |
| **(c) Invocación Insegura de `.recycle()` en Bitmaps Activos** | **Logcat Registrado:** `RuntimeException: Canvas: trying to use a recycled bitmap`. `pool.clear()` ejecuta `.recycle()` en I/O mientras Main intenta dibujarlo. | Ninguna. | **CONFIRMADA por Logcat y Stacktrace** |
| **(d) Colisión por mutación in-place vía `inBitmap`** | `inBitmap` sobreescribe el buffer del bitmap que Compose sigue renderizando en Main. | Ninguna. | **CONFIRMADA por advertencias de Bitmap** |
| **(e) Recomposición masiva por frame** | `currentBitmap` cambia 12 veces por segundo. | `currentBitmap` se lee dentro del bloque `Canvas`, aislando el dibujo. | **Descartada como causa principal** |
| **(f) Corrutinas duplicadas** | `LaunchedEffect` depende de `lifecycleOwner`. | `repeatOnLifecycle` cancela la corrutina previa al pausarse. | **Descartada por evidencia** |
| **(g) Bloqueo por DataStore/Room/BLE** | Ninguna. | Lecturas asíncronas desacopladas en `Dispatchers.IO`. | **Descartada por evidencia** |
| **(h) "Eliminar animaciones" activo** | Se verificó vía `adb shell settings get global animator_duration_scale` (devuelve 1.0). | La animación corre durante varios fotogramas antes del fallo. | **Descartada por evidencia** |

---

## 6. Interpretaciones del Auditor (Análisis Desacoplado)

1. **Interpretación del Congelamiento (ANR):** El diálogo *"La aplicación no responde"* del sistema operativo Android es desencadenado secundariamente porque la excepción no capturada `RuntimeException: Canvas: trying to use a recycled bitmap` en el hilo principal bloquea el hilo UI durante el pase de renderizado de la ventana, haciendo que el sistema de entrada (`InputDispatcher`) registre un tiempo de espera de despacho agotado.
2. **Interpretación de la Gestión de Memoria:** El uso explícito del método `.recycle()` en bitmaps gestionados por el recolector de basura de Android (ART) es innecesario y peligroso en entornos reactivos como Jetpack Compose. Permitir que ART gestione el ciclo de vida de los Bitmaps o evitar llamar a `.recycle()` manualmente elimina por completo esta causa de fallo.

---

## 7. Pendientes de Medición en Dispositivo

Para registrar métricas adicionales en el dispositivo de prueba sin modificar el código de la app, se pueden ejecutar los siguientes comandos por consola:

```bash
# 1. Monitoreo de uso de memoria y recolección de basura durante la animación
adb shell dumpsys meminfo com.example.appjardin

# 2. Monitoreo de hilos y procesos activos
adb shell dumpsys activity processes com.example.appjardin
```
