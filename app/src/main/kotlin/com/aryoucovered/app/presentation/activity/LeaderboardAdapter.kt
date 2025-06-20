package com.aryoucovered.app.presentation.activity

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aryoucovered.app.R
import java.text.NumberFormat
import java.util.*

class LeaderboardAdapter(
    private val entries: List<LeaderboardActivity.LeaderboardEntry>
) : RecyclerView.Adapter<LeaderboardAdapter.LeaderboardViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeaderboardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_leaderboard, parent, false)
        return LeaderboardViewHolder(view)
    }

    override fun onBindViewHolder(holder: LeaderboardViewHolder, position: Int) {
        holder.bind(entries[position])
    }

    override fun getItemCount(): Int = entries.size

    class LeaderboardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val rankText: TextView = itemView.findViewById(R.id.rank_text)
        private val nameText: TextView = itemView.findViewById(R.id.name_text)
        private val scoreText: TextView = itemView.findViewById(R.id.score_text)
        private val crownIcon: ImageView = itemView.findViewById(R.id.crown_icon)
        private val rankBackground: View = itemView.findViewById(R.id.rank_background)
        private val itemContainer: View = itemView.findViewById(R.id.item_container)

        fun bind(entry: LeaderboardActivity.LeaderboardEntry) {
            nameText.text = entry.name
            
            // Format score with commas
            val formatter = NumberFormat.getNumberInstance(Locale.US)
            scoreText.text = formatter.format(entry.score)

            if (entry.isWinner) {
                // Special styling for winner (#1 - Red)
                crownIcon.visibility = View.VISIBLE
                rankText.text = "#1"
                rankBackground.setBackgroundResource(R.drawable.winner_rank_background)
                itemContainer.setBackgroundResource(R.drawable.winner_item_background)
                nameText.setTextColor(itemView.context.getColor(R.color.white))
                scoreText.setTextColor(itemView.context.getColor(R.color.white))
                rankText.setTextColor(itemView.context.getColor(R.color.white))
            } else {
                crownIcon.visibility = View.GONE
                rankText.text = "#${entry.rank}"
                
                when (entry.rank) {
                    2 -> {
                        // Gold for 2nd place
                        rankBackground.setBackgroundResource(R.drawable.gold_rank_background)
                        itemContainer.setBackgroundResource(R.drawable.gold_item_background)
                        nameText.setTextColor(itemView.context.getColor(R.color.white))
                        scoreText.setTextColor(itemView.context.getColor(R.color.white))
                        rankText.setTextColor(itemView.context.getColor(R.color.white))
                    }
                    3 -> {
                        // Silver for 3rd place
                        rankBackground.setBackgroundResource(R.drawable.silver_rank_background)
                        itemContainer.setBackgroundResource(R.drawable.silver_item_background)
                        nameText.setTextColor(itemView.context.getColor(R.color.white))
                        scoreText.setTextColor(itemView.context.getColor(R.color.white))
                        rankText.setTextColor(itemView.context.getColor(R.color.white))
                    }
                    4 -> {
                        // Bronze for 4th place
                        rankBackground.setBackgroundResource(R.drawable.bronze_rank_background)
                        itemContainer.setBackgroundResource(R.drawable.bronze_item_background)
                        nameText.setTextColor(itemView.context.getColor(R.color.white))
                        scoreText.setTextColor(itemView.context.getColor(R.color.white))
                        rankText.setTextColor(itemView.context.getColor(R.color.white))
                    }
                    else -> {
                        // Regular styling for ranks 5+
                        rankBackground.setBackgroundResource(R.drawable.regular_rank_background)
                        itemContainer.setBackgroundResource(R.drawable.regular_item_background)
                        nameText.setTextColor(itemView.context.getColor(R.color.white))
                        scoreText.setTextColor(itemView.context.getColor(R.color.white))
                        rankText.setTextColor(itemView.context.getColor(R.color.white))
                    }
                }
            }
        }
    }
} 