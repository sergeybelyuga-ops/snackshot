package app.snackshot.ui.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.ZoneId

/** The device time zone, read on every use: the user may travel while the app runs. */
@Module
@InstallIn(SingletonComponent::class)
object TimeModule {
    @Provides
    fun zone(): ZoneId = ZoneId.systemDefault()
}
