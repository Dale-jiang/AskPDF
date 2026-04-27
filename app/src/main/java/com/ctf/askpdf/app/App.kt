package com.ctf.askpdf.app

import android.app.Application
import com.ctf.askpdf.data.app

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        app = this
    }


}