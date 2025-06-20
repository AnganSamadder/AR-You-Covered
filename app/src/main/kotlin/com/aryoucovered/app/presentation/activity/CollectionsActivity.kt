package com.aryoucovered.app.presentation.activity

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aryoucovered.app.R
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class CollectionsActivity : AppCompatActivity() {

    private lateinit var titleText: TextView
    private lateinit var artifactsTitle: TextView
    private lateinit var peopleTitle: TextView
    private lateinit var artifactsRecyclerView: RecyclerView
    private lateinit var peopleRecyclerView: RecyclerView
    private lateinit var backButton: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        setContentView(R.layout.activity_collections)

        initViews()
        setupClickListeners()
        setupRecyclerViews()
        setupResponsiveLayout()
    }

    private fun initViews() {
        titleText = findViewById(R.id.collections_title)
        artifactsTitle = findViewById(R.id.artifacts_title)
        peopleTitle = findViewById(R.id.people_title)
        artifactsRecyclerView = findViewById(R.id.artifacts_recycler_view)
        peopleRecyclerView = findViewById(R.id.people_recycler_view)
        backButton = findViewById(R.id.back_button)
    }

    private fun setupClickListeners() {
        backButton.setOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerViews() {
        // Setup artifacts grid (3 columns)
        artifactsRecyclerView.layoutManager = GridLayoutManager(this, 3)
        artifactsRecyclerView.adapter = CollectionItemAdapter(
            createArtifactPlaceholders(), 
            CollectionItemAdapter.ItemType.ARTIFACT
        )

        // Setup people grid (3 columns for more compact layout)
        val peopleLayoutManager = GridLayoutManager(this, 3)
        peopleRecyclerView.layoutManager = peopleLayoutManager
        val peopleData = createPeoplePlaceholders()
        val peopleAdapter = CollectionItemAdapter(
            peopleData, 
            CollectionItemAdapter.ItemType.PERSON
        )
        peopleRecyclerView.adapter = peopleAdapter
        
        // Debug: Log the number of people items
        android.util.Log.d("Collections", "People items count: ${peopleData.size}")
        
        // Force the RecyclerView to layout all its children
        peopleRecyclerView.post {
            // Calculate the expected height for all items (3 columns now)
            val expectedRows = (peopleData.size + 2) / 3 // 3 columns, so divide by 3 and round up
            val itemHeightDp = 200 + 16 + 20 // item height + padding + margins
            val itemHeightPx = (itemHeightDp * resources.displayMetrics.density).toInt()
            val expectedHeight = expectedRows * itemHeightPx
            
            android.util.Log.d("Collections", "Expected height: $expectedHeight px for $expectedRows rows (3 columns)")
            android.util.Log.d("Collections", "Current RecyclerView height: ${peopleRecyclerView.height}")
            android.util.Log.d("Collections", "Current child count: ${peopleLayoutManager.childCount}")
            
            // Ensure the RecyclerView can show all items
            peopleRecyclerView.requestLayout()
        }
    }

    private fun setupResponsiveLayout() {
        // Post this calculation to run after the layout is measured
        titleText.post {
            val screenWidth = resources.displayMetrics.widthPixels
            scaleTextSizes(screenWidth)
        }
    }

    private fun scaleTextSizes(screenWidth: Int) {
        val density = resources.displayMetrics.density
        val titleTextSizePx = screenWidth * 0.08f
        val sectionTextSizePx = screenWidth * 0.06f
        
        val titleTextSizeSp = titleTextSizePx / density
        val sectionTextSizeSp = sectionTextSizePx / density
        
        titleText.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, titleTextSizeSp)
        artifactsTitle.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, sectionTextSizeSp)
        peopleTitle.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, sectionTextSizeSp)
    }

    private fun createArtifactPlaceholders(): List<CollectionItem> {
        return listOf(
            CollectionItem("Roller Skates", false),
            CollectionItem("Sewing Machine", false),
            CollectionItem("Farm Uniform", false),
            CollectionItem("Mystery Item 1", false),
            CollectionItem("Mystery Item 2", false),
            CollectionItem("Mystery Item 3", false)
        )
    }

    private fun createPeoplePlaceholders(): List<CollectionItem> {
        return listOf(
            CollectionItem("Jake", false),
            CollectionItem("Ed", false),
            CollectionItem("Ford", false),
            CollectionItem("Jon", false),
            CollectionItem("Michael", false),
            CollectionItem("Mystery Person", false)
        )
    }

    data class CollectionItem(
        val name: String,
        val isDiscovered: Boolean
    )
} 