package com.aryoucovered.app.presentation.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.aryoucovered.app.R
import com.aryoucovered.app.feature.ar.ARActivity
import com.aryoucovered.app.feature.auth.SigninActivity
import com.aryoucovered.app.feature.game.GameActivity
import com.aryoucovered.app.feature.map.MapActivity
import com.aryoucovered.app.presentation.activity.SettingsActivity
import com.aryoucovered.app.presentation.activity.CollectionsActivity
import com.aryoucovered.app.presentation.components.LogoOverlayComponent
import com.aryoucovered.app.presentation.activity.MainActivity
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity(), LogoOverlayComponent.LogoOverlayCallbacks {
    
    // Animation constants for consistent scaling and positioning
    companion object {
        // Title positioning
        private const val TITLE_TOP_MARGIN_RATIO = 0.10f  // Title positioned 10% from top of screen
        
        // Hunt button scaling
        private const val HUNT_BUTTON_SIZE_RATIO = 0.50f  // Hunt button will be 50% of screen width (more reasonable size)
        private const val HUNT_BUTTON_TEXT_RATIO = 0.25f  // Hunt button text will be 25% of button diameter
        private const val HUNT_BUTTON_MARGIN_RATIO = 0.05f  // 5% margin between title and button
        
        // Text scaling ratios
        private const val TITLE_TEXT_RATIO = 0.08f  // Title text will be 8% of screen width
        private const val BUTTON_TEXT_RATIO = 0.045f // Button text will be 4.5% of screen width
    }

    private lateinit var titleText: TextView
    private lateinit var startHuntButton: Button
    private lateinit var settingsButton: ImageButton
    private lateinit var logoutButton: ImageButton
    private lateinit var arButton: Button
    private lateinit var gameButton: Button
    private lateinit var huntButtonContainer: android.view.View
    private lateinit var logoOverlay: LogoOverlayComponent
    
    private lateinit var auth: FirebaseAuth
    private var isLoggedIn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()

        initViews()
        setupClickListeners()
        setupResponsiveLayout()
        
        updateUIBasedOnLoginStatus()
    }

    private fun initViews() {
        titleText = findViewById(R.id.title_text)
        startHuntButton = findViewById(R.id.start_hunt_button)
        settingsButton = findViewById(R.id.settings_button)
        logoutButton = findViewById(R.id.logout_button)
        arButton = findViewById(R.id.ar_button)
        gameButton = findViewById(R.id.game_button)
        huntButtonContainer = findViewById(R.id.play_button_container)
        logoOverlay = findViewById(R.id.logo_overlay)
        
        // Set up the logo overlay callbacks
        logoOverlay.setCallbacks(this)
        logoOverlay.setMapTheme(false) // Use default theme with white text
    }

    private fun setupResponsiveLayout() {
        // Post this calculation to run after the layout is measured
        huntButtonContainer.post {
            val parentView = huntButtonContainer.parent as android.view.View
            val actualScreenWidth = parentView.width
            val actualScreenHeight = parentView.height
            
            // Apply responsive text scaling
            scaleTextSizes(actualScreenWidth)
            
            // Position title responsively
            scaleAndPositionTitle(actualScreenHeight)
            
            // Scale and position hunt button relative to title
            scaleAndPositionHuntButton(actualScreenWidth, actualScreenHeight)
        }
    }

    private fun scaleTextSizes(screenWidth: Int) {
        // Calculate responsive text sizes in pixels, then convert to SP
        val density = resources.displayMetrics.density
        val titleTextSizePx = screenWidth * TITLE_TEXT_RATIO
        val buttonTextSizePx = screenWidth * BUTTON_TEXT_RATIO
        
        // Calculate hunt button text size relative to the button's actual size (not screen width)
        val huntButtonSize = (screenWidth * HUNT_BUTTON_SIZE_RATIO).toInt()
        val huntButtonTextSizePx = huntButtonSize * HUNT_BUTTON_TEXT_RATIO
        
        // Convert to SP units for proper text scaling
        val titleTextSizeSp = titleTextSizePx / density
        val buttonTextSizeSp = buttonTextSizePx / density
        val huntButtonTextSizeSp = huntButtonTextSizePx / density
        
        // Scale title text
        titleText.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, titleTextSizeSp)
        
        // Scale hunt button text (sized relative to button diameter with padding)
        startHuntButton.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, huntButtonTextSizeSp)
        
        // Scale other button texts
        arButton.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, buttonTextSizeSp)
        gameButton.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, buttonTextSizeSp)
    }

    private fun scaleAndPositionHuntButton(screenWidth: Int, screenHeight: Int) {
        // Calculate hunt button size (circular)
        val huntButtonSize = (screenWidth * HUNT_BUTTON_SIZE_RATIO).toInt()
        
        // Resize hunt button container to be circular
        resizeView(huntButtonContainer, huntButtonSize, huntButtonSize)
        
        // Calculate responsive margin between title and hunt button
        val huntButtonTopMargin = (screenHeight * HUNT_BUTTON_MARGIN_RATIO).toInt()
        
        // Position hunt button container relative to title
        val huntButtonLayoutParams = huntButtonContainer.layoutParams as android.widget.RelativeLayout.LayoutParams
        huntButtonLayoutParams.topMargin = huntButtonTopMargin
        huntButtonLayoutParams.addRule(android.widget.RelativeLayout.CENTER_HORIZONTAL)
        huntButtonLayoutParams.addRule(android.widget.RelativeLayout.BELOW, R.id.title_text)
        huntButtonLayoutParams.removeRule(android.widget.RelativeLayout.ALIGN_PARENT_TOP)
        huntButtonContainer.layoutParams = huntButtonLayoutParams
    }

    private fun resizeView(view: android.view.View, width: Int, height: Int) {
        val layoutParams = view.layoutParams
        layoutParams.width = width
        layoutParams.height = height
        view.layoutParams = layoutParams
    }

    private fun scaleAndPositionTitle(screenHeight: Int) {
        // Calculate responsive top margin for title
        val titleTopMargin = (screenHeight * TITLE_TOP_MARGIN_RATIO).toInt()
        
        // Position title relative to screen height
        val titleLayoutParams = titleText.layoutParams as android.widget.RelativeLayout.LayoutParams
        titleLayoutParams.topMargin = titleTopMargin
        titleLayoutParams.addRule(android.widget.RelativeLayout.CENTER_HORIZONTAL)
        titleLayoutParams.removeRule(android.widget.RelativeLayout.ALIGN_PARENT_TOP)
        titleText.layoutParams = titleLayoutParams
    }

    private fun updateUIBasedOnLoginStatus() {
        isLoggedIn = auth.currentUser != null
        
        if (isLoggedIn) {
            startHuntButton.text = "HUNT"
            logoutButton.visibility = android.view.View.VISIBLE
            settingsButton.visibility = android.view.View.VISIBLE
        } else {
            startHuntButton.text = "LOGIN"
            logoutButton.visibility = android.view.View.GONE
            settingsButton.visibility = android.view.View.GONE
        }
        
        // Update the logo overlay authentication state
        logoOverlay.updateAuthenticationState()
    }

    private fun setupClickListeners() {
        startHuntButton.setOnClickListener {
            if (isLoggedIn) {
                startActivity(Intent(this, MapActivity::class.java))
            } else {
                startActivity(Intent(this, SigninActivity::class.java))
            }
        }

        settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        
        logoutButton.setOnClickListener {
            if (isLoggedIn) {
                auth.signOut()
                updateUIBasedOnLoginStatus()
                android.widget.Toast.makeText(this, "Logged out", android.widget.Toast.LENGTH_SHORT).show()
            }
        }

        arButton.setOnClickListener {
            startActivity(Intent(this, ARActivity::class.java))
        }

        gameButton.setOnClickListener {
            startActivity(Intent(this, ARActivity::class.java))
        }
        
        // Add manual refresh by tapping title (for testing)
        titleText.setOnClickListener {
            android.widget.Toast.makeText(this, "Refreshing layout...", android.widget.Toast.LENGTH_SHORT).show()
            setupResponsiveLayout()
        }
    }

    override fun onResume() {
        super.onResume()
        updateUIBasedOnLoginStatus()
        
        // Ensure logo overlay starts collapsed when returning to home page
        logoOverlay.forceCollapse()
    }
    
    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        
        // Reset logo overlay on configuration change
        logoOverlay.forceCollapse()
        
        // Recalculate layout
        huntButtonContainer.postDelayed({
            setupResponsiveLayout()
        }, 200)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            // Recalculate when window gains focus
            huntButtonContainer.postDelayed({
                setupResponsiveLayout()
            }, 50)
        }
    }

    // LogoOverlayComponent.LogoOverlayCallbacks implementation
    override fun onProfileClicked() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    override fun onCollectionClicked() {
        startActivity(Intent(this, CollectionsActivity::class.java))
    }

    override fun onStoreClicked() {
        startActivity(Intent(this, StoreActivity::class.java))
    }

    override fun onLeaderboardClicked() {
        startActivity(Intent(this, LeaderboardActivity::class.java))
    }
} 