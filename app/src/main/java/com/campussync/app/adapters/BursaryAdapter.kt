package com.campussync.app.adapters

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.campussync.app.databinding.ItemBursaryBinding
import com.campussync.app.models.Bursary

/**
 * Adapter for displaying Bursary opportunities.
 */
class BursaryAdapter : ListAdapter<Bursary, BursaryAdapter.BursaryViewHolder>(BursaryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BursaryViewHolder {
        val binding = ItemBursaryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BursaryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BursaryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class BursaryViewHolder(private val binding: ItemBursaryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(bursary: Bursary) {
            binding.tvBursaryName.text = bursary.name
            binding.tvProvider.text = bursary.provider
            binding.tvDescription.text = bursary.description
            binding.tvClosingDate.text = "Closing: ${bursary.closingDate}"
            
            binding.btnApply.setOnClickListener {
                if (bursary.applyUrl.isNotEmpty()) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(bursary.applyUrl))
                    it.context.startActivity(intent)
                }
            }
        }
    }

    class BursaryDiffCallback : DiffUtil.ItemCallback<Bursary>() {
        override fun areItemsTheSame(oldItem: Bursary, newItem: Bursary): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Bursary, newItem: Bursary): Boolean = oldItem == newItem
    }
}
