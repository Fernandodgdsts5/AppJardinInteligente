package com.example.appjardin.domain.ai

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

class LiteRtDiagnosisClassifier(private val context: Context) : DiagnosisClassifier {
    private var interpreter: Interpreter? = null
    private var reusableBuffer: ByteBuffer? = null
    var detectedChannelOrder: TensorChannelOrder = DiagnosisModelConfig.defaultChannelOrder
        private set

    private fun loadModelFile(context: Context, modelPath: String) = context.assets.openFd(modelPath).use { fd ->
        FileInputStream(fd.fileDescriptor).channel.map(
            FileChannel.MapMode.READ_ONLY,
            fd.startOffset,
            fd.declaredLength
        )
    }

    private fun initialize() {
        if (interpreter != null) return
        try {
            val mappedModel = loadModelFile(context, DiagnosisModelConfig.MODEL_FILE_NAME)
            val options = Interpreter.Options().apply {
                numThreads = 4
            }
            val interp = Interpreter(mappedModel, options)

            val inputTensor = interp.getInputTensor(0)
            val inputShape = inputTensor.shape()

            when {
                inputShape.contentEquals(intArrayOf(1, 3, DiagnosisModelConfig.IMGSZ, DiagnosisModelConfig.IMGSZ)) -> {
                    detectedChannelOrder = TensorChannelOrder.NCHW
                }
                inputShape.contentEquals(intArrayOf(1, DiagnosisModelConfig.IMGSZ, DiagnosisModelConfig.IMGSZ, 3)) -> {
                    detectedChannelOrder = TensorChannelOrder.NHWC
                }
                else -> {
                    interp.close()
                    throw IllegalArgumentException("Unsupported input tensor shape: ${inputShape.contentToString()}. Expected [1, 3, 224, 224] or [1, 224, 224, 3]")
                }
            }

            val outputTensor = interp.getOutputTensor(0)
            val outputShape = outputTensor.shape()
            if (!outputShape.contentEquals(intArrayOf(1, 2))) {
                interp.close()
                throw IllegalArgumentException("Unsupported output tensor shape: ${outputShape.contentToString()}. Expected [1, 2]")
            }

            interpreter = interp
            Log.d(
                "LiteRtClassifier",
                "Model loaded successfully: ${DiagnosisModelConfig.MODEL_FILE_NAME}. Input shape: ${inputShape.contentToString()} ($detectedChannelOrder), Output shape: ${outputShape.contentToString()}"
            )
        } catch (e: Exception) {
            Log.e("LiteRtClassifier", "Error loading model ${DiagnosisModelConfig.MODEL_FILE_NAME}: ${e.message}", e)
            throw e
        }
    }

    override suspend fun classify(bitmap: Bitmap): DiagnosisPrediction = withContext(Dispatchers.Default) {
        initialize()
        val interp = interpreter ?: throw IllegalStateException("Interpreter not initialized")

        val croppedBitmap = ImagePreprocessor.centerCrop(bitmap, DiagnosisModelConfig.IMGSZ)
        reusableBuffer = ImagePreprocessor.toByteBuffer(
            bitmap = croppedBitmap,
            targetSize = DiagnosisModelConfig.IMGSZ,
            channelOrder = detectedChannelOrder,
            reusableBuffer = reusableBuffer
        )

        val outputArray = Array(1) { FloatArray(2) }

        interp.run(reusableBuffer, outputArray)

        val (classIdx, confidence) = Postprocessor.processOutputs(outputArray[0])
        val resultEnum = DiagnosisModelConfig.CLASS_MAP[classIdx] ?: DiagnosisResult.UNHEALTHY

        DiagnosisPrediction(resultEnum, confidence)
    }

    override fun close() {
        interpreter?.close()
        interpreter = null
        reusableBuffer = null
    }
}
