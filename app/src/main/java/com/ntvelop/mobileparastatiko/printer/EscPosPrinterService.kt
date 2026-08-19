package com.ntvelop.mobileparastatiko.printer

import com.ntvelop.mobileparastatiko.api.AadeBookInvoiceType
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

enum class PaperWidth(val charsPerLine: Int) {
    MM58(32),
    MM80(48)
}

/**
 * ESC/POS Thermal Printing Driver & Formatter for Mobile-Parastatiko.
 * Formats receipt layout for 58mm and 80mm thermal printers with full myDATA & Delivery Note metadata.
 */
object EscPosPrinterService {

    // ESC/POS Commands
    private val ESC_INIT = byteArrayOf(0x1B, 0x40)
    private val ALIGN_LEFT = byteArrayOf(0x1B, 0x61, 0x00)
    private val ALIGN_CENTER = byteArrayOf(0x1B, 0x61, 0x01)
    private val ALIGN_RIGHT = byteArrayOf(0x1B, 0x61, 0x02)
    private val BOLD_ON = byteArrayOf(0x1B, 0x45, 0x01)
    private val BOLD_OFF = byteArrayOf(0x1B, 0x45, 0x00)
    private val DOUBLE_SIZE_ON = byteArrayOf(0x1D, 0x21, 0x11)
    private val DOUBLE_SIZE_OFF = byteArrayOf(0x1D, 0x21, 0x00)
    private val FEED_LINE = byteArrayOf(0x0A)
    private val CUT_PAPER = byteArrayOf(0x1D, 0x56, 0x41, 0x10)

    /**
     * Generates complete ESC/POS binary command stream for an invoice / delivery note.
     */
    fun buildEscPosCommands(invoice: AadeBookInvoiceType, paperWidth: PaperWidth = PaperWidth.MM80): ByteArray {
        val bos = ByteArrayOutputStream()

        bos.write(ESC_INIT)

        // Header Section
        bos.write(ALIGN_CENTER)
        bos.write(BOLD_ON)
        bos.write(DOUBLE_SIZE_ON)
        bos.write(encodeText("MOBILE-PARASTATIKO\n"))
        bos.write(DOUBLE_SIZE_OFF)

        val docTypeName = getDocTypeName(invoice.invoiceHeader.invoiceType)
        bos.write(encodeText("$docTypeName\n"))
        bos.write(BOLD_OFF)
        bos.write(encodeText("Σειρά: ${invoice.invoiceHeader.series}  ΑΑ: ${invoice.invoiceHeader.aa}\n"))
        bos.write(encodeText("Ημ/νία: ${invoice.invoiceHeader.issueDate}\n"))
        bos.write(encodeText(divider(paperWidth)))

        // Parties Section
        bos.write(ALIGN_LEFT)
        bos.write(BOLD_ON)
        bos.write(encodeText("ΕΚΔΟΤΗΣ:\n"))
        bos.write(BOLD_OFF)
        invoice.issuer?.let { iss ->
            bos.write(encodeText("  ΑΦΜ: ${iss.vatNumber} (${iss.country})\n"))
            iss.name?.let { bos.write(encodeText("  Ονομασία: $it\n")) }
        }

        invoice.counterpart?.let { cp ->
            bos.write(BOLD_ON)
            bos.write(encodeText("ΛΗΠΤΗΣ:\n"))
            bos.write(BOLD_OFF)
            bos.write(encodeText("  ΑΦΜ: ${cp.vatNumber} (${cp.country})\n"))
            cp.name?.let { bos.write(encodeText("  Ονομασία: $it\n")) }
        }

        // Delivery Note Metadata
        invoice.invoiceHeader.movePurpose?.let { moveP ->
            bos.write(encodeText("Σκοπός Διακίνησης: ${getMovePurposeTitle(moveP, invoice.invoiceHeader.otherMovePurposeTitle)}\n"))
        }

        bos.write(encodeText(divider(paperWidth)))

        // Items Table
        bos.write(BOLD_ON)
        val headerLine = formatTwoColumns("Περιγραφή", "Αξία (€)", paperWidth.charsPerLine)
        bos.write(encodeText("$headerLine\n"))
        bos.write(BOLD_OFF)
        bos.write(encodeText(divider(paperWidth)))

        invoice.invoiceDetails.forEach { row ->
            val descr = row.itemDescr ?: "Προϊόν ${row.lineNumber}"
            val qtyStr = row.quantity?.let { "x$it " } ?: ""
            val lineRight = String.format("%.2f", row.netValue)
            val lineLeft = "${row.lineNumber}. $descr $qtyStr"

            bos.write(encodeText("${formatTwoColumns(lineLeft, lineRight, paperWidth.charsPerLine)}\n"))
        }

        bos.write(encodeText(divider(paperWidth)))

        // Summary Totals
        val s = invoice.invoiceSummary
        bos.write(encodeText(formatTwoColumns("Καθαρή Αξία:", String.format("%.2f €", s.totalNetValue), paperWidth.charsPerLine) + "\n"))
        bos.write(encodeText(formatTwoColumns("ΦΠΑ:", String.format("%.2f €", s.totalVatAmount), paperWidth.charsPerLine) + "\n"))

        bos.write(BOLD_ON)
        bos.write(DOUBLE_SIZE_ON)
        bos.write(encodeText(formatTwoColumns("ΣΥΝΟΛΟ:", String.format("%.2f €", s.totalGrossValue), paperWidth.charsPerLine / 2) + "\n"))
        bos.write(DOUBLE_SIZE_OFF)
        bos.write(BOLD_OFF)

        bos.write(encodeText(divider(paperWidth)))

        // myDATA MARK & QR Code Info
        bos.write(ALIGN_CENTER)
        invoice.mark?.let { markVal ->
            bos.write(BOLD_ON)
            bos.write(encodeText("MARK: $markVal\n"))
            bos.write(BOLD_OFF)
        }
        invoice.qrCodeUrl?.let { qr ->
            bos.write(encodeText("QR URL:\n$qr\n"))
        }

        bos.write(FEED_LINE)
        bos.write(FEED_LINE)
        bos.write(CUT_PAPER)

        return bos.toByteArray()
    }

    private fun getDocTypeName(type: String): String {
        return when (type) {
            "1.1" -> "ΤΙΜΟΛΟΓΙΟ ΠΩΛΗΣΗΣ"
            "1.2" -> "ΤΙΜΟΛΟΓΙΟ ΕΝΔΟΚΟΙΝΟΤΙΚΟ"
            "1.5" -> "ΕΚΚΑΘΑΡΙΣΗ ΤΡΙΤΩΝ"
            "8.4" -> "ΑΠΟΔΕΙΞΗ POS"
            "8.5" -> "ΕΠΙΣΤΡΟΦΗ POS"
            "8.6" -> "ΔΕΛΤΙΟ ΠΑΡΑΓΓΕΛΙΑΣ ΕΣΤΙΑΣΗΣ"
            "9.1" -> "ΣΥΣΧΕΤΙΖΟΜΕΝΟ ΔΕΛΤΙΟ ΑΠΟΣΤΟΛΗΣ"
            "9.2" -> "ΣΥΓΚΕΝΤΡΩΤΙΚΟ ΔΕΛΤΙΟ ΑΠΟΣΤΟΛΗΣ"
            "9.3" -> "ΨΗΦΙΑΚΟ ΔΕΛΤΙΟ ΑΠΟΣΤΟΛΗΣ"
            "10.1" -> "ΔΕΛΤΙΟ ΠΟΣΟΤΙΚΗΣ ΠΑΡΑΛΑΒΗΣ (ΣΥΣΧΕΤΙΖΟΜΕΝΟ)"
            "10.2" -> "ΔΕΛΤΙΟ ΠΟΣΟΤΙΚΗΣ ΠΑΡΑΛΑΒΗΣ (ΜΗ ΣΥΣΧΕΤΙΖΟΜΕΝΟ)"
            "11.1" -> "ΑΠΥ ΑΓΑΘΩΝ"
            "11.2" -> "ΑΠΥ ΥΠΗΡΕΣΙΩΝ"
            "11.4" -> "ΠΙΣΤΩΤΙΚΟ ΛΙΑΝΙΚΗΣ"
            else -> "ΠΑΡΑΣΤΑΤΙΚΟ ($type)"
        }
    }

    private fun getMovePurposeTitle(purpose: Int, otherTitle: String?): String {
        return when (purpose) {
            1 -> "Πώληση"
            2 -> "Πώληση για Λογαριασμό Τρίτων"
            3 -> "Δειγματισμός"
            4 -> "Έκθεση"
            5 -> "Επιστροφή"
            7 -> "Επεξεργασία"
            8 -> "Διακίνηση μεταξύ Υποκαταστημάτων"
            9 -> "Αγορά"
            19 -> "Άλλο (${otherTitle ?: ""})"
            else -> "Κωδικός $purpose"
        }
    }

    private fun divider(paperWidth: PaperWidth): String {
        return "-".repeat(paperWidth.charsPerLine) + "\n"
    }

    private fun formatTwoColumns(left: String, right: String, maxChars: Int): String {
        val available = maxChars - right.length
        val truncatedLeft = if (left.length > available - 1) left.substring(0, available - 1) else left
        val spaces = " ".repeat(maxOf(1, available - truncatedLeft.length))
        return "$truncatedLeft$spaces$right"
    }

    private fun encodeText(text: String): ByteArray {
        return text.toByteArray(Charset.forName("ISO-8859-7"))
    }
}
