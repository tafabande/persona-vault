package com.pims.vault.core.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.pims.vault.domain.model.PublicResumeData
import java.io.File
import java.io.FileOutputStream

object ResumePdfGenerator {

    private const val PAGE_WIDTH = 595 // A4 standard width at 72dpi
    private const val PAGE_HEIGHT = 842 // A4 standard height at 72dpi
    private const val MARGIN_X = 40f
    private const val MARGIN_BOTTOM = 50f
    private const val CONTENT_WIDTH = PAGE_WIDTH - (MARGIN_X * 2)

    private val COLOR_PRIMARY = Color.rgb(24, 43, 73) // Deep Navy Accent
    private val COLOR_TEXT_MAIN = Color.rgb(30, 30, 30) // Near Black Main Text
    private val COLOR_TEXT_MUTED = Color.rgb(100, 105, 115) // Slate Secondary Text
    private val COLOR_DIVIDER = Color.rgb(215, 220, 228) // Subtle Accent Line

    /**
     * Generates a styled, print-ready PDF resume document from PublicResumeData.
     */
    fun generateResumePdf(context: Context, data: PublicResumeData): File {
        val pdfDoc = PdfDocument()
        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var page = pdfDoc.startPage(pageInfo)
        var canvas = page.canvas

        val paintTitle = Paint().apply {
            color = COLOR_PRIMARY
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val paintSubtitle = Paint().apply {
            color = COLOR_PRIMARY
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val paintSectionHeader = Paint().apply {
            color = COLOR_PRIMARY
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            letterSpacing = 0.08f
        }

        val paintBodyBold = Paint().apply {
            color = COLOR_TEXT_MAIN
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val paintBody = Paint().apply {
            color = COLOR_TEXT_MAIN
            textSize = 9f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }

        val paintMuted = Paint().apply {
            color = COLOR_TEXT_MUTED
            textSize = 8.5f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }

        val paintDivider = Paint().apply {
            color = COLOR_DIVIDER
            strokeWidth = 1f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        var currentY = 50f

        fun checkPageBreak(neededSpace: Float) {
            if (currentY + neededSpace > PAGE_HEIGHT - MARGIN_BOTTOM) {
                pdfDoc.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                page = pdfDoc.startPage(pageInfo)
                canvas = page.canvas
                currentY = 50f
            }
        }

        fun drawSectionHeader(title: String) {
            checkPageBreak(35f)
            currentY += 14f
            canvas.drawText(title.uppercase(), MARGIN_X, currentY, paintSectionHeader)
            currentY += 5f
            canvas.drawLine(MARGIN_X, currentY, MARGIN_X + CONTENT_WIDTH, currentY, paintDivider)
            currentY += 12f
        }

        // 1. CANDIDATE HEADER
        canvas.drawText(data.fullName.ifBlank { "Personal Dossier" }, MARGIN_X, currentY, paintTitle)
        currentY += 16f

        if (data.headline.isNotBlank()) {
            canvas.drawText(data.headline, MARGIN_X, currentY, paintSubtitle)
            currentY += 14f
        }

        // Contact Bar
        val contactItems = mutableListOf<String>()
        if (!data.primaryEmail.isNullOrBlank()) contactItems.add(data.primaryEmail)
        if (!data.primaryPhone.isNullOrBlank()) contactItems.add(data.primaryPhone)
        if (data.location.isNotBlank()) contactItems.add(data.location)
        if (!data.primaryAddress.isNullOrBlank() && data.primaryAddress != data.location) {
            contactItems.add(data.primaryAddress)
        }

        if (contactItems.isNotEmpty()) {
            val contactLine = contactItems.joinToString("   •   ")
            canvas.drawText(contactLine, MARGIN_X, currentY, paintMuted)
            currentY += 14f
        }

        // Header Divider
        currentY += 4f
        canvas.drawLine(MARGIN_X, currentY, MARGIN_X + CONTENT_WIDTH, currentY, Paint().apply {
            color = COLOR_PRIMARY
            strokeWidth = 1.8f
            style = Paint.Style.STROKE
            isAntiAlias = true
        })
        currentY += 10f

        // 2. PROFESSIONAL SUMMARY / BIO
        if (!data.bioOrSummary.isNullOrBlank()) {
            drawSectionHeader("Professional Profile")
            val summaryLines = wrapText(data.bioOrSummary, CONTENT_WIDTH, paintBody)
            for (line in summaryLines) {
                checkPageBreak(14f)
                canvas.drawText(line, MARGIN_X, currentY, paintBody)
                currentY += 13f
            }
        }

        // 3. WORK EXPERIENCE
        if (data.hasCareerHistory) {
            drawSectionHeader("Experience")
            for (emp in data.employments) {
                checkPageBreak(45f)
                // Role title and dates
                canvas.drawText(emp.position, MARGIN_X, currentY, paintBodyBold)
                val dateStr = listOfNotNull(
                    emp.startDate?.takeIf { it.isNotBlank() },
                    if (emp.isCurrent) "Present" else emp.endDate?.takeIf { it.isNotBlank() }
                ).joinToString(" – ")

                if (dateStr.isNotBlank()) {
                    val dateWidth = paintMuted.measureText(dateStr)
                    canvas.drawText(dateStr, MARGIN_X + CONTENT_WIDTH - dateWidth, currentY, paintMuted)
                }
                currentY += 12f

                // Company and Location
                val companyInfo = listOfNotNull(
                    emp.company.takeIf { it.isNotBlank() },
                    emp.location?.takeIf { it.isNotBlank() },
                    emp.department?.takeIf { it.isNotBlank() }
                ).joinToString(" | ")
                if (companyInfo.isNotBlank()) {
                    canvas.drawText(companyInfo, MARGIN_X, currentY, paintMuted)
                    currentY += 12f
                }

                // Responsibilities bullet points
                if (!emp.responsibilities.isNullOrBlank()) {
                    val bullets = emp.responsibilities.split("\n", ";").filter { it.isNotBlank() }
                    for (b in bullets) {
                        val wrapped = wrapText(b.trim().removePrefix("•").trim(), CONTENT_WIDTH - 14f, paintBody)
                        for ((idx, line) in wrapped.withIndex()) {
                            checkPageBreak(13f)
                            if (idx == 0) {
                                canvas.drawText("•", MARGIN_X + 2f, currentY, paintBody)
                            }
                            canvas.drawText(line, MARGIN_X + 12f, currentY, paintBody)
                            currentY += 12f
                        }
                    }
                }
                currentY += 6f
            }
        }

        // 4. EDUCATION & QUALIFICATIONS
        if (data.educations.isNotEmpty()) {
            drawSectionHeader("Education")
            for (edu in data.educations) {
                checkPageBreak(38f)
                canvas.drawText(edu.qualification, MARGIN_X, currentY, paintBodyBold)
                val eduDates = listOfNotNull(edu.startDate?.takeIf { it.isNotBlank() }, edu.endDate?.takeIf { it.isNotBlank() }).joinToString(" – ")
                if (eduDates.isNotBlank()) {
                    val dWidth = paintMuted.measureText(eduDates)
                    canvas.drawText(eduDates, MARGIN_X + CONTENT_WIDTH - dWidth, currentY, paintMuted)
                }
                currentY += 12f

                val instLine = listOfNotNull(
                    edu.institution.takeIf { it.isNotBlank() },
                    edu.fieldOfStudy?.takeIf { it.isNotBlank() },
                    edu.country?.takeIf { it.isNotBlank() }
                ).joinToString(" | ")
                if (instLine.isNotBlank()) {
                    canvas.drawText(instLine, MARGIN_X, currentY, paintMuted)
                    currentY += 12f
                }
                if (!edu.grade.isNullOrBlank()) {
                    canvas.drawText("Grade / Honors: ${edu.grade}", MARGIN_X, currentY, paintMuted)
                    currentY += 11f
                }
                currentY += 4f
            }
        }

        // 5. CERTIFICATIONS
        if (data.certificates.isNotEmpty()) {
            drawSectionHeader("Certifications")
            for (cert in data.certificates) {
                checkPageBreak(28f)
                canvas.drawText(cert.qualification, MARGIN_X, currentY, paintBodyBold)
                if (!cert.endDate.isNullOrBlank()) {
                    val dWidth = paintMuted.measureText(cert.endDate)
                    canvas.drawText(cert.endDate, MARGIN_X + CONTENT_WIDTH - dWidth, currentY, paintMuted)
                }
                currentY += 12f
                if (cert.institution.isNotBlank()) {
                    canvas.drawText("Issuing Organization: ${cert.institution}", MARGIN_X, currentY, paintMuted)
                    currentY += 12f
                }
                currentY += 3f
            }
        }

        // 6. KEY ATTRIBUTES & CUSTOM FIELDS
        if (data.hasAttributes) {
            drawSectionHeader("Skills & Personal Universe")
            val halfCol = (CONTENT_WIDTH - 20f) / 2f
            var col = 0
            var colStartY = currentY

            for (field in data.customFields) {
                checkPageBreak(24f)
                val x = if (col == 0) MARGIN_X else MARGIN_X + halfCol + 20f
                canvas.drawText(field.label.uppercase(), x, currentY, paintSectionHeader)
                canvas.drawText(field.value, x, currentY + 11f, paintBody)

                if (col == 0) {
                    col = 1
                } else {
                    col = 0
                    currentY += 24f
                    colStartY = currentY
                }
            }
            if (col == 1) currentY += 24f
        }

        // 7. PUBLIC LINKS & SOCIAL ACCOUNTS
        if (data.hasSocialLinks) {
            drawSectionHeader("Online & Social Links")
            for (acc in data.socialAccounts) {
                checkPageBreak(16f)
                val platformName = acc.customPlatformName ?: acc.platform.displayName
                canvas.drawText("• $platformName:", MARGIN_X, currentY, paintBodyBold)
                canvas.drawText(acc.handleOrUrl, MARGIN_X + 90f, currentY, paintBody)
                currentY += 13f
            }
        }

        pdfDoc.finishPage(page)

        // Write output to cache directory
        val exportDir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
        val safeName = data.fullName.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "Persona" }
        val targetFile = File(exportDir, "${safeName}_Resume.pdf")

        FileOutputStream(targetFile).use { fos ->
            pdfDoc.writeTo(fos)
        }
        pdfDoc.close()

        return targetFile
    }

    /**
     * Helper to wrap text into lines fitting within maxWidth.
     */
    private fun wrapText(text: String, maxWidth: Float, paint: Paint): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) <= maxWidth) {
                currentLine = testLine
            } else {
                if (currentLine.isNotEmpty()) lines.add(currentLine)
                currentLine = word
            }
        }
        if (currentLine.isNotEmpty()) lines.add(currentLine)
        return lines
    }

    /**
     * Launches a system Share / View intent for the generated PDF.
     */
    fun shareResumePdf(context: Context, pdfFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Public Resume Dossier")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share or Save Resume PDF"))
    }
}
