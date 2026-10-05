import os
import json
import shutil

try:
    from ultralytics import YOLO
except ImportError:
    print("Error: Ultralytics no está instalado. Instálalo con 'pip install ultralytics'.")
    exit(1)

model_path = "ml/modelo/best.pt"
if not os.path.exists(model_path):
    print(f"Error: No se encontró el modelo en {model_path}.")
    exit(1)

print(f"Cargando modelo {model_path}...")
model = YOLO(model_path)

# Extraer metadata
imgsz = model.model.args['imgsz'] if hasattr(model.model, 'args') and 'imgsz' in model.model.args else 224
if isinstance(imgsz, list) or isinstance(imgsz, tuple):
    imgsz = imgsz[0]

names = model.names
labels = [names[i] for i in range(len(names))]

print(f"Clases detectadas: {labels}")
print(f"Tamaño de imagen esperado (imgsz): {imgsz}")

# Mapear a HEALTHY / UNHEALTHY
labelMap = {}
for label in labels:
    lbl_lower = label.lower()
    if any(word in lbl_lower for word in ['healthy', 'sana', 'sano']):
        labelMap[label] = "HEALTHY"
    elif any(word in lbl_lower for word in ['unhealthy', 'enferma', 'disease', 'sick']):
        labelMap[label] = "UNHEALTHY"
    else:
        print(f"Advertencia: No se pudo inferir si la clase '{label}' es HEALTHY o UNHEALTHY. Se marcará como UNKNOWN para revisión manual.")
        labelMap[label] = "UNKNOWN"

config = {
    "imgsz": imgsz,
    "labels": labels,
    "labelMap": labelMap,
    "normalization": "RGB_0_1",
    "modelVersion": "1.0"
}

assets_dir = "app/src/main/assets/models"
os.makedirs(assets_dir, exist_ok=True)

config_path = os.path.join(assets_dir, "model_config.json")
with open(config_path, "w", encoding="utf-8") as f:
    json.dump(config, f, indent=4, ensure_ascii=False)
print(f"Configuración guardada en {config_path}")

print("Exportando modelo a TFLite (FP32)...")
try:
    # int8 o fp16 puede requerir datos de calibración, usamos el default (fp32)
    export_path = model.export(format="tflite", imgsz=imgsz)

    # Ultralytics crea una carpeta con el nombre del modelo, o devuelve la ruta
    # export_path usualmente apunta a la carpeta best_saved_model/best_float32.tflite o similar.
    # En YOLOv8: model.export genera 'best_saved_model/best_float32.tflite' o similar.

    tflite_file = None
    if os.path.isdir(export_path):
        for root, dirs, files in os.walk(export_path):
            for file in files:
                if file.endswith(".tflite"):
                    tflite_file = os.path.join(root, file)
                    break
    elif str(export_path).endswith(".tflite"):
        tflite_file = export_path

    if tflite_file and os.path.exists(tflite_file):
        dest_tflite = os.path.join(assets_dir, "plant_classifier.tflite")
        shutil.copy(tflite_file, dest_tflite)
        print(f"Modelo TFLite copiado exitosamente a {dest_tflite}")
    else:
        print(f"No se pudo encontrar el archivo .tflite generado en {export_path}")
except Exception as e:
    print(f"Error durante la exportación: {e}")
