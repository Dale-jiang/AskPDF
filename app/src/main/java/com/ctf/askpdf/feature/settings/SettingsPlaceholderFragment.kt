package com.ctf.askpdf.feature.settings

import android.os.Bundle
import com.ctf.askpdf.databinding.FragmentSettingsStubBinding
import com.ctf.askpdf.presentation.base.BaseFragment

class SettingsPlaceholderFragment : BaseFragment<FragmentSettingsStubBinding>(FragmentSettingsStubBinding::inflate) {

    /**
     * Settings tab 暂不实现，保留顶层 ViewPager 占位。
     */
    override fun initView(savedInstanceState: Bundle?) = Unit
}
