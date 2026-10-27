package com.tally.counter.widget

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.tally.counter.appContainer
import kotlinx.coroutines.launch

/**
 * Invisible trampoline started from a widget cell: resets that app's count, opens the app,
 * and finishes immediately. Using an activity (rather than a broadcast) keeps the launch
 * within Android's background-activity-start rules.
 */
class OpenAppActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val packageName = intent?.getStringExtra(EXTRA_PACKAGE) ?: intent?.data?.lastPathSegment
        if (packageName != null) {
            val container = appContainer
            container.applicationScope.launch {
                container.countRepository.resetApp(packageName)
            }
            val launch = packageManager.getLaunchIntentForPackage(packageName)
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                try {
                    startActivity(launch)
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(this, "Couldn't open the app", Toast.LENGTH_SHORT).show()
                } catch (e: SecurityException) {
                    Toast.makeText(this, "Couldn't open the app", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Count reset. This app can't be opened directly.", Toast.LENGTH_SHORT).show()
            }
        }
        finish()
    }

    companion object {
        const val ACTION_OPEN = "com.tally.counter.action.OPEN_APP"
        const val EXTRA_PACKAGE = "com.tally.counter.extra.PACKAGE"
    }
}
