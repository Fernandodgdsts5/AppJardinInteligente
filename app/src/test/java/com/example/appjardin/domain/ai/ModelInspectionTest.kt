package com.example.appjardin.domain.ai

import org.junit.Ignore
import org.junit.Test
import org.tensorflow.lite.Interpreter
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ModelInspectionTest {

    @Test
    @Ignore("Requires Android environment/native TFLite JNI library")
    fun inspectTfliteModel() {
        val modelFile = File("src/main/assets/models/plant_health_yolo26n_v1.tflite")
        if (!modelFile.exists()) {
            println("Model file not found at ${modelFile.absolutePath}")
            return
        }

        println("Model file found. Size: ${modelFile.length()} bytes")
        val buffer = ByteBuffer.allocateDirect(modelFile.length().toInt()).order(ByteOrder.nativeOrder())
        buffer.put(modelFile.readBytes())
        buffer.rewind()

        val interpreter = Interpreter(buffer)
        val inputTensor = interpreter.getInputTensor(0)
        println("INPUT TENSOR:")
        println("  Shape: ${inputTensor.shape().contentToString()}")
        println("  DataType: ${inputTensor.dataType()}")

        val outputTensor = interpreter.getOutputTensor(0)
        println("OUTPUT TENSOR:")
        println("  Shape: ${outputTensor.shape().contentToString()}")
        println("  DataType: ${outputTensor.dataType()}")

        interpreter.close()
    }
}
