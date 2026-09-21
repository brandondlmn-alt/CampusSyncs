package com.campussync.app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.campussync.app.databinding.ItemAccommodationBinding
import com.campussync.app.models.Accommodation

/**
 * Adapter for displaying student accommodation options.
 */
class AccommodationAdapter : ListAdapter<Accommodation, AccommodationAdapter.AccommodationViewHolder>(AccommodationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccommodationViewHolder {
        val binding = ItemAccommodationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AccommodationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AccommodationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AccommodationViewHolder(private val binding: ItemAccommodationBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(accommodation: Accommodation) {
            binding.tvAccommodationName.text = accommodation.name
            binding.tvLocation.text = accommodation.location
            binding.tvPrice.text = "Price: ${accommodation.price}"
            binding.tvDescription.text = accommodation.description
        }
    }

    class AccommodationDiffCallback : DiffUtil.ItemCallback<Accommodation>() {
        override fun areItemsTheSame(oldItem: Accommodation, newItem: Accommodation): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Accommodation, newItem: Accommodation): Boolean = oldItem == newItem
    }
}