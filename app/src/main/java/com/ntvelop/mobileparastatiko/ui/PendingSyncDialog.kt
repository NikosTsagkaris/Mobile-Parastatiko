package com.ntvelop.mobileparastatiko.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ntvelop.mobileparastatiko.ui.theme.DarkSurface
import com.ntvelop.mobileparastatiko.ui.theme.NeonGreen
import java.text.SimpleDateFormat
import java.util.*

data class PendingSyncItem(
    val id: String = UUID.randomUUID().toString(),
    val actionType: String, // REGISTER_TRANSFER, CONFIRM_DELIVERY, REJECT_NOTE, CANCEL_NOTE
    val docMark: Long,
    val timestamp: Long = System.currentTimeMillis()
)

@Composable
fun PendingSyncDialog(
    pendingList: List<PendingSyncItem>,
    onDismiss: () -> Unit,
    onRemoveItem: (Int) -> Unit,
    onSyncAll: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Offline Ουρά myDATA",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = NeonGreen
                        )
                        Text(
                            text = "${pendingList.size} εκκρεμείς ενέργειες προς αποστολή",
                            fontSize = 12.sp,
                            color = Color.LightGray
                        )
                    }
                    Icon(
                        Icons.Default.WifiOff,
                        contentDescription = null,
                        tint = Color(0xFFFFA000),
                        modifier = Modifier.size(26.dp)
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    thickness = 0.5.dp,
                    color = Color.DarkGray
                )

                if (pendingList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Όλες οι διαβιβάσεις έχουν ολοκληρωθεί στην ΑΑΔΕ!",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        itemsIndexed(pendingList) { index, item ->
                            PendingItemRow(
                                item = item,
                                onDelete = { onRemoveItem(index) }
                            )
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("ΚΛΕΙΣΙΜΟ", color = Color.LightGray)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = onSyncAll,
                        enabled = pendingList.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black)
                    ) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("ΣΥΓΧΡΟΝΙΣΜΟΣ", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingItemRow(
    item: PendingSyncItem,
    onDelete: () -> Unit
) {
    val dateStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(item.timestamp))
    val actionTitle = when (item.actionType) {
        "REGISTER_TRANSFER" -> "Έναρξη Διακίνησης"
        "CONFIRM_DELIVERY" -> "Επιβεβαίωση Παραλαβής"
        "REJECT_NOTE" -> "Απόρριψη Παραστατικού"
        "CANCEL_NOTE" -> "Ακύρωση Παραστατικού"
        else -> item.actionType
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = actionTitle,
                    color = NeonGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Text(
                    text = "MARK: ${if (item.docMark != 0L) item.docMark else "Pending"} • $dateStr",
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Διαγραφή",
                    tint = Color.Red.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}