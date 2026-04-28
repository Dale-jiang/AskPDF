package com.ctf.askpdf.app

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.ctf.askpdf.core.locale.AppLanguageConfig
import com.ctf.askpdf.data.app
import com.ctf.askpdf.data.local.selectedLanguageTag

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        app = this
        AppLifecycleUtils.register(this)
        applySavedLanguage()
    }

    /**
     * 应用用户上次选择的语言；未选择时按系统语言匹配支持语言。
     */
    private fun applySavedLanguage() {
        val languageTag = AppLanguageConfig.resolveLanguageTag(selectedLanguageTag)
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag))
    }

}
