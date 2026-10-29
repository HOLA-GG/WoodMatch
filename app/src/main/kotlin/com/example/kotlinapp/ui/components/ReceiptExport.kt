package com.example.kotlinapp.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.kotlinapp.data.model.WithdrawalRequest
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object ReceiptExport {
    private fun safeRecord(request: WithdrawalRequest): JSONObject {
        val receipt = request.receipt.orEmpty()
        return JSONObject().put("id", request.id).put("title", receipt["title"] ?: request.wasteTitle)
            .put("quantity", receipt["quantity"] ?: "").put("unit", receipt["unit"] ?: "")
            .put("authorName", receipt["authorName"] ?: "Generador").put("requesterName", receipt["requesterName"] ?: request.requesterName)
            .put("status", request.status).put("createdAt", request.createdAt).put("proposedDate", request.proposedDate)
            .put("completedAt", request.completedAt).put("passValidatedAt", request.passValidatedAt)
            .put("timeline", JSONArray(request.timeline.map { event -> JSONObject().put("action", event["action"] ?: event["status"] ?: "Movimiento").put("at", event["at"]) }))
    }

    fun share(context: Context, request: WithdrawalRequest, format: String) {
        check(request.isCompleted && request.receipt != null) { "La entrega aún no tiene comprobante." }
        val folder = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(folder, "comprobante-${request.id.replace(Regex("[^a-zA-Z0-9_-]"), "")}.${if (format == "pdf") "pdf" else "json"}")
        val record = safeRecord(request)
        if (format == "pdf") writePdf(file, record) else file.writeText(record.toString(2))
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND)
            .setType(if (format == "pdf") "application/pdf" else "application/json")
            .putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), "Guardar comprobante de WoodMatch"))
    }

    private fun writePdf(file: File, data: JSONObject) {
        val document = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        var number = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(420, 595, number).create())
        var y = 108f
        fun heading() {
            paint.color = Color.rgb(37, 86, 62)
            page.canvas.drawRect(0f, 0f, 420f, 76f, paint)
            paint.color = Color.WHITE; paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); paint.textSize = 22f
            page.canvas.drawText("WoodMatch", 28f, 34f, paint)
            paint.textSize = 12f; page.canvas.drawText("Comprobante de aprovechamiento", 28f, 56f, paint)
        }
        fun footer() {
            paint.color = Color.DKGRAY; paint.textSize = 8f; paint.typeface = Typeface.DEFAULT
            page.canvas.drawText("Registro de entrega · no es factura fiscal · página $number", 28f, 573f, paint)
        }
        fun line(text: String, bold: Boolean = false) {
            val words = text.replace('\n', ' ').split(' ')
            val wrapped = mutableListOf<String>(); var current = ""
            paint.textSize = 10f
            words.forEach { word -> val proposed = if (current.isEmpty()) word else "$current $word"; if (paint.measureText(proposed) > 360f && current.isNotEmpty()) { wrapped += current; current = word } else current = proposed }
            if (current.isNotEmpty()) wrapped += current
            wrapped.forEach { value ->
                if (y > 540f) { footer(); document.finishPage(page); number++; page = document.startPage(PdfDocument.PageInfo.Builder(420, 595, number).create()); heading(); y = 108f }
                paint.color = Color.rgb(35, 48, 42); paint.typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT; paint.textSize = 10f
                page.canvas.drawText(value, 28f, y, paint); y += 16f
            }
            y += 5f
        }
        try {
            heading()
            line("ENTREGA COMPLETADA", true)
            line("Registro: ${data.optString("id")}")
            line(data.optString("title"), true)
            line("Material entregado: ${data.opt("quantity")} ${data.opt("unit")}")
            line("Entrega: ${data.optString("authorName")}")
            line("Recibe: ${data.optString("requesterName")}")
            line("Creada: ${activityDate(data.optLong("createdAt"))}")
            line("Fecha acordada: ${data.optString("proposedDate")}")
            if (!data.isNull("passValidatedAt")) line("Pase validado: ${activityDate(data.optLong("passValidatedAt"))}")
            line("Completada: ${activityDate(data.optLong("completedAt"))}")
            line("TRAZABILIDAD", true)
            val timeline = data.getJSONArray("timeline")
            for (i in 0 until timeline.length()) { val event = timeline.getJSONObject(i); line("${activityDate(event.optLong("at"))} · ${activityAction(event.optString("action"))}") }
            line("Ambas personas confirmaron la entrega. Este registro omite teléfonos y ubicación exacta.")
            footer(); document.finishPage(page)
            file.outputStream().use { document.writeTo(it) }
        } finally { document.close() }
    }
}
