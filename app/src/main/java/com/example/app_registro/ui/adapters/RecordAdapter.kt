package com.example.app_registro.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.app_registro.R
import com.example.app_registro.data.PaymentStatus
import com.example.app_registro.data.Record
import com.example.app_registro.databinding.ItemRecordBinding
import com.example.app_registro.ui.AlarmPickerHelper
import java.util.Date

class RecordAdapter(
    private val onEdit: (Record) -> Unit,
    private val onDelete: (Record) -> Unit
) : ListAdapter<Record, RecordAdapter.RecordViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        val binding = ItemRecordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RecordViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RecordViewHolder(private val binding: ItemRecordBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())

        fun bind(record: Record) {
            binding.productNameText.text = record.productName
            binding.metaText.text = binding.root.context.getString(
                R.string.record_meta,
                record.code,
                record.quantity,
                record.responsible,
                record.storeName
            )
            binding.dateText.text = dateFormat.format(Date(record.createdAtMillis))
            binding.alarmText.text = if (record.alarmAtMillis == null) {
                binding.root.context.getString(R.string.no_alarm_selected)
            } else {
                binding.root.context.getString(
                    R.string.alarm_item_text,
                    AlarmPickerHelper.formatAlarm(record.alarmAtMillis)
                )
            }
            binding.statusChip.text = when (record.paymentStatus) {
                PaymentStatus.PENDIENTE -> binding.root.context.getString(R.string.pending)
                PaymentStatus.PAGADO_PARCIAL -> binding.root.context.getString(R.string.partial_paid)
                PaymentStatus.PAGADO -> binding.root.context.getString(R.string.paid)
            }
            binding.editButton.setOnClickListener { onEdit(record) }
            binding.deleteButton.setOnClickListener { onDelete(record) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<Record>() {
        override fun areItemsTheSame(oldItem: Record, newItem: Record): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Record, newItem: Record): Boolean = oldItem == newItem
    }
}
