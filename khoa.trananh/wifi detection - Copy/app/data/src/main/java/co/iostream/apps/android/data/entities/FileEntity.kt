package co.iostream.apps.android.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import co.iostream.apps.android.core.iofile.FileUtils

@Entity(tableName = "files")
data class FileEntity(
    @ColumnInfo(name = "path") var path: String,
) {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0

    @ColumnInfo(name = "fileType")
    var fileType: Int = FileUtils.Type.Unknown.ordinal
}