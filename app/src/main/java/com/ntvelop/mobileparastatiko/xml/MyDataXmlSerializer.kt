package com.ntvelop.mobileparastatiko.xml

import com.ntvelop.mobileparastatiko.api.*
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Pure Kotlin XML Serialization & Parsing Engine strictly compliant with AADE myDATA XSD v2.0.2.
 * Zero external library runtime requirements, fully unit-testable.
 */
object MyDataXmlSerializer {

    private const val INVOICE_NS = "http://www.aade.gr/myDATA/invoice/v1.0"
    private const val INCOME_CLS_NS = "https://www.aade.gr/myDATA/incomeClassificaton/v1.0"
    private const val EXPENSES_CLS_NS = "https://www.aade.gr/myDATA/expensesClassificaton/v1.0"

    /**
     * Serializes InvoicesDoc object into AADE compliant XML String.
     */
    fun serializeInvoicesDoc(doc: InvoicesDoc): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<InvoicesDoc xmlns=\"$INVOICE_NS\" ")
        sb.append("xmlns:icls=\"$INCOME_CLS_NS\" ")
        sb.append("xmlns:ecls=\"$EXPENSES_CLS_NS\">\n")

        doc.invoices?.forEach { invoice ->
            sb.append("  <invoice>\n")
            invoice.uid?.let { sb.append("    <uid>$it</uid>\n") }
            invoice.mark?.let { sb.append("    <mark>$it</mark>\n") }
            invoice.cancelledByMark?.let { sb.append("    <cancelledByMark>$it</cancelledByMark>\n") }
            invoice.authenticationCode?.let { sb.append("    <authenticationCode>$it</authenticationCode>\n") }
            invoice.transmissionFailure?.let { sb.append("    <transmissionFailure>$it</transmissionFailure>\n") }

            // Issuer
            invoice.issuer?.let { issuer ->
                sb.append("    <issuer>\n")
                sb.append("      <vatNumber>${issuer.vatNumber}</vatNumber>\n")
                sb.append("      <country>${issuer.country}</country>\n")
                sb.append("      <branch>${issuer.branch}</branch>\n")
                issuer.name?.let { sb.append("      <name>${escapeXml(it)}</name>\n") }
                issuer.address?.let { addr ->
                    sb.append("      <address>\n")
                    addr.street?.let { sb.append("        <street>${escapeXml(it)}</street>\n") }
                    addr.number?.let { sb.append("        <number>${escapeXml(it)}</number>\n") }
                    addr.postalCode?.let { sb.append("        <postalCode>${escapeXml(it)}</postalCode>\n") }
                    addr.city?.let { sb.append("        <city>${escapeXml(it)}</city>\n") }
                    sb.append("      </address>\n")
                }
                sb.append("    </issuer>\n")
            }

            // Counterpart
            invoice.counterpart?.let { cp ->
                sb.append("    <counterpart>\n")
                sb.append("      <vatNumber>${cp.vatNumber}</vatNumber>\n")
                sb.append("      <country>${cp.country}</country>\n")
                sb.append("      <branch>${cp.branch}</branch>\n")
                cp.name?.let { sb.append("      <name>${escapeXml(it)}</name>\n") }
                cp.address?.let { addr ->
                    sb.append("      <address>\n")
                    addr.street?.let { sb.append("        <street>${escapeXml(it)}</street>\n") }
                    addr.number?.let { sb.append("        <number>${escapeXml(it)}</number>\n") }
                    addr.postalCode?.let { sb.append("        <postalCode>${escapeXml(it)}</postalCode>\n") }
                    addr.city?.let { sb.append("        <city>${escapeXml(it)}</city>\n") }
                    sb.append("      </address>\n")
                }
                sb.append("    </counterpart>\n")
            }

            // Invoice Header
            val h = invoice.invoiceHeader
            sb.append("    <invoiceHeader>\n")
            sb.append("      <series>${escapeXml(h.series)}</series>\n")
            sb.append("      <aa>${escapeXml(h.aa)}</aa>\n")
            sb.append("      <issueDate>${h.issueDate}</issueDate>\n")
            sb.append("      <invoiceType>${h.invoiceType}</invoiceType>\n")
            h.vatPaymentSuspension?.let { sb.append("      <vatPaymentSuspension>$it</vatPaymentSuspension>\n") }
            h.currency?.let { sb.append("      <currency>$it</currency>\n") }
            h.exchangeRate?.let { sb.append("      <exchangeRate>$it</exchangeRate>\n") }
            h.isDeliveryNote?.let { sb.append("      <isDeliveryNote>$it</isDeliveryNote>\n") }
            h.movePurpose?.let { sb.append("      <movePurpose>$it</movePurpose>\n") }
            h.otherMovePurposeTitle?.let { sb.append("      <otherMovePurposeTitle>${escapeXml(it)}</otherMovePurposeTitle>\n") }
            h.receivingNotePurpose?.let { sb.append("      <receivingNotePurpose>$it</receivingNotePurpose>\n") }
            h.otherReceivingNotePurposeTitle?.let { sb.append("      <otherReceivingNotePurposeTitle>${escapeXml(it)}</otherReceivingNotePurposeTitle>\n") }
            h.nonObligatedRecipient?.let { sb.append("      <nonObligatedRecipient>$it</nonObligatedRecipient>\n") }
            h.withoutDigitalTransportTracking?.let { sb.append("      <withoutDigitalTransportTracking>$it</withoutDigitalTransportTracking>\n") }
            h.correlatedInvoices?.forEach { sb.append("      <correlatedInvoices>$it</correlatedInvoices>\n") }
            sb.append("    </invoiceHeader>\n")

            // Payment Details
            invoice.paymentMethods?.let { pmList ->
                if (pmList.isNotEmpty()) {
                    sb.append("    <paymentMethods>\n")
                    pmList.forEach { pm ->
                        sb.append("      <paymentMethodDetails>\n")
                        sb.append("        <type>${pm.type}</type>\n")
                        sb.append("        <amount>${String.format("%.2f", pm.amount).replace(',', '.')}</amount>\n")
                        pm.paymentMethodInfo?.let { sb.append("        <paymentMethodInfo>${escapeXml(it)}</paymentMethodInfo>\n") }
                        pm.tipAmount?.let { sb.append("        <tipAmount>${String.format("%.2f", it).replace(',', '.')}</tipAmount>\n") }
                        pm.transactionId?.let { sb.append("        <transactionId>${escapeXml(it)}</transactionId>\n") }
                        pm.ECRToken?.let { sb.append("        <ECRToken>${escapeXml(it)}</ECRToken>\n") }
                        sb.append("      </paymentMethodDetails>\n")
                    }
                    sb.append("    </paymentMethods>\n")
                }
            }

            // Invoice Details (Rows)
            invoice.invoiceDetails.forEach { r ->
                sb.append("    <invoiceDetails>\n")
                sb.append("      <lineNumber>${r.lineNumber}</lineNumber>\n")
                r.recType?.let { sb.append("      <recType>$it</recType>\n") }
                r.itemCode?.let { sb.append("      <itemCode>${escapeXml(it)}</itemCode>\n") }
                r.itemDescr?.let { sb.append("      <itemDescr>${escapeXml(it)}</itemDescr>\n") }
                r.TaricNo?.let { sb.append("      <TaricNo>${escapeXml(it)}</TaricNo>\n") }
                r.quantity?.let { sb.append("      <quantity>$it</quantity>\n") }
                r.measurementUnit?.let { sb.append("      <measurementUnit>$it</measurementUnit>\n") }
                r.otherMeasurementUnitQuantity?.let { sb.append("      <otherMeasurementUnitQuantity>$it</otherMeasurementUnitQuantity>\n") }
                r.otherMeasurementUnitTitle?.let { sb.append("      <otherMeasurementUnitTitle>${escapeXml(it)}</otherMeasurementUnitTitle>\n") }
                sb.append("      <netValue>${String.format("%.2f", r.netValue).replace(',', '.')}</netValue>\n")
                sb.append("      <vatCategory>${r.vatCategory}</vatCategory>\n")
                sb.append("      <vatAmount>${String.format("%.2f", r.vatAmount).replace(',', '.')}</vatAmount>\n")
                r.vatExemptionCategory?.let { sb.append("      <vatExemptionCategory>$it</vatExemptionCategory>\n") }
                r.diakinisissMark?.let { sb.append("      <diakinisissMark>$it</diakinisissMark>\n") }
                sb.append("    </invoiceDetails>\n")
            }

            // Taxes Totals
            invoice.taxesTotals?.let { taxes ->
                if (taxes.isNotEmpty()) {
                    sb.append("    <taxesTotals>\n")
                    taxes.forEach { t ->
                        sb.append("      <taxes>\n")
                        sb.append("        <taxType>${t.taxType}</taxType>\n")
                        t.taxCategory?.let { sb.append("        <taxCategory>$it</taxCategory>\n") }
                        t.underlyingValue?.let { sb.append("        <underlyingValue>${String.format("%.2f", it).replace(',', '.')}</underlyingValue>\n") }
                        sb.append("        <taxAmount>${String.format("%.2f", t.taxAmount).replace(',', '.')}</taxAmount>\n")
                        sb.append("      </taxes>\n")
                    }
                    sb.append("    </taxesTotals>\n")
                }
            }

            // Invoice Summary
            val s = invoice.invoiceSummary
            sb.append("    <invoiceSummary>\n")
            sb.append("      <totalNetValue>${String.format("%.2f", s.totalNetValue).replace(',', '.')}</totalNetValue>\n")
            sb.append("      <totalVatAmount>${String.format("%.2f", s.totalVatAmount).replace(',', '.')}</totalVatAmount>\n")
            sb.append("      <totalWithheldAmount>${String.format("%.2f", s.totalWithheldAmount).replace(',', '.')}</totalWithheldAmount>\n")
            sb.append("      <totalFeesAmount>${String.format("%.2f", s.totalFeesAmount).replace(',', '.')}</totalFeesAmount>\n")
            sb.append("      <totalStampDutyAmount>${String.format("%.2f", s.totalStampDutyAmount).replace(',', '.')}</totalStampDutyAmount>\n")
            sb.append("      <totalOtherTaxesAmount>${String.format("%.2f", s.totalOtherTaxesAmount).replace(',', '.')}</totalOtherTaxesAmount>\n")
            sb.append("      <totalDeductionsAmount>${String.format("%.2f", s.totalDeductionsAmount).replace(',', '.')}</totalDeductionsAmount>\n")
            sb.append("      <totalGrossValue>${String.format("%.2f", s.totalGrossValue).replace(',', '.')}</totalGrossValue>\n")
            sb.append("    </invoiceSummary>\n")

            invoice.qrCodeUrl?.let { sb.append("    <qrCodeUrl>${escapeXml(it)}</qrCodeUrl>\n") }

            sb.append("  </invoice>\n")
        }

        sb.append("</InvoicesDoc>")
        return sb.toString()
    }

    /**
     * Serializes RegisterTransfer payload.
     */
    fun serializeRegisterTransfer(req: RegisterTransferRequest): String {
        return """<?xml version="1.0" encoding="UTF-8"?>
<Transport>
  <qrUrl>${escapeXml(req.qrUrl)}</qrUrl>
  <transportDetail>
    <vehicleNumber>${escapeXml(req.transportDetail.vehicleNumber)}</vehicleNumber>
    <transportType>${req.transportDetail.transportType}</transportType>
    ${req.transportDetail.carrierVatNumber?.let { "<carrierVatNumber>$it</carrierVatNumber>" } ?: ""}
  </transportDetail>
</Transport>"""
    }

    /**
     * Serializes ConfirmDeliveryOutcome payload.
     */
    fun serializeConfirmDeliveryOutcome(req: ConfirmDeliveryOutcomeRequest): String {
        return """<?xml version="1.0" encoding="UTF-8"?>
<ConfirmDeliveryOutcomeRequest>
  <qrUrl>${escapeXml(req.qrUrl)}</qrUrl>
  <outcome>${req.outcome}</outcome>
</ConfirmDeliveryOutcomeRequest>"""
    }

    /**
     * Serializes RejectDeliveryNote payload.
     */
    fun serializeRejectDeliveryNote(req: RejectDeliveryNoteRequest): String {
        val reasonXml = req.rejectionReason?.let { "<rejectionReason>${escapeXml(it)}</rejectionReason>" } ?: ""
        return """<?xml version="1.0" encoding="UTF-8"?>
<RejectDeliveryNoteRequest>
  <qrUrl>${escapeXml(req.qrUrl)}</qrUrl>
  $reasonXml
</RejectDeliveryNoteRequest>"""
    }

    /**
     * Serializes CancelDeliveryNote payload.
     */
    fun serializeCancelDeliveryNote(req: CancelDeliveryNoteRequest): String {
        val reasonXml = req.cancellationReason?.let { "<cancellationReason>${escapeXml(it)}</cancellationReason>" } ?: ""
        return """<?xml version="1.0" encoding="UTF-8"?>
<CancelDeliveryNoteRequest>
  <qrUrl>${escapeXml(req.qrUrl)}</qrUrl>
  $reasonXml
</CancelDeliveryNoteRequest>"""
    }

    /**
     * Serializes GenerateGroupQRCode payload.
     */
    fun serializeGroupQRCode(req: GroupQRCodeRequest): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<GenerateGroupQRCodeRequest>\n")
        sb.append("  <qrUrls>\n")
        req.qrUrls.qrUrlList.forEach { url ->
            sb.append("    <qrUrl>${escapeXml(url)}</qrUrl>\n")
        }
        sb.append("  </qrUrls>\n")
        sb.append("</GenerateGroupQRCodeRequest>")
        return sb.toString()
    }

    /**
     * Deserializes AADE ResponseDoc XML.
     */
    fun parseResponseDoc(xml: String): ResponseDoc {
        val dbFactory = DocumentBuilderFactory.newInstance()
        dbFactory.isNamespaceAware = false
        val dBuilder = dbFactory.newDocumentBuilder()
        val doc = dBuilder.parse(ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8)))
        doc.documentElement.normalize()

        val responseList = mutableListOf<Response>()
        val nList: NodeList = doc.getElementsByTagName("response")

        for (i in 0 until nList.length) {
            val node = nList.item(i)
            if (node is Element) {
                val index = getTagValue(node, "index")?.toIntOrNull()
                val invoiceUid = getTagValue(node, "invoiceUid")
                val invoiceMark = getTagValue(node, "invoiceMark")?.toLongOrNull()
                val transferMark = getTagValue(node, "transferMark")?.toLongOrNull()
                val rejectMark = getTagValue(node, "rejectMark")?.toLongOrNull()
                val deliveryOutcomeMark = getTagValue(node, "deliveryOutcomeMark")?.toLongOrNull()
                val invoiceDeliveryStatus = getTagValue(node, "invoiceDeliveryStatus")
                val status = getTagValue(node, "status")
                val qrUrl = getTagValue(node, "qrUrl")
                val statusCode = getTagValue(node, "statusCode") ?: "Success"

                val errorItems = mutableListOf<ErrorItem>()
                val errorNodes = node.getElementsByTagName("error")
                for (j in 0 until errorNodes.length) {
                    val errNode = errorNodes.item(j)
                    if (errNode is Element) {
                        val msg = getTagValue(errNode, "message") ?: ""
                        val code = getTagValue(errNode, "code") ?: ""
                        errorItems.add(ErrorItem(msg, code))
                    }
                }

                val errorsObj = if (errorItems.isNotEmpty()) Errors(errorItems) else null

                responseList.add(
                    Response(
                        index = index,
                        invoiceUid = invoiceUid,
                        invoiceMark = invoiceMark,
                        transferMark = transferMark,
                        rejectMark = rejectMark,
                        deliveryOutcomeMark = deliveryOutcomeMark,
                        invoiceDeliveryStatus = invoiceDeliveryStatus,
                        status = status,
                        qrUrl = qrUrl,
                        statusCode = statusCode,
                        errors = errorsObj
                    )
                )
            }
        }

        return ResponseDoc(responses = responseList)
    }

    /**
     * Deserializes GetDeliveryNoteStatusResponse XML.
     */
    fun parseGetDeliveryStatusResponse(xml: String): GetDeliveryStatusResponse {
        val dbFactory = DocumentBuilderFactory.newInstance()
        dbFactory.isNamespaceAware = false
        val dBuilder = dbFactory.newDocumentBuilder()
        val doc = dBuilder.parse(ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8)))
        doc.documentElement.normalize()

        val root = doc.documentElement
        val invoiceMark = getTagValue(root, "invoiceMark")
        val status = getTagValue(root, "status")
        val invoiceDeliveryStatusAlt = getTagValue(root, "invoiceDeliveryStatus")
        val dispatchTimestamp = getTagValue(root, "dispatchTimestamp")

        val responseDoc = parseResponseDoc(xml)

        return GetDeliveryStatusResponse(
            invoiceMark = invoiceMark,
            status = status,
            invoiceDeliveryStatusAlt = invoiceDeliveryStatusAlt,
            dispatchTimestamp = dispatchTimestamp,
            responses = responseDoc.responses
        )
    }

    private fun getTagValue(elem: Element, tagName: String): String? {
        val list = elem.getElementsByTagName(tagName)
        if (list.length > 0) {
            val item = list.item(0)
            if (item != null && item.parentNode == elem) {
                return item.textContent
            }
            return item?.textContent
        }
        return null
    }

    private fun escapeXml(str: String): String {
        return str.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
