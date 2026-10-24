package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.CableLinkEntity
import com.example.data.model.NetworkNodeEntity
import com.example.data.model.NodeStatus
import com.example.data.model.NodeType
import kotlinx.coroutines.flow.Flow

@Dao
interface NetworkDao {
    @Query("SELECT * FROM network_nodes ORDER BY updatedAt DESC")
    fun getAllNodes(): Flow<List<NetworkNodeEntity>>

    @Query("SELECT * FROM network_nodes ORDER BY updatedAt DESC")
    suspend fun getAllNodesList(): List<NetworkNodeEntity>

    @Query("SELECT * FROM network_nodes WHERE id = :id")
    suspend fun getNodeById(id: Long): NetworkNodeEntity?

    @Query("SELECT * FROM network_nodes WHERE deviceType = :type")
    fun getNodesByType(type: NodeType): Flow<List<NetworkNodeEntity>>

    @Query("SELECT * FROM network_nodes WHERE parentBoxId = :boxId")
    fun getNodesConnectedToBox(boxId: Long): Flow<List<NetworkNodeEntity>>

    @Query("""
        SELECT * FROM network_nodes 
        WHERE networkNumber LIKE '%' || :query || '%' 
           OR name LIKE '%' || :query || '%' 
           OR customerName LIKE '%' || :query || '%'
           OR customerPhone LIKE '%' || :query || '%'
           OR ipAddress LIKE '%' || :query || '%'
           OR deviceModel LIKE '%' || :query || '%'
        ORDER BY 
           CASE WHEN networkNumber = :query THEN 1 
                WHEN networkNumber LIKE :query || '%' THEN 2 
                WHEN name LIKE :query || '%' THEN 3
                ELSE 4 END, 
           updatedAt DESC
    """)
    fun searchNodes(query: String): Flow<List<NetworkNodeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNode(node: NetworkNodeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNodes(nodes: List<NetworkNodeEntity>)

    @Update
    suspend fun updateNode(node: NetworkNodeEntity)

    @Query("UPDATE network_nodes SET status = :status WHERE id = :id")
    suspend fun updateNodeStatus(id: Long, status: NodeStatus)

    @Query("UPDATE network_nodes SET status = :status WHERE parentBoxId = :boxId")
    suspend fun updateAllBoxChildrenStatus(boxId: Long, status: NodeStatus)

    @Delete
    suspend fun deleteNode(node: NetworkNodeEntity)

    @Query("DELETE FROM network_nodes WHERE id = :id")
    suspend fun deleteNodeById(id: Long)

    @Query("DELETE FROM network_nodes")
    suspend fun deleteAllNodes()

    // Cable Links
    @Query("SELECT * FROM cable_links")
    fun getAllLinks(): Flow<List<CableLinkEntity>>

    @Query("SELECT * FROM cable_links")
    suspend fun getAllLinksList(): List<CableLinkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLink(link: CableLinkEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLinks(links: List<CableLinkEntity>)

    @Delete
    suspend fun deleteLink(link: CableLinkEntity)

    @Query("DELETE FROM cable_links WHERE fromNodeId = :nodeId OR toNodeId = :nodeId")
    suspend fun deleteLinksForNode(nodeId: Long)

    @Query("DELETE FROM cable_links")
    suspend fun deleteAllLinks()
}
