package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.example.data.repository.NetworkRepository
import com.example.map.TileManager
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BackupRestoreDialog(
    repository: NetworkRepository,
    tileManager: TileManager,
    userEmail: String?,
    onDismiss: () -> Unit,
    onDataRestored: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isExporting by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    var cachedSizeMB by remember { mutableDoubleStateOf(tileManager.getCachedDataSizeMB()) }

    var importJsonText by remember { mutableStateOf("") }
    var showImportTextInput by remember { mutableStateOf(false) }
    var replaceExisting by remember { mutableStateOf(false) }

    fun exportAndShareFile() {
        scope.launch {
            isExporting = true
            try {
                val jsonString = repository.exportToJson()
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val fileName = "AlAmir_Network_Backup_$timestamp.json"

                val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
                val exportFile = File(exportDir, fileName)
                exportFile.writeText(jsonString)

                val fileUri: Uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    exportFile
                )

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, fileUri)
                    putExtra(Intent.EXTRA_SUBJECT, "نسخة احتياطية - خريطة شبكة الأمير")
                    putExtra(Intent.EXTRA_TEXT, "ملف النسخة الاحتياطية لشبكة الأمير - تاريخ: $timestamp")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                context.startActivity(Intent.createChooser(shareIntent, "حفظ النسخة الاحتياطية في Google Drive أو مشاركتها"))
            } catch (e: Exception) {
                Toast.makeText(context, "فشل التصدير: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            } finally {
                isExporting = false
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "النسخ الاحتياطي وحماية البيانات",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Google Account Info & Privacy Box
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "الخصوصية والأمان المتقدم:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "حساب المالك: ${userEmail ?: "مستخدم محلي مسجل"}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "بيانات شبكتك ومودمات العملاء وكلمات المرور مشفرة ومحفوظة بأمان محلياً. لا يمكن لأي حساب آخر رؤيتها أو التعديل عليها إلا عند تصديرك ملف نسخة احتياطية ومشاركته بنفسك.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Export Section
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "1. تصدير نسخة احتياطية كاملة (JSON)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "تصدير جميع النقاط، الكابلات، بيانات المشتركين والإحداثيات لملف واحد لحفظه في Google Drive أو إرساله لهاتف آخر.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { exportAndShareFile() },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isExporting
                            ) {
                                if (isExporting) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("تصدير ومشاركة النسخة الاحتياطية")
                                }
                            }
                        }
                    }

                    // Import Section
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "2. استعادة نسخة احتياطية من ملف",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "استرجاع شبكتك عند تغيير الهاتف أو بعد التهيئة بدون أي فقدان للبيانات.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedButton(
                                onClick = { showImportTextInput = !showImportTextInput },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(imageVector = Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("لصق نص النسخة الاحتياطية (JSON) واستعادتها")
                            }

                            if (showImportTextInput) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = importJsonText,
                                    onValueChange = { importJsonText = it },
                                    label = { Text("الصق محتوى ملف JSON هنا") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp),
                                    maxLines = 6
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = replaceExisting,
                                        onCheckedChange = { replaceExisting = it }
                                    )
                                    Text(
                                        text = "استبدال البيانات الحالية بالكامل",
                                        fontSize = 12.sp
                                    )
                                }

                                Button(
                                    onClick = {
                                        if (importJsonText.isBlank()) {
                                            Toast.makeText(context, "يرجى لصق نص النسخة أولاً", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        scope.launch {
                                            isImporting = true
                                            val res = repository.importFromJson(importJsonText, replaceExisting)
                                            isImporting = false
                                            if (res.isSuccess) {
                                                Toast.makeText(context, "تمت استعادة ${res.getOrNull()} جهاز بنجاح!", Toast.LENGTH_LONG).show()
                                                showImportTextInput = false
                                                importJsonText = ""
                                                onDataRestored()
                                                onDismiss()
                                            } else {
                                                Toast.makeText(context, "خطأ في الاستيراد: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isImporting
                                ) {
                                    if (isImporting) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    } else {
                                        Text("تطبيق واستعادة البيانات")
                                    }
                                }
                            }
                        }
                    }

                    // Offline Map Tile Cache Info
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Map, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "ذاكرة الخريطة دون إنترنت (Offline Tiles):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = String.format(java.util.Locale.US, "المساحة المستهلكة من صور الأقمار الصناعية المخزنة: %.1f ميجابايت", cachedSizeMB),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    tileManager.clearCache()
                                    cachedSizeMB = tileManager.getCachedDataSizeMB()
                                    Toast.makeText(context, "تم مسح الذاكرة المؤقتة للخرائط", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("تفريغ ذاكرة الخرائط المؤقتة")
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("إغلاق")
                }
            }
        }
    }
}
