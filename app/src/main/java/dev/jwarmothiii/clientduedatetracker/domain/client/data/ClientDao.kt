package dev.jwarmothiii.clientduedatetracker.domain.client.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import dev.jwarmothiii.clientduedatetracker.database.entity.ClientEntity

@Dao
interface ClientDao {
    @Insert
    suspend fun insert(client: ClientEntity): Long

    @Update
    suspend fun update(client: ClientEntity)

    @Query("SELECT * FROM clients WHERE public_id = :publicId")
    suspend fun findByPublicId(publicId: String): ClientEntity?

    @Query("SELECT id FROM clients WHERE public_id = :publicId")
    suspend fun internalId(publicId: String): Long?

    @Query("DELETE FROM clients WHERE public_id = :publicId")
    suspend fun delete(publicId: String): Int

    @Query("SELECT COUNT(*) FROM clients")
    suspend fun count(): Int
}
