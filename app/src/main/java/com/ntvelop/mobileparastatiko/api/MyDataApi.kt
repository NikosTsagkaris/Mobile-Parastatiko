package com.ntvelop.mobileparastatiko.api

import retrofit2.Call
import retrofit2.http.*

/**
 * myDATA REST API v2.0.2 & Phase B Digital Delivery Note Endpoints
 */
interface MyDataApi {

    /**
     * 1. Invoice Submission
     */
    @POST("SendInvoices")
    @Headers("Content-Type: text/xml; charset=utf-8")
    fun sendInvoices(@Body xmlBody: String): Call<ResponseDoc>

    /**
     * 2. Income Classification
     */
    @POST("SendIncomeClassification")
    @Headers("Content-Type: text/xml; charset=utf-8")
    fun sendIncomeClassification(@Body xmlBody: String): Call<ResponseDoc>

    /**
     * 3. Expenses Classification
     */
    @POST("SendExpensesClassification")
    @Headers("Content-Type: text/xml; charset=utf-8")
    fun sendExpensesClassification(@Body xmlBody: String): Call<ResponseDoc>

    /**
     * 4. Payment Method Reporting
     */
    @POST("SendPaymentsMethod")
    @Headers("Content-Type: text/xml; charset=utf-8")
    fun sendPaymentsMethod(@Body xmlBody: String): Call<ResponseDoc>

    /**
     * 5. Invoice Cancellation
     */
    @POST("CancelInvoice")
    fun cancelInvoice(
        @Query("mark") mark: Long,
        @Query("entityVatNumber") entityVatNumber: String? = null
    ): Call<ResponseDoc>

    /**
     * 6. Data Retrieval (Receiver)
     */
    @GET("RequestDocs")
    fun requestDocs(
        @Query("mark") mark: Long? = null,
        @Query("dateFrom") dateFrom: String? = null,
        @Query("dateTo") dateTo: String? = null,
        @Query("nextPartitionKey") nextPartitionKey: String? = null,
        @Query("nextRowKey") nextRowKey: String? = null
    ): Call<RequestedInvoicesDoc>

    /**
     * 7. Data Retrieval (Transmitter)
     */
    @GET("RequestTransmittedDocs")
    fun requestTransmittedDocs(
        @Query("mark") mark: Long? = null,
        @Query("dateFrom") dateFrom: String? = null,
        @Query("dateTo") dateTo: String? = null,
        @Query("nextPartitionKey") nextPartitionKey: String? = null,
        @Query("nextRowKey") nextRowKey: String? = null
    ): Call<RequestedInvoicesDoc>

    /**
     * 8. Digital Delivery Note Lifecycle Endpoints (Phase B)
     */

    // RegisterTransfer: Declaration of transport dispatch or transshipment by carrier/issuer
    @POST("RegisterTransfer")
    @Headers("Content-Type: text/xml; charset=utf-8")
    fun registerTransfer(@Body xmlBody: String): Call<ResponseDoc>

    // ConfirmDeliveryOutcome: Confirmation of delivery (FULL, PARTIAL, NONE)
    @POST("ConfirmDeliveryOutcome")
    @Headers("Content-Type: text/xml; charset=utf-8")
    fun confirmDeliveryOutcome(@Body xmlBody: String): Call<ResponseDoc>

    // RejectDeliveryNote: Full rejection of delivery note by recipient
    @POST("RejectDeliveryNote")
    @Headers("Content-Type: text/xml; charset=utf-8")
    fun rejectDeliveryNote(@Body xmlBody: String): Call<ResponseDoc>

    // CancelDeliveryNote: Issuer cancellation prior to InTransit
    @POST("CancelDeliveryNote")
    @Headers("Content-Type: text/xml; charset=utf-8")
    fun cancelDeliveryNote(@Body xmlBody: String): Call<ResponseDoc>

    // GetDeliveryNoteStatus: Retrieval of full dispatch history and status
    @GET("GetDeliveryNoteStatus")
    fun getDeliveryNoteStatus(
        @Query("mark") mark: String? = null,
        @Query("qrUrl") qrUrl: String? = null
    ): Call<GetDeliveryStatusResponse>

    // GenerateGroupQRCode & RequestGroupQRDetails
    @POST("GenerateGroupQRCode")
    @Headers("Content-Type: text/xml; charset=utf-8")
    fun generateGroupQRCode(@Body xmlBody: String): Call<ResponseDoc>

    @GET("RequestGroupQRDetails")
    fun requestGroupQRDetails(@Query("qrUrl") qrUrl: String): Call<RequestGroupQRDetailsResponse>

    // ConfirmDeliveryReturn: Declaration of returned goods to issuer
    @POST("ConfirmDeliveryReturn")
    @Headers("Content-Type: text/xml; charset=utf-8")
    fun confirmDeliveryReturn(@Body xmlBody: String): Call<ResponseDoc>
}
