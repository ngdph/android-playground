package co.iostream.apps.android.data.repositories

import co.iostream.apps.android.data.daos.FileDao
import co.iostream.apps.android.data.entities.FileEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileRepository @Inject constructor(private val fileDao: FileDao) {
    suspend fun getAll(): List<FileEntity> {
        return fileDao.getAll()
    }

    suspend fun getByPath(path: String): List<FileEntity> {
        return fileDao.getByPath(path)
    }

    suspend fun insert(item: FileEntity): Long {
        return fileDao.insert(item)
    }

    suspend fun update(item: FileEntity): Int {
        return fileDao.update(item)
    }

    suspend fun deleteByPath(path: String): Int {
        return fileDao.deleteByPath(path)
    }

    suspend fun deleteAll(): Int {
        return fileDao.deleteAll()
    }
}