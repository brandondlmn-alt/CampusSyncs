package com.campussync.app.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.campussync.app.R
import com.campussync.app.adapters.ExpenseAdapter
import com.campussync.app.data.BudgetRepository
import com.campussync.app.databinding.DialogAddExpenseBinding
import com.campussync.app.databinding.FragmentBudgetBinding
import com.campussync.app.models.BudgetSettings
import com.campussync.app.models.Expense
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import java.util.Locale

class BudgetFragment : Fragment() {

    private var _binding: FragmentBudgetBinding? = null
    private val binding get() = _binding!!

    private val repository = BudgetRepository()
    private lateinit var adapter: ExpenseAdapter
    private var budgetSettings: BudgetSettings? = null
    private var currentExpenses: List<Expense> = emptyList()
    private val TAG = "BudgetFragment"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBudgetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        loadBudgetSettings()
        observeExpenses()

        binding.fabAddExpense.setOnClickListener { showAddEditExpenseDialog(null) }
        binding.btnEditBudget.setOnClickListener { showSetAllowanceDialog() }
    }

    private fun setupRecyclerView() {
        adapter = ExpenseAdapter(
            onEditClick = { expense -> showAddEditExpenseDialog(expense) },
            onDeleteClick = { expense -> confirmDeletion(expense) }
        )
        binding.rvExpenses.adapter = adapter
    }

    private fun loadBudgetSettings() {
        viewLifecycleOwner.lifecycleScope.launch {
            repository.getBudgetSettings().fold(
                onSuccess = { settings ->
                    if (_binding != null) {
                        budgetSettings = settings
                        updateBudgetUI(currentExpenses)
                    }
                },
                onFailure = { e -> Log.e(TAG, "Failed to load budget settings", e) }
            )
        }
    }

    private fun observeExpenses() {
        if (_binding == null) return
        binding.progressBar.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                repository.getCurrentMonthExpenses().collect { expenses ->
                    if (_binding != null) {
                        currentExpenses = expenses
                        binding.progressBar.visibility = View.GONE
                        adapter.submitList(expenses)
                        updateBudgetUI(expenses)
                        binding.tvEmptyState.visibility = if (expenses.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
            } catch (e: Exception) {
                if (_binding != null) {
                    binding.progressBar.visibility = View.GONE
                    Log.e(TAG, "Error observing expenses", e)
                    Toast.makeText(requireContext(), "Failed to load expenses.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateBudgetUI(expenses: List<Expense>) {
        if (_binding == null) return
        val allowance = budgetSettings?.monthlyAllowance ?: 0.0
        val totalSpent = expenses.sumOf { it.amount }
        val remaining = allowance - totalSpent
        
        binding.tvBudgetStatus.text = String.format(Locale.getDefault(), "R %.2f remaining of R %.2f", remaining, allowance)

        if (allowance > 0) {
            val progress = ((totalSpent / allowance) * 100).toInt()
            binding.progressBudget.progress = progress.coerceIn(0, 100)
            if (totalSpent > allowance) {
                binding.progressBudget.setIndicatorColor(ContextCompat.getColor(requireContext(), R.color.accent_red))
            } else {
                binding.progressBudget.setIndicatorColor(ContextCompat.getColor(requireContext(), R.color.primary_blue))
            }
        } else {
            binding.progressBudget.progress = 0
        }
    }

    private fun showSetAllowanceDialog() {
        val container = FrameLayout(requireContext())
        val lp = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.setMargins(28.dpToPx(), 16.dpToPx(), 28.dpToPx(), 0.dpToPx())
        
        val til = TextInputLayout(requireContext(), null, com.google.android.material.R.style.Widget_Material3_TextInputLayout_OutlinedBox)
        til.layoutParams = lp
        til.hint = "Monthly Allowance (R)"
        til.setBoxCornerRadii(20.dpToPx().toFloat(), 20.dpToPx().toFloat(), 20.dpToPx().toFloat(), 20.dpToPx().toFloat())
        
        val editText = TextInputEditText(til.context)
        editText.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        editText.setText(budgetSettings?.monthlyAllowance?.toString() ?: "")
        til.addView(editText)
        container.addView(til)
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Set Monthly Allowance")
            .setView(container)
            .setPositiveButton("Save") { _, _ ->
                val amount = editText.text.toString().toDoubleOrNull() ?: 0.0
                viewLifecycleOwner.lifecycleScope.launch {
                    repository.updateBudgetSettings(amount).fold(
                        onSuccess = {
                            budgetSettings = BudgetSettings(monthlyAllowance = amount)
                            Snackbar.make(binding.root, "Allowance updated", Snackbar.LENGTH_SHORT).show()
                            loadBudgetSettings()
                        },
                        onFailure = { Toast.makeText(requireContext(), "Update failed", Toast.LENGTH_SHORT).show() }
                    )
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddEditExpenseDialog(expense: Expense?) {
        val dialogBinding = DialogAddExpenseBinding.inflate(layoutInflater)
        val isEdit = expense != null
        val categories = arrayOf("Food", "Transport", "Printing", "Entertainment", "Other")
        dialogBinding.actvCategory.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories))

        if (isEdit) {
            dialogBinding.tvDialogTitle.text = "Edit Expense"
            dialogBinding.actvCategory.setText(expense?.category, false)
            dialogBinding.etAmount.setText(expense?.amount.toString())
            dialogBinding.etDescription.setText(expense?.description)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setPositiveButton(if (isEdit) "Update" else "Add", null)
            .setNegativeButton("Cancel", null)
            .create().apply {
                setOnShowListener {
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val category = dialogBinding.actvCategory.text.toString()
                        val amount = dialogBinding.etAmount.text.toString().toDoubleOrNull()
                        val desc = dialogBinding.etDescription.text.toString().trim()

                        if (category.isEmpty() || amount == null) {
                            Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }

                        val newExpense = (expense ?: Expense()).copy(category = category, amount = amount, description = desc)
                        viewLifecycleOwner.lifecycleScope.launch {
                            val result = if (isEdit) repository.updateExpense(newExpense) else repository.addExpense(newExpense)
                            if (_binding != null) {
                                result.fold(
                                    onSuccess = { dismiss(); Snackbar.make(binding.root, "Expense saved", Snackbar.LENGTH_SHORT).show() },
                                    onFailure = { e -> Log.e(TAG, "Save failed", e); Toast.makeText(requireContext(), "Error saving", Toast.LENGTH_SHORT).show() }
                                )
                            }
                        }
                    }
                }
                show()
            }
    }

    private fun confirmDeletion(expense: Expense) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Expense")
            .setMessage("Remove this expense?")
            .setPositiveButton("Delete") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    val result = repository.deleteExpense(expense.id)
                    if (_binding != null) {
                        result.fold(
                            onSuccess = { Snackbar.make(binding.root, "Expense removed", Snackbar.LENGTH_SHORT).show() },
                            onFailure = { Toast.makeText(requireContext(), "Delete failed", Toast.LENGTH_SHORT).show() }
                        )
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
