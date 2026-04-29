package com.example.app_registro.ui

import android.app.AlertDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.app_registro.R
import com.example.app_registro.data.LocalStore
import com.example.app_registro.data.PaymentStatus
import com.example.app_registro.data.Record
import com.example.app_registro.databinding.ActivityRecordsBinding
import com.example.app_registro.databinding.DialogEditRecordBinding
import com.example.app_registro.notifications.ReminderScheduler
import com.example.app_registro.ui.adapters.RecordAdapter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecordsActivity : BaseActivity() {
    private lateinit var binding: ActivityRecordsBinding
    private lateinit var adapter: RecordAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireUser()

        binding = ActivityRecordsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = RecordAdapter(
            onEdit = { showEditDialog(it) },
            onDelete = {
                ReminderScheduler.cancelRecordAlarm(this, it.id)
                LocalStore.deleteRecord(it.id)
                refreshRecords()
            }
        )

        binding.recordsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.recordsRecyclerView.adapter = adapter
        binding.toolbar.setNavigationOnClickListener { finish() }
        refreshRecords()
    }

    override fun onResume() {
        super.onResume()
        refreshRecords()
    }

    private fun refreshRecords() {
        val records = LocalStore.getRecords()
        adapter.submitList(records)
        binding.emptyStateText.isVisible = records.isEmpty()
    }

    private fun showEditDialog(record: Record) {
        val dialogBinding = DialogEditRecordBinding.inflate(layoutInflater)
        val statuses = PaymentStatus.entries.toList()
        val labels = statuses.map { paymentStatusLabel(it) }
        dialogBinding.productNameText.text = record.productName
        dialogBinding.codeText.text = record.code
        dialogBinding.dateText.text = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            .format(Date(record.createdAtMillis))
        dialogBinding.quantityEditText.setText(record.quantity.toString())
        dialogBinding.responsibleEditText.setText(record.responsible)
        dialogBinding.storeEditText.setText(record.storeName)
        var selectedAlarmAtMillis = record.alarmAtMillis
        dialogBinding.paymentStatusSpinner.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, labels)
        dialogBinding.paymentStatusSpinner.setSelection(statuses.indexOf(record.paymentStatus))
        dialogBinding.alarmSummaryText.text = if (selectedAlarmAtMillis == null) {
            getString(R.string.no_alarm_selected)
        } else {
            getString(R.string.alarm_selected, AlarmPickerHelper.formatAlarm(selectedAlarmAtMillis))
        }
        dialogBinding.selectAlarmButton.setOnClickListener {
            AlarmPickerHelper.showTimePicker(this, selectedAlarmAtMillis) { selectedMillis ->
                selectedAlarmAtMillis = selectedMillis
                dialogBinding.alarmSummaryText.text =
                    getString(R.string.alarm_selected, AlarmPickerHelper.formatAlarm(selectedAlarmAtMillis))
            }
        }
        dialogBinding.clearAlarmButton.setOnClickListener {
            selectedAlarmAtMillis = null
            dialogBinding.alarmSummaryText.text = getString(R.string.no_alarm_selected)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.edit_record)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.save, null)
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val quantity = dialogBinding.quantityEditText.text.toString().toIntOrNull()
                val responsible = dialogBinding.responsibleEditText.text.toString().trim()
                val storeName = dialogBinding.storeEditText.text.toString().trim()
                if (quantity == null || quantity <= 0 || responsible.isBlank() || storeName.isBlank()) {
                    Toast.makeText(this, R.string.complete_fields, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val updatedRecord = LocalStore.createRecord(
                    productName = record.productName,
                    code = record.code,
                    quantity = quantity,
                    responsible = responsible,
                    storeName = storeName,
                    alarmAtMillis = selectedAlarmAtMillis,
                    paymentStatus = statuses[dialogBinding.paymentStatusSpinner.selectedItemPosition],
                    createdBy = record.createdBy,
                    existingId = record.id,
                    createdAtMillis = record.createdAtMillis
                )
                LocalStore.saveRecord(updatedRecord)
                ReminderScheduler.scheduleRecordAlarm(this, updatedRecord)
                refreshRecords()
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun paymentStatusLabel(status: PaymentStatus): String {
        return when (status) {
            PaymentStatus.PENDIENTE -> getString(R.string.pending)
            PaymentStatus.PAGADO_PARCIAL -> getString(R.string.partial_paid)
            PaymentStatus.PAGADO -> getString(R.string.paid)
        }
    }
}
