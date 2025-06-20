package com.aryoucovered.app.presentation.activity

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aryoucovered.app.R

class CollectionItemAdapter(
    private val items: List<CollectionsActivity.CollectionItem>,
    private val itemType: ItemType = ItemType.ARTIFACT
) : RecyclerView.Adapter<CollectionItemAdapter.CollectionItemViewHolder>() {

    enum class ItemType {
        ARTIFACT, PERSON
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CollectionItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_collection, parent, false)
        return CollectionItemViewHolder(view, itemType)
    }

    override fun onBindViewHolder(holder: CollectionItemViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class CollectionItemViewHolder(
        itemView: View, 
        private val itemType: ItemType
    ) : RecyclerView.ViewHolder(itemView) {
        
        private val questionMark: TextView = itemView.findViewById(R.id.question_mark)
        private val itemName: TextView = itemView.findViewById(R.id.item_name)
        private val itemContainer: FrameLayout = itemView.findViewById(R.id.item_container)

        init {
            // Adjust height based on item type
            val layoutParams = itemContainer.layoutParams
            layoutParams.height = when (itemType) {
                ItemType.PERSON -> dpToPx(200) // Much taller vertical rectangle for people
                ItemType.ARTIFACT -> dpToPx(120) // Square-ish for artifacts
            }
            itemContainer.layoutParams = layoutParams
        }

        fun bind(item: CollectionsActivity.CollectionItem) {
            itemName.text = item.name
            
            // Show question mark for all items (reverted from silhouettes)
            questionMark.visibility = View.VISIBLE
            questionMark.text = "?"
            itemView.alpha = 0.7f
            android.util.Log.d("CollectionItem", "Showing question mark for: ${item.name}")
            
            // Note: When item.isDiscovered becomes true in the future,
            // we would show the actual models in full color instead of question marks
        }

        private fun dpToPx(dp: Int): Int {
            val density = itemView.context.resources.displayMetrics.density
            return (dp * density).toInt()
        }
    }
} 