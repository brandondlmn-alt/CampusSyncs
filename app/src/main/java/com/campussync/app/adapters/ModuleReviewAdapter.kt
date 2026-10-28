package com.campussync.app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.campussync.app.databinding.ItemModuleReviewBinding
import com.campussync.app.models.EnrolledModule

/**
 * Adapter to display and allow editing of auto-assigned modules during registration review.
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
        
        // Remove listeners before setting text to avoid infinite loops or wrong data updates
        holder.binding.etModuleCode.setText(module.code)
        holder.binding.etModuleName.setText(module.name)

        holder.binding.etModuleCode.doOnTextChanged { text, _, _, _ ->
            module.code = text.toString().trim()
        }

        holder.binding.etModuleName.doOnTextChanged { text, _, _, _ ->
            module.name = text.toString().trim()
        }
    }

    override fun getItemCount() = modules.size

    fun getModules(): List<EnrolledModule> = modules
}
