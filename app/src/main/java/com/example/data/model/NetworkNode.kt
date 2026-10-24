package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.util.CryptoUtil

enum class NodeType(val titleAr: String) {
    CUSTOMER_MODEM("مودم منزلي"),
    WATERPROOF_BOX("صندوق وتر بروف (سوتش)"),
    OUTDOOR_ANTENNA("أنتينة خارجية (كامبة / برج)"),
    MIKROTIK_ROUTER("روتر مايكروتك دمج / توزيع"),
    SWITCH("سوتش داخلي / فرعي"),
    OTHER("جهاز شبكة آخر")
}

enum class NodeStatus(val labelAr: String) {
    ACTIVE("يعمل بشكل سليم"),
    FAULT_CUT("عطل / سلك مقطوع"),
    MAINTENANCE("قيد الصيانة والتجهيز")
}

@Entity(tableName = "network_nodes")
data class NetworkNodeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String = "",                  // اسم العميل أو معرف الجهاز
    val networkNumber: String = "",         // رقم الشبكة / رقم المودم (مثال: 105 أو A-22)
    val deviceType: NodeType = NodeType.CUSTOMER_MODEM,
    val ipAddress: String = "",             // عنوان IP للجهاز
    val macAddress: String = "",            // عنوان MAC للجهاز (اختياري)
    val adminUsername: String = "admin",    // اسم المستخدم للإعدادات / PPPoE
    val adminPasswordEncrypted: String = "",// كلمة المرور المشفرة عند الحفظ
    val deviceModel: String = "",           // موديل الجهاز (مثل Tenda F3, TP-Link WR840N)
    val customerName: String = "",          // اسم العميل المشترك
    val customerPhone: String = "",         // رقم هاتف العميل للتواصل المباشر
    val locationNotes: String = "",         // ملاحظات الموقع والعنوان
    val latitude: Double = 0.0,             // خط العرض
    val longitude: Double = 0.0,            // خط الطول
    val status: NodeStatus = NodeStatus.ACTIVE,
    val parentBoxId: Long? = null,          // معرف صندوق الوتر بروف المغذي
    val boxPortNumber: Int = 1,             // رقم المنفذ في السوتش (Port 1 - 16)
    val cableLengthMeters: Int = 0,         // طول سلك الإيثرنت الواصل بالمتر
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    // Decrypted password property for UI presentation
    val adminPasswordDecrypted: String
        get() = CryptoUtil.decrypt(adminPasswordEncrypted)

    // Primary display label on map marker badge
    val markerLabel: String
        get() = if (networkNumber.isNotBlank()) {
            "$networkNumber - ${name.ifBlank { customerName }}"
        } else {
            name.ifBlank { customerName.ifBlank { "جهاز شبكة" } }
        }
}

@Entity(tableName = "cable_links")
data class CableLinkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fromNodeId: Long = 0,
    val toNodeId: Long = 0,
    val cableType: String = "Ethernet Cat6", // نوع السلك (سلك شبكة، شبكة وكهرباء، رابط لاسلكي)
    val status: NodeStatus = NodeStatus.ACTIVE,
    val notes: String = ""
)
