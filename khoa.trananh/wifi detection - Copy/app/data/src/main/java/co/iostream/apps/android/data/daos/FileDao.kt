package co.iostream.apps.android.data.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import co.iostream.apps.android.data.entities.FileEntity

@Dao
interface FileDao {
    @Query("SELECT * FROM files")
    suspend fun getAll(): List<FileEntity>

    @Query("SELECT * FROM files where path = :path")
    suspend fun getByPath(path: String): List<FileEntity>

    @Insert
    suspend fun insert(item: FileEntity): Long

    @Update
    suspend fun update(item: FileEntity): Int

    @Query("DELETE FROM files WHERE path = :path")
    suspend fun deleteByPath(path: String): Int

    @Query("DELETE FROM files")
    suspend fun deleteAll(): Int
}