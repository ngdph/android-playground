package co.iostream.apps.android.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import co.iostream.apps.android.data.daos.FileDao
import co.iostream.apps.android.data.entities.FileEntity

@Database(version = 4, entities = [FileEntity::class], exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fileDao(): FileDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        val migration = Migration(3, 4) {
            // Your migration strategy here
            it.execSQL("ALTER TABLE code_items ADD COLUMN label INTEGER")
        }

        fun getInst(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                Room.databaseBuilder(context, AppDatabase::class.java, context.packageName)
                    .fallbackToDestructiveMigration()
                    .build().also {
                        instance = it
                    }
            }
        }
    }
}