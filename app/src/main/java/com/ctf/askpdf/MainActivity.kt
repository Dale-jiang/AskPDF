package com.ctf.askpdf

import android.Manifest
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.ctf.askpdf.databinding.ActivityMainBinding
import com.ctf.askpdf.document.model.DocumentTab
import com.ctf.askpdf.feature.document.DocumentBucketFragment
import com.ctf.askpdf.feature.document.DocumentHubViewModel
import com.ctf.askpdf.feature.settings.SettingsPlaceholderFragment
import com.ctf.askpdf.presentation.base.BaseActivity
import com.ctf.askpdf.presentation.permission.StoragePermissionAskActivity
import com.ctf.askpdf.presentation.permission.hasDocumentStoragePermission
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : BaseActivity<ActivityMainBinding>(ActivityMainBinding::inflate) {

    private val viewModel by viewModels<DocumentHubViewModel>()
    private val mainTabs by lazy {
        listOf(
            MainTab(binding.tabHome, R.drawable.main_home_red),
            MainTab(binding.tabRecentFiles, R.drawable.main_recent_red),
            MainTab(binding.tabCollection, R.drawable.main_collection_red),
            MainTab(binding.tabSettings, R.drawable.main_settings_red)
        )
    }
    private val legacyStorageLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (hasDocumentStoragePermission()) {
            onStoragePermissionGranted()
        } else {
            viewModel.permissionMissingLiveData.postValue(true)
        }
    }
    private val settingsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (hasDocumentStoragePermission()) {
            onStoragePermissionGranted()
        } else {
            viewModel.permissionMissingLiveData.postValue(true)
        }
    }

    override fun onAttachedToWindow() = myEnableEdgeToEdge(binding.container)

    /**
     * 初始化首页基础 UI、顶层 tab 和文件权限状态。
     */
    override fun initView(savedInstanceState: Bundle?) {
        refreshHomeDate()
        initMainPager()
        updateStoragePermissionState()
    }

    override fun onResume() {
        super.onResume()
        refreshHomeDate()
        if (hasDocumentStoragePermission()) {
            onStoragePermissionGranted()
        }
    }

    override fun observeData() {
        viewModel.requestPermissionLiveData.observe(this) {
            checkStoragePermission()
        }
    }

    /**
     * 刷新首页日期文本，页面恢复时同步当天日期。
     */
    private fun refreshHomeDate() {
        val dateFormatter = DateTimeFormatter.ofPattern(
            getString(R.string.home_date_pattern),
            Locale.getDefault()
        )
        binding.tvDate.text = LocalDate.now().format(dateFormatter)
    }

    /**
     * 初始化顶层 ViewPager，并装载 Home、Recent、Collection 和 Settings 占位页。
     */
    private fun initMainPager() {
        val pages = listOf(
            DocumentBucketFragment.newInstance(DocumentTab.HOME),
            DocumentBucketFragment.newInstance(DocumentTab.RECENT),
            DocumentBucketFragment.newInstance(DocumentTab.COLLECTION),
            SettingsPlaceholderFragment()
        )
        binding.viewPager.isUserInputEnabled = false
        binding.viewPager.offscreenPageLimit = pages.size
        binding.viewPager.adapter = object : FragmentStateAdapter(supportFragmentManager, lifecycle) {
            override fun getItemCount(): Int = pages.size
            override fun createFragment(position: Int): Fragment = pages[position]
        }
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                selectMainTab(position)
            }
        })
        mainTabs.forEachIndexed { index, tab ->
            tab.view.setOnClickListener {
                binding.viewPager.setCurrentItem(index, false)
            }
        }
        selectMainTab(0)
    }

    /**
     * 检查并申请文档读取权限。
     */
    private fun checkStoragePermission() {
        if (hasDocumentStoragePermission()) {
            onStoragePermissionGranted()
            return
        }
        viewModel.permissionMissingLiveData.postValue(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            settingsLauncher.launch(Intent(this, StoragePermissionAskActivity::class.java))
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            requestLegacyStorage()
        } else {
            requestLegacyStorage()
        }
    }

    /**
     * 首次进入只更新权限 UI，不主动跳系统授权页。
     */
    private fun updateStoragePermissionState() {
        if (hasDocumentStoragePermission()) {
            onStoragePermissionGranted()
        } else {
            viewModel.permissionMissingLiveData.postValue(true)
        }
    }

    /**
     * 申请 Android 10 及以下的外部存储读写权限。
     */
    private fun requestLegacyStorage() {
        legacyStorageLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE))
    }

    /**
     * 获取权限后扫描本机文档。
     */
    private fun onStoragePermissionGranted() {
        viewModel.permissionMissingLiveData.postValue(false)
        viewModel.scanDocuments(this)
    }

    /**
     * 切换首页底部 tab 的选中颜色和图标颜色。
     */
    private fun selectMainTab(selectedIndex: Int) {
        updateHeaderTitle(selectedIndex)
        val activeColor = ContextCompat.getColor(this, R.color.main_tab_active)
        val inactiveColor = ContextCompat.getColor(this, R.color.main_tab_inactive)
        mainTabs.forEachIndexed { index, tab ->
            val tintColor = if (index == selectedIndex) activeColor else inactiveColor
            tab.view.setTextColor(tintColor)
            tab.view.setTintedTopDrawable(tab.iconRes, tintColor)
        }
    }

    /**
     * 根据当前主 tab 更新顶部标题，Home 显示应用名。
     */
    private fun updateHeaderTitle(selectedIndex: Int) {
        val titleRes = when (selectedIndex) {
            1 -> R.string.tab_recent_files
            2 -> R.string.tab_collection
            3 -> R.string.tab_settings
            else -> R.string.app_name
        }
        binding.appName.setText(titleRes)
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
