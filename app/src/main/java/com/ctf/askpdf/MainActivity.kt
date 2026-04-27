package com.ctf.askpdf

import android.os.Bundle
import com.ctf.askpdf.databinding.ActivityMainBinding
import com.ctf.askpdf.presentation.base.BaseActivity

class MainActivity : BaseActivity<ActivityMainBinding>(ActivityMainBinding::inflate) {

    /**
     * 初始化首页基础 UI。
     */
    override fun initView(savedInstanceState: Bundle?) = Unit
}
