package com.example.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.data.model.NetworkNodeEntity
import com.example.data.model.NodeStatus
import com.example.data.model.NodeType
import com.example.util.CryptoUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditNodeDialog(
    initialNode: NetworkNodeEntity?,
    initialLat: Double,
    initialLng: Double,
    availableBoxes: List<NetworkNodeEntity>,
    onDismiss: () -> Unit,
    onSave: (NetworkNodeEntity) -> Unit
) {
    val context = LocalContext.current

    var deviceType by remember { mutableStateOf(initialNode?.deviceType ?: NodeType.CUSTOMER_MODEM) }
    var networkNumber by remember { mutableStateOf(initialNode?.networkNumber ?: "") }
    var name by remember { mutableStateOf(initialNode?.name ?: "") }
    var customerName by remember { mutableStateOf(initialNode?.customerName ?: "") }
    var customerPhone by remember { mutableStateOf(initialNode?.customerPhone ?: "") }
    var ipAddress by remember { mutableStateOf(initialNode?.ipAddress ?: "") }
    var macAddress by remember { mutableStateOf(initialNode?.macAddress ?: "") }
    var adminUsername by remember { mutableStateOf(initialNode?.adminUsername ?: "admin") }
    var adminPassword by remember { mutableStateOf(initialNode?.adminPasswordDecrypted ?: "") }
    var deviceModel by remember { mutableStateOf(initialNode?.deviceModel ?: "") }
    var locationNotes by remember { mutableStateOf(initialNode?.locationNotes ?: "") }
    var selectedParentBoxId by remember { mutableStateOf<Long?>(initialNode?.parentBoxId) }
    var boxPortNumber by remember { mutableIntStateOf(initialNode?.boxPortNumber ?: 1) }
    var cableLength by remember { mutableStateOf(initialNode?.cableLengthMeters?.toString() ?: "30") }
    var status by remember { mutableStateOf(initialNode?.status ?: NodeStatus.ACTIVE) }

    var latitude by remember { mutableDoubleStateOf(initialNode?.latitude ?: initialLat) }
    var longitude by remember { mutableDoubleStateOf(initialNode?.longitude ?: initialLng) }

    var isTypeMenuExpanded by remember { mutableStateOf(false) }
    var isBoxMenuExpanded by remember { mutableStateOf(false) }

    @SuppressLint("MissingPermission")
    fun fetchCurrentGpsLocation() {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        val loc = if (isGpsEnabled) {
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        } else if (isNetworkEnabled) {
            locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        } else null

        if (loc != null) {
            latitude = loc.latitude
            longitude = loc.longitude
            Toast.makeText(context, "تم تحديد موقعك الحالي عبر GPS", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "يرجى تشغيل خدمة الموقع GPS", Toast.LENGTH_SHORT).show()
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            fetchCurrentGpsLocation()
        } else {
            Toast.makeText(context, "تم رفض إذن تحديد الموقع", Toast.LENGTH_SHORT).show()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Dialog Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (initialNode == null) Icons.Default.AddLocationAlt else Icons.Default.EditLocation,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (initialNode == null) "إضافة نقطة / جهاز على الخريطة" else "تعديل بيانات الجهاز",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "إلغاء")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Scrollable Form Fields
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Device Type Selector
                    ExposedDropdownMenuBox(
                        expanded = isTypeMenuExpanded,
                        onExpandedChange = { isTypeMenuExpanded = !isTypeMenuExpanded }
                    ) {
                        OutlinedTextField(
                            value = deviceType.titleAr,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("نوع الجهاز / النقطة *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isTypeMenuExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = isTypeMenuExpanded,
                            onDismissRequest = { isTypeMenuExpanded = false }
                        ) {
                            NodeType.values().forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.titleAr) },
                                    onClick = {
                                        deviceType = type
                                        isTypeMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Network Number
                    OutlinedTextField(
                        value = networkNumber,
                        onValueChange = { networkNumber = it },
                        label = { Text("رقم الشبكة / رقم المودم (مثال: 105) *") },
                        placeholder = { Text("105") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null) }
                    )

                    // Customer Name or Device Name
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("اسم الجهاز أو اسم العميل *") },
                        placeholder = { Text("منزل أحمد علي أو صندوق وتر بروف #1") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
                    )

                    // Customer Phone (for modems)
                    if (deviceType == NodeType.CUSTOMER_MODEM) {
                        OutlinedTextField(
                            value = customerPhone,
                            onValueChange = { customerPhone = it },
                            label = { Text("رقم هاتف العميل للتواصل") },
                            placeholder = { Text("771234567") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) }
                        )
                    }

                    // Feeder Waterproof Box Dropdown
                    if (availableBoxes.isNotEmpty() && deviceType != NodeType.MIKROTIK_ROUTER) {
                        val currentBox = availableBoxes.find { it.id == selectedParentBoxId }
                        ExposedDropdownMenuBox(
                            expanded = isBoxMenuExpanded,
                            onExpandedChange = { isBoxMenuExpanded = !isBoxMenuExpanded }
                        ) {
                            OutlinedTextField(
                                value = currentBox?.let { "#${it.networkNumber} ${it.name}" } ?: "غير متصل بصندوق (مباشر)",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("صندوق الوتر بروف المغذي") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isBoxMenuExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = isBoxMenuExpanded,
                                onDismissRequest = { isBoxMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("غير محدد (مباشر)") },
                                    onClick = {
                                        selectedParentBoxId = null
                                        isBoxMenuExpanded = false
                                    }
                                )
                                availableBoxes.forEach { box ->
                                    DropdownMenuItem(
                                        text = { Text("#${box.networkNumber} - ${box.name}") },
                                        onClick = {
                                            selectedParentBoxId = box.id
                                            isBoxMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Switch Port Number
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "رقم المنفذ (Port):", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Slider(
                                value = boxPortNumber.toFloat(),
                                onValueChange = { boxPortNumber = it.toInt() },
                                valueRange = 1f..16f,
                                steps = 14,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "منفذ $boxPortNumber", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }

                    // IP Address
                    OutlinedTextField(
                        value = ipAddress,
                        onValueChange = { ipAddress = it },
                        label = { Text("عنوان IP للجهاز (مثال: 192.168.1.105)") },
                        placeholder = { Text("192.168.1.105") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Dns, contentDescription = null) }
                    )

                    // MAC Address
                    OutlinedTextField(
                        value = macAddress,
                        onValueChange = { macAddress = it },
                        label = { Text("عنوان MAC (اختياري)") },
                        placeholder = { Text("C8:3A:35:12:34:05") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Username & Password
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = adminUsername,
                            onValueChange = { adminUsername = it },
                            label = { Text("اسم المستخدم") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = adminPassword,
                            onValueChange = { adminPassword = it },
                            label = { Text("كلمة المرور") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    // Device Model & Cable Length
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = deviceModel,
                            onValueChange = { deviceModel = it },
                            label = { Text("موديل الجهاز (Tenda / TP-Link)") },
                            modifier = Modifier.weight(1.3f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = cableLength,
                            onValueChange = { cableLength = it },
                            label = { Text("طول السلك (متر)") },
                            modifier = Modifier.weight(0.7f),
                            singleLine = true
                        )
                    }

                    // Notes
                    OutlinedTextField(
                        value = locationNotes,
                        onValueChange = { locationNotes = it },
                        label = { Text("ملاحظات الموقع والعنوان") },
                        placeholder = { Text("عمارة فلان - الدور الثاني - شقة 4") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )

                    // GPS Coordinates display & Capture button
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "الإحداثيات الجغرافية:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = String.format(java.util.Locale.US, "%.5f, %.5f", latitude, longitude),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            FilledTonalButton(
                                onClick = {
                                    val fineGranted = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                    ) == PackageManager.PERMISSION_GRANTED

                                    if (fineGranted) {
                                        fetchCurrentGpsLocation()
                                    } else {
                                        locationPermissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    }
                                }
                            ) {
                                Icon(imageVector = Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("موقعي الآن", fontSize = 12.sp)
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Save & Cancel Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (name.isBlank() && networkNumber.isBlank()) {
                                Toast.makeText(context, "يرجى إدخال اسم أو رقم للجهاز", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            val nodeToSave = NetworkNodeEntity(
                                id = initialNode?.id ?: 0L,
                                name = name.trim(),
                                networkNumber = networkNumber.trim(),
                                deviceType = deviceType,
                                ipAddress = ipAddress.trim(),
                                macAddress = macAddress.trim(),
                                adminUsername = adminUsername.trim(),
                                adminPasswordEncrypted = CryptoUtil.encrypt(adminPassword.trim()),
                                deviceModel = deviceModel.trim(),
                                customerName = if (customerName.isNotBlank()) customerName.trim() else name.trim(),
                                customerPhone = customerPhone.trim(),
                                locationNotes = locationNotes.trim(),
                                latitude = latitude,
                                longitude = longitude,
                                status = status,
                                parentBoxId = selectedParentBoxId,
                                boxPortNumber = boxPortNumber,
                                cableLengthMeters = cableLength.toIntOrNull() ?: 0,
                                updatedAt = System.currentTimeMillis()
                            )
                            onSave(nodeToSave)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("حفظ على الخريطة")
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(0.5f)
                    ) {
                        Text("إلغاء")
                    }
                }
            }
        }
    }
}
