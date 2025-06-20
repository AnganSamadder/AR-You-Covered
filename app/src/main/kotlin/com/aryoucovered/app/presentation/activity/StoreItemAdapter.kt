package com.aryoucovered.app.presentation.activity

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.aryoucovered.app.R

class StoreItemAdapter(
    private val items: List<StoreActivity.StoreItem>,
    private val onItemClick: (StoreActivity.StoreItem) -> Unit
) : RecyclerView.Adapter<StoreItemAdapter.StoreItemViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoreItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_store, parent, false)
        return StoreItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: StoreItemViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item, onItemClick)
    }

    override fun getItemCount(): Int = items.size

    class StoreItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val itemImage: ImageView = itemView.findViewById(R.id.item_image)
        private val itemName: TextView = itemView.findViewById(R.id.item_name)
        private val itemPrice: TextView = itemView.findViewById(R.id.item_price)

        fun bind(item: StoreActivity.StoreItem, onItemClick: (StoreActivity.StoreItem) -> Unit) {
            itemName.text = item.name
            itemPrice.text = "${item.price} pts"
            
            // Use the actual image for the item
            val imageResource = getImageResourceForItem(item.imageFileName)
            itemImage.setImageResource(imageResource)
            
            // Set click listener on the entire item view
            itemView.setOnClickListener {
                android.util.Log.d("StoreItemAdapter", "Item clicked: ${item.name}")
                onItemClick(item)
            }
            
            // Also set click listener on the inner container to ensure it works
            val innerContainer = itemView.findViewById<LinearLayout>(R.id.store_item_container)
            innerContainer?.setOnClickListener {
                android.util.Log.d("StoreItemAdapter", "Inner container clicked: ${item.name}")
                onItemClick(item)
            }
        }
        
        private fun getImageResourceForItem(fileName: String): Int {
            return when (fileName) {
                "nike_hoodie.jpg" -> R.drawable.nike_hoodie
                "stuffed_animal.jpg" -> R.drawable.stuffed_animal
                "scrunchie.jpg" -> R.drawable.scrunchie
                "aviator_glasses.jpg" -> R.drawable.aviator_glasses
                "earrings.jpg" -> R.drawable.earrings
                "cap.jpg" -> R.drawable.cap
                "sporty_shirt.jpg" -> R.drawable.sporty_shirt
                "cropped_hoodie.jpg" -> R.drawable.cropped_hoodie
                else -> R.drawable.sf_logo
            }
        }
    }
} 