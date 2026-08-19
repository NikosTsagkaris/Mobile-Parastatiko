package com.ntvelop.mobileparastatiko.api

import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Namespace
import org.simpleframework.xml.Root

/**
 * Phase 2 - Digital Delivery Note Models (Ψηφιακό Δελτίο Αποστολής - Β' Φάση)
 * Based on myDATA DEV specs.
 */

enum class DeliveryStatus(val text: String) {
    Registered("Καταχωρημένο (Registered)"),
    InTransit("Προς Διακίνηση / Σε Μεταφορά (InTransit)"),
    DeliveredByCarrier("Παραδόθηκε (Delivered)"),
    Completed("Ολοκληρώθηκε (Completed)"),
    Rejected("Απορριφθείσα (Rejected)"),
    Cancelled("Ακυρωμένο (Cancelled)"),
    FailedDelivery("Αποτυχία παράδοσης (FailedDelivery)"),
    Unknown("Άγνωστο");

    companion object {
        fun fromApiString(value: String?): DeliveryStatus {
            if (value == null || value.isBlank()) return Unknown
            val v = value.lowercase().trim()
            
            return when {
                v == "registered" || v.contains("καταχωρημενο") || v.contains("καταχωρημένο") || v.contains("διαβιβαστει") || v.contains("διαβιβαστεί") || v.contains("διαβιβασμενο") || v.contains("διαβιβασμένο") || v.contains("επιτυχια") || v.contains("επιτυχία") || v == "1" -> Registered
                v == "intransit" || v.contains("in transit") || v.contains("διακινηση") || v.contains("διακίνηση") || v.contains("μεταφορα") || v.contains("μεταφορά") || v.contains("υπο διακινηση") || v.contains("υπό διακίνηση") || v == "2" -> InTransit
                v == "deliveredbycarrier" || v.contains("delivered") || v.contains("παραδοθηκε") || v.contains("παραδόθηκε") || v.contains("παραλαβη") || v.contains("παραλαβή") || v == "3" -> DeliveredByCarrier
                v == "completed" || v.contains("ολοκληρωθηκε") || v.contains("ολοκληρώθηκε") || v.contains("ολοκληρωση") || v.contains("ολοκλήρωση") || v == "5" -> Completed
                v == "rejected" || v.contains("απορριφ") || v.contains("απόρριψη") || v == "6" -> Rejected
                v == "cancelled" || v.contains("ακυρω") || v.contains("ακυρο") || v.contains("άκυρο") || v.contains("ακυρωση") || v.contains("ακύρωση") -> Cancelled
                v == "faileddelivery" || v.contains("αποτυχια") || v.contains("αποτυχία") || v.contains("failed") || v == "4" -> FailedDelivery
                else -> {
                    entries.find { it.name.lowercase().equals(v, ignoreCase = true) } ?: Unknown
                }
            }
        }
    }
}

@Root(name = "ProviderSignatureType", strict = false)
data class ProviderSignatureType(
    // The second E was Greek, changed to English in Phase 2
    @field:Element(name = "EndToEndReferenceID", required = false)
    var endToEndReferenceID: String? = null
)

@Root(name = "TransportDetailType", strict = false)
data class TransportDetailType(
    // Range (1-7). If transportType != 7, vehicleNumber is mandatory during RegisterTransfer
    @field:Element(name = "transportType", required = false)
    var transportType: Int? = null,

    @field:Element(name = "VehicleNumber", required = false)
    var vehicleNumber: String? = null
)

@Root(name = "InvoicesDoc", strict = false)
data class InvoicesDoc(
    @field:Element(name = "invoiceDeliveryStatus", required = false)
    var invoiceDeliveryStatus: String? = null,
    @field:ElementList(inline = true, entry = "invoice", required = false)
    var invoices: List<Invoice>? = null,
    @field:Element(name = "errors", required = false)
    var errors: Errors? = null
)

@Root(name = "RequestedDoc", strict = false)
data class RequestedInvoicesDoc(
    @field:Element(name = "invoicesDoc", required = false)
    var invoicesDoc: InvoicesDocWrapper? = null,

    @field:Element(name = "cancelledInvoicesDoc", required = false)
    var cancelledInvoicesDoc: String? = null, // Dummy to avoid extra complex maps for now

    @field:Element(name = "errors", required = false)
    var errors: Errors? = null
)

@Root(name = "invoicesDoc", strict = false)
data class InvoicesDocWrapper(
    @field:ElementList(inline = true, entry = "invoice", required = false)
    var invoices: List<Invoice>? = null
)

@Root(name = "invoice", strict = false)
data class Invoice(
    @field:Element(name = "invoiceDeliveryStatus", required = false)
    var invoiceDeliveryStatus: String? = null,

    @field:Element(name = "status", required = false)
    var status: String? = null,

    @field:Element(name = "qrUrl", required = false)
    var qrUrl: String? = null,

    @field:Element(name = "mark", required = false)
    var mark: Long? = null
)

@Root(name = "ResponseDoc", strict = false)
data class ResponseDoc(
    @field:ElementList(inline = true, entry = "response", required = false)
    var responses: List<Response>? = null
)

@Root(name = "response", strict = false)
data class Response(
    @field:Element(name = "index", required = false)
    var index: Int? = null,

    @field:Element(name = "invoiceUid", required = false)
    var invoiceUid: String? = null,

    @field:Element(name = "invoiceMark", required = false)
    var invoiceMark: Long? = null,

    @field:Element(name = "transferMark", required = false)
    var transferMark: Long? = null,

    @field:Element(name = "rejectMark", required = false)
    var rejectMark: Long? = null,

    @field:Element(name = "deliveryOutcomeMark", required = false)
    var deliveryOutcomeMark: Long? = null,

    @field:Element(name = "invoiceDeliveryStatus", required = false)
    var invoiceDeliveryStatus: String? = null,

    @field:Element(name = "status", required = false)
    var status: String? = null,

    @field:Element(name = "qrUrl", required = false)
    var qrUrl: String? = null,

    @field:Element(name = "statusCode", required = true)
    var statusCode: String = "",

    @field:Element(name = "errors", required = false)
    var errors: Errors? = null
)

@Root(name = "GetDeliveryNoteStatusResponse", strict = false)
@Namespace(reference = "http://www.aade.gr/myDATA/invoice/v1.0")
data class GetDeliveryStatusResponse(
    @field:Element(name = "invoiceMark", required = false)
    var invoiceMark: String? = null, // Changed to String to be safe per XSD

    @field:Element(name = "status", required = false)
    var status: String? = null,

    @field:Element(name = "invoiceDeliveryStatus", required = false)
    var invoiceDeliveryStatusAlt: String? = null,

    @field:Element(name = "dispatchTimestamp", required = false)
    var dispatchTimestamp: String? = null,

    @field:ElementList(inline = true, entry = "response", required = false)
    var responses: List<Response>? = null,

    @field:Element(name = "errors", required = false)
    var errors: Errors? = null
)

@Root(name = "DeliveryEventType", strict = false)
data class DeliveryEvent(
    @field:Element(name = "eventType", required = true)
    var eventType: String = "",

    @field:Element(name = "eventTimestamp", required = true)
    var eventTimestamp: String = "",

    @field:Element(name = "actorVat", required = false)
    var actorVat: String? = null,

    @field:Element(name = "mark", required = false)
    var mark: Long? = null
)

@Root(name = "errors", strict = false)
data class Errors(
    @field:ElementList(inline = true, entry = "error", required = false)
    var errorList: List<ErrorItem>? = null
)

@Root(name = "error", strict = false)
data class ErrorItem(
    @field:Element(name = "message", required = true)
    var message: String = "",

    @field:Element(name = "code", required = true)
    var code: String = ""
)

// --- Phase 2 Request Models ---

@Root(name = "Transport", strict = false)
data class RegisterTransferRequest(
    @field:Element(name = "qrUrl", required = true)
    var qrUrl: String = "",
    
    @field:Element(name = "transportDetail", required = true)
    var transportDetail: TransportDetailRequest
)

@Root(name = "TransportDetailType", strict = false)
data class TransportDetailRequest(
    @field:Element(name = "vehicleNumber", required = true)
    var vehicleNumber: String = "",
    
    @field:Element(name = "transportType", required = true)
    var transportType: Int = 1,

    @field:Element(name = "carrierVatNumber", required = false)
    var carrierVatNumber: String? = null
)

@Root(name = "ConfirmDeliveryOutcomeRequest", strict = false)
data class ConfirmDeliveryOutcomeRequest(
    @field:Element(name = "qrUrl", required = true)
    var qrUrl: String = "",
    
    @field:Element(name = "outcome", required = true)
    var outcome: String = "FULL" // FULL, PARTIAL, NONE
)

@Root(name = "RejectDeliveryNoteRequest", strict = false)
data class RejectDeliveryNoteRequest(
    @field:Element(name = "qrUrl", required = true)
    var qrUrl: String = "",
    
    @field:Element(name = "rejectionReason", required = false)
    var rejectionReason: String? = null
)

@Root(name = "CancelDeliveryNoteRequest", strict = false)
data class CancelDeliveryNoteRequest(
    @field:Element(name = "qrUrl", required = true)
    var qrUrl: String = "",
    
    @field:Element(name = "cancellationReason", required = false)
    var cancellationReason: String? = null
)

@Root(name = "GenerateGroupQRCodeRequest", strict = false)
data class GroupQRCodeRequest(
    @field:Element(name = "qrUrls", required = true)
    var qrUrls: QrUrlsWrapper
)

data class QrUrlsWrapper(
    @field:ElementList(inline = true, entry = "qrUrl", required = true)
    var qrUrlList: List<String>
)

@Root(name = "RequestGroupQRDetailsResponse", strict = false)
data class RequestGroupQRDetailsResponse(
    @field:ElementList(inline = true, entry = "invoiceMark", required = false)
    var invoiceMarks: List<Long>? = null
)
