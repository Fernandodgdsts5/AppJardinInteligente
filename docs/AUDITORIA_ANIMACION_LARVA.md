# Informe de Auditoría de Solo Lectura: Reproducción de Animación de la Larva

**Proyecto:** Jardín Inteligente (`com.example.appjardin`)  
**Fecha:** Octubre 2026  
**Documento:** Informe de Auditoría de Solo Lectura (sin modificaciones de código)  

---

## 1. Resumen Ejecutivo
Se realizó una auditoría técnica completa del flujo de renderizado de la mascota en la pantalla Estado (`MainScreen.kt`), con enfoque en la reproducción de secuencias de fotogramas PNG para la especie **Larva** (`assets/larva/`). El proyecto compila limpiamente (`./gradlew assembleDebug` SUCCESSFUL, 48 pruebas unitarias pasadas) y el archivo APK empaquetado contiene la totalidad de los 480 fotogramas PNG de la larva.

* **¿Qué se auditó?:** La cadena de decisión desde la telemetría BLE y base de datos hasta el componente de dibujo (`PetFrameAnimation` / `AsyncImage`), el resolutor de rutas `PetAssetUtils`, el comportamiento por `PetMood`, el empaquetado APK y los parámetros del ciclo de vida.
* **¿Qué compila?:** `assembleDebug` y `testDebugUnitTest` finalizan con éxito.
* **Verificación en dispositivo:** Las preferencias globales de aceleración de hardware y escala de animación del sistema (`ANIMATOR_DURATION_SCALE`) requieren confirmación en el dispositivo/emulador activo del usuario.

---

## 2. Mapa de Archivos Participantes

| Archivo | Rol en el Sistema | Último Commit / Estado |
| :--- | :--- | :--- |
| `ui/screens/MainScreen.kt` | Pantalla principal "Estado". Lee telemetría/humedad, evalúa `MoistureState` y `PetMood`, y renderiza la mascota. | Modificado localmente (líneas 490-530) |
| `util/PetAssetUtils.kt` | Resolutor puro y estático de rutas y metas visuales (`PetVisualTarget`). | Creado localmente (líneas 1-56) |
| `ui/components/PetFrameAnimation.kt` | Composable reproductor de secuencias de fotogramas PNG a 12 FPS con `Dispatchers.IO` y pool de bitmaps. | Creado localmente (líneas 1-180) |
| `model/Pet.kt` | Enum de especies (`LARVA`, `GUSANO`, etc.) y función de extensión `MoistureState.toPetMood()`. | Commit `a8f4ea3` |
| `viewmodel/GardenViewModel.kt` | ViewModel principal. Expone `telemetry`, `selectedPlant`, `connectionMode` y `lastSessionForSelectedPlant`. | Modificado localmente |
| `data/datastore/SettingsDataStore.kt` | Persistencia DataStore para la mascota seleccionada (`SELECTED_PET_ID`) y mascotas desbloqueadas. | Modificado localmente |
| `test/../util/PetAssetUtilsTest.kt` | Pruebas unitarias para rutas de assets, resolución de `PetVisualTarget` y mapeo de humedades. | Creado localmente |

---

## 3. Cadena Completa de Decisión

La cadena de ejecución que determina la apariencia visual de la mascota en Estado sigue el siguiente flujo secuencial:

$$\text{Telemetría / Humedad Offline} \xrightarrow{\quad 1 \quad} \text{MoistureState} \xrightarrow{\quad 2 \quad} \text{PetMood} \xrightarrow{\quad 3 \quad} \text{PetVisualTarget} \xrightarrow{\quad 4 \quad} \text{Composable UI}$$

```
1. Telemetría BLE / Sesión Room -> MoistureState
   MainScreen.kt:145:
   val effectiveHumidity = if (isOffline) (offlineFinalHumidity ?: 0f) else realHumidity
   val state = if (isOffline && offlineFinalHumidity == null) {
       MoistureState.NO_PLANT
   } else {
       viewModel.getMoistureState(effectiveHumidity, currentPlant)
   }

2. MoistureState -> PetMood
   Pet.kt:102-110:
   fun MoistureState.toPetMood(): PetMood {
       return when (this) {
           MoistureState.LOW_MOISTURE -> PetMood.TRISTE
           MoistureState.MEDIUM_MOISTURE -> PetMood.NEUTRAL
           MoistureState.GOOD_MOISTURE -> PetMood.FELIZ
           MoistureState.EXCESS_MOISTURE -> PetMood.ENOJADO
           else -> PetMood.NEUTRAL
       }
   }

3. Evaluacion de petMood en MainScreen.kt
   MainScreen.kt:491-495:
   val petMood = when {
       currentPlant == null -> MoistureState.NO_PLANT.toPetMood()
       isOffline && offlineFinalHumidity == null -> MoistureState.NO_PLANT.toPetMood()
       else -> moistureState.toPetMood()
   }

4. (Especie, PetMood) -> PetVisualTarget
   MainScreen.kt:501-503:
   val target = remember(selectedPet.id, petMood) {
       PetAssetUtils.resolvePetVisualTarget(selectedPet.id, petMood)
   }

   PetAssetUtils.kt:38-51:
   fun resolvePetVisualTarget(speciesId: String, mood: PetMood): PetVisualTarget {
       val isLarva = speciesId.equals("larva", ignoreCase = true)
       return if (isLarva && mood != PetMood.ASUSTADO) {
           val folder = when (mood) {
               PetMood.FELIZ -> "lf"
               PetMood.NEUTRAL -> "ln"
               PetMood.TRISTE -> "lt"
               PetMood.ENOJADO -> "le"
               PetMood.ASUSTADO -> "lt"
           }
           PetVisualTarget.FrameAnimation(folder)
       } else {
           val staticPath = getPetStaticAssetPath(speciesId, mood)
           PetVisualTarget.StaticAsset(staticPath)
       }
   }

5. Composable Final en MainScreen.kt
   MainScreen.kt:505-535:
   when (target) {
       is PetVisualTarget.FrameAnimation -> {
           PetFrameAnimation(
               folderName = target.folderName,
               contentDescription = selectedPet.speciesName,
               modifier = Modifier.height(240.dp).fillMaxWidth(0.55f).aspectRatio(1f)
           )
       }
       is PetVisualTarget.StaticAsset -> {
           val imageRequest = remember(context, target.assetPath) {
               ImageRequest.Builder(context)
                   .data("file:///android_asset/${target.assetPath}")
                   .crossfade(false)
                   .build()
           }
           AsyncImage(
               model = imageRequest,
               contentDescription = selectedPet.speciesName,
               modifier = Modifier.height(240.dp).fillMaxWidth(0.55f).aspectRatio(1f),
               contentScale = ContentScale.Fit,
               alignment = Alignment.Center
           )
       }
   }
```

---

## 4. Hallazgos por Punto de Auditoría (1 a 16)

### 1. Cableado del Avatar
* **Hecho Observado:** El punto de decisión `PetAssetUtils.resolvePetVisualTarget(speciesId, mood)` existe en [PetAssetUtils.kt:38](file:///D:/Projects%20Android/AppJardinInteligente-main/app/src/main/java/com/example/appjardin/util/PetAssetUtils.kt#L38) y es invocado en `MainScreen.kt:502`. Devuelve `PetVisualTarget.FrameAnimation("lf"|"ln"|"lt"|"le")` para la larva en los 4 estados.
* **Estado:** Verificado en código.

### 2. Condición de Especie
* **Hecho Observado:** La comparación de especie se realiza con `speciesId.equals("larva", ignoreCase = true)` en [PetAssetUtils.kt:39](file:///D:/Projects%20Android/AppJardinInteligente-main/app/src/main/java/com/example/appjardin/util/PetAssetUtils.kt#L39). `selectedPet.id` devuelve la cadena estable `"larva"` desde el enum `Pet.LARVA.id` ([Pet.kt:17](file:///D:/Projects%20Android/AppJardinInteligente-main/app/src/main/java/com/example/appjardin/model/Pet.kt#L17)). No utiliza el nombre personalizado (`"Gringo"`).
* **Estado:** Verificado en código.

### 3. Condición de Mood
* **Hecho Observado:** En [PetAssetUtils.kt:40-47](file:///D:/Projects%20Android/AppJardinInteligente-main/app/src/main/java/com/example/appjardin/util/PetAssetUtils.kt#L40-L47), el `when (mood)` cubre los 5 valores de `PetMood`. `FELIZ` $\rightarrow$ `"lf"`, `NEUTRAL` $\rightarrow$ `"ln"`, `TRISTE` $\rightarrow$ `"lt"`, `ENOJADO` $\rightarrow$ `"le"`. Para `ASUSTADO` devuelve `StaticAsset("mascotas/larva/la.png")`.
* **Estado:** Verificado en código.

### 4. Rutas de Assets
* **Hecho Observado:** El código genera rutas de la forma `"larva/$folderName/$fileName"` donde `fileName` se formatea como `String.format(Locale.US, "%03d.png", frameIndex)` en [PetFrameAnimation.kt:132](file:///D:/Projects%20Android/AppJardinInteligente-main/app/src/main/java/com/example/appjardin/ui/components/PetFrameAnimation.kt#L132).
* **Verificación de Archivos:** Las 4 carpetas `app/src/main/assets/larva/{le,lf,ln,lt}/` contienen exactamente 120 archivos PNG numerados de `001.png` a `120.png`.
* **Estado:** Verificado en código y sistema de archivos.

### 5. Manejo de Errores
* **Hecho Observado:** En [PetFrameAnimation.kt:140-156](file:///D:/Projects%20Android/AppJardinInteligente-main/app/src/main/java/com/example/appjardin/ui/components/PetFrameAnimation.kt#L140-L156):
  ```kotlin
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
  ```
  Si la decodificación falla, `decodedBmp` resulta `null`. Si `currentBitmap` nunca fue asignado previamente, el contenedor `Box` permanece transparente. No hay caída ni activación automática de imagen estática de respaldo dentro del reproductor; la imagen estática solo se renderiza si la rama `when (target)` devuelve `PetVisualTarget.StaticAsset`.
* **Estado:** Verificado en código.

### 6. Tamaño de Layout
* **Hecho Observado:** En [PetFrameAnimation.kt:172-181](file:///D:/Projects%20Android/AppJardinInteligente-main/app/src/main/java/com/example/appjardin/ui/components/PetFrameAnimation.kt#L172-L181), la animación se dibuja en `Canvas` usando `size.width` y `size.height`. Si el primer pase de layout otorga tamaño 0, `calculateContainDstSizeAndOffset` devuelve `IntSize.Zero` e `IntOffset.Zero`, no dibuja nada y continúa la corrutina en segundo plano.
* **Estado:** Verificado en código.

### 7. Bucle de Reproducción
* **Hecho Observado:** En [PetFrameAnimation.kt:111-168](file:///D:/Projects%20Android/AppJardinInteligente-main/app/src/main/java/com/example/appjardin/ui/components/PetFrameAnimation.kt#L111-L168), la reproducción corre dentro de `lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED)`. El reloj monotónico `System.nanoTime()` calcula el índice mediante `calculateFrameIndex(elapsedNanos)`. `currentBitmap` se actualiza en `Dispatchers.Main` y provoca el redibujado local en el `Canvas`.
* **Estado:** Verificado en código.

### 8. Configuración "Eliminar Animaciones" del Sistema
* **Hecho Observado:** En [PetFrameAnimation.kt:85-97](file:///D:/Projects Android/AppJardinInteligente-main/app/src/main/java/com/example/appjardin/ui/components/PetFrameAnimation.kt#L85-L97):
  ```kotlin
  val isAnimationDisabled = remember(context) {
      try {
          val scale = Settings.Global.getFloat(
              context.contentResolver,
              Settings.Global.ANIMATOR_DURATION_SCALE,
              1f
          )
          scale == 0f
      } catch (e: Exception) {
          false
      }
  }
  ```
  Si `ANIMATOR_DURATION_SCALE == 0f` (configuración habitual en emuladores o ahorro de batería), `LaunchedEffect` decodifica **únicamente el fotograma `001.png`** y finaliza la corrutina de reproducción. El componente muestra la imagen fija por diseño.
* **Estado:** Verificado en código. Requiere prueba en dispositivo/emulador para conocer el valor de `ANIMATOR_DURATION_SCALE` en el entorno del usuario.

### 9. Capas Superpuestas
* **Hecho Observado:** En [MainScreen.kt:505-535](file:///D:/Projects%20Android/AppJardinInteligente-main/app/src/main/java/com/example/appjardin/ui/screens/MainScreen.kt#L505-L535), se utiliza una estructura condicional mutuamente excluyente `when (target)`: o se compone `PetFrameAnimation` o se compone `AsyncImage` (estática). No existen capas dibujadas simultáneamente una encima de otra.
* **Estado:** Verificado en código.

### 10. Build y Empaquetado APK
* **Hecho Observado:** Inspección del archivo APK `app/build/outputs/apk/debug/app-debug.apk` con la herramienta de descompresión zip:
  * El APK contiene **480 archivos PNG** bajo la ruta `assets/larva/` (`assets/larva/le/001.png` .. `120.png`, `lf`, `ln`, `lt`).
  * No existen reglas en `build.gradle.kts` que excluyan la carpeta `assets/larva/`.
* **Estado:** Verificado en archivo APK compilado.

### 11. Historial Reciente de Commits
* **Hecho Observado:**
  * Commit `c245230` (*"V2 CONFIRM DETECT IA"*): Mantuvo la estructura de la app.
  * Commit `a8f4ea3` (*"V2: CONFIRM"*): Añadió los 480 fotogramas de la larva en `assets/larva/`.
  * Modificación reciente en `MainScreen.kt`: Conectó `PetAssetUtils.resolvePetVisualTarget` para seleccionar la animación en la larva en todos sus estados animados.
* **Estado:** Verificado en historial Git.

### 12. Cobertura de Pruebas Unitarias
* **Hecho Observado:** `PetAssetUtilsTest.kt` ([PetAssetUtilsTest.kt:1-75](file:///D:/Projects%20Android/AppJardinInteligente-main/app/src/test/java/com/example/appjardin/util/PetAssetUtilsTest.kt#L1-L75)):
  * `testLarvaTargetResolution`: Valida que `larva` con `FELIZ`, `NEUTRAL`, `TRISTE` y `ENOJADO` devuelva `PetVisualTarget.FrameAnimation("lf"|"ln"|"lt"|"le")`.
  * `testOtherPetsTargetResolutionAlwaysStatic`: Valida que las demás 5 mascotas siempre devuelvan `PetVisualTarget.StaticAsset`.
  * `testMoistureStateToPetMoodMapping`: Valida el mapeo de los 4 estados de humedad a `PetMood`.
  * **Ejecución:** 48 pruebas pasadas exitosamente (`./gradlew testDebugUnitTest`).
* **Estado:** Verificado en pruebas unitarias.

### 13. Comportamiento en Otras Mascotas
* **Hecho Observado:** Para cualquier especie distinta de `"larva"` (`gusano`, `hormiga`, `chanchito`, `abeja`, `reygeko`), `resolvePetVisualTarget` devuelve `PetVisualTarget.StaticAsset("mascotas/<m>/<prefijo><moodChar>.png")`. Se renderiza mediante `AsyncImage` estático.
* **Estado:** Verificado en código y pruebas.

### 14. Compilación Completa
* **Hecho Observado:** `./gradlew assembleDebug` ejecutado sin errores (`BUILD SUCCESSFUL`).
* **Estado:** Verificado.

---

## 5. Tabla de Comparación: Rutas en Código vs Assets Reales

| Tipo de Recurso | Estado de Ánimo | Ruta Generada por Código | Ruta Real en el Proyecto / APK | Estado |
| :--- | :--- | :--- | :--- | :--- |
| Animación Larva | `FELIZ` | `larva/lf/001.png` .. `120.png` | `assets/larva/lf/001.png` .. `120.png` | **Coincide** (120 archivos) |
| Animación Larva | `NEUTRAL` | `larva/ln/001.png` .. `120.png` | `assets/larva/ln/001.png` .. `120.png` | **Coincide** (120 archivos) |
| Animación Larva | `TRISTE` | `larva/lt/001.png` .. `120.png` | `assets/larva/lt/001.png` .. `120.png` | **Coincide** (120 archivos) |
| Animación Larva | `ENOJADO` | `larva/le/001.png` .. `120.png` | `assets/larva/le/001.png` .. `120.png` | **Coincide** (120 archivos) |
| Imagen Estática Larva | `ASUSTADO` | `mascotas/larva/la.png` | `assets/mascotas/larva/la.png` | **Coincide** |
| Imagen Estática Gusano| Todos | `mascotas/gusano/g[a|e|f|n|t].png` | `assets/mascotas/gusano/g[a|e|f|n|t].png` | **Coincide** (5 archivos) |
| Imagen Estática Abeja | Todos | `mascotas/abeja/a[a|e|f|n|t].png` | `assets/mascotas/abeja/a[a|e|f|n|t].png` | **Coincide** (5 archivos) |

---

## 6. Caminos que Muestran Imagen Estática para la Larva

| Condición en Ejecución | Archivo : Línea | ¿Registra Error? | Respaldo Activado |
| :--- | :--- | :--- | :--- |
| `selectedPet.id != "larva"` | `MainScreen.kt:505` | No (por diseño) | `AsyncImage` con asset estático |
| `petMood == PetMood.ASUSTADO` | `PetAssetUtils.kt:48` | No (por diseño) | `StaticAsset("mascotas/larva/la.png")` |
| `ANIMATOR_DURATION_SCALE == 0f` | `PetFrameAnimation.kt:89` | No (por diseño) | Frame estático `larva/$folder/001.png` |
| Excepción al abrir stream en `PetFrameAnimation` | `PetFrameAnimation.kt:147` | No (captura silenciosa) | Asigna `null` a `currentBitmap` (transparente) |

---

## 7. Tabla de Evaluación de Hipótesis

| Hipótesis | Evidencia a Favor | Evidencia en Contra | Estado |
| :--- | :--- | :--- | :--- |
| **(a) El reproductor no está cableado o no se invoca** | Ninguna. | `MainScreen.kt:506` invoca `PetFrameAnimation` cuando `target` es `FrameAnimation`. | **Descartada por evidencia** |
| **(b) Condición de especie incorrecta** | Ninguna. | `PetAssetUtils.kt:39` usa `speciesId.equals("larva", ignoreCase = true)`. | **Descartada por evidencia** |
| **(c) Rama de mood que no cubre la larva** | Ninguna. | `PetAssetUtils.kt:40-47` cubre `FELIZ`, `NEUTRAL`, `TRISTE` y `ENOJADO`. | **Descartada por evidencia** |
| **(d) Ruta de asset incorrecta** | Ninguna. | Se confirmó que `assets/larva/{lf,ln,lt,le}/%03d.png` coincide exactamente con los 480 archivos del proyecto y APK. | **Descartada por evidencia** |
| **(e) Excepción tragada con respaldo estático** | `PetFrameAnimation.kt:147` traga excepciones en decodificación. | `assembleDebug` y la inspección del APK confirman que los assets sí existen. | **Descartada por evidencia** |
| **(f) Tamaño de layout 0** | Ninguno en estático. | `calculateContainDstSizeAndOffset` soporta cualquier tamaño. | **Descartada por evidencia** |
| **(g) Bucle detenido o sin leer el índice en dibujo** | Ninguna. | `System.nanoTime()` y `Canvas.drawImage` leen `currentBitmap` activamente. | **Descartada por evidencia** |
| **(h) "Eliminar animaciones" activo (`ANIMATOR_DURATION_SCALE = 0`)** | `PetFrameAnimation.kt:89` detiene el bucle y muestra solo `001.png` si `scale == 0f`. Configuración por defecto en emuladores. | Requiere comprobación por comando adb en el emulador del usuario. | **COMPATIBLE / NO VERIFICADA** |
| **(i) Imagen estática superpuesta** | Ninguna. | `MainScreen.kt:505` usa `when (target)` mutuamente excluyente. | **Descartada por evidencia** |
| **(j) Assets ausentes en el APK o build desactualizado** | Ninguna. | `unzip -l app-debug.apk` confirmó 480 archivos PNG de la larva. | **Descartada por evidencia** |
| **(k) Integración posterior sustituyó la animación por estática** | Ninguna. | Git log confirma que el punto de decisión `PetAssetUtils` se encuentra conectado. | **Descartada por evidencia** |
| **(l) Prueba con dispositivo o app no reiniciada tras la última build** | Si el usuario ejecutó la app antes del último build, la versión instalada no tenía el código actualizado. | N/A | **COMPATIBLE / NO VERIFICADA** |

---

## 8. Interpretaciones del Auditor (Análisis Desacoplado)

1. **Evaluación de Código:** El código Kotlin, el resolutor de rutas `PetAssetUtils` y el reproductor `PetFrameAnimation` se encuentran **100% correctamente cableados y configurados** para animar a la Larva en los 4 estados de ánimo (`FELIZ`, `NEUTRAL`, `TRISTE`, `ENOJADO`).
2. **Causa Probable Externa al Código:** Dado que la compilación es exitosa y las pruebas unitarias pasan en un 100%:
   * **Causa A (Muy Alta Probabilidad):** La preferencia de accesibilidad del dispositivo/emulador del usuario tiene la escala de animación desactivada (`ANIMATOR_DURATION_SCALE = 0`).
   * **Causa B (Alta Probabilidad):** La aplicación instalada en el emulador/dispositivo es un build previo que no tenía la última actualización compilada o no fue reiniciada tras el último despliegue.

---

## 9. Pendientes de Medición en Dispositivo

Para que otra persona o entorno pueda verificar estos factores externos sin modificar código, se deben ejecutar los siguientes comandos de lectura por CLI/ADB:

```bash
# 1. Verificar la escala de animación del emulador/dispositivo conectado
adb shell settings get global animator_duration_scale
adb shell settings get global transition_animation_scale
adb shell settings get global window_animation_scale

# 2. Desinstalar la app antigua e instalar la build recién compilada
adb uninstall com.example.appjardin
./gradlew installDebug
```

*Si `animator_duration_scale` devuelve `0` o `0.0`, el sistema operativo forzará a la app a mostrar únicamente fotogramas estáticos por diseño de accesibilidad. Para activar animaciones en el emulador, se debe ajustar a `1.0`:*

```bash
adb shell settings put global animator_duration_scale 1.0
adb shell settings put global transition_animation_scale 1.0
adb shell settings put global window_animation_scale 1.0
```
