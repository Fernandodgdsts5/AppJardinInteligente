package com.example.appjardin.ui.screens

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.appjardin.R
import com.example.appjardin.data.local.DiagnosisEntity
import com.example.appjardin.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosisDetailScreen(
    diagnosis: DiagnosisEntity?,
    activeColor: Color,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    if (diagnosis == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Diagnóstico no encontrado", color = Color.Gray)
        }
        return
    }

    val isHealthy = diagnosis.result == "HEALTHY"
    val titleColor = if (isHealthy) ColorGoodMoisture else ColorExcessMoisture
    val titleText = if (isHealthy) stringResource(R.string.diagnosis_healthy_title) else stringResource(R.string.diagnosis_unhealthy_title)
    val descText = if (isHealthy) stringResource(R.string.diagnosis_healthy_desc) else stringResource(R.string.diagnosis_unhealthy_desc)
    
    val confPercent = (diagnosis.confidence * 100).roundToInt()
    val confText = if (isHealthy) {
        stringResource(R.string.diagnosis_healthy_confidence_format, confPercent)
    } else {
        stringResource(R.string.diagnosis_unhealthy_confidence_format, confPercent)
    }

    val recomText = if (isHealthy) stringResource(R.string.diagnosis_healthy_recom) else stringResource(R.string.diagnosis_unhealthy_recom)

    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val dateStr = dateFormat.format(Date(diagnosis.createdAt))

    var isExporting by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle de Diagnóstico", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = activeColor)
            )
        },
        containerColor = CreamBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (diagnosis.modelVersion == "debug-fake") {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.DarkGray),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "MODO PRUEBA (Modelo ausente)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // 1. Foto
            AsyncImage(
                model = File(diagnosis.imagePath),
                contentDescription = "Foto diagnosticada",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )

            // Content Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // 2. Título
                    Text(
                        text = titleText,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = titleColor
                    )

                    // 3. Descripción
                    Text(
                        text = descText,
                        fontSize = 15.sp,
                        color = DarkText
                    )

                    // 4. (Solo Unhealthy) Posibles causas
                    if (!isHealthy) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = stringResource(R.string.diagnosis_unhealthy_causes_title),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkText
                            )
                            Text(text = stringResource(R.string.diagnosis_cause_1), fontSize = 14.sp, color = DarkText)
                            Text(text = stringResource(R.string.diagnosis_cause_2), fontSize = 14.sp, color = DarkText)
                            Text(text = stringResource(R.string.diagnosis_cause_3), fontSize = 14.sp, color = DarkText)
                        }
                    }

                    // 5. Confianza
                    Text(
                        text = confText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray
                    )

                    // 6. (Solo Unhealthy) Nota
                    if (!isHealthy) {
                        Text(
                            text = stringResource(R.string.diagnosis_unhealthy_note),
                            fontSize = 12.sp,
                            fontStyle = FontStyle.Italic,
                            color = Color.Gray
                        )
                    }

                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))

                    // 7. Recomendación
                    Text(
                        text = stringResource(R.string.diagnosis_recommendation_header),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkText
                    )
                    Text(
                        text = recomText,
                        fontSize = 15.sp,
                        color = DarkText
                    )

                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))

                    // 8. Fecha y Planta (datos secundarios)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(text = "Planta: ${diagnosis.plantName}", fontSize = 13.sp, color = Color.Gray)
                        Text(text = "Fecha: $dateStr", fontSize = 13.sp, color = Color.Gray)
                    }
                }
            }

            // Disclaimer
            Text(
                text = stringResource(R.string.diagnosis_disclaimer),
                fontSize = 12.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )

            // 9. Botón Exportar PDF
            Button(
                onClick = {
                    if (isExporting) return@Button
                    isExporting = true
                    scope.launch {
                        val uri = exportToPdf(
                            context = context,
                            diagnosis = diagnosis,
                            titleText = titleText,
                            descText = descText,
                            confText = confText,
                            recomText = recomText,
                            dateStr = dateStr,
                            isHealthy = isHealthy
                        )
                        isExporting = false
                        if (uri != null) {
                            Toast.makeText(context, "PDF Exportado correctamente", Toast.LENGTH_LONG).show()
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, "application/pdf")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            try {
                                context.startActivity(Intent.createChooser(intent, "Abrir PDF"))
                            } catch (e: Exception) {
                                Toast.makeText(context, "No hay visor de PDF instalado", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "Error al exportar PDF", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(bottom = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = activeColor),
                shape = RoundedCornerShape(28.dp),
                enabled = !isExporting
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isExporting) "Exportando..." else stringResource(R.string.export_pdf),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

private suspend fun exportToPdf(
    context: Context,
    diagnosis: DiagnosisEntity,
    titleText: String,
    descText: String,
    confText: String,
    recomText: String,
    dateStr: String,
    isHealthy: Boolean
): Uri? = withContext(Dispatchers.IO) {
    try {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 at 72 dpi
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()

        var yPos = 50f
        val margin = 50f
        val contentWidth = 495f

        fun drawWrappedText(text: String, startY: Float, width: Float, textSize: Float = 12f, isBold: Boolean = false, isItalic: Boolean = false): Float {
            paint.textSize = textSize
            paint.isFakeBoldText = isBold
            var currY = startY
            val words = text.split(" ")
            var line = ""
            for (word in words) {
                val testLine = if (line.isEmpty()) word else "$line $word"
                if (paint.measureText(testLine) > width) {
                    canvas.drawText(line, margin, currY, paint)
                    currY += (textSize + 6f)
                    line = word
                } else {
                    line = testLine
                }
            }
            if (line.isNotEmpty()) {
                canvas.drawText(line, margin, currY, paint)
                currY += (textSize + 6f)
            }
            return currY
        }

        // Title
        paint.textSize = 22f
        paint.isFakeBoldText = true
        canvas.drawText("Reporte de Diagnóstico", margin, yPos, paint)
        yPos += 35f

        if (diagnosis.modelVersion == "debug-fake") {
            paint.textSize = 12f
            paint.color = android.graphics.Color.RED
            canvas.drawText("MODO PRUEBA (Modelo ausente)", margin, yPos, paint)
            paint.color = android.graphics.Color.BLACK
            yPos += 20f
        }

        // Image
        val imgFile = File(diagnosis.imagePath)
        if (imgFile.exists()) {
            val bitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
            if (bitmap != null) {
                val scaledHeight = (300f / bitmap.width) * bitmap.height
                val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 300, scaledHeight.toInt(), true)
                canvas.drawBitmap(scaledBitmap, margin, yPos, paint)
                yPos += scaledHeight + 25f
                scaledBitmap.recycle()
                bitmap.recycle()
            }
        }

        // Result Title
        paint.textSize = 18f
        paint.isFakeBoldText = true
        paint.color = if (isHealthy) android.graphics.Color.parseColor("#4CAF50") else android.graphics.Color.parseColor("#D32F2F")
        canvas.drawText(titleText, margin, yPos, paint)
        paint.color = android.graphics.Color.BLACK
        yPos += 25f

        // Description
        paint.textSize = 12f
        paint.isFakeBoldText = false
        yPos = drawWrappedText(descText, yPos, contentWidth) + 10f

        // Causes (if unhealthy)
        if (!isHealthy) {
            paint.textSize = 13f
            paint.isFakeBoldText = true
            canvas.drawText(context.getString(R.string.diagnosis_unhealthy_causes_title), margin, yPos, paint)
            yPos += 18f
            paint.textSize = 12f
            paint.isFakeBoldText = false
            canvas.drawText(context.getString(R.string.diagnosis_cause_1), margin + 10f, yPos, paint)
            yPos += 16f
            canvas.drawText(context.getString(R.string.diagnosis_cause_2), margin + 10f, yPos, paint)
            yPos += 16f
            canvas.drawText(context.getString(R.string.diagnosis_cause_3), margin + 10f, yPos, paint)
            yPos += 20f
        }

        // Confidence
        paint.textSize = 12f
        paint.isFakeBoldText = false
        paint.color = android.graphics.Color.GRAY
        canvas.drawText(confText, margin, yPos, paint)
        paint.color = android.graphics.Color.BLACK
        yPos += 20f

        // Note (if unhealthy)
        if (!isHealthy) {
            paint.textSize = 11f
            paint.color = android.graphics.Color.GRAY
            yPos = drawWrappedText(context.getString(R.string.diagnosis_unhealthy_note), yPos, contentWidth) + 10f
            paint.color = android.graphics.Color.BLACK
        }

        // Recommendation
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText(context.getString(R.string.diagnosis_recommendation_header), margin, yPos, paint)
        yPos += 20f
        paint.textSize = 12f
        paint.isFakeBoldText = false
        yPos = drawWrappedText(recomText, yPos, contentWidth) + 20f

        // Secondary data (Plant & Date)
        paint.textSize = 11f
        paint.color = android.graphics.Color.GRAY
        canvas.drawText("Planta: ${diagnosis.plantName}", margin, yPos, paint)
        yPos += 16f
        canvas.drawText("Fecha: $dateStr", margin, yPos, paint)
        yPos += 30f

        // Disclaimer
        drawWrappedText(context.getString(R.string.diagnosis_disclaimer), yPos, contentWidth, textSize = 10f)

        document.finishPage(page)

        val fileName = "Diagnostico_${diagnosis.plantName}_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}.pdf"
        
        val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val u = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (u != null) {
                resolver.openOutputStream(u)?.use { document.writeTo(it) }
            }
            u
        } else {
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(dir, fileName)
            FileOutputStream(file).use { document.writeTo(it) }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }

        document.close()
        uri
    } catch (e: Exception) {
        null
    }
}
