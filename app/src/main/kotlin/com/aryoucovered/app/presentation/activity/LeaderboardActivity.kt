package com.aryoucovered.app.presentation.activity

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aryoucovered.app.R

class LeaderboardActivity : AppCompatActivity() {

    private lateinit var titleText: TextView
    private lateinit var leaderboardRecyclerView: RecyclerView
    private lateinit var leaderboardAdapter: LeaderboardAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leaderboard)

        initViews()
        setupLeaderboard()
        setupResponsiveLayout()
    }

    private fun initViews() {
        titleText = findViewById(R.id.leaderboard_title)
        leaderboardRecyclerView = findViewById(R.id.leaderboard_recycler_view)
    }

    private fun setupLeaderboard() {
        // Create hardcoded leaderboard data
        val leaderboardData = listOf(
            LeaderboardEntry(1, "Jake", 99999, true), // Winner with crown
            LeaderboardEntry(2, "Angan", 24390, false),
            LeaderboardEntry(3, "Jennifer", 19850, false),
            LeaderboardEntry(4, "Akinni", 16420, false),
            LeaderboardEntry(5, "Michael Tipsord", 14560, false), // State Farm CEO
            LeaderboardEntry(6, "Rand Harbert", 12780, false), // State Farm Executive
            LeaderboardEntry(7, "Jonathan Adkisson", 11240, false), // State Farm Executive
            LeaderboardEntry(8, "Teresa Scharn", 9920, false), // State Farm Executive
            LeaderboardEntry(9, "Paul Smith", 8650, false),
            LeaderboardEntry(10, "Rachel Chen", 7340, false),
            LeaderboardEntry(11, "David Johnson", 6180, false),
            LeaderboardEntry(12, "Sarah Williams", 5020, false)
        )

        leaderboardAdapter = LeaderboardAdapter(leaderboardData)
        leaderboardRecyclerView.layoutManager = LinearLayoutManager(this)
        leaderboardRecyclerView.adapter = leaderboardAdapter
    }

    private fun setupResponsiveLayout() {
        titleText.post {
            val screenWidth = resources.displayMetrics.widthPixels
            scaleTextSizes(screenWidth)
        }
    }

    private fun scaleTextSizes(screenWidth: Int) {
        val density = resources.displayMetrics.density
        val titleTextSizePx = screenWidth * 0.08f
        val titleTextSizeSp = titleTextSizePx / density
        
        titleText.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, titleTextSizeSp)
    }

    data class LeaderboardEntry(
        val rank: Int,
        val name: String,
        val score: Int,
        val isWinner: Boolean = false
    )
} 