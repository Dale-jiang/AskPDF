package com.ctf.askpdf.presentation.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.addCallback
import androidx.lifecycle.lifecycleScope
import com.ctf.askpdf.MainActivity
import com.ctf.askpdf.core.timer.CountdownTimer
import com.ctf.askpdf.databinding.ActivitySplashBinding
import com.ctf.askpdf.presentation.base.BaseActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity<ActivitySplashBinding>(ActivitySplashBinding::inflate) {

    private var loadingStartTime = 0L
    private var hasScheduledLaunchAction = false

    private val countdownTimer by lazy {
        CountdownTimer(
            totalTimeMillis = MAX_LOADING_TIME_MILLIS,
            intervalMillis = TICK_INTERVAL_MILLIS,
            onTick = {
                if (true) {
                    scheduleLaunchAction { openMainPage() }
                }
            },
            onFinish = {
                scheduleLaunchAction { openMainPage() }
            }
        )
    }

    /**
     * 初始化启动页展示状态并屏蔽返回键。
     */
    override fun initView(savedInstanceState: Bundle?) {
        onBackPressedDispatcher.addCallback(this) {
            // 启动页不响应返回，避免中断初始化流程。
        }
        startLoading()
    }

    /**
     * 启动加载计时，记录开始时间并重置防重复跳转标记。
     */
    private fun startLoading() {
        loadingStartTime = System.currentTimeMillis()
        hasScheduledLaunchAction = false
        countdownTimer.reset()
        countdownTimer.start()
        requestLaunchResource()
    }

    /**
     * 请求启动页依赖资源；后续接入广告时，有缓存可直接调用 onLaunchResourceReady。
     */
    private fun requestLaunchResource() {
        onLaunchResourceReady()
    }

    /**
     * 启动页依赖资源就绪后进入下一步，内部会保证最短展示时间。
     */
    private fun onLaunchResourceReady() {
        scheduleLaunchAction { openMainPage() }
    }

    /**
     * 统一调度跳转动作，确保只执行一次并满足最短展示时长。
     */
    private fun scheduleLaunchAction(action: () -> Unit) {
        if (hasScheduledLaunchAction) return
        hasScheduledLaunchAction = true
        countdownTimer.stop()
        lifecycleScope.launch(Dispatchers.Main) {
            val elapsed = System.currentTimeMillis() - loadingStartTime
            val delayMillis = (MIN_LOADING_TIME_MILLIS - elapsed).coerceAtLeast(0L)
            if (delayMillis > 0L) delay(delayMillis)
            if (!isFinishing && !isDestroyed) {
                action()
            }
        }
    }

    /**
     * 跳转到主页面并清理启动页任务栈。
     */
    private fun openMainPage() {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        finish()
    }

    override fun onDestroy() {
        countdownTimer.cancel()
        super.onDestroy()
    }

    private companion object {
        private const val MAX_LOADING_TIME_MILLIS = 16_000L
        private const val MIN_LOADING_TIME_MILLIS = 3_000L
        private const val TICK_INTERVAL_MILLIS = 200L
    }
}
