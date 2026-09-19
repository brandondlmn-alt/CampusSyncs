package com.campussync.app.activities

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.campussync.app.adapters.ModuleAdapter
import com.campussync.app.data.ModuleRepository
import com.campussync.app.databinding.ActivityModulesBinding
import com.campussync.app.databinding.DialogAddModuleBinding
import com.campussync.app.models.Module
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/**
 * Activity for students to manage their academic modules.
 * Defines module codes, names, and target classes per week.
 */
class ModulesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityModulesBinding
    private val repository = ModuleRepository()
    private lateinit var adapter: ModuleAdapter
    private val TAG = "ModulesActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityModulesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        observeModules()

        binding.fabAddModule.setOnClickListener {
            showAddEditDialog(null)
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        adapter = ModuleAdapter(
            onEditClick = { module -> showAddEditDialog(module) },
            onDeleteClick = { module -> confirmDeletion(module) }
        )
        binding.rvModules.adapter = adapter
    }

    private fun observeModules() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            repository.getModules().collect { modules ->
                binding.progressBar.visibility = View.GONE
                adapter.submitList(modules)
                binding.tvEmptyState.visibility = if (modules.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun showAddEditDialog(module: Module?) {
        val dialogBinding = DialogAddModuleBinding.inflate(layoutInflater)
        val isEdit = module != null

        if (isEdit) {
            dialogBinding.tvDialogTitle.text = "Edit Module"
            dialogBinding.etModuleCode.setText(module?.code)
            dialogBinding.etModuleName.setText(module?.name)
            dialogBinding.etTargetClasses.setText(module?.targetClassesPerWeek.toString())
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setPositiveButton(if (isEdit) "Update" else "Add", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            button.setOnClickListener {
                val code = dialogBinding.etModuleCode.text.toString().trim()
                val name = dialogBinding.etModuleName.text.toString().trim()
                val target = dialogBinding.etTargetClasses.text.toString().toIntOrNull()

                if (code.isEmpty() || name.isEmpty() || target == null) {
                    Toast.makeText(this@ModulesActivity, "Please fill all fields", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val newModule = (module ?: Module()).copy(
                    code = code,
                    name = name,
                    targetClassesPerWeek = target
                )

                lifecycleScope.launch {
                    val result = if (isEdit) repository.updateModule(newModule) else repository.addModule(newModule)
                    result.fold(
                        onSuccess = {
                            dialog.dismiss()
                            Snackbar.make(binding.root, "Module saved", Snackbar.LENGTH_SHORT).show()
                        },
                        onFailure = { e ->
                            Log.e(TAG, "Save failed", e)
                            Toast.makeText(this@ModulesActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
        dialog.show()
    }

    private fun confirmDeletion(module: Module) {
        AlertDialog.Builder(this)
            .setTitle("Delete Module")
            .setMessage("Remove ${module.code}? This will not delete marks or timetable entries associated with it.")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    repository.deleteModule(module.id).fold(
                        onSuccess = { Snackbar.make(binding.root, "Module removed", Snackbar.LENGTH_SHORT).show() },
                        onFailure = { Toast.makeText(this@ModulesActivity, "Delete failed", Toast.LENGTH_SHORT).show() }
                    )
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
