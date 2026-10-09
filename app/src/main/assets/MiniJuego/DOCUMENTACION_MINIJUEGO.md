# Documentación técnica del minijuego

**Propósito:** describir el comportamiento comprobado del minijuego y especificar una integración futura con Android/WebView sin presuponer ni modificar la arquitectura de esa aplicación.

## 1. Descripción general

«¿Regar o esperar?» es una experiencia web autocontenida de Jardín Inteligente. La persona juega seis situaciones de cuidado de plantas: observa humedad, temperatura y tiempo desde el último riego, y elige **Regar** o **Esperar y monitorear**. Cada respuesta se valida, muestra una explicación y puede otorgar monedas y XP. Al terminar la sexta situación se muestra un resumen con aciertos y recompensas.

El juego utiliza ilustraciones locales de invernadero, plantas, insectos/mascotas, marca y recompensas. La interfaz admite pantallas verticales y horizontales y, en orientación horizontal de poca altura, se ajusta para evitar desplazamiento de la página.

Actualmente es una aplicación web independiente: **no está conectada a Android ni a una base de datos de la aplicación**. Mantiene un saldo local propio en `localStorage`. No hay puente JavaScript/Android ni eventos de integración implementados.

## 2. Estructura real del proyecto

```text
MiniJuego/
├── DOCUMENTACION_MINIJUEGO.md
├── index.html
├── css/
│   └── style.css
├── js/
│   └── game.js
└── imagenes/
    ├── branding/
    │   ├── logo.png
    │   └── logoSinFondo.png
    ├── fondos/
    │   ├── fondo.png
    │   └── fondoNoche.png
    ├── mascotas/
    │   ├── af.png
    │   ├── cf.png
    │   ├── gf.png
    │   ├── hf.png
    │   ├── lf.png
    │   └── rf.png
    ├── plantas/
    │   ├── geranio.png
    │   ├── helecho.png
    │   └── tomate.png
    ├── referencia/
    │   └── WhatsApp Image 2026-10-06 at 7.51.53 PM.jpeg
    └── ui/
        ├── m.png
        ├── ms.png
        └── xp.png
```

| Archivo | Responsabilidad y datos | Dependencias / notas para Android |
|---|---|---|
| `index.html` | Estructura semántica: encabezado y saldo, sensor, nivel visual, planta, botones, feedback, bienvenida y resultado. Carga CSS y JavaScript con rutas relativas. | Punto de entrada para WebView. Las imágenes iniciales están declaradas aquí. El nombre inicial de mascota también debe coincidir con el primer render dinámico. |
| `css/style.css` | Estilos, layout adaptable, fondos, transiciones, tamaños táctiles, orientación horizontal, safe areas y preferencias de movimiento reducido. | No importa CSS externo ni contiene llamadas de integración. |
| `js/game.js` | Datos de los seis casos y cuatro rangos visuales de XP; estados de partida; selección aleatoria sin repetición dentro de una partida; validación, recompensa, perfil, renderizado y persistencia. | JavaScript vanilla. Es el único código que actualmente cambia monedas y XP. No expone un contrato de integración. |
| `imagenes/branding/logoSinFondo.png` | Logo en encabezado y bienvenida. | Ruta local relativa. |
| `imagenes/branding/logo.png` | Variante del logo. | Está presente, pero el juego actual no la referencia. |
| `imagenes/fondos/fondo.png` | Escenario diurno del invernadero. | Se referencia en CSS. |
| `imagenes/fondos/fondoNoche.png` | Fondo nocturno que CSS muestra cuando termina la partida (`data-sky="night"`). | Se referencia en CSS. |
| `imagenes/mascotas/*.png` | Retrato de mascota asociado a cada caso. | Nombres mostrados: Miel, Troll, Coco, Luna, Gringo y Oscar; ver la tabla de casos. |
| `imagenes/plantas/*.png` | Geranio, helecho y tomatera mostrados en los casos. | Las rutas se forman en JavaScript a partir del nombre local de archivo. |
| `imagenes/ui/m.png` | Icono de monedas del saldo del encabezado. | Ruta relativa. |
| `imagenes/ui/ms.png` | Icono de monedas de la recompensa final. | Ruta relativa. |
| `imagenes/ui/xp.png` | Icono de XP en encabezado y resultado. | Ruta relativa. |
| `imagenes/referencia/WhatsApp Image 2026-10-06 at 7.51.53 PM.jpeg` | Referencia visual entregada para orientar el diseño. | No se carga ni se muestra dentro del juego. |

Las rutas que utiliza la aplicación son relativas a `index.html`; no se encontraron rutas absolutas del equipo (por ejemplo, `C:\...` o `D:\...`).

## 3. Flujo actual del juego

```text
Carga de index.html y css/style.css
  ↓
js/game.js inicializa los elementos, intenta cargar coins/xp y dibuja perfil
  ↓
Pantalla de bienvenida (saldo local, juego aún sin iniciar)
  ↓
“Comenzar partida”: se reinician los contadores de la partida y se barajan los seis casos
  ↓
Presentación de sensor, planta, mascota, explicación breve y consejo
  ↓
El usuario responde Regar / Esperar
  ↓
La respuesta se bloquea mientras aparece feedback y se compara con answer
  ├── Correcta: incrementa aciertos y racha, agrega monedas/XP, guarda perfil
  └── Incorrecta: no agrega recompensa y pone la racha en cero
  ↓
“Siguiente ronda” carga el caso que sigue en el orden aleatorio
  ↓
Tras responder el sexto caso: pantalla de resultado, total de aciertos y recompensa de esta partida
  ↓
“Jugar otra vez”: comienza una partida nueva; el saldo acumulado se conserva
```

La respuesta debe enviarse una sola vez por ronda: `state.phase` cambia de `playing` a `feedback` y las dos opciones quedan deshabilitadas hasta la siguiente ronda. La delegación de clic se registra una vez en `document` durante `init()`. No se crean temporizadores ni ciclos de animación JavaScript.

### Estados internos

`state.phase` toma los valores `start`, `playing`, `feedback` y `finished`. `state` también contiene el índice de ronda, el número de respuestas correctas, la racha, el orden de casos de la partida y las recompensas ganadas durante esa partida.

`profile` contiene únicamente `coins` y `xp`, acumulados entre partidas. No hay menú de selección de nivel ni selección libre de caso.

## 4. Casos, niveles y dificultad

Los seis objetos de `SCENARIOS` son seis situaciones concretas en **una sola partida secuencial de seis rondas**. Se baraja una copia de la lista y cada situación aparece una vez por partida. En partidas posteriores pueden volver a aparecer. Los objetos **no tienen un campo `id`**, nivel, dificultad, lista de opciones, puntuación propia ni valor de recompensa propio: se utilizan `plant`, `plantName`, `plantAlt`, `pet`, `petName`, `moisture`, `temperature`, `watered`, `answer`, `description`, `tip` y `explanation`.

| Posición en `SCENARIOS` (índice interno + 1, no ID estable) | Planta | Mascota y archivo | Humedad | Temperatura | Último riego | `answer` |
|---:|---|---|---:|---:|---|---|
| 1 | Geranio | Miel (`af.png`) | 22% | 28 °C | Hace 18 horas | `REGAR` |
| 2 | Helecho | Troll (`cf.png`) | 82% | 21 °C | Hace 1 hora | `ESPERAR` |
| 3 | Tomatera | Coco (`gf.png`) | 34% | 32 °C | Hace 6 horas | `REGAR` |
| 4 | Geranio | Luna (`hf.png`) | 74% | 19 °C | Hace 2 horas | `ESPERAR` |
| 5 | Helecho joven | Gringo (`lf.png`) | 28% | 25 °C | Hace 12 horas | `REGAR` |
| 6 | Tomatera | Oscar (`rf.png`) | 48% | 23 °C | Ayer | `ESPERAR` |

Los seis casos tienen combinaciones sensor/decisión/contexto distintas; no se trata de una lista de 50 situaciones. Las especies y algunas mediciones se repiten, pero varían los valores, contexto y, en la mayoría, la recomendación. No existen actualmente IDs únicos con los que el host pueda identificar establemente un caso después de barajar.

### Rangos visuales de XP (no son niveles de contenido)

`LEVELS` define cuatro rangos para el indicador de experiencia:

| Número mostrado | Nombre | Intervalo de XP (`minimum` incluido, `next` como límite superior) |
|---:|---|---:|
| 1 | Semilla | 0–99 |
| 2 | Brote | 100–299 |
| 3 | Guardián verde | 300–599 |
| 4 | Maestro del agua | 600–999 y XP superior |

No hay casos asociados a estos rangos, bloqueos, desbloqueos ni condición de avance por nivel; el cálculo solo representa el XP acumulado en la barra y el título. En el rango final la barra llega como máximo a 100% y el contador mostrado se limita a 1000 XP; el perfil, en cambio, sigue acumulando XP por encima de 1000.

**Requisito futuro de 50 casos por nivel:** el código no implementa niveles de contenido. Por tanto, la cuenta comprobable es:

| Nivel de contenido | Casos actuales | ¿Cumple mínimo de 50? |
|---|---:|---|
| No existe una entidad de nivel/casos en el juego | 6 casos en la lista única `SCENARIOS` | No; son 6 en total y no se deben contar los cuatro rangos visuales como niveles de contenido. |

El juego no ofrece una pantalla de victoria/derrota por nivel. Al completar las seis rondas muestra resultado según aciertos: 6, «¡Jardín perfecto!»; 4–5, «¡Buen trabajo, jardinero!»; 0–3, «¡Tu jardín puede crecer!». Este resultado no modifica la recompensa.

## 5. Monedas

### Estado actual

- La propiedad de estado se llama `profile.coins`; su valor inicial en una instalación sin dato guardado es `0`.
- Se muestra en `#total-coins` en el encabezado y la suma de la partida se presenta en `#earned-coins` en el resultado.
- Solo una respuesta correcta concede monedas. La regla se calcula en `submitAnswer()` tras incrementar la racha:
  - racha nueva de 1 o 2: 20 monedas;
  - racha nueva de 3 o más: 30 monedas por respuesta correcta.
- La regla se aplica a cada acierto mientras la racha siga siendo 3 o superior; no hay recompensa por responder mal ni una recompensa adicional por terminar la partida.
- No hay tope, compra, tienda, gasto ni ajuste negativo de monedas en el código actual.
- El total se almacena en el JSON bajo la clave `jardin-inteligente-progreso`, propiedad `coins`. Se guarda inmediatamente después de cada respuesta correcta.
- Si se recarga, se intenta restaurar ese total; si no existe el dato o no se puede leer, el juego parte de cero en memoria. La partida actual y sus recompensas pendientes no se guardan.

### Integración futura

Al iniciar, el host debería proporcionar el saldo vigente del usuario para que el juego lo muestre; al acertar, el juego debe comunicar el hecho que puede justificar una recompensa. **No debe pedir al host que incremente automáticamente el importe arbitrario enviado por JavaScript.** Android debe recalcular y aplicar la recompensa autorizada. El contrato conceptual está en las secciones 9–11.

## 6. Experiencia (XP) y rangos

### Estado actual

- La propiedad es `profile.xp`; el valor inicial cuando no existe persistencia es `0`.
- Se muestra en `#total-xp`, en `#xp-progress` y visualmente en `#level-progress`.
- Solo se concede XP por respuesta correcta: 30 XP si la racha tras el acierto es 1–2; 40 XP si la racha es 3 o superior.
- La respuesta incorrecta pone la racha a 0, sin quitar XP ya obtenida. Los aciertos posteriores empiezan una nueva racha.
- No hay requisito de XP para comenzar, avanzar ronda ni recibir otra recompensa. Los cuatro umbrales solo etiquetan el saldo acumulado.
- La persistencia comparte la clave `jardin-inteligente-progreso`, propiedad `xp`, y se escribe en cada respuesta correcta junto con las monedas.

### Integración futura

Android debe proporcionar el XP inicial y reconciliar cada respuesta válida con sus propias reglas. La interfaz debe reflejar el saldo resultante, evitando sumar dos veces una recompensa si Android también envía el total actualizado.

## 7. Tabla de recompensas actuales

| Evento | Condición | Monedas | XP | Acción actual |
|---|---|---:|---:|---|
| Respuesta correcta con racha 1–2 | `submitAnswer()` incrementa la racha a 1 o 2 | +20 | +30 | Actualiza `state.rewards` y `profile`; persiste perfil y renderiza encabezado/progreso. |
| Respuesta correcta con racha ≥3 | `submitAnswer()` incrementa la racha a 3 o más | +30 | +40 | Igual que arriba. |
| Respuesta incorrecta | Selección distinta de `scenario.answer` | +0 | +0 | Racha a 0; no guarda perfil porque los saldos no cambian. |
| Finalizar sexta ronda | Se pulsa «Ver resultado» después de la respuesta de la ronda 6 | +0 adicional | +0 adicional | Muestra el resultado y las sumas ya ganadas durante la partida. |
| Reiniciar partida | Se pulsa «Jugar otra vez» | +0 | +0 | Limpia contadores de partida, conserva el perfil acumulado. |

No existen recompensas variables definidas en los datos del caso, ni bonificación por puntuación final. No hay límite máximo de saldo en el código cliente.

## 8. Persistencia actual

El juego usa variables JavaScript en memoria más `localStorage`. No se encontraron `sessionStorage`, IndexedDB, cookies, archivos JSON remotos ni backend.

| Dato | ¿Se persiste? | Ubicación / actualización | ¿Se recupera al recargar? |
|---|---|---|---|
| Monedas acumuladas | Sí | `localStorage["jardin-inteligente-progreso"]`, propiedad `coins`; después de cada acierto. | Sí, si el dato es válido y el WebView permite almacenamiento local. |
| XP acumulado | Sí | Misma clave, propiedad `xp`; después de cada acierto. | Sí, bajo las mismas condiciones. |
| `state.phase` / pantalla | No | Memoria. | No. Vuelve a bienvenida. |
| Partida, orden barajado e índice de ronda | No | `state.scenarios` y `state.roundIndex` en memoria. | No. No se reanuda una partida. |
| Aciertos, racha y recompensas de la partida | No | Propiedades de `state` en memoria. | No. Se reinician. |
| Casos respondidos / desbloqueos | No | No se implementan como datos persistentes. | No aplicable. |
| Identidad del usuario | No | No se implementa. | No aplicable. |

El cargador valida que `coins` y `xp` sean números finitos no negativos; los datos mal formados o errores de acceso se capturan y muestran un aviso de almacenamiento, y el juego continúa con saldo cero. `saveProfile()` también captura errores y muestra el mismo aviso. Este almacenamiento en cliente es editable y **no es una fuente segura ni autoritativa para la economía de Android**.

`localStorage` pertenece al contexto/origen de WebView: no equivale a la base de datos ni a las preferencias existentes de la aplicación Android. También puede causar saldos distintos entre reinstalaciones/perfiles/cuentas. No se guarda un identificador de usuario para aislarlo.

## 9. Contrato conceptual de integración Android ↔ juego

### Estado de implementación

La interfaz de abajo es una **especificación propuesta**, no código actualmente disponible. `game.js` no lee parámetros, no expone una función pública, no implementa `WebViewJavascriptInterface`, no usa `postMessage` ni envía eventos a Android. Se debe acordar e implementar el mecanismo concreto en ambos lados antes de depender de él.

### Android → juego al abrir

El mínimo útil según el código actual es proporcionar saldos autoritativos de monedas y XP. No se necesita un `userId` dentro de la lógica del juego: Android ya debe conocer la cuenta activa y asociar la sincronización a esa cuenta en su propia capa. No hay un estado `gameProgress` actual que pueda restaurarse.

Payload conceptual:

```json
{
  "coins": 450,
  "xp": 620
}
```

| Campo | Necesidad actual | Forma esperada | Si falta |
|---|---|---|---|
| `coins` | Sí para reflejar el monedero de Android | Entero no negativo. Validar en host; el juego debe validar datos al recibirlos. | No sustituir silenciosamente un saldo Android por el valor local antiguo. Acordar una política explícita (por ejemplo, host espera/carga o comunica saldo cero como valor deliberado). |
| `xp` | Sí para reflejar progreso/nivel visual Android | Entero no negativo. | La misma regla de reconciliación que `coins`. |
| `gameProgress` | No se necesita para reanudar porque actualmente no se persiste progreso por caso. | No enviar en esta primera versión, salvo que Android y el juego implementen un modelo nuevo. | No aplicable. |
| `currentLevel` | No hay nivel de contenido. El rango de XP se deriva del total de XP. | No es entrada independiente con el comportamiento actual. | Se calcula visualmente desde `xp`. |
| `userId` | No lo usa el juego ni debe ser necesario para presentar los datos. | Identidad/alcance de la cuenta permanece en Android. Si se necesitase correlación en un evento, preferir un identificador de sesión opaco y no PII. | No aplicable. |

Android debe enviar los saldos antes de mostrar la pantalla interactiva. El host y el juego tienen que definir un mecanismo explícito de inicialización, validación, valores por defecto y actualización de pantalla. No inicializar dos fuentes autoritativas que se sumen entre sí.

### Juego → Android: eventos que conviene soportar

No hay eventos implementados. Se propone comenzar con el evento `ANSWER_SUBMITTED` como fuente de reconciliación de cada decisión. Los nombres y el formato son un contrato a acordar y desarrollar, no nombres que Android pueda invocar hoy.

| Evento propuesto | Momento | Payload conceptual | Tratamiento en Android |
|---|---|---|---|
| `ANSWER_SUBMITTED` | Una vez, después de validar que se pulsó una opción y antes de conceder/confirmar el saldo autoritativo. | `{"event":"ANSWER_SUBMITTED","sessionId":"<id de sesión>","caseId":"<ID estable pendiente>","round":1,"selectedAnswer":"REGAR"}` | Verificar caso, respuesta, estado y duplicados; determinar corrección con reglas autorizadas en Android. No confiar en un campo `correct`, monedas o XP calculado por JS. |
| `GAME_COMPLETED` | Al confirmar la sexta ronda. | `{"event":"GAME_COMPLETED","sessionId":"<id de sesión>","answeredCases":["<IDs estables>"],"correctCount":4,"totalCases":6}` | Usar como resumen/telemetría; validar que los casos y respuestas reportadas pertenecen a esa sesión. No volver a otorgar recompensas ya procesadas por respuesta. |

Los valores entre `<...>` aún no existen en el código y requieren definición. En particular, **no existe `caseId`**: no usar el índice de la lista barajada como ID durable. Antes de emitir el evento, definir IDs inmutables asociados a cada uno de los seis objetos y ampliarlos conforme se creen nuevos casos. `round` es el ordinal temporal 1–6, no identidad del caso.

### Reglas anti-duplicación y saldos de retorno

- Identificar cada sesión y cada decisión con una clave/idempotency key que Android valide; una repetición de un mensaje no puede volver a conceder recompensa.
- `ANSWER_SUBMITTED` debe enviarse una sola vez por interacción, pero el host igualmente debe protegerse frente a reintentos, callbacks duplicados y actividad recreada.
- El host calcula y guarda recompensa/transacción con las reglas de negocio de la aplicación; el juego actual es JavaScript modificable por el cliente.
- Cuando Android devuelva saldos actualizados, sustituir los saldos mostrados con valores autoritativos. No volver a sumarlos localmente si la notificación de respuesta ya produjo un incremento en la UI.
- Reconciliar específicamente qué ocurre con `localStorage`: deshabilitar su papel en la economía integrada o migrar/ignorar su perfil local de forma controlada. No combinarlo automáticamente con el saldo de Room/Android.

## 10. IDs, progreso y casos mínimos

Actualmente:

- `LEVELS` tiene IDs posicionales implícitos (`levelIndex + 1`) y nombres/umbrales; no tiene `id` persistente.
- Los casos no tienen `id` ni clave de negocio estable; la lista solo distingue posiciones en el código.
- No hay IDs de respuestas (se comparan los literales `REGAR` y `ESPERAR`).
- No se persisten nivel, caso actual, casos completados, desbloqueos, rondas contestadas, aciertos, errores, porcentaje ni recompensa por caso.
- El único progreso duradero propio del juego es el saldo `coins`/`xp` en `localStorage`.

Para integración, una representación **conceptual** (no una clase Android ni un modelo ya existente) podría guardar:

```text
Partida del usuario
├── versión del contenido/reglas
├── sessionId
├── estado (en curso/finalizada)
├── casos respondidos con IDs estables
│   ├── respuesta seleccionada
│   ├── corrección validada por Android
│   ├── fecha/orden de respuesta
│   └── transacción de recompensa/idempotency key
└── resumen (aciertos/total)
```

No es posible reanudar una partida concreta con la implementación actual; al recargar comienza en bienvenida y una partida iniciada baraja de nuevo los seis objetos.

## 11. Mapa de datos

```text
             Android / almacenamiento existente (por revisar)
             ├── identidad/cuenta activa
             ├── saldo autoritativo de monedas
             ├── saldo autoritativo de XP
             ├── progreso persistente que se defina
             └── reglas y transacciones antifraude
                      │
                      │ bootstrap conceptual: coins, xp
                      ▼
             Minijuego web actual (index.html + CSS + JS)
             ├── seis casos hardcodeados; sin IDs estables
             ├── selección aleatoria, respuesta y feedback
             ├── saldo cliente en memoria/localStorage
             └── no envía ni recibe eventos actualmente
                      │
                      │ contrato pendiente: ANSWER_SUBMITTED,
                      │ GAME_COMPLETED / sincronización
                      ▼
             Android valida las decisiones y calcula las recompensas
             ├── actualiza saldo mediante la arquitectura existente
             ├── asocia cambios con la cuenta activa
             └── guarda progreso y evita duplicados
```

El saldo local actual solo puede describirse como un prototipo autónomo; no se debe sincronizar sumándolo a los saldos de Android.

## 12. Ejecución en WebView

- **Documento inicial:** `index.html`. Puede empaquetarse, con la misma estructura, bajo `android_asset/minijuego/` y cargarse desde `file:///android_asset/minijuego/index.html`.
- **Rutas:** `css/style.css`, `js/game.js` y `imagenes/...` son relativas al documento. CSS resuelve sus fondos mediante `../imagenes/fondos/...`. Mantener la estructura y respetar mayúsculas/minúsculas al copiar los recursos (el nombre de referencia incluye espacios).
- **Internet/dependencias:** no se encontraron CDN, frameworks, fuentes remotas, imágenes remotas, APIs de red ni servicios. No hay archivos de audio ni reproducción de sonido. La experiencia visual y el contenido pueden funcionar offline con los archivos incluidos.
- **APIs del navegador encontradas:** DOM y `document.addEventListener("click", ...)`; `localStorage` para perfil; CSS media queries, `env(safe-area-inset-*)`, `100dvh`, `prefers-reduced-motion` y atributos `data-*`. No hay bridge Android, `fetch`, `XMLHttpRequest`, Service Worker, canvas, geolocalización, cámara, micrófono, notificaciones, temporizadores ni loops JavaScript.
- **Persistencia de WebView:** para el modo independiente se necesita que WebView permita DOM Storage. Para el modo integrado, el saldo Android debe prevalecer y la persistencia independiente de `localStorage` debe desactivarse, aislarse o eliminarse de forma deliberada.
- **Interacción:** controles HTML `button` con listener delegado de clic. WebView traduce la activación táctil normal de esos controles; verificar también la configuración y el comportamiento táctil en el WebView real.
- **Orientación:** la página responde a viewport/orientación, no solicita por sí sola que Android fuerce landscape. La política de orientación pertenece a la aplicación.
- **Seguridad:** servir el contenido local del APK y limitar navegación/orígenes a lo necesario. No otorgar un bridge genérico que permita a cualquier contenido web cambiar monedas o XP.

Las rutas actuales son adecuadas para la estructura sugerida de assets **si se copia el contenido del proyecto preservando directorios y nombres**. La carga física y compatibilidad deben probarse en el APK final; no se puede certificar desde este proyecto aislado.

## 13. Recursos y dependencias

| Recurso | Ruta local | Uso actual | Externo |
|---|---|---|---|
| Logo transparente | `imagenes/branding/logoSinFondo.png` | Encabezado y bienvenida | No |
| Logo alternativo | `imagenes/branding/logo.png` | No referenciado por la interfaz actual | No |
| Invernadero de día | `imagenes/fondos/fondo.png` | Fondo base y partida | No |
| Invernadero nocturno | `imagenes/fondos/fondoNoche.png` | Fondo de resultado | No |
| Miel | `imagenes/mascotas/af.png` | Caso del geranio seco (22%) | No |
| Troll | `imagenes/mascotas/cf.png` | Caso del helecho húmedo (82%) | No |
| Coco | `imagenes/mascotas/gf.png` | Caso de tomatera con calor | No |
| Luna | `imagenes/mascotas/hf.png` | Caso del geranio húmedo (74%) | No |
| Gringo | `imagenes/mascotas/lf.png` | Caso de helecho joven seco | No |
| Oscar | `imagenes/mascotas/rf.png` | Caso de tomatera con humedad media | No |
| Geranio | `imagenes/plantas/geranio.png` | Casos 1 y 4 | No |
| Helecho | `imagenes/plantas/helecho.png` | Casos 2 y 5 | No |
| Tomate | `imagenes/plantas/tomate.png` | Casos 3 y 6 | No |
| Moneda | `imagenes/ui/m.png` | Saldo del encabezado | No |
| Moneda alternativa | `imagenes/ui/ms.png` | Resumen final | No |
| XP | `imagenes/ui/xp.png` | Saldo del encabezado y resumen final | No |
| Imagen de referencia | `imagenes/referencia/WhatsApp Image 2026-10-06 at 7.51.53 PM.jpeg` | No se renderiza dentro del juego | No |

**Dependencias externas encontradas:** ninguna. El conjunto actual no depende de conexión a Internet.

## 14. Problemas y riesgos detectados

1. **Economía controlada por JavaScript:** el propio cliente concede 20/30 monedas y 30/40 XP y guarda esos valores en almacenamiento editable. No es apropiado como autoridad para una economía vinculada a cuentas.
2. **El juego sigue sumando una economía propia:** al recargar lee el saldo local y no conoce el saldo de la cuenta Android. Integrarlo sin cambiar este comportamiento puede presentar saldos divergentes o duplicados.
3. **Sin contrato Android:** no hay inicialización host→juego, callbacks ni eventos juego→host. Un WebView por sí solo no sincronizará el estado.
4. **Casos sin IDs y pocos casos:** solo seis situaciones, sin ID estable y sin versionado. Una posición en la lista no es una identidad durable, especialmente porque se baraja.
5. **“Niveles” son solo rangos de XP:** cuatro etiquetas visuales pueden confundirse con niveles de contenido; no implementan desbloqueo.
6. **No se persiste el progreso de la partida:** recarga, salida o destrucción de la actividad pierde la sesión, índice, decisiones y resumen parcial.
7. **Identidad no modelada:** el `localStorage` no se asocia con un usuario; reutilizar el mismo WebView con distintas cuentas podría exponer o mezclar saldos locales.
8. **Errores de almacenamiento no se registran ni distinguen:** los bloques `catch` muestran una advertencia genérica tanto si el JSON es inválido como si el storage no está disponible. No es un error fatal, pero dificulta el diagnóstico y no debe ocultarse en el flujo integrado.
9. **Reglas clínicas hardcodeadas y simplificadas:** humedades, recomendaciones y casos están escritos en el JavaScript; no son lecturas reales de sensores ni una integración IoT. El indicador “EN VIVO” es decorativo.
10. **Regla de acierto/recompensa en la misma función de interfaz:** `submitAnswer()` verifica, modifica streak/saldo, persiste y actualiza DOM. La lógica no está separada de una autoridad host.
11. **CSS con soporte WebView dependiente de versión:** se usa `100dvh`, `backdrop-filter`, safe area y `:focus-visible`; confirmar compatibilidad con la versión mínima de Android. Hay fallback de altura con `100vh`; comprobar barras del sistema/ventana edge-to-edge en el host.
12. **Orientación no se fuerza:** el juego se adapta al viewport, pero no cambia orientación Android.
13. **Datos iniciales temporales:** antes de pulsar «Comenzar», sensores muestran placeholders y las opciones están deshabilitadas; eso es intencional en web. Android no debe tratar esos placeholders como datos del juego.

No se encontraron IDs duplicados de caso porque no existen IDs de caso. Tampoco se encontraron dependencias CDN, rutas de recursos absolutas, audio, bucles, temporizadores ni peticiones de red.

## 15. Cambios recomendados antes de integrar con Android

### Críticos

- Acordar e implementar un puente mínimo host/juego con validación de esquema, origen/archivo permitido, errores y secuencia de inicialización; hoy no existe.
- Decidir cuál sistema es autoridad de monedas y XP. Para el uso con cuentas, Android debe cargar y guardar los saldos; el cliente no debe conceder incrementos autoritativos.
- Definir IDs estables para escenarios, una política anti-replay/idempotencia y validación de cada respuesta del lado Android antes de acreditar recompensas.
- Confirmar modelo de cuenta/almacenamiento y verificar la ejecución offline en el WebView real/APK.

### Importantes

- Si el requisito de producto es “mínimo 50 casos por nivel”, definir niveles de contenido y redactar/revisar al menos 50 casos realmente distintos por cada uno. Actualmente solo hay seis y no hay niveles de contenido.
- Definir qué se persiste del juego (si solo saldos o también sesión/respuestas) y cómo se reanuda tras rotación/cierre. Hoy la partida es exclusivamente volátil.
- Establecer la forma de bootstrap Android→JS y actualización Android→JS sin competir con los saldos de `localStorage`.
- Versionar los datos de casos/reglas si Android usará respuestas correctas o recompensas propias.
- Probar rotación, viewport con barras del sistema, toque, accesibilidad, offline, retención y cambio de usuario en la versión concreta del WebView.

### Opcionales

- Separar los escenarios y reglas de UI en un JSON/módulo versionado si el contenido crece sustancialmente.
- Agregar telemetría consentida de sesiones/respuestas si la aplicación lo requiere.
- Considerar pruebas automáticas del contrato y de cada uno de los casos antes de modificar reglas/recompensas.

## 16. Instrucciones para el agente de Android Studio

1. Copiar el minijuego bajo `android_asset/minijuego/` **preservando** `index.html`, `css/`, `js/` e `imagenes/`; cargar `file:///android_asset/minijuego/index.html` y comprobar las rutas y las imágenes en un dispositivo/emulador.
2. Antes de modificar cualquier componente Android, inspeccionar la arquitectura real existente: entidades Room, DAO, Repository, ViewModel, DataStore, gestión de cuenta y flujos actuales de saldos. Adaptar la solución a esa arquitectura; no crear nombres ni tablas por suposición.
3. Definir un bootstrap host→juego con `coins` y `xp` autoritativos. No enviar `userId` si el juego no lo necesita; mantener la identidad y asociación de cuenta en Android. Si no hay saldo disponible, comunicar explícitamente estado de carga/error o un cero confirmado, no mezclar con una copia local antigua.
4. Acordar/implementar el canal host↔JS y esquema versionado. Los eventos sugeridos son `ANSWER_SUBMITTED` y `GAME_COMPLETED`; ninguno está disponible hasta implementarlo. Incluir IDs estables de caso, hoy ausentes, antes de depender de `caseId`.
5. Por respuesta, Android debe validar sesión, caso, selección, corrección, orden e idempotencia. Recalcular las reglas de recompensa vigentes acordadas con producto; no aceptar monedas, XP ni un indicador `correct` enviado por JavaScript como prueba autoritativa.
6. Persistir transacciones/saldos y, solo si el producto lo requiere, progreso de sesión asociado a la cuenta activa. Asegurar que el mismo caso/evento no se acredite dos veces durante retries, recreación de Activity, volver atrás o nueva apertura de WebView.
7. Devolver el saldo actualizado del host al juego como **reemplazo** de monedas/XP mostradas y no como un nuevo incremento local. Retirar o aislar la persistencia económica de `localStorage` en la modalidad Android integrada.
8. Determinar qué significa el requisito de casos/niveles: actualmente hay seis situaciones para una partida, ninguna clasificación de casos por nivel y cuatro bandas de XP visuales. Preparar IDs únicos y 50 casos distintos por cada nivel de contenido definido.
9. Validar modo offline, DOM Storage solo si es necesario, seguridad del contenido local, navegación externa restringida, APIs del WebView disponibles, orientación horizontal, toque, UI en pantalla completa y sincronización tras salir/reabrir.
10. Ningún detalle de tablas, modelos, API, DAOs, nombres de bridge ni clases Android queda especificado aquí: el agente de Android debe determinarlo a partir del proyecto que efectivamente encuentre.

## 17. Checklist de integración

- [ ] El juego carga correctamente desde WebView.
- [ ] Se conserva la estructura local al colocar el juego dentro de `android_asset/minijuego/`.
- [ ] Todas las imágenes funcionan.
- [ ] Todas las rutas son relativas.
- [ ] El juego funciona sin Internet.
- [ ] El contrato acordado permite que Android envíe monedas y XP.
- [ ] Android puede enviar cualquier progreso que efectivamente se implemente.
- [ ] El juego puede comunicar respuestas mediante el bridge implementado.
- [ ] Android puede identificar establemente el caso respondido.
- [ ] Android valida la respuesta y calcula las recompensas.
- [ ] Las monedas se actualizan en el almacenamiento autoritativo de Android.
- [ ] El XP se actualiza en el almacenamiento autoritativo de Android.
- [ ] El progreso se guarda si así lo requiere el producto.
- [ ] El progreso pertenece a la cuenta correcta.
- [ ] No existen recompensas duplicadas ante reintentos o reapertura.
- [ ] Al cerrar y abrir la aplicación se conserva el saldo autoritativo.
- [ ] Al salir del juego se conserva el estado que requiera producto.
- [ ] Los valores mostrados coinciden con la cuenta y el almacenamiento Android.
- [ ] Cada nivel de contenido tiene al menos 50 casos realmente únicos (no cumple en la versión actual).
- [ ] Los casos tienen IDs únicos y estables (no existen en la versión actual).
- [ ] No existen dependencias externas inesperadas.
- [ ] Se validaron orientación horizontal y áreas táctiles en WebView.
- [ ] El juego funciona correctamente dentro del APK en el rango de WebView soportado.
