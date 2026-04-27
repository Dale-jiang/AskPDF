package com.ctf.askpdf

import android.content.res.ColorStateList
import android.os.Bundle
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.viewpager2.widget.ViewPager2
import com.ctf.askpdf.databinding.ActivityMainBinding
import com.ctf.askpdf.presentation.base.BaseActivity

class MainActivity : BaseActivity<ActivityMainBinding>(ActivityMainBinding::inflate) {

    private val mainTabs by lazy {
        listOf(
            MainTab(binding.tabHome, R.drawable.main_home_red),
            MainTab(binding.tabRecentFiles, R.drawable.main_recent_red),
            MainTab(binding.tabCollection, R.drawable.main_collection_red),
            MainTab(binding.tabSettings, R.drawable.main_settings_red)
        )
    }

    override fun onAttachedToWindow() = myEnableEdgeToEdge(binding.container)


    /**
     * 初始化首页基础 UI。
     */
    override fun initView(savedInstanceState: Bundle?) {
        initMainTabs()
    }

    /**
     * 初始化首页底部 tab 的点击事件和 ViewPager 联动。
     */
    private fun initMainTabs() {
        mainTabs.forEachIndexed { index, tab ->
            tab.view.setOnClickListener {
                selectMainTab(index)
                if (binding.viewPager.adapter != null) {
                    binding.viewPager.setCurrentItem(index, false)
                }
            }
        }
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                selectMainTab(position)
            }
        })
        selectMainTab(0)
    }

    /**
     * 切换首页底部 tab 的选中颜色和图标颜色。
     */
    private fun selectMainTab(selectedIndex: Int) {
        val activeColor = ContextCompat.getColor(this, R.color.main_tab_active)
        val inactiveColor = ContextCompat.getColor(this, R.color.main_tab_inactive)
        mainTabs.forEachIndexed { index, tab ->
            val tintColor = if (index == selectedIndex) activeColor else inactiveColor
            tab.view.setTextColor(tintColor)
            tab.view.setTintedTopDrawable(tab.iconRes, tintColor)
        }
    }

    /**
     * 设置带颜色的顶部图标，避免为每个状态维护两套资源。
     */
    private fun AppCompatTextView.setTintedTopDrawable(iconRes: Int, color: Int) {
        val drawable = ContextCompat.getDrawable(context, iconRes)?.mutate() ?: return
        DrawableCompat.setTintList(drawable, ColorStateList.valueOf(color))
        setCompoundDrawablesWithIntrinsicBounds(null, drawable, null, null)
    }

    private data class MainTab(
        val view: AppCompatTextView,
        val iconRes: Int
    )

}
