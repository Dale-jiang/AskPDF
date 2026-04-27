package com.ctf.askpdf.core.timer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CountdownTimer(
    private val totalTimeMillis: Long,
    private val intervalMillis: Long = 1_000L,
    private val onTick: (Long) -> Unit,
    private val onFinish: () -> Unit
) {

    private var remainingTime = totalTimeMillis
    private var job: Job? = null
    private val timerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    /**
     * 启动倒计时，重复调用时不会创建新的计时任务。
     */
    fun start() {
        if (job?.isActive == true) return

        job = timerScope.launch {
            val startTime = System.currentTimeMillis()
            while (remainingTime > 0L) {
                onTick(remainingTime)
                delay(intervalMillis)
                val elapsedTime = System.currentTimeMillis() - startTime
                remainingTime = (totalTimeMillis - elapsedTime).coerceAtLeast(0L)
            }
            onFinish()
        }
    }

    /**
     * 暂停当前计时任务，保留剩余时间。
     */
    fun stop() {
        job?.cancel()
        job = null
    }

    /**
     * 重置倒计时到初始总时长。
     */
    fun reset() {
        stop()
        remainingTime = totalTimeMillis
    }

    /**
     * 彻底取消计时器并释放协程作用域。
     */
    fun cancel() {
        timerScope.cancel()
        job = null
    }
}
