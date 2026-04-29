package com.ctf.askpdf.feature.split

import android.content.Context
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ctf.askpdf.R
import com.ctf.askpdf.databinding.ItemMergePdfBinding
import com.ctf.askpdf.document.model.DocumentFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SplitPdfAdapter(
    private val context: Context,
    private val onSelectionChanged: () -> Unit
) : RecyclerView.Adapter<SplitPdfAdapter.SplitPdfViewHolder>() {

    private var selectedPath: String? = null
    var items: List<DocumentFile> = emptyList()
        private set

    class SplitPdfViewHolder(val binding: ItemMergePdfBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SplitPdfViewHolder {
        val binding = ItemMergePdfBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SplitPdfViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: SplitPdfViewHolder, position: Int) {
        val item = items[position]
        val selected = item.path == selectedPath
        holder.binding.apply {
            root.isSelected = selected
            fileName.text = item.displayName
            fileDesc.text = buildString {
                append(SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(Date(item.dateAdded)))
                append("  ")
                append(Formatter.formatFileSize(context, item.size))
            }
            selectOrder.text = if (selected) "✓" else ""
            selectOrder.setBackgroundResource(if (selected) R.drawable.bg_merge_order_active else R.drawable.bg_merge_order_normal)
            root.setOnClickListener { select(item) }
        }
    }

    /**
     * 刷新 PDF 列表并按需预选指定路径。
     */
    fun submitList(nextItems: List<DocumentFile>, preselectedPath: String? = null) {
        items = nextItems
        if (selectedPath != null && nextItems.none { it.path == selectedPath }) selectedPath = null
        if (preselectedPath.isNullOrBlank().not() && selectedPath == null && nextItems.any { it.path == preselectedPath }) {
            selectedPath = preselectedPath
        }
        notifyDataSetChanged()
        onSelectionChanged()
    }

    /**
     * 返回当前选中的 PDF。
     */
    fun selectedItem(): DocumentFile? {
        return items.firstOrNull { it.path == selectedPath }
    }

    /**
     * 选中单个 PDF。
     */
    private fun select(item: DocumentFile) {
        if (selectedPath == item.path) return
        selectedPath = item.path
        notifyDataSetChanged()
        onSelectionChanged()
    }
}
