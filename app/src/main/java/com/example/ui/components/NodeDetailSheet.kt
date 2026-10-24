package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NetworkNodeEntity
import com.example.data.model.NodeStatus
import com.example.data.model.NodeType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NodeDetailSheet(
    node: NetworkNodeEntity,
    parentBoxName: String?,
    onDismiss: () -> Unit,
    onEditNode: (NetworkNodeEntity) -> Unit,
    onDeleteNode: (NetworkNodeEntity) -> Unit,
    onStatusChanged: (NetworkNodeEntity, NodeStatus) -> Unit
) {
    val context = LocalContext.current
    var isPasswordVisible by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "تم نسخ $label إلى الحافظة", Toast.LENGTH_SHORT).show()
    }

    fun openDialer(phone: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر فتح لوحة الاتصال", Toast.LENGTH_SHORT).show()
        }
    }

    fun openWhatsApp(phone: String) {
        try {
            val cleanPhone = phone.replace(Regex("[^0-9+]"), "")
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone"))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "تطبيق واتساب غير مثبت", Toast.LENGTH_SHORT).show()
        }
    }

    fun openWebAdmin(ip: String) {
        try {
            val url = if (ip.startsWith("http://") || ip.startsWith("https://")) ip else "http://$ip"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر فتح المتصفح", Toast.LENGTH_SHORT).show()
        }
    }

    fun openGpsNavigation(lat: Double, lng: Double, label: String) {
        try {
            val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(label)})")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر فتح تطبيق الملاحة", Toast.LENGTH_SHORT).show()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            // Header: Status badge, Title & Network Number
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Device Icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            when (node.status) {
                                NodeStatus.FAULT_CUT -> Color(0xFFFEE2E2)
                                NodeStatus.MAINTENANCE -> Color(0xFFFEF3C7)
                                NodeStatus.ACTIVE -> Color(0xFFD1FAE5)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (node.deviceType) {
                            NodeType.MIKROTIK_ROUTER -> Icons.Default.Hub
                            NodeType.WATERPROOF_BOX -> Icons.Default.AllInbox
                            NodeType.OUTDOOR_ANTENNA -> Icons.Default.SettingsInputAntenna
                            else -> Icons.Default.Router
                        },
                        contentDescription = null,
                        tint = when (node.status) {
                            NodeStatus.FAULT_CUT -> Color(0xFFDC2626)
                            NodeStatus.MAINTENANCE -> Color(0xFFD97706)
                            NodeStatus.ACTIVE -> Color(0xFF059669)
                        },
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (node.networkNumber.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text(
                                    text = "رقم: #${node.networkNumber}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = node.deviceType.titleAr,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = node.name.ifBlank { node.customerName },
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Status Control Chips (سليم، عطل، صيانة)
            Text(
                text = "حالة الاتصال والخدمة:",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = node.status == NodeStatus.ACTIVE,
                    onClick = { onStatusChanged(node, NodeStatus.ACTIVE) },
                    label = { Text("يعمل بشكل سليم") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFD1FAE5),
                        selectedLabelColor = Color(0xFF065F46)
                    )
                )

                FilterChip(
                    selected = node.status == NodeStatus.FAULT_CUT,
                    onClick = { onStatusChanged(node, NodeStatus.FAULT_CUT) },
                    label = { Text("عطل / سلك مقطوع") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFEE2E2),
                        selectedLabelColor = Color(0xFF991B1B)
                    )
                )

                FilterChip(
                    selected = node.status == NodeStatus.MAINTENANCE,
                    onClick = { onStatusChanged(node, NodeStatus.MAINTENANCE) },
                    label = { Text("صيانة") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFEF3C7),
                        selectedLabelColor = Color(0xFF92400E)
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(16.dp))

            // Customer Contact Section (if present)
            if (node.customerName.isNotBlank() || node.customerPhone.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "بيانات العميل والتواصل:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        if (node.customerName.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = "الاسم: ${node.customerName}", fontSize = 14.sp)
                        }

                        if (node.customerPhone.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "الهاتف: ${node.customerPhone}",
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.weight(1f)
                                )

                                FilledTonalIconButton(
                                    onClick = { openDialer(node.customerPhone) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Phone, contentDescription = "اتصال", modifier = Modifier.size(18.dp))
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                FilledTonalIconButton(
                                    onClick = { openWhatsApp(node.customerPhone) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Chat, contentDescription = "واتساب", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Technical Details Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.SettingsEthernet, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "البيانات الفنية للجهاز:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    // IP Address
                    if (node.ipAddress.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "IP الجهاز:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = node.ipAddress,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { copyToClipboard("IP", node.ipAddress) }, modifier = Modifier.size(32.dp)) {
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "نسخ IP", modifier = Modifier.size(16.dp))
                            }
                            IconButton(onClick = { openWebAdmin(node.ipAddress) }, modifier = Modifier.size(32.dp)) {
                                Icon(imageVector = Icons.Default.OpenInBrowser, contentDescription = "فتح صفحة المودم", modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    // MAC Address
                    if (node.macAddress.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "الماك MAC:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = node.macAddress,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { copyToClipboard("MAC", node.macAddress) }, modifier = Modifier.size(32.dp)) {
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "نسخ MAC", modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    // Username & Password
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "مستخدم الإعدادات:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = node.adminUsername.ifBlank { "admin" },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { copyToClipboard("اسم المستخدم", node.adminUsername) }, modifier = Modifier.size(32.dp)) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "نسخ", modifier = Modifier.size(16.dp))
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "كلمة المرور:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isPasswordVisible) node.adminPasswordDecrypted.ifBlank { "(فارغة)" } else "••••••••",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "إظهار",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(onClick = { copyToClipboard("كلمة المرور", node.adminPasswordDecrypted) }, modifier = Modifier.size(32.dp)) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "نسخ", modifier = Modifier.size(16.dp))
                        }
                    }

                    // Device Model
                    if (node.deviceModel.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "موديل الجهاز: ${node.deviceModel}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Feeder box & port
                    if (parentBoxName != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "الصندوق المغذي: $parentBoxName (منفذ: ${node.boxPortNumber})",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (node.cableLengthMeters > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "طول سلك الإيثرنت: ${node.cableLengthMeters} متر",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (node.locationNotes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "ملاحظات العنوان: ${node.locationNotes}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Navigation button
                Button(
                    onClick = {
                        openGpsNavigation(
                            node.latitude,
                            node.longitude,
                            "#${node.networkNumber} ${node.name}"
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(imageVector = Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("الملاحة GPS")
                }

                // Edit button
                OutlinedButton(
                    onClick = { onEditNode(node) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تعديل")
                }

                // Delete button
                IconButton(
                    onClick = { showDeleteConfirm = true },
                    colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف")
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("تأكيد الحذف") },
            text = { Text("هل أنت متأكد من حذف هذا الجهاز (${node.name.ifBlank { "#${node.networkNumber}" }}) من الخريطة وقاعدة البيانات؟") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteNode(node)
                        onDismiss()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("حذف نهائي")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
