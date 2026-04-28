package com.ctf.askpdf.app

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.ctf.askpdf.data.app
import com.ctf.askpdf.data.local.selectedLanguageTag

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        app = this
        applySavedLanguage()
    }

    /**
     * 应用用户上次选择的语言。
     */
    private fun applySavedLanguage() {
        if (selectedLanguageTag.isNotBlank()) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(selectedLanguageTag))
        }
    }

}
