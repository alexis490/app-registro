package com.example.app_registro.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.app_registro.data.Product
import com.example.app_registro.databinding.ItemProductBinding

class ProductAdapter(
    private val onEdit: (Product) -> Unit,
    private val onDelete: (Product) -> Unit
) : ListAdapter<Product, ProductAdapter.ProductViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ProductViewHolder(private val binding: ItemProductBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(product: Product) {
            binding.productNameText.text = product.name
            binding.productCodeText.text = product.code
            binding.editButton.setOnClickListener { onEdit(product) }
            binding.deleteButton.setOnClickListener { onDelete(product) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<Product>() {
        override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean = oldItem.code == newItem.code

        override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean = oldItem == newItem
    }
}
