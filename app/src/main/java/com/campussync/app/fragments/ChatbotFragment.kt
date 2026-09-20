package com.campussync.app.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
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
 * Intelligent AI Chatbot that provides analytical advice on academics and finances.
 * Saves and loads chat history from Firestore for a persistent experience.
 */
class ChatbotFragment : Fragment() {

    private var _binding: FragmentChatbotBinding? = null
    private val binding get() = _binding!!

    private val chatAdapter = ChatAdapter()
    private val geminiRepo = GeminiRepository()
    private val chatRepo = ChatRepository()

    private val authRepo = AuthRepository()
    private val timetableRepo = TimetableRepository()
    private val markRepo = MarkRepository()
    private val budgetRepo = BudgetRepository()
    private val moduleRepo = ModuleRepository()

    private val chatHistory = mutableListOf<ChatMessage>()
    private val TAG = "ChatbotFragment"

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
                        Log.e(TAG, "Failed to load history", e)
                        addMessage("Ready. (History unavailable)", false, saveToFirestore = false)
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
                    SYSTEM INSTRUCTION: You are a direct Academic and Financial Analyst.
                    1. Respond ONLY to the question asked. 
                    2. NO greetings, NO filler, NO conversational "fluff".
                    3. If asked for a budget/money, do NOT just state the balance. Calculate a daily spending limit for the remaining days of the month and suggest which categories to prioritize.
                    4. If asked about grades, identify the weakest subject and suggest one specific study action.
                    5. If asked about schedule, identify the next most important gap or class.
                    6. Limit response to 25 words max.
                    
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
                    Log.e(TAG, "AI process failed", e)
                }
            }
        }
    }

    private suspend fun buildStudentContext(): String {
        val sb = StringBuilder()
        val now = Calendar.getInstance()
        val daysInMonth = now.getActualMaximum(Calendar.DAY_OF_MONTH)
        val currentDay = now.get(Calendar.DAY_OF_MONTH)
        val daysLeft = daysInMonth - currentDay + 1

        sb.append("Context: Day $currentDay/$daysInMonth ($daysLeft left). ")
        
        authRepo.getCurrentUserProfile().onSuccess { user ->
            user?.let { sb.append("Student: ${it.firstName}. ") }
        }
        
        val marks = markRepo.getMarkEntries().first()
        sb.append("Avg: ${String.format("%.1f", markRepo.calculateWeightedAverage(marks))}%. Marks: ${marks.joinToString { "${it.moduleCode}:${it.mark}%" }}. ")
        
        budgetRepo.getBudgetSettings().onSuccess { settings ->
            val allowance = settings?.monthlyAllowance ?: 0.0
            val expenses = budgetRepo.getCurrentMonthExpenses().first()
            val spent = expenses.sumOf { it.amount }
            sb.append("Finance: R${allowance - spent} left of R$allowance. ")
        }
        
        val timetable = timetableRepo.getTimetableEntries().first()
        sb.append("Schedule: ${timetable.take(5).joinToString { "${it.dayOfWeek} ${it.startTime}(${it.moduleCode})" }}.")
        
        return sb.toString()
    }

    private fun addMessage(content: String, isUser: Boolean, saveToFirestore: Boolean) {
        if (_binding == null) return
        val message = ChatMessage(content, isUser)
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
