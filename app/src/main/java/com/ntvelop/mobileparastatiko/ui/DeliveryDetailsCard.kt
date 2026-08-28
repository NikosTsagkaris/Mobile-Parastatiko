package com.ntvelop.mobileparastatiko.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ntvelop.mobileparastatiko.api.DeliveryStatus
import com.ntvelop.mobileparastatiko.ui.theme.DarkSurface
import com.ntvelop.mobileparastatiko.ui.theme.NeonGreen

@Composable
fun DeliveryDetailsCard(
    mark: Long,
    status: DeliveryStatus,
    invoiceType: String = "9.3 (Δελτίο Αποστολής)",
    movePurpose: String? = null,
    issueDate: String = "-",
    vehiclePlate: String = "Άνευ Οχήματος",
    issuerVat: String = "-",
    recipientVat: String = "-",
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: MARK & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Επίσημο MARK myDATA", fontSize = 11.sp, color = Color.Gray)
                    Text(
                        text = if (mark != 0L) mark.toString() else "Αναμονή καταχώρησης...",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                StatusBadge(status = status)
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                thickness = 0.5.dp,
                color = Color.DarkGray
            )

            // Info Grid
            DetailRow(icon = Icons.Default.Info, label = "Τύπος Παραστατικού", value = invoiceType)
            if (!movePurpose.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                DetailRow(icon = Icons.Default.ShoppingCart, label = "Σκοπός Διακίνησης", value = movePurpose)
            }
            Spacer(Modifier.height(8.dp))
            DetailRow(icon = Icons.Default.DateRange, label = "Ημερομηνία Έκδοσης", value = issueDate)
            Spacer(Modifier.height(8.dp))
            DetailRow(icon = Icons.Default.LocationOn, label = "Όχημα / Πινακίδα", value = vehiclePlate)
            Spacer(Modifier.height(8.dp))
            DetailRow(icon = Icons.Default.Person, label = "ΑΦΜ Εκδότη (Αποστολέας)", value = issuerVat)
            Spacer(Modifier.height(8.dp))
            DetailRow(icon = Icons.Default.Person, label = "ΑΦΜ Λήπτη (Παραλήπτης)", value = recipientVat)
        }
    }
}

@Composable
private fun StatusBadge(status: DeliveryStatus) {
    val (badgeColor, textColor) = when (status) {
        DeliveryStatus.Registered -> Color(0xFF1976D2) to Color.White
        DeliveryStatus.InTransit -> Color(0xFFF57C00) to Color.White
        DeliveryStatus.DeliveredByCarrier -> Color(0xFF7B1FA2) to Color.White
        DeliveryStatus.Completed -> NeonGreen to Color.Black
        DeliveryStatus.Rejected, DeliveryStatus.Cancelled, DeliveryStatus.FailedDelivery -> Color(0xFFD32F2F) to Color.White
        else -> Color.Gray to Color.White
    }

    Surface(
        color = badgeColor,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.padding(4.dp)
    ) {
        Text(
            text = status.text,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, fontSize = 12.sp, color = Color.LightGray, modifier = Modifier.weight(1f))
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}