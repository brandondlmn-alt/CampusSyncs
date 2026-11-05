package com.campussync.app.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.campussync.app.adapters.ChatAdapter
import com.campussync.app.data.*
import com.campussync.app.databinding.FragmentChatbotBinding
import com.campussync.app.models.ChatMessage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * AI Chatbot Fragment that provides analytical advice on academics and finances.
 */
class ChatbotFragment : Fragment() {

    private var _binding: FragmentChatbotBinding? = null
    private val binding get() = _binding!!

    private val geminiRepo = GeminiRepository()
    private val chatRepo = ChatRepository()

    private val authRepo = AuthRepository()
    private val timetableRepo = TimetableRepository()
    private val markRepo = MarkRepository()
    private val budgetRepo = BudgetRepository()

    private val chatHistory = mutableListOf<ChatMessage>()
    private val TAG = "ChatbotFragment"

    // Initialize adapter with Delete callback
    private val chatAdapter = ChatAdapter { message ->
        showDeleteConfirmationDialog(message)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentChatbotBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupInput()
        loadChatHistory()
    }

    private fun setupRecyclerView() {
        binding.rvChat.adapter = chatAdapter
    }

    private fun setupInput() {
        binding.btnSend.setOnClickListener { sendMessage() }
        binding.etMessage.setOnEditorActionListener { _, id, _ ->
            if (id == EditorInfo.IME_ACTION_SEND) { sendMessage(); true } else false
        }
    }

    private fun loadChatHistory() {
        setLoading(true)
        viewLifecycleOwner.lifecycleScope.launch {
            val result = chatRepo.getChatHistory()
            if (_binding != null) {
                setLoading(false)
                result.fold(
                    onSuccess = { history ->
                        chatHistory.clear()
                        chatHistory.addAll(history)
                        if (chatHistory.isEmpty()) {
                            addMessage("Ready. Ask for a budget plan or academic review.", false, saveToFirestore = true)
                        } else {
                            chatAdapter.submitList(chatHistory.toList()) {
                                _binding?.rvChat?.scrollToPosition(chatHistory.size - 1)
                            }
                        }
                    },
                    onFailure = { e ->
                        Log.e(TAG, "History load failed", e)
                        addMessage("Assistant ready.", false, saveToFirestore = false)
                    }
                )
            }
        }
    }

    private fun sendMessage() {
        val userText = binding.etMessage.text.toString().trim()
        if (userText.isEmpty()) return

        addMessage(userText, true, saveToFirestore = true)
        binding.etMessage.setText("")

        setLoading(true)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val context = buildStudentContext()

                val fullPrompt = """
                    SYSTEM INSTRUCTION: You are a direct Academic and Financial Analyst for a university student.
                    1. Respond ONLY to the question asked. 
                    2. NO greetings, NO conversational filler.
                    3. If asked for a budget/money, analyze categories and provide a daily spending limit.
                    4. If asked about grades, identify the weakest subject and suggest one action.
                    5. Keep response under 30 words.
                    
                    STUDENT DATA:
                    $context
                    
                    QUESTION:
                    $userText
                """.trimIndent()

                val result = geminiRepo.generateContent(fullPrompt)

                if (_binding != null) {
                    setLoading(false)
                    result.fold(
                        onSuccess = { addMessage(it.trim(), false, saveToFirestore = true) },
                        onFailure = { Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT).show() }
                    )
                }
            } catch (e: Exception) {
                if (_binding != null) {
                    setLoading(false)
                    Log.e(TAG, "AI processing failed", e)
                }
            }
        }
    }

    /**
     * Reads the Firestore user profile and data to build a personalized context.
     */
    private suspend fun buildStudentContext(): String {
        val sb = StringBuilder()
        val now = Calendar.getInstance()
        val daysLeft = now.getActualMaximum(Calendar.DAY_OF_MONTH) - now.get(Calendar.DAY_OF_MONTH) + 1

        sb.append("Month Progress: Day ${now.get(Calendar.DAY_OF_MONTH)}, $daysLeft days left. ")

        // Read User Profile for Personalization
        authRepo.getCurrentUserProfile().onSuccess { user ->
            user?.let {
                sb.append("Student Name: ${it.firstName}. ")
                if (it.course.isNotEmpty()) sb.append("Course: ${it.course}, Year ${it.yearOfStudy}. ")
            }
        }

        // Read Marks
        val marks = markRepo.getMarkEntries().first()
        if (marks.isNotEmpty()) {
            sb.append("Average: ${String.format("%.1f", markRepo.calculateWeightedAverage(marks))}%. Grades: ${marks.joinToString { "${it.moduleCode}:${it.mark}%" }}. ")
        }

        // Read Budget
        val settingsResult = budgetRepo.getBudgetSettings()
        val allowance = settingsResult.getOrNull()?.monthlyAllowance ?: 0.0
        val expenses = budgetRepo.getCurrentMonthExpenses().first()
        val spent = expenses.sumOf { it.amount }
        val catSummary = expenses.groupBy { it.category }.mapValues { it.value.sumOf { exp -> exp.amount } }

        sb.append("Budget: R${allowance - spent} remaining of R$allowance. Categories: $catSummary. ")

        // Read Timetable
        val timetable = timetableRepo.getTimetableEntries().first()
        if (timetable.isNotEmpty()) {
            sb.append("Upcoming classes: ${timetable.take(3).joinToString { "${it.dayOfWeek} ${it.startTime}(${it.moduleCode})" }}.")
        }

        return sb.toString()
    }

    private fun addMessage(content: String, isUser: Boolean, saveToFirestore: Boolean) {
        if (_binding == null) return
        val message = ChatMessage(content = content, isUser = isUser)
        chatHistory.add(message)
        chatAdapter.submitList(chatHistory.toList()) {
            _binding?.rvChat?.scrollToPosition(chatHistory.size - 1)
        }

        if (saveToFirestore) {
            viewLifecycleOwner.lifecycleScope.launch {
                chatRepo.saveMessage(message)
            }
        }
    }

    /**
     * Shows a dialog to confirm message deletion.
     */
    private fun showDeleteConfirmationDialog(message: ChatMessage) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Message")
            .setMessage("Are you sure you want to delete this message?")
            .setPositiveButton("Delete") { _, _ ->
                deleteMessage(message)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Deletes the message from Firestore and updates the UI.
     */
    private fun deleteMessage(message: ChatMessage) {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = chatRepo.deleteMessage(message.id)
            result.fold(
                onSuccess = {
                    chatHistory.remove(message)
                    chatAdapter.submitList(chatHistory.toList())
                    Toast.makeText(requireContext(), "Message deleted", Toast.LENGTH_SHORT).show()
                },
                onFailure = { e ->
                    Log.e(TAG, "Failed to delete message", e)
                    Toast.makeText(requireContext(), "Failed to delete: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun setLoading(isLoading: Boolean) {
        if (_binding == null) return
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnSend.isEnabled = !isLoading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}