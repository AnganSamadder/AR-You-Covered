package com.aryoucovered.app.presentation.components

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.TextView
import com.aryoucovered.app.R
import com.aryoucovered.app.presentation.activity.SettingsActivity
import com.google.firebase.auth.FirebaseAuth
import androidx.core.content.ContextCompat

class LogoOverlayComponent @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {

    // Animation constants for consistent scaling and positioning
    companion object {
        private const val OVAL_EXPAND_SCALE_X = 1.4f
        private const val OVAL_EXPAND_SCALE_Y = 1.97f
        private const val CIRCLE_NORMAL_SCALE = 1.0f
        private const val CIRCLE_COLLAPSE_SCALE_X = 1.2f
        private const val CIRCLE_COLLAPSE_SCALE_Y = 0.8f
        
        // Movement distances as percentages of screen dimensions
        private const val HORIZONTAL_EXPANSION_RATIO = 0.25f
        private const val VERTICAL_EXPANSION_RATIO = 0.06f
        private const val EXTRA_VERTICAL_EXPANSION_RATIO = 0.20f
        
        // Logo sizing based on screen width for responsive design
        private const val LOGO_SIZE_RATIO = 0.18f
        
        private const val SCREEN_EDGE_PADDING_RATIO = 0.15f
        private const val POSITION_CENTER = 0f
        
        private const val DURATION_SHORT = 100L
        private const val DURATION_LONG = 200L
        private const val DELAY_FIRST = 50L
        private const val DELAY_SECOND = 100L
        private const val DELAY_THIRD = 150L
        private const val DELAY_FOURTH = 180L
        private const val DELAY_FIFTH = 200L
    }

    // Logo animation views
    private lateinit var logoOvalTop: View
    private lateinit var logoOvalBottomLeft: View
    private lateinit var logoOvalBottomRight: View
    private lateinit var logoCircleTop: View
    private lateinit var logoCircleBottomLeft: View
    private lateinit var logoCircleBottomRight: View
    private lateinit var logoCircleExtraTop: View
    private lateinit var labelTop: View
    private lateinit var labelBottomLeft: View
    private lateinit var labelBottomRight: View
    private lateinit var labelExtraTop: View
    private lateinit var closeButton: ImageButton
    
    private var isExpanded = false
    private var isLoggedIn = false
    private var useMapTheme = false // New property for map theme
    
    // Screen center calculation variables
    private var exactScreenCenterX: Float = 0f
    private var ovalCenterPositionX: Float = 0f
    private var ovalRightPositionX: Float = 0f
    private var leftExpandPositionX: Float = 0f
    private var rightExpandPositionX: Float = 0f
    private var dynamicExpandDistance: Float = 0f

    // Firebase auth
    private lateinit var auth: FirebaseAuth

    // Callback interfaces for click actions
    interface LogoOverlayCallbacks {
        fun onProfileClicked()
        fun onCollectionClicked()
        fun onStoreClicked()
        fun onLeaderboardClicked()
    }

    private var callbacks: LogoOverlayCallbacks? = null

    init {
        initializeComponent()
    }

    private fun initializeComponent() {
        // Inflate the logo overlay layout
        LayoutInflater.from(context).inflate(R.layout.component_logo_overlay, this, true)
        
        auth = FirebaseAuth.getInstance()
        
        initViews()
        setupClickListeners()
        
        // Set up global layout listener for responsive sizing
        viewTreeObserver.addOnGlobalLayoutListener {
            if (width > 0 && height > 0) {
                calculateExactCenter()
            }
        }
    }

    private fun initViews() {
        logoOvalTop = findViewById(R.id.logo_oval_top)
        logoOvalBottomLeft = findViewById(R.id.logo_oval_bottom_left)
        logoOvalBottomRight = findViewById(R.id.logo_oval_bottom_right)
        logoCircleTop = findViewById(R.id.logo_circle_top)
        logoCircleBottomLeft = findViewById(R.id.logo_circle_bottom_left)
        logoCircleBottomRight = findViewById(R.id.logo_circle_bottom_right)
        logoCircleExtraTop = findViewById(R.id.logo_circle_extra_top)
        labelTop = findViewById(R.id.label_top)
        labelBottomLeft = findViewById(R.id.label_bottom_left)
        labelBottomRight = findViewById(R.id.label_bottom_right)
        labelExtraTop = findViewById(R.id.label_extra_top)
        closeButton = findViewById(R.id.close_button)
        
        // Apply initial theme colors
        updateThemeColors()
    }

    private fun setupClickListeners() {
        val logoClickListener = OnClickListener {
            if (!isExpanded && isLoggedIn) {
                expandLogo()
            }
        }
        
        // Profile circle (bottom left) - navigate to settings when clicked
        val profileClickListener = OnClickListener {
            if (isLoggedIn) {
                if (!isExpanded) {
                    expandLogo()
                } else {
                    callbacks?.onProfileClicked() ?: run {
                        // Default behavior - open settings
                        context.startActivity(Intent(context, SettingsActivity::class.java))
                    }
                }
            }
        }
        
        logoOvalTop.setOnClickListener(logoClickListener)
        logoOvalBottomLeft.setOnClickListener(profileClickListener)
        logoOvalBottomRight.setOnClickListener(logoClickListener)
        
        // Circle click listeners when expanded
        logoCircleBottomLeft.setOnClickListener {
            if (isExpanded && isLoggedIn) {
                callbacks?.onProfileClicked() ?: run {
                    context.startActivity(Intent(context, SettingsActivity::class.java))
                }
            }
        }
        
        logoCircleTop.setOnClickListener {
            if (isExpanded && isLoggedIn) {
                callbacks?.onCollectionClicked()
            }
        }
        
        logoCircleBottomRight.setOnClickListener {
            if (isExpanded && isLoggedIn) {
                callbacks?.onStoreClicked()
            }
        }
        
        logoCircleExtraTop.setOnClickListener {
            if (isExpanded && isLoggedIn) {
                callbacks?.onLeaderboardClicked()
            }
        }
        
        // Label click listeners
        labelBottomLeft.setOnClickListener {
            if (isExpanded && isLoggedIn) {
                callbacks?.onProfileClicked() ?: run {
                    context.startActivity(Intent(context, SettingsActivity::class.java))
                }
            }
        }
        
        labelTop.setOnClickListener {
            if (isExpanded && isLoggedIn) {
                callbacks?.onCollectionClicked()
            }
        }
        
        labelBottomRight.setOnClickListener {
            if (isExpanded && isLoggedIn) {
                callbacks?.onStoreClicked()
            }
        }
        
        labelExtraTop.setOnClickListener {
            if (isExpanded && isLoggedIn) {
                callbacks?.onLeaderboardClicked()
            }
        }
        
        closeButton.setOnClickListener {
            if (isExpanded) {
                collapseLogo()
            }
        }
    }

    private fun calculateExactCenter() {
        val actualScreenWidth = width
        val actualScreenHeight = height
        val actualScreenCenterX = actualScreenWidth / 2f
        
        exactScreenCenterX = actualScreenCenterX
        
        // Calculate responsive logo size based on screen width
        val responsiveLogoSize = (actualScreenWidth * LOGO_SIZE_RATIO).toInt()
        
        // Apply responsive sizing to all logo elements
        resizeLogoElements(responsiveLogoSize)
        
        // Apply responsive text scaling
        scaleTextSizes(actualScreenWidth)
        
        // Get the oval's width in pixels after resizing
        val ovalWidthPx = responsiveLogoSize
        val ovalHeightPx = responsiveLogoSize
        
        // Calculate responsive vertical positioning for top oval
        val bottomOvalMarginFromBottom = 79  // dp - this is the fixed margin for bottom ovals
        val bottomOvalMarginPx = bottomOvalMarginFromBottom * resources.displayMetrics.density
        val ovalSpacing = ovalHeightPx * 0.6f  // Spacing is 60% of oval height
        val topOvalMarginFromBottom = bottomOvalMarginPx + ovalSpacing
        
        // Position ovals touching exactly at center (no overlap)
        ovalCenterPositionX = actualScreenCenterX - ovalWidthPx
        ovalRightPositionX = actualScreenCenterX
        
        // Calculate expansion distance
        dynamicExpandDistance = actualScreenWidth * HORIZONTAL_EXPANSION_RATIO
        leftExpandPositionX = -dynamicExpandDistance
        rightExpandPositionX = dynamicExpandDistance
        
        // Apply responsive positioning to elements
        positionLogoElements(topOvalMarginFromBottom, ovalWidthPx)
    }

    private fun resizeLogoElements(size: Int) {
        resizeView(logoOvalTop, size, size)
        resizeView(logoOvalBottomLeft, size, size)
        resizeView(logoOvalBottomRight, size, size)
        resizeView(logoCircleTop, size, size)
        resizeView(logoCircleBottomLeft, size, size)
        resizeView(logoCircleBottomRight, size, size)
        resizeView(logoCircleExtraTop, size, size)
    }
    
    private fun resizeView(view: View, width: Int, height: Int) {
        val layoutParams = view.layoutParams
        layoutParams.width = width
        layoutParams.height = height
        view.layoutParams = layoutParams
    }

    private fun scaleTextSizes(screenWidth: Int) {
        val density = resources.displayMetrics.density
        val labelTextSizePx = screenWidth * 0.035f  // Label text ratio
        val labelTextSizeSp = labelTextSizePx / density
        
        scaleTextInView(labelTop, labelTextSizeSp)
        scaleTextInView(labelBottomLeft, labelTextSizeSp)
        scaleTextInView(labelBottomRight, labelTextSizeSp)
        scaleTextInView(labelExtraTop, labelTextSizeSp)
    }
    
    private fun scaleTextInView(view: View, textSizeSp: Float) {
        if (view is TextView) {
            view.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, textSizeSp)
            view.setSingleLine(true)
            view.gravity = android.view.Gravity.CENTER
        }
    }

    private fun positionLogoElements(topOvalMarginFromBottom: Float, ovalWidthPx: Int) {
        // Position top elements
        val topOvalLayoutParams = logoOvalTop.layoutParams as RelativeLayout.LayoutParams
        topOvalLayoutParams.bottomMargin = topOvalMarginFromBottom.toInt()
        logoOvalTop.layoutParams = topOvalLayoutParams
        
        val topCircleLayoutParams = logoCircleTop.layoutParams as RelativeLayout.LayoutParams
        topCircleLayoutParams.bottomMargin = topOvalMarginFromBottom.toInt()
        logoCircleTop.layoutParams = topCircleLayoutParams
        
        val extraTopCircleLayoutParams = logoCircleExtraTop.layoutParams as RelativeLayout.LayoutParams
        extraTopCircleLayoutParams.bottomMargin = topOvalMarginFromBottom.toInt()
        logoCircleExtraTop.layoutParams = extraTopCircleLayoutParams
        
        // Position left elements
        val leftLayoutParams = logoOvalBottomLeft.layoutParams as RelativeLayout.LayoutParams
        leftLayoutParams.marginStart = ovalCenterPositionX.toInt().coerceAtLeast(0)
        leftLayoutParams.removeRule(RelativeLayout.ALIGN_PARENT_START)
        leftLayoutParams.removeRule(RelativeLayout.CENTER_HORIZONTAL)
        leftLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
        logoOvalBottomLeft.layoutParams = leftLayoutParams
        
        // Position right elements
        val rightLayoutParams = logoOvalBottomRight.layoutParams as RelativeLayout.LayoutParams
        rightLayoutParams.marginStart = ovalRightPositionX.toInt()
        rightLayoutParams.removeRule(RelativeLayout.ALIGN_PARENT_START)
        rightLayoutParams.removeRule(RelativeLayout.CENTER_HORIZONTAL)
        rightLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
        logoOvalBottomRight.layoutParams = rightLayoutParams
        
        // Position circles at same locations as ovals
        val leftCircleLayoutParams = logoCircleBottomLeft.layoutParams as RelativeLayout.LayoutParams
        leftCircleLayoutParams.marginStart = ovalCenterPositionX.toInt().coerceAtLeast(0)
        leftCircleLayoutParams.removeRule(RelativeLayout.ALIGN_PARENT_START)
        leftCircleLayoutParams.removeRule(RelativeLayout.CENTER_HORIZONTAL)
        leftCircleLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
        logoCircleBottomLeft.layoutParams = leftCircleLayoutParams
        
        val rightCircleLayoutParams = logoCircleBottomRight.layoutParams as RelativeLayout.LayoutParams
        rightCircleLayoutParams.marginStart = ovalRightPositionX.toInt()
        rightCircleLayoutParams.removeRule(RelativeLayout.ALIGN_PARENT_START)
        rightCircleLayoutParams.removeRule(RelativeLayout.CENTER_HORIZONTAL)
        rightCircleLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
        logoCircleBottomRight.layoutParams = rightCircleLayoutParams
        
        // Position labels
        positionLabels(ovalWidthPx, topOvalMarginFromBottom)
    }

    private fun positionLabels(ovalWidthPx: Int, topOvalMarginFromBottom: Float) {
        // Position labels below their corresponding circles
        val leftCircleCenterX = ovalCenterPositionX + (ovalWidthPx / 2f)
        val rightCircleCenterX = ovalRightPositionX + (ovalWidthPx / 2f)
        
        // Left label
        val leftLabelLayoutParams = labelBottomLeft.layoutParams as RelativeLayout.LayoutParams
        val labelWidthApprox = (60 * resources.displayMetrics.density).toInt()
        leftLabelLayoutParams.marginStart = (leftCircleCenterX - (labelWidthApprox / 2f)).toInt().coerceAtLeast(0)
        leftLabelLayoutParams.bottomMargin = 95
        leftLabelLayoutParams.removeRule(RelativeLayout.ALIGN_PARENT_START)
        leftLabelLayoutParams.removeRule(RelativeLayout.CENTER_HORIZONTAL)
        leftLabelLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
        labelBottomLeft.layoutParams = leftLabelLayoutParams
        
        // Right label
        val rightLabelLayoutParams = labelBottomRight.layoutParams as RelativeLayout.LayoutParams
        rightLabelLayoutParams.marginStart = (rightCircleCenterX - (labelWidthApprox / 2f)).toInt()
        rightLabelLayoutParams.bottomMargin = 95
        rightLabelLayoutParams.removeRule(RelativeLayout.ALIGN_PARENT_START)
        rightLabelLayoutParams.removeRule(RelativeLayout.CENTER_HORIZONTAL)
        rightLabelLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
        labelBottomRight.layoutParams = rightLabelLayoutParams
        
        // Top labels
        val topLabelMarginFromBottom = topOvalMarginFromBottom - (25 * resources.displayMetrics.density)
        val topLabelLayoutParams = labelTop.layoutParams as RelativeLayout.LayoutParams
        topLabelLayoutParams.bottomMargin = topLabelMarginFromBottom.toInt()
        labelTop.layoutParams = topLabelLayoutParams
        
        val extraTopLabelLayoutParams = labelExtraTop.layoutParams as RelativeLayout.LayoutParams
        extraTopLabelLayoutParams.bottomMargin = topLabelMarginFromBottom.toInt()
        labelExtraTop.layoutParams = extraTopLabelLayoutParams
    }

    private fun expandLogo() {
        isExpanded = true
        
        // Show all circles and labels
        logoCircleTop.visibility = VISIBLE
        logoCircleBottomLeft.visibility = VISIBLE
        logoCircleBottomRight.visibility = VISIBLE
        logoCircleExtraTop.visibility = VISIBLE
        labelTop.visibility = VISIBLE
        labelBottomLeft.visibility = VISIBLE
        labelBottomRight.visibility = VISIBLE
        labelExtraTop.visibility = VISIBLE
        
        // Set initial alpha to 0
        logoCircleTop.alpha = 0f
        logoCircleBottomLeft.alpha = 0f
        logoCircleBottomRight.alpha = 0f
        logoCircleExtraTop.alpha = 0f
        labelTop.alpha = 0f
        labelBottomLeft.alpha = 0f
        labelBottomRight.alpha = 0f
        labelExtraTop.alpha = 0f
        
        // Animate ovals
        animateOvals()
        
        // Animate circles
        animateCircles()
        
        // Animate labels
        animateLabels()
        
        // Show close button
        closeButton.visibility = VISIBLE
        closeButton.animate()
            .alpha(1f)
            .setDuration(DURATION_SHORT)
            .setStartDelay(DELAY_THIRD)
            .start()
    }

    private fun animateOvals() {
        logoOvalTop.animate()
            .alpha(0f)
            .setDuration(DURATION_SHORT)
            .start()
        logoOvalTop.animate()
            .scaleX(OVAL_EXPAND_SCALE_X)
            .scaleY(OVAL_EXPAND_SCALE_Y)
            .translationY(-VERTICAL_EXPANSION_RATIO * height)
            .setDuration(DURATION_LONG)
            .start()
            
        logoOvalBottomLeft.animate()
            .alpha(0f)
            .setDuration(DURATION_SHORT)
            .start()
        logoOvalBottomLeft.animate()
            .scaleX(OVAL_EXPAND_SCALE_X)
            .scaleY(OVAL_EXPAND_SCALE_Y)
            .translationX(leftExpandPositionX)
            .setDuration(DURATION_LONG)
            .start()
            
        logoOvalBottomRight.animate()
            .alpha(0f)
            .setDuration(DURATION_SHORT)
            .start()
        logoOvalBottomRight.animate()
            .scaleX(OVAL_EXPAND_SCALE_X)
            .scaleY(OVAL_EXPAND_SCALE_Y)
            .translationX(rightExpandPositionX)
            .setDuration(DURATION_LONG)
            .start()
    }

    private fun animateCircles() {
        logoCircleTop.animate()
            .alpha(1f)
            .setDuration(DURATION_SHORT)
            .setStartDelay(DELAY_FIRST)
            .start()
        logoCircleTop.animate()
            .scaleX(CIRCLE_NORMAL_SCALE)
            .scaleY(CIRCLE_NORMAL_SCALE)
            .translationY(-VERTICAL_EXPANSION_RATIO * height)
            .setDuration(DURATION_LONG)
            .setStartDelay(DELAY_FIRST)
            .start()
            
        logoCircleBottomLeft.animate()
            .alpha(1f)
            .setDuration(DURATION_SHORT)
            .setStartDelay(DELAY_FIRST)
            .start()
        logoCircleBottomLeft.animate()
            .scaleX(CIRCLE_NORMAL_SCALE)
            .scaleY(CIRCLE_NORMAL_SCALE)
            .translationX(leftExpandPositionX)
            .setDuration(DURATION_LONG)
            .setStartDelay(DELAY_FIRST)
            .start()
            
        logoCircleBottomRight.animate()
            .alpha(1f)
            .setDuration(DURATION_SHORT)
            .setStartDelay(DELAY_FIRST)
            .start()
        logoCircleBottomRight.animate()
            .scaleX(CIRCLE_NORMAL_SCALE)
            .scaleY(CIRCLE_NORMAL_SCALE)
            .translationX(rightExpandPositionX)
            .setDuration(DURATION_LONG)
            .setStartDelay(DELAY_FIRST)
            .start()

        logoCircleExtraTop.animate()
            .alpha(1f)
            .setDuration(DURATION_SHORT)
            .setStartDelay(DELAY_SECOND)
            .start()
        logoCircleExtraTop.animate()
            .scaleX(CIRCLE_NORMAL_SCALE)
            .scaleY(CIRCLE_NORMAL_SCALE)
            .translationY(-EXTRA_VERTICAL_EXPANSION_RATIO * height)
            .setDuration(DURATION_LONG)
            .setStartDelay(DELAY_SECOND)
            .start()
    }

    private fun animateLabels() {
        labelTop.animate()
            .alpha(1f)
            .translationY(-VERTICAL_EXPANSION_RATIO * height)
            .setDuration(DURATION_LONG)
            .setStartDelay(DELAY_THIRD)
            .start()

        labelBottomLeft.animate()
            .alpha(1f)
            .translationX(leftExpandPositionX)
            .setDuration(DURATION_LONG)
            .setStartDelay(DELAY_THIRD)
            .start()

        labelBottomRight.animate()
            .alpha(1f)
            .translationX(rightExpandPositionX)
            .setDuration(DURATION_LONG)
            .setStartDelay(DELAY_THIRD)
            .start()

        labelExtraTop.animate()
            .alpha(1f)
            .translationY(-EXTRA_VERTICAL_EXPANSION_RATIO * height)
            .setDuration(DURATION_LONG)
            .setStartDelay(DELAY_FIFTH)
            .start()
    }

    private fun collapseLogo() {
        isExpanded = false
        
        closeButton.animate()
            .alpha(0f)
            .setDuration(DURATION_SHORT)
            .withEndAction {
                closeButton.visibility = INVISIBLE
            }
            .start()
        
        // Animate circles out
        animateCirclesOut()
        
        // Animate labels out
        animateLabelsOut()
        
        // Animate ovals back
        animateOvalsBack()
    }

    private fun animateCirclesOut() {
        logoCircleTop.animate()
            .alpha(0f)
            .setDuration(DURATION_SHORT)
            .setStartDelay(DELAY_FOURTH)
            .start()
        logoCircleTop.animate()
            .scaleX(CIRCLE_COLLAPSE_SCALE_X)
            .scaleY(CIRCLE_COLLAPSE_SCALE_Y)
            .translationY(POSITION_CENTER)
            .setDuration(DURATION_LONG)
            .setStartDelay(DELAY_SECOND)
            .withEndAction {
                logoCircleTop.visibility = INVISIBLE
                logoCircleTop.scaleX = CIRCLE_NORMAL_SCALE
                logoCircleTop.scaleY = CIRCLE_NORMAL_SCALE
            }
            .start()
            
        logoCircleBottomLeft.animate()
            .alpha(0f)
            .setDuration(DURATION_SHORT)
            .setStartDelay(DELAY_FOURTH)
            .start()
        logoCircleBottomLeft.animate()
            .scaleX(CIRCLE_COLLAPSE_SCALE_X)
            .scaleY(CIRCLE_COLLAPSE_SCALE_Y)
            .translationX(POSITION_CENTER)
            .setDuration(DURATION_LONG)
            .setStartDelay(DELAY_SECOND)
            .withEndAction {
                logoCircleBottomLeft.visibility = INVISIBLE
                logoCircleBottomLeft.scaleX = CIRCLE_NORMAL_SCALE
                logoCircleBottomLeft.scaleY = CIRCLE_NORMAL_SCALE
            }
            .start()
            
        logoCircleBottomRight.animate()
            .alpha(0f)
            .setDuration(DURATION_SHORT)
            .setStartDelay(DELAY_FOURTH)
            .start()
        logoCircleBottomRight.animate()
            .scaleX(CIRCLE_COLLAPSE_SCALE_X)
            .scaleY(CIRCLE_COLLAPSE_SCALE_Y)
            .translationX(POSITION_CENTER)
            .setDuration(DURATION_LONG)
            .setStartDelay(DELAY_SECOND)
            .withEndAction {
                logoCircleBottomRight.visibility = INVISIBLE
                logoCircleBottomRight.scaleX = CIRCLE_NORMAL_SCALE
                logoCircleBottomRight.scaleY = CIRCLE_NORMAL_SCALE
            }
            .start()
        
        logoCircleExtraTop.animate()
            .alpha(0f)
            .setDuration(DURATION_SHORT)
            .setStartDelay(DELAY_FOURTH)
            .start()
        logoCircleExtraTop.animate()
            .scaleX(CIRCLE_COLLAPSE_SCALE_X)
            .scaleY(CIRCLE_COLLAPSE_SCALE_Y)
            .translationY(POSITION_CENTER)
            .setDuration(DURATION_LONG)
            .setStartDelay(DELAY_SECOND)
            .withEndAction {
                logoCircleExtraTop.visibility = INVISIBLE
                logoCircleExtraTop.scaleX = CIRCLE_NORMAL_SCALE
                logoCircleExtraTop.scaleY = CIRCLE_NORMAL_SCALE
            }
            .start()
    }

    private fun animateLabelsOut() {
        labelTop.animate()
            .alpha(0f)
            .translationY(POSITION_CENTER)
            .setDuration(DURATION_LONG)
            .setStartDelay(DELAY_SECOND)
            .withEndAction {
                labelTop.visibility = INVISIBLE
            }
            .start()

        labelBottomLeft.animate()
            .alpha(0f)
            .translationX(POSITION_CENTER)
            .setDuration(DURATION_LONG)
            .setStartDelay(DELAY_SECOND)
            .withEndAction {
                labelBottomLeft.visibility = INVISIBLE
            }
            .start()

        labelBottomRight.animate()
            .alpha(0f)
            .translationX(POSITION_CENTER)
            .setDuration(DURATION_LONG)
            .setStartDelay(DELAY_SECOND)
            .withEndAction {
                labelBottomRight.visibility = INVISIBLE
            }
            .start()

        labelExtraTop.animate()
            .alpha(0f)
            .translationY(POSITION_CENTER)
            .setDuration(DURATION_LONG)
            .setStartDelay(DELAY_FIRST)
            .withEndAction {
                labelExtraTop.visibility = INVISIBLE
            }
            .start()
    }

    private fun animateOvalsBack() {
        logoOvalTop.animate()
            .alpha(1f)
            .setDuration(DURATION_SHORT)
            .setStartDelay(DELAY_FIFTH)
            .start()
        logoOvalTop.animate()
            .scaleX(CIRCLE_NORMAL_SCALE)
            .scaleY(CIRCLE_NORMAL_SCALE)
            .translationY(POSITION_CENTER)
            .setDuration(DURATION_LONG)
            .setStartDelay(DELAY_THIRD)
            .start()
            
        logoOvalBottomLeft.animate()
            .alpha(1f)
            .setDuration(DURATION_SHORT)
            .setStartDelay(DELAY_FIFTH)
            .start()
        logoOvalBottomLeft.animate()
            .scaleX(CIRCLE_NORMAL_SCALE)
            .scaleY(CIRCLE_NORMAL_SCALE)
            .translationX(POSITION_CENTER)
            .setDuration(DURATION_LONG)
            .setStartDelay(DELAY_THIRD)
            .start()
            
        logoOvalBottomRight.animate()
            .alpha(1f)
            .setDuration(DURATION_SHORT)
            .setStartDelay(DELAY_FIFTH)
            .start()
        logoOvalBottomRight.animate()
            .scaleX(CIRCLE_NORMAL_SCALE)
            .scaleY(CIRCLE_NORMAL_SCALE)
            .translationX(POSITION_CENTER)
            .setDuration(DURATION_LONG)
            .setStartDelay(DELAY_THIRD)
            .start()
    }

    // Public methods for external control
    fun setCallbacks(callbacks: LogoOverlayCallbacks) {
        this.callbacks = callbacks
    }

    fun setMapTheme(useMapTheme: Boolean) {
        this.useMapTheme = useMapTheme
        updateThemeColors()
    }
    
    private fun updateThemeColors() {
        val textColor = if (useMapTheme) {
            ContextCompat.getColor(context, R.color.red) // Red text on map
        } else {
            ContextCompat.getColor(context, R.color.white) // White text on main screen
        }
        
        val closeButtonBg = if (useMapTheme) {
            R.drawable.white_circle_button // White button on map for contrast
        } else {
            R.drawable.red_circle_button // Red button on main screen
        }
        
        // Choose close button icon based on theme
        val closeButtonIcon = if (useMapTheme) {
            R.drawable.ic_close_red // Red X on map
        } else {
            R.drawable.ic_close // White X on main screen
        }
        
        // Choose logo drawables based on theme
        val ovalDrawable = if (useMapTheme) {
            R.drawable.state_farm_oval_inverted // Red oval on map
        } else {
            R.drawable.state_farm_oval_white // White oval on main screen
        }
        
        val circleDrawable = if (useMapTheme) {
            R.drawable.state_farm_circle_inverted // Red circles on map
        } else {
            R.drawable.state_farm_circle // White circles on main screen
        }
        
        // Update all label colors
        (labelTop as? TextView)?.setTextColor(textColor)
        (labelBottomLeft as? TextView)?.setTextColor(textColor)
        (labelBottomRight as? TextView)?.setTextColor(textColor)
        (labelExtraTop as? TextView)?.setTextColor(textColor)
        
        // Update close button background
        closeButton.setBackgroundResource(closeButtonBg)
        
        // Update close button icon
        closeButton.setImageResource(closeButtonIcon)
        
        // Update logo oval drawables
        (logoOvalTop as? android.widget.ImageView)?.setImageResource(ovalDrawable)
        (logoOvalBottomLeft as? android.widget.ImageView)?.setImageResource(ovalDrawable)
        (logoOvalBottomRight as? android.widget.ImageView)?.setImageResource(ovalDrawable)
        
        // Update logo circle drawables
        (logoCircleTop as? android.widget.ImageView)?.setImageResource(circleDrawable)
        (logoCircleBottomLeft as? android.widget.ImageView)?.setImageResource(circleDrawable)
        (logoCircleBottomRight as? android.widget.ImageView)?.setImageResource(circleDrawable)
        (logoCircleExtraTop as? android.widget.ImageView)?.setImageResource(circleDrawable)
    }

    fun updateAuthenticationState() {
        isLoggedIn = auth.currentUser != null
        enableLogoClickability(isLoggedIn)
    }
    
    private fun enableLogoClickability(enabled: Boolean) {
        logoOvalTop.isClickable = enabled
        logoOvalBottomLeft.isClickable = enabled
        logoOvalBottomRight.isClickable = enabled
    }

    fun reset() {
        if (isExpanded) {
            collapseLogo()
        }
    }

    fun forceCollapse() {
        isExpanded = false
        
        // Hide all expanded elements immediately
        logoCircleTop.visibility = INVISIBLE
        logoCircleBottomLeft.visibility = INVISIBLE
        logoCircleBottomRight.visibility = INVISIBLE
        logoCircleExtraTop.visibility = INVISIBLE
        labelTop.visibility = INVISIBLE
        labelBottomLeft.visibility = INVISIBLE
        labelBottomRight.visibility = INVISIBLE
        labelExtraTop.visibility = INVISIBLE
        closeButton.visibility = INVISIBLE
        
        // Reset ovals to default state
        logoOvalTop.alpha = 1f
        logoOvalTop.scaleX = CIRCLE_NORMAL_SCALE
        logoOvalTop.scaleY = CIRCLE_NORMAL_SCALE
        logoOvalTop.translationY = POSITION_CENTER
        
        logoOvalBottomLeft.alpha = 1f
        logoOvalBottomLeft.scaleX = CIRCLE_NORMAL_SCALE
        logoOvalBottomLeft.scaleY = CIRCLE_NORMAL_SCALE
        logoOvalBottomLeft.translationX = POSITION_CENTER
        
        logoOvalBottomRight.alpha = 1f
        logoOvalBottomRight.scaleX = CIRCLE_NORMAL_SCALE
        logoOvalBottomRight.scaleY = CIRCLE_NORMAL_SCALE
        logoOvalBottomRight.translationX = POSITION_CENTER
        
        // Reset all other elements
        resetCircleStates()
    }
    
    private fun resetCircleStates() {
        logoCircleTop.alpha = 0f
        logoCircleTop.scaleX = CIRCLE_NORMAL_SCALE
        logoCircleTop.scaleY = CIRCLE_NORMAL_SCALE
        logoCircleTop.translationY = POSITION_CENTER
        
        logoCircleBottomLeft.alpha = 0f
        logoCircleBottomLeft.scaleX = CIRCLE_NORMAL_SCALE
        logoCircleBottomLeft.scaleY = CIRCLE_NORMAL_SCALE
        logoCircleBottomLeft.translationX = POSITION_CENTER
        
        logoCircleBottomRight.alpha = 0f
        logoCircleBottomRight.scaleX = CIRCLE_NORMAL_SCALE
        logoCircleBottomRight.scaleY = CIRCLE_NORMAL_SCALE
        logoCircleBottomRight.translationX = POSITION_CENTER
        
        logoCircleExtraTop.alpha = 0f
        logoCircleExtraTop.scaleX = CIRCLE_NORMAL_SCALE
        logoCircleExtraTop.scaleY = CIRCLE_NORMAL_SCALE
        logoCircleExtraTop.translationY = POSITION_CENTER
        
        labelTop.alpha = 0f
        labelTop.translationY = POSITION_CENTER
        labelBottomLeft.alpha = 0f
        labelBottomLeft.translationX = POSITION_CENTER
        labelBottomRight.alpha = 0f
        labelBottomRight.translationX = POSITION_CENTER
        labelExtraTop.alpha = 0f
        labelExtraTop.translationY = POSITION_CENTER
        
        closeButton.alpha = 0f
    }
} 