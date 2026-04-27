package com.ctf.askpdf.data

import com.ctf.askpdf.app.App
import com.ctf.askpdf.data.db.AskPdfDatabase

typealias CallBack = () -> Unit
internal lateinit var app: App
val askPdfDatabase by lazy { AskPdfDatabase.getInstance(app) }
