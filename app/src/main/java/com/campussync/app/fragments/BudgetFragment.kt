package com.campussync.app.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.campussync.app.R
import com.campussync.app.adapters.ExpenseAdapter
import com.campussync.app.data.BudgetRepository
import com.campussync.app.databinding.DialogAddExpenseBinding
import com.campussync.app.databinding.FragmentBudgetBinding
import com.campussync.app.models.BudgetSettings
import com.campussync.app.models.Expense
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Fragment for tracking student expenses and managing a monthly budget.
 * Displays a circular progress of the remaining budget and a list of current month expenses.
 */
class BudgetFragment : Fragment() {

    private var _binding: FragmentBudgetBinding? = null
    private val binding get() = _binding!!

    private val repository = BudgetRepository()
    private lateinit var adapter: ExpenseAdapter
    private var budgetSettings: BudgetSettings? = null
    private var currentExpenses: List<Expense> = emptyList()
    private val TAG = "BudgetFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBudgetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadBudgetSettings()
        observeExpenses()

        binding.fabAddExpense.setOnClickListener {
            showAddEditExpenseDialog(null)
        }

        binding.btnEditBudget.setOnClickListener {
            showSetAllowanceDialog()
        }
    }

    private fun setupRecyclerView() {
        adapter = ExpenseAdapter(
            onEditClick = { expense -> showAddEditExpenseDialog(expense) },
            onDeleteClick = { expense -> confirmDeletion(expense) }
        )
        binding.rvExpenses.adapter = adapter
    }

    private fun loadBudgetSettings() {
        lifecycleScope.launch {
            repository.getBudgetSettings().fold(
                onSuccess = { settings ->
                    budgetSettings = settings
                    updateBudgetUI(currentExpenses)
                },
                onFailure = { e ->
                    Log.e(TAG, "Failed to load budget settings", e)
                }
            )
        }
    }

    private fun observeExpenses() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            repository.getCurrentMonthExpenses().collect { expenses ->
                currentExpenses = expenses
                binding.progressBar.visibility = View.GONE
                adapter.submitList(expenses)
                updateBudgetUI(expenses)
                binding.tvEmptyState.visibility = if (expenses.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun updateBudgetUI(expenses: List<Expense>) {
        val allowance = budgetSettings?.monthlyAllowance ?: 0.0
        val totalSpent = expenses.sumOf { it.amount }
        val remaining = allowance - totalSpent
        
        binding.tvBudgetStatus.text = String.format(
            Locale.getDefault(),
            "R %.2f remaining of R %.2f",
            remaining,
            allowance
        )

        if (allowance > 0) {
            val progress = ((totalSpent / allowance) * 100).toInt()
            binding.progressBudget.progress = progress.coerceIn(0, 100)
            
            // Visual feedback: change color if over budget
            if (totalSpent > allowance) {
                binding.progressBudget.setIndicatorColor(resources.getColor(android.R.color.holo_red_dark, null))
            } else {
                binding.progressBudget.setIndicatorColor(resources.getColor(R.color.colorPrimary, null))
            }
        } else {
            binding.progressBudget.progress = 0
        }
    }

    private fun showSetAllowanceDialog() {
        val editText = TextInputEditText(requireContext())
        editText.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        editText.setText(budgetSettings?.monthlyAllowance?.toString() ?: "")
        
        AlertDialog.Builder(requireContext())
            .setTitle("Set Monthly Allowance")
            .setMessage("Enter your total budget for the month:")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val amount = editText.text.toString().toDoubleOrNull() ?: 0.0
                lifecycleScope.launch {
                    repository.updateBudgetSettings(amount).fold(
                        onSuccess = {
                            budgetSettings = BudgetSettings(monthlyAllowance = amount)
                            Snackbar.make(binding.root, "Allowance updated", Snackbar.LENGTH_SHORT).show()
                            updateBudgetUI(currentExpenses)
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
        val categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
        dialogBinding.actvCategory.setAdapter(categoryAdapter)

        if (isEdit) {
            dialogBinding.tvDialogTitle.text = "Edit Expense"
            dialogBinding.actvCategory.setText(expense?.category, false)
            dialogBinding.etAmount.setText(expense?.amount.toString())
            dialogBinding.etDescription.setText(expense?.description)
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .setPositiveButton(if (isEdit) "Update" else "Add", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            button.setOnClickListener {
                val category = dialogBinding.actvCategory.text.toString()
                val amount = dialogBinding.etAmount.text.toString().toDoubleOrNull()
                val desc = dialogBinding.etDescription.text.toString().trim()

                if (category.isEmpty() || amount == null) {
                    Toast.makeText(requireContext(), "Please enter category and amount", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val newExpense = (expense ?: Expense()).copy(
                    category = category,
                    amount = amount,
                    description = desc
                )

                lifecycleScope.launch {
                    val result = if (isEdit) repository.updateExpense(newExpense) else repository.addExpense(newExpense)
                    result.fold(
                        onSuccess = {
                            dialog.dismiss()
                            Snackbar.make(binding.root, "Expense saved", Snackbar.LENGTH_SHORT).show()
                        },
                        onFailure = { e ->
                            Log.e(TAG, "Save failed", e)
                            Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
        dialog.show()
    }

    private fun confirmDeletion(expense: Expense) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Expense")
            .setMessage("Remove this expense: ${expense.description}?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    repository.deleteExpense(expense.id).fold(
                        onSuccess = { Snackbar.make(binding.root, "Expense removed", Snackbar.LENGTH_SHORT).show() },
                        onFailure = { Toast.makeText(requireContext(), "Delete failed", Toast.LENGTH_SHORT).show() }
                    )
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
