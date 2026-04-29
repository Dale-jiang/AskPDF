package com.ctf.askpdf.feature.split

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.util.LruCache
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.ctf.askpdf.R
import com.ctf.askpdf.databinding.ItemSplitPageBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class SplitPageAdapter(
    private val pageCount: Int,
    private val pdfRenderer: PdfRenderer?,
    private val lifecycleScope: CoroutineScope,
    private val onSelectionChanged: () -> Unit
) : RecyclerView.Adapter<SplitPageAdapter.SplitPageViewHolder>() {

    companion object {
        private const val THUMBNAIL_WIDTH = 160
        private const val THUMBNAIL_HEIGHT = 220
        private const val CACHE_SIZE_KB = 12 * 1024
    }

    private val selectedPages = mutableListOf<Int>()
    private val thumbnailCache = object : LruCache<Int, Bitmap>(CACHE_SIZE_KB) {
        override fun sizeOf(key: Int, value: Bitmap): Int {
            return value.byteCount / 1024
        }
    }
    private val renderMutex = Mutex()

    class SplitPageViewHolder(val binding: ItemSplitPageBinding) : RecyclerView.ViewHolder(binding.root) {
        var thumbnailJob: Job? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SplitPageViewHolder {
        val binding = ItemSplitPageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SplitPageViewHolder(binding)
    }

    override fun getItemCount(): Int = pageCount

    override fun onBindViewHolder(holder: SplitPageViewHolder, position: Int) {
        val selected = selectedPages.contains(position)
        val selectedOrder = selectedPages.indexOf(position) + 1
        holder.binding.apply {
            root.isSelected = selected
            pageNumber.text = (position + 1).toString()
            pageLabel.text = root.context.getString(R.string.split_page_number, position + 1)
            selectedMark.isVisible = selected
            selectedMark.setBackgroundResource(R.drawable.bg_merge_order_active)
            selectedMark.text = if (selected) selectedOrder.toString() else ""
            pagePreview.setImageBitmap(thumbnailCache.get(position))
            holder.thumbnailJob?.cancel()
            if (pdfRenderer == null) {
                pagePreview.setImageDrawable(null)
            } else if (thumbnailCache.get(position) == null) {
                pagePreview.setImageDrawable(null)
                holder.thumbnailJob = lifecycleScope.launch {
                    val bitmap = renderThumbnail(position)
                    if (bitmap != null) {
                        thumbnailCache.put(position, bitmap)
                        if (holder.bindingAdapterPosition == position) {
                            pagePreview.setImageBitmap(bitmap)
                        }
                    }
                }
            }
            root.setOnClickListener { togglePage(position) }
        }
    }

    override fun onViewRecycled(holder: SplitPageViewHolder) {
        holder.thumbnailJob?.cancel()
        holder.thumbnailJob = null
        super.onViewRecycled(holder)
    }

    /**
     * 返回当前已选页码，页码从 0 开始。
     */
    fun selectedItems(): List<Int> {
        return selectedPages.sorted()
    }

    /**
     * 切换单页选择状态。
     */
    private fun togglePage(position: Int) {
        if (selectedPages.contains(position)) {
            selectedPages.remove(position)
        } else {
            selectedPages.add(position)
        }
        notifyDataSetChanged()
        onSelectionChanged()
    }

    /**
     * 渲染指定页的缩略图，用于拆分前预览页面内容。
     */
    private suspend fun renderThumbnail(position: Int): Bitmap? = withContext(Dispatchers.IO) {
        runCatching {
            val renderer = pdfRenderer ?: return@withContext null
            renderMutex.withLock {
                renderer.openPage(position).use { page ->
                    Bitmap.createBitmap(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, Bitmap.Config.ARGB_8888).also { bitmap ->
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    }
                }
            }
        }.getOrNull()
    }
}
