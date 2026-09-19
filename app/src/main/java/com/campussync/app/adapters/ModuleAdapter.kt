package com.campussync.app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.campussync.app.databinding.ItemModuleBinding
import com.campussync.app.models.Module

/**
 * Adapter for the Modules RecyclerView.
 */
class ModuleAdapter(
    private val onEditClick: (Module) -> Unit,
    private val onDeleteClick: (Module) -> Unit
) : ListAdapter<Module, ModuleAdapter.ModuleViewHolder>(ModuleDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModuleViewHolder {
        val binding = ItemModuleBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ModuleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ModuleViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ModuleViewHolder(private val binding: ItemModuleBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(module: Module) {
            binding.tvModuleCode.text = module.code
            binding.tvModuleName.text = module.name
            binding.tvTargetClasses.text = "Target: ${module.targetClassesPerWeek} classes per week"

            binding.root.setOnClickListener { onEditClick(module) }
            binding.root.setOnLongClickListener {
                onDeleteClick(module)
                true
            }
        }
    }

    class ModuleDiffCallback : DiffUtil.ItemCallback<Module>() {
        override fun areItemsTheSame(oldItem: Module, newItem: Module): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Module, newItem: Module): Boolean =
            oldItem == newItem
    }
}
