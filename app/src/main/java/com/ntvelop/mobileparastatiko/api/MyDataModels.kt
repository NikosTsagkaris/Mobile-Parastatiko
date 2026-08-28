package com.ntvelop.mobileparastatiko.api

import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Namespace
import org.simpleframework.xml.Root
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Phase 2 - Digital Delivery Note Models (Ψηφιακό Δελτίο Αποστολής - Β' Φάση)
 * & myDATA REST API v2.0.2 Domain Specifications.
 */

enum class DeliveryStatus(val text: String) {
    Registered("Καταχωρημένο (Registered)"),
    InTransit("Προς Διακίνηση / Σε Μεταφορά (InTransit)"),
    DeliveredByCarrier("Παραδόθηκε (Delivered)"),
    Completed("Ολοκληρώθηκε (Completed)"),
    Rejected("Απορριφθείσα (Rejected)"),
    Cancelled("Ακυρωμένο (Cancelled)"),
    FailedDelivery("Αποτυχία παράδοσης (FailedDelivery)"),
    InTransitReturn("Επιστροφή σε Διακίνηση (InTransitReturn)"),
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
                v == "intransitreturn" || v.contains("return") || v.contains("επιστροφ") -> InTransitReturn
                else -> entries.find { it.name.equals(v, ignoreCase = true) } ?: Unknown
            }
        }
    }
}

/**
 * Address Representation for Parties
 */
@Root(name = "AddressType", strict = false)
data class AddressType(
    @field:Element(name = "street", required = false)
    var street: String? = null,
    @field:Element(name = "number", required = false)
    var number: String? = null,
    @field:Element(name = "postalCode", required = false)
    var postalCode: String? = null,
    @field:Element(name = "city", required = false)
    var city: String? = null
)

/**
 * Other Correlated Entity Representation (Transporter, Recipient, Representative etc.)
 */
@Root(name = "EntityType", strict = false)
data class EntityType(
    @field:Element(name = "type", required = true)
    var type: Int = 1, // 1: Tax Rep, 2: Intermediary, 3: Transporter, 4: Goods Recipient, 5: Sender/Seller, 6: Other
    @field:Element(name = "entityData", required = true)
    var entityData: PartyType = PartyType()
)

/**
 * Party representation (Issuer or Counterpart)
 */
@Root(name = "PartyType", strict = false)
data class PartyType(
    @field:Element(name = "vatNumber", required = true)
    var vatNumber: String = "",

    @field:Element(name = "country", required = true)
    var country: String = "GR",

    @field:Element(name = "branch", required = true)
    var branch: Int = 0,

    @field:Element(name = "name", required = false)
    var name: String? = null,

    @field:Element(name = "address", required = false)
    var address: AddressType? = null
)

/**
 * Invoice Header schema compliant with Appendix 8.1
 */
@Root(name = "InvoiceHeaderType", strict = false)
data class InvoiceHeaderType(
    @field:Element(name = "series", required = true)
    var series: String = "0",

    @field:Element(name = "aa", required = true)
    var aa: String = "1",

    @field:Element(name = "issueDate", required = true)
    var issueDate: String = "", // Format: YYYY-MM-DD

    @field:Element(name = "invoiceType", required = true)
    var invoiceType: String = "1.1", // 1.1, 1.2, 1.5, 8.4, 8.5, 8.6, 9.1, 9.2, 9.3, 10.1, 10.2, 11.1, 11.2, 11.4

    @field:Element(name = "vatPaymentSuspension", required = false)
    var vatPaymentSuspension: Boolean? = false,

    @field:Element(name = "currency", required = false)
    var currency: String? = "EUR",

    @field:Element(name = "exchangeRate", required = false)
    var exchangeRate: Double? = null,

    @field:Element(name = "isDeliveryNote", required = false)
    var isDeliveryNote: Boolean? = false,

    @field:Element(name = "movePurpose", required = false)
    var movePurpose: Int? = null, // 1: Sale, 2: 3rd-party, 3: Sampling, 4: Exhibition, 5: Return, 7: Processing, 8: Inter-branch, 9: Purchase, 19: Other

    @field:Element(name = "otherMovePurposeTitle", required = false)
    var otherMovePurposeTitle: String? = null, // Mandatory if movePurpose = 19

    @field:Element(name = "receivingNotePurpose", required = false)
    var receivingNotePurpose: Int? = null, // 1: Non-obligated, 2: Refusal, 3: Intra-community, 4: 3rd-country, 5: Quantitative check, 6: Partial/Non-delivery, 7: Other

    @field:Element(name = "otherReceivingNotePurposeTitle", required = false)
    var otherReceivingNotePurposeTitle: String? = null, // Mandatory if receivingNotePurpose = 7

    @field:Element(name = "nonObligatedRecipient", required = false)
    var nonObligatedRecipient: Boolean? = false,

    @field:Element(name = "withoutDigitalTransportTracking", required = false)
    var withoutDigitalTransportTracking: Boolean? = false,

    @field:ElementList(inline = true, entry = "correlatedInvoices", required = false)
    var correlatedInvoices: List<Long>? = null
)

/**
 * Line item row element (`InvoiceRowType`)
 */
@Root(name = "InvoiceRowType", strict = false)
data class InvoiceRowType(
    @field:Element(name = "lineNumber", required = true)
    var lineNumber: Int = 1,

    @field:Element(name = "recType", required = false)
    var recType: Int? = null, // 2: Fee w/ VAT, 3: Other Tax w/ VAT, 6: Gift Card, 7: Negative Values / Shortages

    @field:Element(name = "itemCode", required = false)
    var itemCode: String? = null,

    @field:Element(name = "itemDescr", required = false)
    var itemDescr: String? = null,

    @field:Element(name = "TaricNo", required = false)
    var TaricNo: String? = null,

    @field:Element(name = "quantity", required = false)
    var quantity: Double? = null,

    @field:Element(name = "measurementUnit", required = false)
    var measurementUnit: Int? = null, // 1: Pcs, 2: Kg, 3: Liters, 4: Meters, 5: Sqm, 6: Cbm, 7: Other Pcs

    @field:Element(name = "otherMeasurementUnitQuantity", required = false)
    var otherMeasurementUnitQuantity: Double? = null, // Mandatory if measurementUnit = 7

    @field:Element(name = "otherMeasurementUnitTitle", required = false)
    var otherMeasurementUnitTitle: String? = null, // Mandatory if measurementUnit = 7

    @field:Element(name = "netValue", required = true)
    var netValue: Double = 0.0,

    @field:Element(name = "vatCategory", required = true)
    var vatCategory: Int = 1, // 1: 24%, 2: 13%, 3: 6%, 4: 17%, 5: 9%, 6: 4%, 7: 0%, 8: Without VAT, 9: 3%, 10: 4%

    @field:Element(name = "vatAmount", required = true)
    var vatAmount: Double = 0.0,

    @field:Element(name = "vatExemptionCategory", required = false)
    var vatExemptionCategory: Int? = null, // Mandatory if vatCategory = 7 (Codes 1-31)

    @field:Element(name = "diakinisissMark", required = false)
    var diakinisissMark: Long? = null
)

/**
 * Summary totals (`InvoiceSummaryType`)
 */
@Root(name = "InvoiceSummaryType", strict = false)
data class InvoiceSummaryType(
    @field:Element(name = "totalNetValue", required = true)
    var totalNetValue: Double = 0.0,

    @field:Element(name = "totalVatAmount", required = true)
    var totalVatAmount: Double = 0.0,

    @field:Element(name = "totalWithheldAmount", required = false)
    var totalWithheldAmount: Double = 0.0,

    @field:Element(name = "totalFeesAmount", required = false)
    var totalFeesAmount: Double = 0.0,

    @field:Element(name = "totalStampDutyAmount", required = false)
    var totalStampDutyAmount: Double = 0.0,

    @field:Element(name = "totalOtherTaxesAmount", required = false)
    var totalOtherTaxesAmount: Double = 0.0,

    @field:Element(name = "totalDeductionsAmount", required = false)
    var totalDeductionsAmount: Double = 0.0,

    @field:Element(name = "totalGrossValue", required = true)
    var totalGrossValue: Double = 0.0
) {
    /**
     * Checks mathematical equality:
     * totalGrossValue == totalNetValue + totalVatAmount + totalOtherTaxesAmount + totalStampDutyAmount + totalFeesAmount - totalWithheldAmount - totalDeductionsAmount
     */
    fun isCalculatedGrossValid(): Boolean {
        val expected = BigDecimal.valueOf(totalNetValue)
            .add(BigDecimal.valueOf(totalVatAmount))
            .add(BigDecimal.valueOf(totalOtherTaxesAmount))
            .add(BigDecimal.valueOf(totalStampDutyAmount))
            .add(BigDecimal.valueOf(totalFeesAmount))
            .subtract(BigDecimal.valueOf(totalWithheldAmount))
            .subtract(BigDecimal.valueOf(totalDeductionsAmount))
            .setScale(2, RoundingMode.HALF_UP)

        val actual = BigDecimal.valueOf(totalGrossValue).setScale(2, RoundingMode.HALF_UP)
        return expected.compareTo(actual) == 0
    }
}

/**
 * Tax summary wrapper
 */
@Root(name = "TaxTotalsType", strict = false)
data class TaxTotalsType(
    @field:Element(name = "taxType", required = true)
    var taxType: Int = 1,
    @field:Element(name = "taxCategory", required = false)
    var taxCategory: Int? = null,
    @field:Element(name = "underlyingValue", required = false)
    var underlyingValue: Double? = null,
    @field:Element(name = "taxAmount", required = true)
    var taxAmount: Double = 0.0
)

/**
 * Payment method detail representation
 */
@Root(name = "PaymentMethodDetailType", strict = false)
data class PaymentMethodDetailType(
    @field:Element(name = "type", required = true)
    var type: Int = 1, // 1: Cash, 3: Cheque, 5: POS/Card, 7: Account/Transfer
    @field:Element(name = "amount", required = true)
    var amount: Double = 0.0,
    @field:Element(name = "paymentMethodInfo", required = false)
    var paymentMethodInfo: String? = null,
    @field:Element(name = "tipAmount", required = false)
    var tipAmount: Double? = null,
    @field:Element(name = "transactionId", required = false)
    var transactionId: String? = null,
    @field:Element(name = "ECRToken", required = false)
    var ECRToken: String? = null
)

/**
 * Complete AadeBookInvoiceType domain model
 */
@Root(name = "invoice", strict = false)
data class AadeBookInvoiceType(
    @field:Element(name = "uid", required = false)
    var uid: String? = null,

    @field:Element(name = "mark", required = false)
    var mark: Long? = null,

    @field:Element(name = "cancelledByMark", required = false)
    var cancelledByMark: Long? = null,

    @field:Element(name = "authenticationCode", required = false)
    var authenticationCode: String? = null,

    @field:Element(name = "transmissionFailure", required = false)
    var transmissionFailure: Int? = null, // 1..4 (3 = loss of connectivity)

    @field:Element(name = "issuer", required = false)
    var issuer: PartyType? = null,

    @field:Element(name = "counterpart", required = false)
    var counterpart: PartyType? = null,

    @field:Element(name = "invoiceHeader", required = true)
    var invoiceHeader: InvoiceHeaderType = InvoiceHeaderType(),

    @field:ElementList(name = "paymentMethods", inline = false, entry = "paymentMethodDetails", required = false)
    var paymentMethods: List<PaymentMethodDetailType>? = null,

    @field:ElementList(inline = true, entry = "invoiceDetails", required = true)
    var invoiceDetails: List<InvoiceRowType> = emptyList(),

    @field:ElementList(name = "taxesTotals", inline = false, entry = "taxes", required = false)
    var taxesTotals: List<TaxTotalsType>? = null,

    @field:Element(name = "invoiceSummary", required = true)
    var invoiceSummary: InvoiceSummaryType = InvoiceSummaryType(),

    @field:Element(name = "qrCodeUrl", required = false)
    var qrCodeUrl: String? = null,

    @field:Element(name = "otherCorrelatedEntities", required = false)
    var otherCorrelatedEntities: List<EntityType>? = null,

    @field:Element(name = "invoiceDeliveryStatus", required = false)
    var invoiceDeliveryStatus: String? = null
)

/**
 * Envelope for SendInvoices payload
 */
@Root(name = "InvoicesDoc", strict = false)
@Namespace(reference = "http://www.aade.gr/myDATA/invoice/v1.0")
data class InvoicesDoc(
    @field:Element(name = "invoiceDeliveryStatus", required = false)
    var invoiceDeliveryStatus: String? = null,

    @field:ElementList(inline = true, entry = "invoice", required = false)
    var invoices: List<AadeBookInvoiceType>? = null,

    @field:Element(name = "errors", required = false)
    var errors: Errors? = null
)

// Legacy alias for compatibility with existing views
typealias Invoice = AadeBookInvoiceType

@Root(name = "ProviderSignatureType", strict = false)
data class ProviderSignatureType(
    @field:Element(name = "EndToEndReferenceID", required = false)
    var endToEndReferenceID: String? = null
)

@Root(name = "TransportDetailType", strict = false)
data class TransportDetailType(
    @field:Element(name = "transportType", required = false)
    var transportType: Int? = null, // Range 1-7

    @field:Element(name = "VehicleNumber", required = false)
    var vehicleNumber: String? = null,

    @field:Element(name = "carrierVatNumber", required = false)
    var carrierVatNumber: String? = null
)

@Root(name = "RequestedDoc", strict = false)
data class RequestedInvoicesDoc(
    @field:Element(name = "invoicesDoc", required = false)
    var invoicesDoc: InvoicesDocWrapper? = null,

    @field:Element(name = "cancelledInvoicesDoc", required = false)
    var cancelledInvoicesDoc: String? = null,

    @field:Element(name = "errors", required = false)
    var errors: Errors? = null
)

@Root(name = "invoicesDoc", strict = false)
data class InvoicesDocWrapper(
    @field:ElementList(inline = true, entry = "invoice", required = false)
    var invoices: List<AadeBookInvoiceType>? = null
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
    var invoiceMark: String? = null,

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
    var transportDetail: TransportDetailRequest = TransportDetailRequest()
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
    var qrUrls: QrUrlsWrapper = QrUrlsWrapper(emptyList())
)

data class QrUrlsWrapper(
    @field:ElementList(inline = true, entry = "qrUrl", required = true)
    var qrUrlList: List<String> = emptyList()
)

@Root(name = "RequestGroupQRDetailsResponse", strict = false)
data class RequestGroupQRDetailsResponse(
    @field:ElementList(inline = true, entry = "invoiceMark", required = false)
    var invoiceMarks: List<Long>? = null
)

/**
 * Validation Result Class
 */
data class DomainValidationResult(
    val isValid: Boolean,
    val errors: List<String>
)

/**
 * Domain Validation Engine for AADE myDATA Schemas
 */
object MyDataValidator {

    /**
     * Επίσημος Αλγόριθμος Modulo 11 της ΑΑΔΕ για Ελληνικά ΑΦΜ
     */
    fun isValidGreekVat(vat: String?): Boolean {
        if (vat.isNullOrBlank()) return false
        val cleanVat = vat.trim()
        if (!cleanVat.matches(Regex("^[0-9]{9}$"))) return false
        if (cleanVat == "000000000") return false

        var sum = 0
        for (i in 0 until 8) {
            val digit = cleanVat[i].digitToInt()
            val weight = 1 shl (8 - i) // 2^(8-i)
            sum += digit * weight
        }

        val remainder = sum % 11
        val checkDigit = if (remainder == 10) 0 else remainder

        return checkDigit == cleanVat[8].digitToInt()
    }

    fun validateInvoice(invoice: AadeBookInvoiceType): DomainValidationResult {
        val errors = mutableListOf<String>()

        // 1. Header validations
        val header = invoice.invoiceHeader
        if (header.movePurpose == 19 && header.otherMovePurposeTitle.isNullOrBlank()) {
            errors.add("Rule 3.1: 'otherMovePurposeTitle' is mandatory when movePurpose = 19 (Other).")
        }

        if ((header.invoiceType == "10.1" || header.invoiceType == "10.2") && header.receivingNotePurpose == null) {
            errors.add("Rule 3.1: 'receivingNotePurpose' is mandatory for Document Types 10.1 & 10.2.")
        }

        if (header.receivingNotePurpose == 7 && header.otherReceivingNotePurposeTitle.isNullOrBlank()) {
            errors.add("Rule 3.1: 'otherReceivingNotePurposeTitle' is mandatory when receivingNotePurpose = 7.")
        }

        if (!header.currency.equals("EUR", ignoreCase = true) && (header.exchangeRate == null || header.exchangeRate!! <= 0)) {
            errors.add("Rule 3.1: 'exchangeRate' is mandatory when currency is not EUR.")
        }

        // 2. Issuer & Counterpart VAT validation (Modulo 11)
        invoice.issuer?.let { party ->
            if (party.vatNumber.isBlank()) {
                errors.add("Rule 3.2: Issuer VAT Number is mandatory.")
            } else if (party.country.equals("GR", ignoreCase = true) && !isValidGreekVat(party.vatNumber)) {
                errors.add("Rule 3.2: Issuer VAT (${party.vatNumber}) is mathematically invalid (Modulo 11).")
            }
        }

        invoice.counterpart?.let { party ->
            if (party.vatNumber.isNotBlank() && party.country.equals("GR", ignoreCase = true) && !isValidGreekVat(party.vatNumber)) {
                errors.add("Rule 3.2: Counterpart VAT (${party.vatNumber}) is mathematically invalid (Modulo 11).")
            }
        }

        // 3. Line item validations
        if (invoice.invoiceDetails.isEmpty()) {
            errors.add("Rule 3.3: Invoice must contain at least one line item.")
        }

        val lineNumbers = mutableSetOf<Int>()
        invoice.invoiceDetails.forEachIndexed { index, row ->
            val rowNum = index + 1
            if (row.lineNumber <= 0 || !lineNumbers.add(row.lineNumber)) {
                errors.add("Rule 3.3 Line $rowNum: 'lineNumber' must be strictly positive and unique per document.")
            }

            if (row.vatCategory == 7 && row.vatExemptionCategory == null) {
                errors.add("Rule 3.3 Line $rowNum: 'vatExemptionCategory' is mandatory when vatCategory = 7 (0% / Exempt).")
            }

            if (row.measurementUnit == 7 && row.otherMeasurementUnitTitle.isNullOrBlank()) {
                errors.add("Rule 3.3 Line $rowNum: 'otherMeasurementUnitTitle' is mandatory when measurementUnit = 7.")
            }

            // Calculate expected VAT amount
            val vatRate = getVatRate(row.vatCategory)
            val expectedVat = BigDecimal.valueOf(row.netValue)
                .multiply(BigDecimal.valueOf(vatRate))
                .setScale(2, RoundingMode.HALF_UP)
                .toDouble()

            val actualVat = BigDecimal.valueOf(row.vatAmount).setScale(2, RoundingMode.HALF_UP).toDouble()
            if (Math.abs(expectedVat - actualVat) > 0.05) {
                errors.add("Rule 3.3 Line $rowNum: vatAmount ($actualVat) does not match expected calculation netValue * rate ($expectedVat).")
            }
        }

        // 4. Summary Balance Validation
        val summary = invoice.invoiceSummary
        if (!summary.isCalculatedGrossValid()) {
            errors.add("Rule 3.4: Summary totalGrossValue (${summary.totalGrossValue}) does not equal netValue + vat + taxes - withheld - deductions.")
        }

        return DomainValidationResult(errors.isEmpty(), errors)
    }

    fun getVatRate(vatCategory: Int): Double {
        return when (vatCategory) {
            1 -> 0.24
            2 -> 0.13
            3 -> 0.06
            4 -> 0.17
            5 -> 0.09
            6 -> 0.04
            7 -> 0.00
            8 -> 0.00
            9 -> 0.03
            10 -> 0.04
            else -> 0.00
        }
    }
}