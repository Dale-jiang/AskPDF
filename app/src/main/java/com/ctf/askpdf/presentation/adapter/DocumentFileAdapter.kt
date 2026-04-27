package com.ctf.askpdf.presentation.adapter

import android.content.Context
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ctf.askpdf.R
import com.ctf.askpdf.databinding.ItemDocumentFileBinding
import com.ctf.askpdf.document.model.DocumentFile
import com.ctf.askpdf.document.model.DocumentTab
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DocumentFileAdapter(
    private val context: Context,
    private val tab: DocumentTab,
    private val itemClick: (DocumentFile) -> Unit,
    private val moreClick: (DocumentFile) -> Unit
) : ListAdapter<DocumentFile, DocumentFileAdapter.DocumentViewHolder>(DocumentDiffCallback()) {

    class DocumentViewHolder(val binding: ItemDocumentFileBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocumentViewHolder {
        val binding = ItemDocumentFileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DocumentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DocumentViewHolder, position: Int) {
        val item = getItem(position)
        holder.binding.apply {
            itemIcon.setImageResource(item.resolveType()?.iconRes ?: R.drawable.ic_pdf)
            itemMore.setImageResource(if (tab == DocumentTab.COLLECTION) R.drawable.main_collection_red else R.drawable.ic_item_more)
            itemName.text = item.displayName
            itemDesc.text = buildString {
                append(SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(Date(item.dateAdded)))
                append("  ")
                append(Formatter.formatFileSize(context, item.size))
            }
            root.setOnClickListener { itemClick(item) }
            itemMore.setOnClickListener { moreClick(item) }
        }
    }

    class DocumentDiffCallback : DiffUtil.ItemCallback<DocumentFile>() {
        override fun areItemsTheSame(oldItem: DocumentFile, newItem: DocumentFile): Boolean {
            return oldItem.path == newItem.path
        }

        override fun areContentsTheSame(oldItem: DocumentFile, newItem: DocumentFile): Boolean {
            return oldItem == newItem
        }
    }
}
