package com.campussync.app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.campussync.app.databinding.ItemModuleReviewBinding
import com.campussync.app.models.EnrolledModule

/**
 * Read-only adapter to display auto-assigned modules during registration review.
 */
class ModuleReviewAdapter(private val modules: List<EnrolledModule>) :
    RecyclerView.Adapter<ModuleReviewAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemModuleReviewBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemModuleReviewBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val module = modules[position]
        holder.binding.tvModuleCode.text = module.code
        holder.binding.tvModuleName.text = module.name
    }

    override fun getItemCount() = modules.size
}
