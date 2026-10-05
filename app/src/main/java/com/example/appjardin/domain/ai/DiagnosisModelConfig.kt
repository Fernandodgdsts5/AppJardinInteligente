package com.example.appjardin.domain.ai

enum class TensorChannelOrder {
    NCHW, // [1, 3, 224, 224]
    NHWC  // [1, 224, 224, 3]
}

object DiagnosisModelConfig {
    const val MODEL_FILE_NAME = "models/plant_health_yolo26n_v1.tflite"
    const val IMGSZ = 224
    const val MODEL_VERSION = "plant_health_yolo26n_v1"

    var defaultChannelOrder: TensorChannelOrder = TensorChannelOrder.NHWC

    val CLASS_MAP = mapOf(
        0 to DiagnosisResult.HEALTHY,
        1 to DiagnosisResult.UNHEALTHY
    )
}
