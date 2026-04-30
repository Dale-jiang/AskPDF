package com.ctf.askpdf.app

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.PowerManager
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.ctf.askpdf.MainActivity
import com.ctf.askpdf.core.locale.AppLanguageConfig
import com.ctf.askpdf.data.local.selectedLanguageTag
import com.ctf.askpdf.presentation.splash.SplashActivity
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

object AppLifecycleUtils {

    private const val WARM_LAUNCH_TIME = 300L

    private val activityRefs = Collections.newSetFromMap(
        ConcurrentHashMap<WeakReference<Activity>, Boolean>()
    )

    private val appCallbacks = AppCallbacks()

    private fun addActivity(activity: Activity) {
        cleanActivityRefs()
        activityRefs.add(WeakReference(activity))
    }

    private fun removeActivity(activity: Activity) {
        activityRefs.removeIf { it.get() == null || it.get() == activity }
    }

    private fun cleanActivityRefs() {
        activityRefs.removeIf { it.get() == null }
    }

    private fun finishAllExceptMain() {
        cleanActivityRefs()
        activityRefs.forEach { ref ->
            ref.get()
                ?.takeIf { it !is MainActivity }
                ?.finish()
        }
    }

    private val appScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, err -> err.printStackTrace() })

    val activityCount = AtomicInteger(0)

    @Volatile
    private var isHotStart = false

    @Volatile
    private var isToSettingPage = false

    private var backgroundJob: Job? = null

    fun register(app: Application) {
        app.registerActivityLifecycleCallbacks(appCallbacks)
    }

    fun unregister(application: Application) {
        application.unregisterActivityLifecycleCallbacks(appCallbacks)
        appScope.cancel()
    }

    fun markNavigatingToSetting(flag: Boolean) {
        isToSettingPage = flag
    }

    private fun cancelBackgroundJob() {
        backgroundJob?.cancel()
        backgroundJob = null
    }

    private fun scheduleHotStartCheck() {
        cancelBackgroundJob()
        backgroundJob = appScope.launch {
            delay(WARM_LAUNCH_TIME)
            isHotStart = true
            finishAllExceptMain()
        }
    }

    private class AppCallbacks : Application.ActivityLifecycleCallbacks {

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            addActivity(activity)
        }

        override fun onActivityStarted(activity: Activity) {
            val count = activityCount.incrementAndGet()
            if (count > 0) cancelBackgroundJob()
            if (isHotStart && isDeviceInteractive(activity)) {
                isHotStart = false
                if (activity is SplashActivity) return
                if (!isToSettingPage) {
                    activity.startActivity(Intent(activity, SplashActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                }
                isToSettingPage = false
            }
        }

        override fun onActivityResumed(activity: Activity) {
            refreshActivityLanguage(activity)
        }

        override fun onActivityPaused(activity: Activity) = Unit

        override fun onActivityStopped(activity: Activity) {
            val count = activityCount.decrementAndGet().coerceAtLeast(0)
            if (!isToSettingPage && count == 0) {
                scheduleHotStartCheck()
            }
        }

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

        override fun onActivityDestroyed(activity: Activity) {
            removeActivity(activity)
        }

        private fun isDeviceInteractive(activity: Activity): Boolean {
            val pm = activity.getSystemService(Context.POWER_SERVICE) as PowerManager
            return pm.isInteractive
        }

        /**
         * Activity 回到前台时刷新应用和当前页面语言资源。
         */
        private fun refreshActivityLanguage(activity: Activity) {
            val languageTag = AppLanguageConfig.resolveLanguageTag(selectedLanguageTag)
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag))
            AppLanguageConfig.refreshResources(activity.applicationContext, languageTag)
            AppLanguageConfig.refreshResources(activity, languageTag)
        }
    }
}
