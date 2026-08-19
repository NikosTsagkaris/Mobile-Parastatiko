package com.ntvelop.mobileparastatiko

import com.ntvelop.mobileparastatiko.api.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Comprehensive Unit Tests verifying invoice mathematical balance equations and AADE domain validation rules.
 */
class InvoiceValidationTest {

    @Test
    fun testMathematicalEquality_ValidGrossValue_ReturnsTrue() {
        val summary = InvoiceSummaryType(
            totalNetValue = 100.00,
            totalVatAmount = 24.00,
            totalOtherTaxesAmount = 5.00,
            totalStampDutyAmount = 2.00,
            totalFeesAmount = 3.00,
            totalWithheldAmount = 10.00,
            totalDeductionsAmount = 4.00,
            totalGrossValue = 120.00 // 100 + 24 + 5 + 2 + 3 - 10 - 4 = 120.00
        )

        assertTrue(summary.isCalculatedGrossValid())
    }

    @Test
    fun testMathematicalEquality_InvalidGrossValue_ReturnsFalse() {
        val summary = InvoiceSummaryType(
            totalNetValue = 100.00,
            totalVatAmount = 24.00,
            totalGrossValue = 150.00 // Mismatch!
        )

        assertFalse(summary.isCalculatedGrossValid())
    }

    @Test
    fun testMovePurpose19_MissingTitle_FailsValidation() {
        val invoice = AadeBookInvoiceType(
            issuer = PartyType(vatNumber = "094000000"),
            counterpart = PartyType(vatNumber = "999999999"),
            invoiceHeader = InvoiceHeaderType(
                movePurpose = 19,
                otherMovePurposeTitle = null // Should fail!
            ),
            invoiceDetails = listOf(InvoiceRowType(lineNumber = 1, netValue = 100.0, vatCategory = 1, vatAmount = 24.0)),
            invoiceSummary = InvoiceSummaryType(totalNetValue = 100.0, totalVatAmount = 24.0, totalGrossValue = 124.0)
        )

        val result = MyDataValidator.validateInvoice(invoice)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("otherMovePurposeTitle") })
    }

    @Test
    fun testVatCategory7_MissingExemptionCode_FailsValidation() {
        val invoice = AadeBookInvoiceType(
            issuer = PartyType(vatNumber = "094000000"),
            counterpart = PartyType(vatNumber = "999999999"),
            invoiceHeader = InvoiceHeaderType(),
            invoiceDetails = listOf(
                InvoiceRowType(
                    lineNumber = 1,
                    netValue = 100.0,
                    vatCategory = 7, // 0% Exempt
                    vatAmount = 0.0,
                    vatExemptionCategory = null // Missing exemption code!
                )
            ),
            invoiceSummary = InvoiceSummaryType(totalNetValue = 100.0, totalVatAmount = 0.0, totalGrossValue = 100.0)
        )

        val result = MyDataValidator.validateInvoice(invoice)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("vatExemptionCategory") })
    }

    @Test
    fun testType101_ShortageDiscrepancy_RecType7_ValidationPasses() {
        // Recipient receives less than declared -> Correlated Type 10.1 (ΔΠΠ) with recType = 7 (Shortage)
        val invoice = AadeBookInvoiceType(
            issuer = PartyType(vatNumber = "094000000"),
            counterpart = PartyType(vatNumber = "999999999"),
            invoiceHeader = InvoiceHeaderType(
                invoiceType = "10.1",
                receivingNotePurpose = 6, // Partial/Non-delivery
                correlatedInvoices = listOf(400000123456789L)
            ),
            invoiceDetails = listOf(
                InvoiceRowType(
                    lineNumber = 1,
                    recType = 7, // Shortage correction line
                    itemDescr = "Έλλειψη 2 τμχ κατά την παραλαβή",
                    quantity = -2.0,
                    netValue = 0.0,
                    vatCategory = 1,
                    vatAmount = 0.0
                )
            ),
            invoiceSummary = InvoiceSummaryType(totalNetValue = 0.0, totalVatAmount = 0.0, totalGrossValue = 0.0)
        )

        val result = MyDataValidator.validateInvoice(invoice)
        assertTrue(result.isValid)
    }
}
