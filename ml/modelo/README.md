# Modelo de Diagnóstico de Plantas

Para utilizar el modelo real de IA en la app, sigue estos pasos:

1. Coloca tu archivo `best.pt` (entrenado con Ultralytics YOLO) en esta carpeta (`ml/modelo/`).
2. Abre una terminal y asegúrate de tener instalado `ultralytics` (`pip install ultralytics`).
3. Ejecuta el script de exportación ubicado en la raíz de `ml/`:
   ```bash
   python ml/exportar_tflite.py
   ```
4. El script exportará el modelo a formato TFLite (LiteRT) y lo copiará junto con su configuración (`model_config.json`) a `app/src/main/assets/models/`.
5. Si alguna de las etiquetas del modelo no se reconoce automáticamente como `HEALTHY` o `UNHEALTHY`, edita el archivo `model_config.json` en la carpeta `assets` para corregirlo.
6. Recompila la aplicación de Android.
