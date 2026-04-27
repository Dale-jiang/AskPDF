package com.ctf.askpdf.presentation.permission

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.ctf.askpdf.databinding.ActivityPermissionAskBinding
import com.ctf.askpdf.presentation.base.BaseActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StoragePermissionAskActivity : BaseActivity<ActivityPermissionAskBinding>(ActivityPermissionAskBinding::inflate) {

    private var checker: Job? = null
    private var openedSettings = false

    override fun initView(savedInstanceState: Bundle?) = Unit

    override fun onResume() {
        super.onResume()
        if (openedSettings) {
            finish()
            return
        }

        openedSettings = true
        openStorageSettings()
        startPermissionChecker()
    }

    @SuppressLint("InlinedApi")
    private fun openStorageSettings() {
        runCatching {
            startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = "package:$packageName".toUri()
                addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            })
        }.onFailure {
            runCatching {
                startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                })
            }
        }
    }

    /**
     * 在设置页授权后尽快结束当前引导页。
     */
    private fun startPermissionChecker() {
        checker?.cancel()
        checker = lifecycleScope.launch(Dispatchers.IO) {
            while (hasDocumentStoragePermission().not()) {
                delay(150)
            }
            withContext(Dispatchers.Main) {
                startActivity(Intent(this@StoragePermissionAskActivity, StoragePermissionAskActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                })
                finish()
            }
        }
    }

    override fun onDestroy() {
        checker?.cancel()
        checker = null
        super.onDestroy()
    }
}
