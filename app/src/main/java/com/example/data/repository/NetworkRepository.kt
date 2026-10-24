package com.example.data.repository

import android.content.Context
import com.example.data.db.AppDatabase
import com.example.data.db.NetworkDao
import com.example.data.model.CableLinkEntity
import com.example.data.model.NetworkNodeEntity
import com.example.data.model.NodeStatus
import com.example.data.model.NodeType
import com.example.util.CryptoUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class NetworkRepository(context: Context) {
    private val database = AppDatabase.getInstance(context)
    private val dao: NetworkDao = database.networkDao()

    val allNodes: Flow<List<NetworkNodeEntity>> = dao.getAllNodes()
    val allLinks: Flow<List<CableLinkEntity>> = dao.getAllLinks()

    fun searchNodes(query: String): Flow<List<NetworkNodeEntity>> = dao.searchNodes(query)

    fun getNodesConnectedToBox(boxId: Long): Flow<List<NetworkNodeEntity>> =
        dao.getNodesConnectedToBox(boxId)

    suspend fun getNodeById(id: Long): NetworkNodeEntity? = withContext(Dispatchers.IO) {
        dao.getNodeById(id)
    }

    suspend fun saveNode(node: NetworkNodeEntity): Long = withContext(Dispatchers.IO) {
        if (node.id == 0L) {
            val newId = dao.insertNode(node)
            // If parentBoxId is specified, automatically create a cable link between parent and child
            if (node.parentBoxId != null && node.parentBoxId > 0) {
                dao.insertLink(
                    CableLinkEntity(
                        fromNodeId = node.parentBoxId,
                        toNodeId = newId,
                        cableType = if (node.deviceType == NodeType.CUSTOMER_MODEM) "Ethernet Drop" else "Trunk & Power",
                        status = node.status
                    )
                )
            }
            newId
        } else {
            dao.updateNode(node)
            node.id
        }
    }

    suspend fun updateNodeStatus(id: Long, status: NodeStatus) = withContext(Dispatchers.IO) {
        dao.updateNodeStatus(id, status)
    }

    suspend fun updateBoxChildrenStatus(boxId: Long, status: NodeStatus) = withContext(Dispatchers.IO) {
        dao.updateAllBoxChildrenStatus(boxId, status)
    }

    suspend fun deleteNode(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteLinksForNode(id)
        dao.deleteNodeById(id)
    }

    suspend fun addCableLink(fromId: Long, toId: Long, cableType: String) = withContext(Dispatchers.IO) {
        dao.insertLink(CableLinkEntity(fromNodeId = fromId, toNodeId = toId, cableType = cableType))
    }

    suspend fun deleteCableLink(link: CableLinkEntity) = withContext(Dispatchers.IO) {
        dao.deleteLink(link)
    }

    // Export entire network database to portable JSON
    suspend fun exportToJson(): String = withContext(Dispatchers.IO) {
        val nodes = dao.getAllNodesList()
        val links = dao.getAllLinksList()

        val root = JSONObject()
        root.put("app", "خريطة شبكة الأمير")
        root.put("version", "1.0.0")
        root.put("exportedAt", System.currentTimeMillis())
        root.put("totalNodes", nodes.size)

        val nodesArray = JSONArray()
        for (node in nodes) {
            val nodeObj = JSONObject()
            nodeObj.put("id", node.id)
            nodeObj.put("name", node.name)
            nodeObj.put("networkNumber", node.networkNumber)
            nodeObj.put("deviceType", node.deviceType.name)
            nodeObj.put("ipAddress", node.ipAddress)
            nodeObj.put("macAddress", node.macAddress)
            nodeObj.put("adminUsername", node.adminUsername)
            nodeObj.put("adminPasswordEncrypted", node.adminPasswordEncrypted)
            nodeObj.put("deviceModel", node.deviceModel)
            nodeObj.put("customerName", node.customerName)
            nodeObj.put("customerPhone", node.customerPhone)
            nodeObj.put("locationNotes", node.locationNotes)
            nodeObj.put("latitude", node.latitude)
            nodeObj.put("longitude", node.longitude)
            nodeObj.put("status", node.status.name)
            nodeObj.put("parentBoxId", node.parentBoxId ?: -1L)
            nodeObj.put("boxPortNumber", node.boxPortNumber)
            nodeObj.put("cableLengthMeters", node.cableLengthMeters)
            nodesArray.put(nodeObj)
        }
        root.put("nodes", nodesArray)

        val linksArray = JSONArray()
        for (link in links) {
            val linkObj = JSONObject()
            linkObj.put("id", link.id)
            linkObj.put("fromNodeId", link.fromNodeId)
            linkObj.put("toNodeId", link.toNodeId)
            linkObj.put("cableType", link.cableType)
            linkObj.put("status", link.status.name)
            linkObj.put("notes", link.notes)
            linksArray.put(linkObj)
        }
        root.put("links", linksArray)

        root.toString(2)
    }

    // Import from JSON string, with duplicate handling and preservation of encrypted fields
    suspend fun importFromJson(jsonString: String, replaceExisting: Boolean): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            val nodesArray = root.optJSONArray("nodes") ?: return@withContext Result.failure(Exception("ملف غير صالح: لا توجد بيانات أجهزة"))
            val linksArray = root.optJSONArray("links")

            if (replaceExisting) {
                dao.deleteAllLinks()
                dao.deleteAllNodes()
            }

            val importedNodes = mutableListOf<NetworkNodeEntity>()
            for (i in 0 until nodesArray.length()) {
                val obj = nodesArray.getJSONObject(i)
                val typeName = obj.optString("deviceType", NodeType.CUSTOMER_MODEM.name)
                val deviceType = try { NodeType.valueOf(typeName) } catch (e: Exception) { NodeType.CUSTOMER_MODEM }
                val statusName = obj.optString("status", NodeStatus.ACTIVE.name)
                val status = try { NodeStatus.valueOf(statusName) } catch (e: Exception) { NodeStatus.ACTIVE }
                val parentId = obj.optLong("parentBoxId", -1L).takeIf { it > 0 }

                val node = NetworkNodeEntity(
                    id = if (replaceExisting) obj.optLong("id", 0L) else 0L,
                    name = obj.optString("name", ""),
                    networkNumber = obj.optString("networkNumber", ""),
                    deviceType = deviceType,
                    ipAddress = obj.optString("ipAddress", ""),
                    macAddress = obj.optString("macAddress", ""),
                    adminUsername = obj.optString("adminUsername", "admin"),
                    adminPasswordEncrypted = obj.optString("adminPasswordEncrypted", ""),
                    deviceModel = obj.optString("deviceModel", ""),
                    customerName = obj.optString("customerName", ""),
                    customerPhone = obj.optString("customerPhone", ""),
                    locationNotes = obj.optString("locationNotes", ""),
                    latitude = obj.optDouble("latitude", 0.0),
                    longitude = obj.optDouble("longitude", 0.0),
                    status = status,
                    parentBoxId = parentId,
                    boxPortNumber = obj.optInt("boxPortNumber", 1),
                    cableLengthMeters = obj.optInt("cableLengthMeters", 0)
                )
                importedNodes.add(node)
            }

            dao.insertNodes(importedNodes)

            if (linksArray != null) {
                val importedLinks = mutableListOf<CableLinkEntity>()
                for (i in 0 until linksArray.length()) {
                    val obj = linksArray.getJSONObject(i)
                    val statusName = obj.optString("status", NodeStatus.ACTIVE.name)
                    val status = try { NodeStatus.valueOf(statusName) } catch (e: Exception) { NodeStatus.ACTIVE }

                    val link = CableLinkEntity(
                        id = if (replaceExisting) obj.optLong("id", 0L) else 0L,
                        fromNodeId = obj.optLong("fromNodeId", 0L),
                        toNodeId = obj.optLong("toNodeId", 0L),
                        cableType = obj.optString("cableType", "Ethernet Cat6"),
                        status = status,
                        notes = obj.optString("notes", "")
                    )
                    importedLinks.add(link)
                }
                dao.insertLinks(importedLinks)
            }

            Result.success(importedNodes.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Prepopulate realistic initial sample network data for Eng. Abdulrahim
    suspend fun prepopulateSampleDataIfNeeded() = withContext(Dispatchers.IO) {
        val existing = dao.getAllNodesList()
        if (existing.isNotEmpty()) return@withContext

        // Base location coordinates (Middle Eastern city center - Sana'a / vibrant neighborhood grid)
        val baseLat = 15.3694
        val baseLng = 44.1910

        // 1. Mikrotik Distribution Core
        val mikrotikNode = NetworkNodeEntity(
            name = "مركز المايكروتك الرئيسي (دمج وتوزيع)",
            networkNumber = "CORE-01",
            deviceType = NodeType.MIKROTIK_ROUTER,
            ipAddress = "192.168.88.1",
            macAddress = "D4:CA:6D:E1:2A:01",
            adminUsername = "admin",
            adminPasswordEncrypted = CryptoUtil.encrypt("AlAmir@Mikrotik2026"),
            deviceModel = "MikroTik CCR1036-8G-2S+",
            customerName = "مقر الشبكة الرئيسي",
            customerPhone = "+967771234567",
            locationNotes = "غرفة السيرفرات - برج البث الرئيسي",
            latitude = baseLat,
            longitude = baseLng,
            status = NodeStatus.ACTIVE
        )
        val coreId = dao.insertNode(mikrotikNode)

        // 2. Waterproof Box 1 (Street Corner Pole)
        val box1 = NetworkNodeEntity(
            name = "صندوق وتر بروف #1 - الشارع العام",
            networkNumber = "BOX-01",
            deviceType = NodeType.WATERPROOF_BOX,
            ipAddress = "192.168.88.11",
            macAddress = "E0:D5:5E:44:11:02",
            adminUsername = "admin",
            adminPasswordEncrypted = CryptoUtil.encrypt("admin123"),
            deviceModel = "Switch D-Link 16 Ports Metal",
            customerName = "كامبة رقم 1 - جوار جامع النور",
            customerPhone = "+967770000001",
            locationNotes = "كامبة حديد بارتفاع 6 أمتار بالشارع الرئيسي",
            latitude = baseLat + 0.0012,
            longitude = baseLng + 0.0015,
            status = NodeStatus.ACTIVE,
            parentBoxId = coreId,
            boxPortNumber = 1
        )
        val box1Id = dao.insertNode(box1)

        // 3. Waterproof Box 2 (Second Pole with outdoor antennas)
        val box2 = NetworkNodeEntity(
            name = "صندوق وتر بروف #2 - الكامبة الثانية",
            networkNumber = "BOX-02",
            deviceType = NodeType.WATERPROOF_BOX,
            ipAddress = "192.168.88.12",
            macAddress = "E0:D5:5E:44:11:03",
            adminUsername = "admin",
            adminPasswordEncrypted = CryptoUtil.encrypt("admin123"),
            deviceModel = "Switch TP-Link PoE 8 Ports",
            customerName = "كامبة رقم 2 - تقاطع الحارة",
            customerPhone = "+967770000002",
            locationNotes = "عمود إسمنتي مع وتر بروف مضاد للأمطار",
            latitude = baseLat + 0.0025,
            longitude = baseLng + 0.0028,
            status = NodeStatus.ACTIVE,
            parentBoxId = box1Id,
            boxPortNumber = 8
        )
        val box2Id = dao.insertNode(box2)

        // 4. Outdoor Antenna on Box 2
        val antenna = NetworkNodeEntity(
            name = "أنتينة سيكتور أوت دور (بث لاسلكي)",
            networkNumber = "ANT-5G",
            deviceType = NodeType.OUTDOOR_ANTENNA,
            ipAddress = "192.168.88.20",
            macAddress = "00:27:22:9C:31:AA",
            adminUsername = "ubnt",
            adminPasswordEncrypted = CryptoUtil.encrypt("ubnt@2026"),
            deviceModel = "Ubiquiti Rocket M5 AC",
            customerName = "برج التوزيع اللاسلكي",
            customerPhone = "+967771234567",
            locationNotes = "موجهة نحو الحي الشرقي",
            latitude = baseLat + 0.0026,
            longitude = baseLng + 0.0029,
            status = NodeStatus.ACTIVE,
            parentBoxId = box2Id,
            boxPortNumber = 1
        )
        val antId = dao.insertNode(antenna)

        // 5. Customer Modems
        // Customer 105 (exact scenario mentioned in prompt!)
        val cust105 = NetworkNodeEntity(
            name = "منزل الأخ أحمد صالح العنسي",
            networkNumber = "105",
            deviceType = NodeType.CUSTOMER_MODEM,
            ipAddress = "192.168.1.105",
            macAddress = "C8:3A:35:12:34:05",
            adminUsername = "user_105",
            adminPasswordEncrypted = CryptoUtil.encrypt("AhmdPass@99"),
            deviceModel = "TP-Link TL-WR840N",
            customerName = "أحمد صالح العنسي",
            customerPhone = "+967771122334",
            locationNotes = "عمارة الصلاحي - الدور الثالث - شقة 5",
            latitude = baseLat + 0.0018,
            longitude = baseLng + 0.0019,
            status = NodeStatus.ACTIVE,
            parentBoxId = box1Id,
            boxPortNumber = 3,
            cableLengthMeters = 45
        )
        val c105Id = dao.insertNode(cust105)

        val cust101 = NetworkNodeEntity(
            name = "منزل الأخ محمد علي الحيمي",
            networkNumber = "101",
            deviceType = NodeType.CUSTOMER_MODEM,
            ipAddress = "192.168.1.101",
            macAddress = "C8:3A:35:12:34:01",
            adminUsername = "user_101",
            adminPasswordEncrypted = CryptoUtil.encrypt("Mohmd@881"),
            deviceModel = "Tenda F3 Wireless N300",
            customerName = "محمد علي الحيمي",
            customerPhone = "+967775566778",
            locationNotes = "منزل شعبي دورين - جوار الصيدلية",
            latitude = baseLat + 0.0009,
            longitude = baseLng + 0.0022,
            status = NodeStatus.ACTIVE,
            parentBoxId = box1Id,
            boxPortNumber = 2,
            cableLengthMeters = 30
        )
        val c101Id = dao.insertNode(cust101)

        val cust102 = NetworkNodeEntity(
            name = "بقالة ومغسلة البركة",
            networkNumber = "102",
            deviceType = NodeType.CUSTOMER_MODEM,
            ipAddress = "192.168.1.102",
            macAddress = "B0:BE:76:88:99:02",
            adminUsername = "user_102",
            adminPasswordEncrypted = CryptoUtil.encrypt("Baraka@2026"),
            deviceModel = "Mercusys MW301R",
            customerName = "فؤاد قاسم الشميري",
            customerPhone = "+967773344556",
            locationNotes = "المحل التجاري على الشارع الفرعي",
            latitude = baseLat + 0.0029,
            longitude = baseLng + 0.0033,
            status = NodeStatus.FAULT_CUT, // Example fault (cut cable / outage)
            parentBoxId = box2Id,
            boxPortNumber = 4,
            cableLengthMeters = 65
        )
        val c102Id = dao.insertNode(cust102)

        // Connect Cable Links
        dao.insertLinks(
            listOf(
                CableLinkEntity(fromNodeId = coreId, toNodeId = box1Id, cableType = "Ethernet Cat6 & Power (رئيسي)"),
                CableLinkEntity(fromNodeId = box1Id, toNodeId = box2Id, cableType = "Ethernet Cat6 & Power (كامبة لكامبة)"),
                CableLinkEntity(fromNodeId = box2Id, toNodeId = antId, cableType = "PoE Cat6"),
                CableLinkEntity(fromNodeId = box1Id, toNodeId = c105Id, cableType = "Ethernet Cat6 (سلك منزل)"),
                CableLinkEntity(fromNodeId = box1Id, toNodeId = c101Id, cableType = "Ethernet Cat6 (سلك منزل)"),
                CableLinkEntity(fromNodeId = box2Id, toNodeId = c102Id, cableType = "Ethernet Cat6 (سلك منزل)", status = NodeStatus.FAULT_CUT)
            )
        )
    }
}
