package com.ctf.askpdf.feature.settings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ctf.askpdf.R
import com.ctf.askpdf.databinding.ItemLanguageOptionBinding

class LanguageOptionAdapter(
    private val items: List<LanguageOption>,
    private val onSelected: (Int) -> Unit
) : RecyclerView.Adapter<LanguageOptionAdapter.LanguageViewHolder>() {

    var selectedIndex: Int = 0
        private set

    class LanguageViewHolder(val binding: ItemLanguageOptionBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
        val binding = ItemLanguageOptionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LanguageViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
        val item = items[position]
        val checked = position == selectedIndex
        holder.binding.languageName.text = item.displayName
        holder.binding.languageName.setTextColor(
            holder.itemView.context.getColor(if (checked) R.color.text_color else R.color.settings_language_inactive)
        )
        holder.binding.languageCheck.setImageResource(if (checked) R.drawable.ic_language_checked else R.drawable.ic_language_unchecked)
        holder.binding.root.setOnClickListener { selectPosition(holder.bindingAdapterPosition) }
    }

    /**
     * 设置初始选中项。
     */
    fun setInitialSelection(index: Int) {
        selectedIndex = index.coerceIn(items.indices)
    }

    /**
     * 切换当前语言选中项并刷新变更行。
     */
    private fun selectPosition(position: Int) {
        if (position == RecyclerView.NO_POSITION || position == selectedIndex) return
        val oldIndex = selectedIndex
        selectedIndex = position
        notifyItemChanged(oldIndex)
        notifyItemChanged(selectedIndex)
        onSelected(selectedIndex)
    }
}
