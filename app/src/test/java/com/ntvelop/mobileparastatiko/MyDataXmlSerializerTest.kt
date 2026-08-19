package com.ntvelop.mobileparastatiko

import com.ntvelop.mobileparastatiko.api.*
import com.ntvelop.mobileparastatiko.xml.MyDataXmlSerializer
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit Tests for MyDataXmlSerializer verifying XML payload structure, AADE namespaces, and parsing logic.
 */
class MyDataXmlSerializerTest {

    @Test
    fun testSerializeInvoicesDoc_StructureAndNamespace() {
        val invoice = AadeBookInvoiceType(
            issuer = PartyType(vatNumber = "094000000", country = "GR", branch = 0),
            counterpart = PartyType(vatNumber = "999999999", country = "GR", branch = 0, name = "TEST CLIENT"),
            invoiceHeader = InvoiceHeaderType(
                series = "A",
                aa = "10",
                issueDate = "2026-08-19",
                invoiceType = "9.3",
                isDeliveryNote = true,
                movePurpose = 1
            ),
            invoiceDetails = listOf(
                InvoiceRowType(
                    lineNumber = 1,
                    itemDescr = "Product A",
                    quantity = 5.0,
                    netValue = 100.00,
                    vatCategory = 1,
                    vatAmount = 24.00
                )
            ),
            invoiceSummary = InvoiceSummaryType(
                totalNetValue = 100.00,
                totalVatAmount = 24.00,
                totalGrossValue = 124.00
            )
        )

        val xml = MyDataXmlSerializer.serializeInvoicesDoc(InvoicesDoc(invoices = listOf(invoice)))

        assertTrue(xml.contains("<InvoicesDoc xmlns=\"http://www.aade.gr/myDATA/invoice/v1.0\""))
        assertTrue(xml.contains("<vatNumber>094000000</vatNumber>"))
        assertTrue(xml.contains("<vatNumber>999999999</vatNumber>"))
        assertTrue(xml.contains("<series>A</series>"))
        assertTrue(xml.contains("<invoiceType>9.3</invoiceType>"))
        assertTrue(xml.contains("<totalGrossValue>124.00</totalGrossValue>"))
    }

    @Test
    fun testSerializeRegisterTransfer_OutputStructure() {
        val req = RegisterTransferRequest(
            qrUrl = "https://mydata.aade.gr/qr/400000123456789",
            transportDetail = TransportDetailRequest(vehicleNumber = "KHH1234", transportType = 1)
        )

        val xml = MyDataXmlSerializer.serializeRegisterTransfer(req)

        assertTrue(xml.contains("<Transport>"))
        assertTrue(xml.contains("<qrUrl>https://mydata.aade.gr/qr/400000123456789</qrUrl>"))
        assertTrue(xml.contains("<vehicleNumber>KHH1234</vehicleNumber>"))
    }

    @Test
    fun testParseResponseDoc_ExtractsMarkAndStatusCode() {
        val sampleXml = """<?xml version="1.0" encoding="UTF-8"?>
<ResponseDoc>
  <response>
    <index>1</index>
    <invoiceUid>ABC123456789</invoiceUid>
    <invoiceMark>400000123456789</invoiceMark>
    <qrUrl>https://mydataapidev.aade.gr/qr/400000123456789</qrUrl>
    <statusCode>Success</statusCode>
  </response>
</ResponseDoc>"""

        val doc = MyDataXmlSerializer.parseResponseDoc(sampleXml)

        assertNotNull(doc.responses)
        assertEquals(1, doc.responses!!.size)
        val first = doc.responses!![0]
        assertEquals(1, first.index)
        assertEquals("ABC123456789", first.invoiceUid)
        assertEquals(400000123456789L, first.invoiceMark)
        assertEquals("Success", first.statusCode)
    }
}
