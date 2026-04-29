package com.ctf.askpdf.feature.merge

import android.content.Context
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.ctf.askpdf.R
import com.ctf.askpdf.databinding.ItemMergePdfBinding
import com.ctf.askpdf.document.model.DocumentFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MergePdfAdapter(
    private val context: Context,
    private val onSelectionChanged: () -> Unit
) : RecyclerView.Adapter<MergePdfAdapter.MergePdfViewHolder>() {

    companion object {
        const val MAX_SELECTED_COUNT = 20
    }

    private val selectedPaths = mutableListOf<String>()
    var items: List<DocumentFile> = emptyList()
        private set

    class MergePdfViewHolder(val binding: ItemMergePdfBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MergePdfViewHolder {
        val binding = ItemMergePdfBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MergePdfViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: MergePdfViewHolder, position: Int) {
        val item = items[position]
        val order = selectedPaths.indexOf(item.path) + 1
        holder.binding.apply {
            root.isSelected = order > 0
            fileName.text = item.displayName
            fileDesc.text = buildString {
                append(SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(Date(item.dateAdded)))
                append("  ")
                append(Formatter.formatFileSize(context, item.size))
            }
            selectOrder.text = if (order > 0) order.toString() else ""
            selectOrder.setBackgroundResource(if (order > 0) R.drawable.bg_merge_order_active else R.drawable.bg_merge_order_normal)
            root.setOnClickListener { toggleSelection(item) }
        }
    }

    /**
     * 刷新 PDF 列表并保留仍存在的选中项。
     */
    fun submitList(nextItems: List<DocumentFile>, preselectedPath: String? = null) {
        items = nextItems
        selectedPaths.removeAll { path -> nextItems.none { it.path == path } }
        if (preselectedPath.isNullOrBlank().not() && selectedPaths.isEmpty() && nextItems.any { it.path == preselectedPath }) {
            selectedPaths.add(preselectedPath!!)
        }
        notifyDataSetChanged()
        onSelectionChanged()
    }

    /**
     * 返回当前按选择顺序排列的 PDF。
     */
    fun selectedItems(): List<DocumentFile> {
        return selectedPaths.mapNotNull { path -> items.firstOrNull { it.path == path } }
    }

    /**
     * 切换选中状态，最多允许 20 个文件。
     */
    private fun toggleSelection(item: DocumentFile) {
        val oldOrder = selectedPaths.indexOf(item.path)
        if (oldOrder >= 0) {
            selectedPaths.removeAt(oldOrder)
        } else if (selectedPaths.size < MAX_SELECTED_COUNT) {
            selectedPaths.add(item.path)
        } else {
            Toast.makeText(context, context.getString(R.string.merge_max_selected, MAX_SELECTED_COUNT), Toast.LENGTH_SHORT).show()
            return
        }
        notifyDataSetChanged()
        onSelectionChanged()
    }
}
