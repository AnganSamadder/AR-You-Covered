package com.aryoucovered.app.presentation.activity

import android.os.Bundle
import android.widget.TextView
import android.widget.Button
import android.widget.ImageView
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aryoucovered.app.R
import android.widget.ImageButton
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class StoreActivity : AppCompatActivity() {

    private lateinit var titleText: TextView
    private lateinit var pointsText: TextView
    private lateinit var storeRecyclerView: RecyclerView
    private lateinit var itemOverlay: View
    private lateinit var overlayImage: ImageView
    private lateinit var overlayTitle: TextView
    private lateinit var overlayDescription: TextView
    private lateinit var overlayPrice: TextView
    private lateinit var redeemButton: Button
    private lateinit var closeOverlayButton: ImageButton
    
    private var currentPoints = 2500 // Starting points for demo
    private var selectedItem: StoreItem? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        setContentView(R.layout.activity_store)
        
        initViews()
        setupClickListeners()
        setupRecyclerView()
        setupResponsiveLayout()
        updatePointsDisplay()
    }
    
    private fun initViews() {
        titleText = findViewById(R.id.store_title)
        pointsText = findViewById(R.id.points_text)
        storeRecyclerView = findViewById(R.id.store_recycler_view)
        itemOverlay = findViewById(R.id.item_overlay)
        overlayImage = findViewById(R.id.overlay_image)
        overlayTitle = findViewById(R.id.overlay_title)
        overlayDescription = findViewById(R.id.overlay_description)
        overlayPrice = findViewById(R.id.overlay_price)
        redeemButton = findViewById(R.id.redeem_button)
        closeOverlayButton = findViewById(R.id.close_overlay_button)
        
        // Handle window insets for edge-to-edge display
        val headerBar = findViewById<View>(R.id.header_bar)
        headerBar.setOnApplyWindowInsetsListener { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val density = resources.displayMetrics.density
            val basePaddingHorizontal = (24 * density).toInt() // 24dp in pixels
            val basePaddingTop = (48 * density).toInt() // 48dp in pixels  
            val basePaddingBottom = (16 * density).toInt() // 16dp in pixels
            
            view.setPadding(
                basePaddingHorizontal + systemBars.left,
                basePaddingTop + systemBars.top,
                basePaddingHorizontal + systemBars.right,
                basePaddingBottom
            )
            insets
        }
    }
    
    private fun setupClickListeners() {
        closeOverlayButton.setOnClickListener {
            hideItemOverlay()
        }
        
        redeemButton.setOnClickListener {
            selectedItem?.let { item ->
                if (currentPoints >= item.price) {
                    currentPoints -= item.price
                    updatePointsDisplay()
                    hideItemOverlay()
                    Toast.makeText(this, "Successfully redeemed ${item.name}!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Not enough points!", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        // Close overlay when clicking on the background
        itemOverlay.setOnClickListener {
            hideItemOverlay()
        }
    }
    
    private fun setupRecyclerView() {
        val layoutManager = GridLayoutManager(this, 2)
        storeRecyclerView.layoutManager = layoutManager
        
        val storeItems = createStoreItems()
        val adapter = StoreItemAdapter(storeItems) { item ->
            showItemOverlay(item)
        }
        storeRecyclerView.adapter = adapter
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
        val pointsTextSizePx = screenWidth * 0.05f
        
        val titleTextSizeSp = titleTextSizePx / density
        val pointsTextSizeSp = pointsTextSizePx / density
        
        titleText.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, titleTextSizeSp)
        pointsText.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, pointsTextSizeSp)
    }
    
    private fun updatePointsDisplay() {
        pointsText.text = "$currentPoints PTS"
    }
    
    private fun showItemOverlay(item: StoreItem) {
        android.util.Log.d("StoreActivity", "showItemOverlay called for item: ${item.name}")
        
        selectedItem = item
        overlayTitle.text = item.name
        overlayDescription.text = item.description
        overlayPrice.text = "${item.price} PTS"
        
        // Set the image resource
        val imageResource = getImageResourceForItem(item.imageFileName)
        overlayImage.setImageResource(imageResource)
        
        // Update redeem button state
        redeemButton.isEnabled = currentPoints >= item.price
        redeemButton.alpha = if (currentPoints >= item.price) 1.0f else 0.5f
        
        android.util.Log.d("StoreActivity", "Setting overlay visibility to VISIBLE")
        
        // Show overlay with animation
        itemOverlay.visibility = View.VISIBLE
        itemOverlay.alpha = 0f
        itemOverlay.animate()
            .alpha(1f)
            .setDuration(300)
            .start()
    }
    
    private fun hideItemOverlay() {
        itemOverlay.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                itemOverlay.visibility = View.GONE
                selectedItem = null
            }
            .start()
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
    
    private fun createStoreItems(): List<StoreItem> {
        return listOf(
            StoreItem(
                name = "Nike Hoodie", 
                description = "Premium Nike hoodie perfect for staying warm and stylish. Made with high-quality materials for ultimate comfort.",
                price = 750,
                imageFileName = "nike_hoodie.jpg"
            ),
            StoreItem(
                name = "Stuffed Animal", 
                description = "Adorable and cuddly stuffed animal companion. Perfect for decoration or as a gift for someone special.",
                price = 300,
                imageFileName = "stuffed_animal.jpg"
            ),
            StoreItem(
                name = "Hair Scrunchie", 
                description = "Stylish hair scrunchie to complete your look. Comfortable and trendy accessory for any hairstyle.",
                price = 150,
                imageFileName = "scrunchie.jpg"
            ),
            StoreItem(
                name = "Aviator Glasses", 
                description = "Classic aviator sunglasses with UV protection. Look cool while protecting your eyes from the sun.",
                price = 450,
                imageFileName = "aviator_glasses.jpg"
            ),
            StoreItem(
                name = "Earrings", 
                description = "Elegant earrings that add sparkle to any outfit. Perfect for special occasions or everyday wear.",
                price = 200,
                imageFileName = "earrings.jpg"
            ),
            StoreItem(
                name = "Baseball Cap", 
                description = "Comfortable baseball cap for sun protection and style. Adjustable fit for all head sizes.",
                price = 250,
                imageFileName = "cap.jpg"
            ),
            StoreItem(
                name = "Sporty Shirt", 
                description = "Athletic shirt perfect for workouts or casual wear. Breathable fabric keeps you comfortable all day.",
                price = 400,
                imageFileName = "sporty_shirt.jpg"
            ),
            StoreItem(
                name = "Cropped Hoodie", 
                description = "Trendy cropped hoodie for a modern look. Combines comfort with contemporary fashion style.",
                price = 500,
                imageFileName = "cropped_hoodie.jpg"
            )
        )
    }
    
    data class StoreItem(
        val name: String,
        val description: String,
        val price: Int,
        val imageFileName: String
    )
} 