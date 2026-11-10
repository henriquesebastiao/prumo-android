package com.henriquesebastiao.prumo.core.database

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert

/**
 * The app's local database (`prumo.db`). For now it keeps the server's last answer to each request a
 * screen made (D21), so the screens open with data and work without the server.
 */
@Database(entities = [ResponseEntity::class], version = 1)
abstract class PrumoDatabase : RoomDatabase() {
    abstract fun responses(): ResponseDao

    companion object {
        const val NAME = "prumo.db"
    }
}

/**
 * The body of a `GET` as the server sent it (JSON, derived values included), for the account that
 * asked ([owner]: the server's address and the username), so another account never sees it.
 */
@Entity(tableName = "responses", primaryKeys = ["owner", "path"])
data class ResponseEntity(
    val owner: String,
    /** The path and query under the server's address: `api/v1/garage/vehicles?archived=false`. */
    val path: String,
    val body: String,
    /** When the request that brought [body] was sent, in epoch milliseconds. */
    @ColumnInfo(name = "fetched_at") val fetchedAt: Long,
    /** A write happened since (here or by a pull to refresh): ask the server first. */
    val stale: Boolean,
)

/** Blocking on purpose: it runs on OkHttp's threads, inside the interceptor. */
@Dao
interface ResponseDao {
    @Query("SELECT * FROM responses WHERE owner = :owner AND path = :path")
    fun get(owner: String, path: String): ResponseEntity?

    @Upsert
    fun put(response: ResponseEntity)

    @Query("DELETE FROM responses WHERE owner = :owner AND path = :path")
    fun remove(owner: String, path: String)

    @Query("UPDATE responses SET stale = 1 WHERE owner = :owner")
    fun expire(owner: String)

    /** Forgets the answers of every other account (another user or server signed in). */
    @Query("DELETE FROM responses WHERE owner != :owner")
    fun keepOnly(owner: String)

    @Query("DELETE FROM responses")
    fun clear()
}
