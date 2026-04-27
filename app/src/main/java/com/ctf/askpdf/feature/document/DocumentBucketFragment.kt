package com.ctf.askpdf.feature.document

import android.os.Bundle
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.ctf.askpdf.R
import com.ctf.askpdf.databinding.FragmentDocumentBucketBinding
import com.ctf.askpdf.document.model.DocumentKind
import com.ctf.askpdf.document.model.DocumentTab
import com.ctf.askpdf.presentation.base.BaseFragment

class DocumentBucketFragment : BaseFragment<FragmentDocumentBucketBinding>(FragmentDocumentBucketBinding::inflate) {

    companion object {
        private const val ARG_DOCUMENT_TAB = "arg_document_tab"

        fun newInstance(tab: DocumentTab): DocumentBucketFragment {
            return DocumentBucketFragment().apply {
                arguments = Bundle().apply { putSerializable(ARG_DOCUMENT_TAB, tab) }
            }
        }
    }

    private val viewModel by activityViewModels<DocumentHubViewModel>()
    private val documentTab by lazy {
        arguments?.getSerializable(ARG_DOCUMENT_TAB) as? DocumentTab ?: DocumentTab.HOME
    }

    override fun initView(savedInstanceState: Bundle?) {
        initTypePager()
        binding.viewPermission.btnAllow.setOnClickListener {
            viewModel.requestPermissionLiveData.postValue(true)
        }
        binding.viewPermission.root.isVisible = false
        binding.loadingView.isVisible = documentTab == DocumentTab.HOME
    }

    override fun initData() {
        when (documentTab) {
            DocumentTab.RECENT -> viewModel.observeRecentFiles()
            DocumentTab.COLLECTION -> viewModel.observeCollectionFiles()
            DocumentTab.HOME -> Unit
        }
    }

    override fun observeData() {
        viewModel.scannedFilesLiveData.observe(viewLifecycleOwner) {
            if (documentTab == DocumentTab.HOME) binding.loadingView.isVisible = false
        }
        viewModel.permissionMissingLiveData.observe(viewLifecycleOwner) { missing ->
            if (documentTab == DocumentTab.HOME) {
                binding.viewPermission.root.isVisible = missing
                binding.viewPager.isVisible = missing.not()
                binding.loadingView.isVisible = missing.not() && viewModel.scannedFilesLiveData.value == null
            }
        }
    }

    /**
     * 初始化文件类型二级 tab，与内部列表 Fragment 联动。
     */
    private fun initTypePager() {
        val kinds = listOf(DocumentKind.ALL, DocumentKind.PDF, DocumentKind.WORD, DocumentKind.EXCEL, DocumentKind.PPT)
        val titles = listOf(
            getString(R.string.all),
            getString(R.string.pdf),
            getString(R.string.word),
            getString(R.string.excel),
            getString(R.string.ppt)
        )
        binding.viewPager.offscreenPageLimit = kinds.size
        binding.viewPager.adapter = object : FragmentStateAdapter(childFragmentManager, lifecycle) {
            override fun getItemCount(): Int = kinds.size
            override fun createFragment(position: Int): Fragment {
                return DocumentListFragment.newInstance(documentTab, kinds[position])
            }
        }
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = titles[position]
        }.attach()
        adjustTabSpacing()
    }

    /**
     * 设置二级 tab 间距，使其接近参考项目的 pill 布局。
     */
    private fun adjustTabSpacing() {
        val tabs = binding.tabLayout.getChildAt(0) as? ViewGroup ?: return
        for (index in 0 until tabs.childCount) {
            val child = tabs.getChildAt(index)
            val params = child.layoutParams as? ViewGroup.MarginLayoutParams ?: continue
            params.marginStart = 5.dp()
            params.marginEnd = 5.dp()
            child.layoutParams = params
        }
    }

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()
}
