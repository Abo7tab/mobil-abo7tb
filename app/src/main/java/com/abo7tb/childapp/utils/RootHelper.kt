package com.abo7tb.childapp.utils

import android.content.Context
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader

object RootHelper {

    private const val PREF_ROOT_ENABLED = "root_hide_enabled"

    fun isRootAvailable(): Boolean {
        return runSuCommand("id")?.contains("uid=0") == true
    }

    fun hideApp(context: Context): Boolean {
        val pkg = context.packageName
        val ok = runSuCommand("pm hide $pkg") != null || runSuCommand("pm hide --user 0 $pkg") != null
        if (ok) {
            context.getSharedPreferences(Constants.PREFS_NAME + "_root", Context.MODE_PRIVATE)
                .edit().putBoolean(PREF_ROOT_ENABLED, true).apply()
            Timber.d("RootHelper: pm hide success for $pkg")
        }
        return ok
    }

    fun unhideApp(context: Context): Boolean {
        val pkg = context.packageName
        val ok = runSuCommand("pm unhide $pkg") != null || runSuCommand("pm unhide --user 0 $pkg") != null
        context.getSharedPreferences(Constants.PREFS_NAME + "_root", Context.MODE_PRIVATE)
            .edit().putBoolean(PREF_ROOT_ENABLED, false).apply()
        Timber.d("RootHelper: pm unhide success=$ok for $pkg")
        return ok
    }

    fun isRootHideEnabled(context: Context): Boolean {
        return context.getSharedPreferences(Constants.PREFS_NAME + "_root", Context.MODE_PRIVATE)
            .getBoolean(PREF_ROOT_ENABLED, false)
    }

    private fun runSuCommand(command: String): String? {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            val output = BufferedReader(InputStreamReader(process.inputStream)).readText()
            val error = BufferedReader(InputStreamReader(process.errorStream)).readText()
            val exit = process.waitFor()
            if (exit == 0) output.trim().ifEmpty { "ok" } else {
                Timber.w("RootHelper: su failed ($exit): $error")
                null
            }
        } catch (e: Exception) {
            Timber.w(e, "RootHelper: su not available")
            null
        }
    }
}
