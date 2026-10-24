package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.model.NetworkNodeEntity
import com.example.data.model.NodeStatus
import com.example.data.model.NodeType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string resources`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        val devName = context.getString(R.string.developer_name)

        assertEquals("خريطة شبكة الأمير", appName)
        assertEquals("المهندس عبدالرحيم الماخذي", devName)
    }

    @Test
    fun `test room database node insertion and retrieval`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dao = AppDatabase.getInstance(context).networkDao()

        val node = NetworkNodeEntity(
            networkNumber = "105",
            name = "منزل أحمد العنسي",
            customerName = "أحمد العنسي",
            customerPhone = "+967771122334",
            deviceType = NodeType.CUSTOMER_MODEM,
            latitude = 15.3712,
            longitude = 44.1929,
            status = NodeStatus.ACTIVE
        )

        val id = dao.insertNode(node)
        val retrieved = dao.getNodeById(id)

        assertNotNull(retrieved)
        assertEquals("105", retrieved?.networkNumber)
        assertEquals("منزل أحمد العنسي", retrieved?.name)
    }
}
