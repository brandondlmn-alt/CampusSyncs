package com.campussync.app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.campussync.app.databinding.DialogSetReminderBinding
import com.campussync.app.models.Assessment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ReminderBottomSheet(
    private val assessment: Assessment,
    private val onReminderSelected: (Int) -> Unit
) : BottomSheetDialogFragment() {

    private lateinit var binding: DialogSetReminderBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DialogSetReminderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.option1Hour.setOnClickListener { selectReminder(60); dismiss() }
        binding.option3Hours.setOnClickListener { selectReminder(180); dismiss() }
        binding.option1Day.setOnClickListener { selectReminder(1440); dismiss() }
        binding.optionDueTime.setOnClickListener { selectReminder(0); dismiss() }
        binding.optionCancel.setOnClickListener { selectReminder(-1); dismiss() }
    }

    private fun selectReminder(minutes: Int) {
        onReminderSelected(minutes)
    }
}