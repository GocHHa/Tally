package com.tally.counter

import android.app.Application
import android.content.Context
import com.tally.counter.work.WorkScheduler

class TallyApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        WorkScheduler.schedule(this)
    }
}

/** Manual dependency injection entry point, reachable from any Context. */
val Context.appContainer: AppContainer
    get() = (applicationContext as TallyApp).container
