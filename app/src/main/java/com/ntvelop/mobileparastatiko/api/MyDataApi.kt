package com.ntvelop.mobileparastatiko.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url

/**
 * Phase 2 - Digital Delivery Note Endpoints
 */
interface MyDataApi {

    /**
     * ERP Methods
     */

    // 1. RegisterTransfer: Μέθοδος για την έναρξη και καταγραφή της διακίνησης.
    @POST
    fun registerTransfer(@Url url: String, @Body payload: RegisterTransferRequest): Call<ResponseDoc>

    // 2. ConfirmDeliveryOutcome: Μέθοδος για την επιβεβαίωση παράδοσης ή δήλωσης απόρριψης.
    @POST
    fun confirmDeliveryOutcome(@Url url: String, @Body payload: ConfirmDeliveryOutcomeRequest): Call<ResponseDoc>

    // 3. RejectDeliveryNote: Μέθοδος ολικής απόρριψης διακίνησης από το λήπτη
    @POST
    fun rejectDeliveryNote(@Url url: String, @Body payload: RejectDeliveryNoteRequest): Call<ResponseDoc>

    // 3b. CancelDeliveryNote: Μέθοδος ολικής ακύρωσης διακίνησης από τον εκδότη (Phase 2)
    @POST
    fun cancelDeliveryNote(@Url url: String, @Body payload: CancelDeliveryNoteRequest): Call<ResponseDoc>

    // 4. GetDeliveryNoteStatus: Μέθοδος ανάκτησης της τρέχουσας κατάστασης και του ιστορικού διακίνησης.
    @GET
    fun getDeliveryNoteStatus(
        @Url url: String, 
        @Query("qrUrl") qrUrl: String? = null,
        @Query("invoiceMark") invoiceMark: String? = null
    ): Call<GetDeliveryStatusResponse>

    // 5. GenerateGroupQRCode: Δυνατότητα ομαδικής σάρωσης για μαζικές αποστολές.
    @POST
    fun generateGroupQRCode(@Url url: String, @Body payload: GroupQRCodeRequest): Call<ResponseDoc>
    
    // 6. GetGroupQRDetails: Ανάκτηση λεπτομερειών ομαδικής σάρωσης.
    @GET
    fun getGroupQRDetails(@Url url: String, @Query("qrUrl") qrUrl: String): Call<RequestGroupQRDetailsResponse>

    /**
     * Older basic methods with Phase 2 updates (invoiceDeliveryStatus, qrUrl added to responses)
     */
    @POST("SendInvoices")
    fun sendInvoices(@Body payload: String): Call<ResponseDoc>

    @POST("CancelInvoice")
    fun cancelInvoice(@Query("mark") mark: Long): Call<ResponseDoc>

    @GET
    fun requestDocs(@Url url: String, @Query("mark") mark: Long): Call<RequestedInvoicesDoc>

    @GET
    fun requestTransmittedDocs(@Url url: String, @Query("mark") mark: Long): Call<RequestedInvoicesDoc>


    /**
     * PROVIDER Methods
     */
    @POST("myDATAProvider/SendInvoices")
    fun providerSendInvoices(@Body payload: String): retrofit2.Call<ResponseDoc>

    @GET("myDATAProvider/RequestTransmittedDocs")
    fun providerRequestTransmittedDocs(@Query("mark") mark: Long): retrofit2.Call<okhttp3.ResponseBody>
}
