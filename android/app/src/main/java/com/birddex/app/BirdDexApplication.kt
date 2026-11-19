package com.birddex.app

import android.app.Application
import com.birddex.app.data.UserDataCleaner
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/** Root of the Hilt dependency graph. */
@HiltAndroidApp
class BirdDexApplication : Application() {

    @Inject lateinit var userDataCleaner: UserDataCleaner

    override fun onCreate() {
        super.onCreate() // Hilt injects the fields here
        userDataCleaner.start()
    }
}
