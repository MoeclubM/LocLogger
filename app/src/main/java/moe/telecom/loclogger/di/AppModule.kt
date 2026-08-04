package moe.telecom.loclogger.di

import android.content.Context
import androidx.room.Room
import moe.telecom.loclogger.data.local.AppDatabase
import moe.telecom.loclogger.data.local.AnnotationDao
import moe.telecom.loclogger.data.local.TrackDao
import moe.telecom.loclogger.data.local.TrackPointDao
import moe.telecom.loclogger.ui.util.PermissionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun providePermissionManager(
        @ApplicationContext context: Context
    ): PermissionManager = PermissionManager(context)

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        AppDatabase.DATABASE_NAME
    ).fallbackToDestructiveMigration().build()

    @Provides
    fun provideTrackDao(db: AppDatabase): TrackDao = db.trackDao()

    @Provides
    fun provideTrackPointDao(db: AppDatabase): TrackPointDao = db.trackPointDao()

    @Provides
    fun provideAnnotationDao(db: AppDatabase): AnnotationDao = db.annotationDao()
}
