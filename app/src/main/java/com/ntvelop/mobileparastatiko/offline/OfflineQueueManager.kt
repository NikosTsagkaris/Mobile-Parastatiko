package com.ntvelop.mobileparastatiko.offline

import android.content.Context
import android.content.SharedPreferences
import com.ntvelop.mobileparastatiko.api.AadeBookInvoiceType
import com.ntvelop.mobileparastatiko.api.InvoicesDoc
import com.ntvelop.mobileparastatiko.api.MyDataApi
import com.ntvelop.mobileparastatiko.api.ResponseDoc
import com.ntvelop.mobileparastatiko.xml.MyDataXmlSerializer
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Offline Sync Queue Manager for Mobile-Parastatiko.
 * Implements myDATA Rule: If device is offline during dispatch, store transaction with
 * transmissionFailure = 3 (loss of connectivity) and retry upon reconnection.
 */
class OfflineQueueManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("offline_queue_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PENDING_INVOICES = "KEY_PENDING_INVOICES"
    }

    /**
     * Enqueues an invoice with transmissionFailure = 3 (Connectivity Failure)
     */
    @Synchronized
    fun enqueueInvoice(invoice: AadeBookInvoiceType) {
        val invoiceToStore = invoice.copy(transmissionFailure = 3)
        val xml = MyDataXmlSerializer.serializeInvoicesDoc(InvoicesDoc(invoices = listOf(invoiceToStore)))

        val pendingList = getPendingRawList().toMutableList()
        pendingList.add(xml)
        savePendingRawList(pendingList)
    }

    @Synchronized
    fun getPendingRawList(): List<String> {
        val jsonStr = prefs.getString(KEY_PENDING_INVOICES, "[]") ?: "[]"
        val array = JSONArray(jsonStr)
        val list = mutableListOf<String>()
        for (i in 0 until array.length()) {
            list.add(array.getString(i))
        }
        return list
    }

    @Synchronized
    private fun savePendingRawList(list: List<String>) {
        val array = JSONArray()
        list.forEach { array.put(it) }
        prefs.edit().putString(KEY_PENDING_INVOICES, array.toString()).apply()
    }

    @Synchronized
    fun clearQueue() {
        prefs.edit().remove(KEY_PENDING_INVOICES).apply()
    }

    /**
     * Attempts to synchronize queued invoices with AADE myDATA API.
     */
    fun syncQueue(api: MyDataApi, onComplete: (successCount: Int, failedCount: Int) -> Unit) {
        val pending = getPendingRawList()
        if (pending.isEmpty()) {
            onComplete(0, 0)
            return
        }

        var successCount = 0
        var failedCount = 0
        val remaining = mutableListOf<String>()
        var processed = 0

        pending.forEach { xmlPayload ->
            api.sendInvoices(xmlPayload).enqueue(object : Callback<ResponseDoc> {
                override fun onResponse(call: Call<ResponseDoc>, response: Response<ResponseDoc>) {
                    val respDoc = response.body()
                    val firstResp = respDoc?.responses?.firstOrNull()

                    if (response.isSuccessful && (firstResp?.statusCode == "Success" || firstResp?.invoiceMark != null)) {
                        successCount++
                    } else {
                        failedCount++
                        remaining.add(xmlPayload)
                    }

                    processed++
                    if (processed == pending.size) {
                        savePendingRawList(remaining)
                        onComplete(successCount, failedCount)
                    }
                }

                override fun onFailure(call: Call<ResponseDoc>, t: Throwable) {
                    failedCount++
                    remaining.add(xmlPayload)
                    processed++
                    if (processed == pending.size) {
                        savePendingRawList(remaining)
                        onComplete(successCount, failedCount)
                    }
                }
            })
        }
    }
}
