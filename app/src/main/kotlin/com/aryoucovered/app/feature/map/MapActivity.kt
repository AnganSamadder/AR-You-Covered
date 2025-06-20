package com.aryoucovered.app.feature.map

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.mapbox.geojson.Point
import com.mapbox.maps.*
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.viewannotation.geometry
import com.mapbox.maps.viewannotation.viewAnnotationOptions
import com.mapbox.bindgen.Value
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.aryoucovered.app.R
import com.google.android.gms.location.*
import android.location.Location
import android.widget.Toast
import android.os.Looper
import android.util.Log
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.IOException
import com.aryoucovered.app.presentation.activity.SettingsActivity
import com.aryoucovered.app.feature.game.GameActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.core.app.ActivityCompat
import androidx.core.content.PermissionChecker
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.drawable.toBitmap
import com.aryoucovered.app.presentation.components.LogoOverlayComponent
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.MemoryPolicy
import com.squareup.picasso.NetworkPolicy
import com.squareup.picasso.Picasso
import java.io.InputStream
import com.aryoucovered.app.presentation.activity.CollectionsActivity
import com.aryoucovered.app.presentation.activity.StoreActivity
import com.aryoucovered.app.presentation.activity.LeaderboardActivity

/**
 * MapActivity with modern Mapbox integration and gaming-style aesthetic
 * Supports all screen sizes with programmatic layout creation
 * Includes artifact markers, geofencing, and AR game integration
 * Automatically requests location permissions on startup
 */
class MapActivity : AppCompatActivity(), LogoOverlayComponent.LogoOverlayCallbacks {

    private lateinit var mapView: MapView
    private lateinit var mapboxMap: MapboxMap
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var recenterButton: ImageButton
    private lateinit var cameraButton: FloatingActionButton
    private lateinit var currentLocation: Location
    private lateinit var logoOverlay: LogoOverlayComponent
    private lateinit var auth: FirebaseAuth

    companion object {
        // Default location (San Francisco) - fallback if location unavailable
        private val DEFAULT_LOCATION = Point.fromLngLat(-122.4194, 37.7749)
        private const val DEFAULT_ZOOM = 18.0
        private const val DEFAULT_PITCH = 45.0
        private const val DEFAULT_BEARING = 0.0

        // Gaming-style colors
        private const val UI_BACKGROUND_COLOR = 0xDD1A1A1A
        private const val ACCENT_COLOR = 0xFF00E5FF
        private const val SUCCESS_COLOR = 0xFF4CAF50
        
        // Permission request codes
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Firebase auth
        auth = FirebaseAuth.getInstance()
        
        // Initialize location client and request
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createLocationRequest()
        
        setupResponsiveLayout()
        
        // Check and request permissions automatically, then initialize map
        checkAndRequestLocationPermissions()
    }
    
    /**
     * Check if location permissions are granted and request them if needed
     */
    private fun checkAndRequestLocationPermissions() {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        val coarseLocationGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        if (fineLocationGranted && coarseLocationGranted) {
            // Permissions already granted, initialize map
            Log.d("PERMISSIONS", "Location permissions already granted")
            initializeMap()
            return
        }
        
        // Need to request permissions
        val permissionsNeeded = mutableListOf<String>()
        
        if (!fineLocationGranted) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (!coarseLocationGranted) {
            permissionsNeeded.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        
        // Check if we should show permission rationale
        val shouldShowRationale = permissionsNeeded.any { permission ->
            ActivityCompat.shouldShowRequestPermissionRationale(this, permission)
        }
        
        if (shouldShowRationale) {
            showLocationPermissionRationale(permissionsNeeded.toTypedArray())
        } else {
            // Request permissions directly
            Log.d("PERMISSIONS", "Requesting location permissions automatically")
            ActivityCompat.requestPermissions(
                this,
                permissionsNeeded.toTypedArray(),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }
    
    /**
     * Show explanation dialog for why location permissions are needed
     */
    private fun showLocationPermissionRationale(permissions: Array<String>) {
        AlertDialog.Builder(this)
            .setTitle("Location Permission Required")
            .setMessage("This app needs location access to:\n\n" +
                       "• Show your position on the map\n" +
                       "• Find nearby artifacts\n" +
                       "• Enable AR features\n" +
                       "• Provide accurate directions\n\n" +
                       "Please grant location permission to continue.")
            .setPositiveButton("Grant Permission") { _, _ ->
                ActivityCompat.requestPermissions(
                    this,
                    permissions,
                    LOCATION_PERMISSION_REQUEST_CODE
                )
            }
            .setNegativeButton("Skip") { _, _ ->
                // Initialize map without location features
                Toast.makeText(this, "Location features will be limited", Toast.LENGTH_LONG).show()
                initializeMap()
            }
            .setCancelable(false)
            .show()
    }
    
    /**
     * Handle permission request results
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                val fineLocationGranted = grantResults.isNotEmpty() && 
                    permissions.indexOf(Manifest.permission.ACCESS_FINE_LOCATION).let { index ->
                        index >= 0 && grantResults[index] == PackageManager.PERMISSION_GRANTED
                    }
                
                val coarseLocationGranted = grantResults.isNotEmpty() && 
                    permissions.indexOf(Manifest.permission.ACCESS_COARSE_LOCATION).let { index ->
                        index >= 0 && grantResults[index] == PackageManager.PERMISSION_GRANTED
                    }
                
                if (fineLocationGranted || coarseLocationGranted) {
                    Log.d("PERMISSIONS", "Location permission granted!")
                    Toast.makeText(this, "Location permission granted! Initializing map...", Toast.LENGTH_SHORT).show()
                    
                    // Check for background location permission (for geofencing)
                    checkBackgroundLocationPermission()
                } else {
                    Log.d("PERMISSIONS", "Location permission denied")
                    Toast.makeText(this, "Location permission denied. Map will use default location.", Toast.LENGTH_LONG).show()
                }
                
                // Initialize map regardless
                initializeMap()
            }
            
            BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE -> {
                val backgroundLocationGranted = grantResults.isNotEmpty() && 
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
                
                if (backgroundLocationGranted) {
                    Log.d("PERMISSIONS", "Background location permission granted!")
                    Toast.makeText(this, "Background location enabled for better geofencing", Toast.LENGTH_SHORT).show()
                } else {
                    Log.d("PERMISSIONS", "Background location permission denied")
                    Toast.makeText(this, "Geofencing may be limited without background location", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    /**
     * Check and request background location permission (Android 10+)
     */
    private fun checkBackgroundLocationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val backgroundLocationGranted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            
            if (!backgroundLocationGranted) {
                // Only request if foreground permission is granted
                val foregroundGranted = ContextCompat.checkSelfPermission(
                    this, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                
                if (foregroundGranted) {
                    AlertDialog.Builder(this)
                        .setTitle("Background Location")
                        .setMessage("For the best AR experience, please allow background location access. This enables artifact detection even when the app is minimized.")
                        .setPositiveButton("Allow") { _, _ ->
                            ActivityCompat.requestPermissions(
                                this,
                                arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                                BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE
                            )
                        }
                        .setNegativeButton("Skip") { _, _ ->
                            Log.d("PERMISSIONS", "Background location permission skipped")
                        }
                        .show()
                }
            }
        }
    }

    /**
     * Create location request for getting current location
     */
    private fun createLocationRequest() {
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(5000)
            .setMaxUpdateDelayMillis(15000)
            .build()
    }

    /**
     * Creates responsive layout that adapts to all screen sizes
     */
    private fun setupResponsiveLayout() {
        val displayMetrics = DisplayMetrics()
        
        // Use modern WindowMetrics API for API 30+ and fallback for older versions
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            val windowMetrics = windowManager.currentWindowMetrics
            val bounds = windowMetrics.bounds
            displayMetrics.widthPixels = bounds.width()
            displayMetrics.heightPixels = bounds.height()
            displayMetrics.density = resources.displayMetrics.density
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getMetrics(displayMetrics)
        }
        
        // Create main container with responsive margins
        val containerLayout = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.BLACK)
        }

        // Create MapView with responsive sizing
        mapView = MapView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        // Create floating recenter button
        recenterButton = ImageButton(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                (56 * displayMetrics.density).toInt(),
                (56 * displayMetrics.density).toInt()
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.END
                marginEnd = (16 * displayMetrics.density).toInt()
                bottomMargin = (100 * displayMetrics.density).toInt()
            }
            
            // Style the button
            setImageResource(android.R.drawable.ic_menu_mylocation)
            background = ContextCompat.getDrawable(context, android.R.drawable.btn_default)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setPadding(12, 12, 12, 12)
            
            // Set click listener with permission check
            setOnClickListener {
                if (hasLocationPermission()) {
                    centerMapOnUserLocation()
                } else {
                    Toast.makeText(context, "Location permission required to center map", Toast.LENGTH_SHORT).show()
                    // Optionally trigger permission request again
                    checkAndRequestLocationPermissions()
                }
            }
        }

        // Create camera button (like original MapActivity)
        cameraButton = FloatingActionButton(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                (72 * displayMetrics.density).toInt(),
                (72 * displayMetrics.density).toInt()
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                bottomMargin = (20 * displayMetrics.density).toInt()
            }
            
            setImageResource(R.drawable.camera_button_selector)
            backgroundTintList = null // Remove default FAB background to use our custom design
            elevation = 8f
            compatElevation = 8f
            scaleType = ImageView.ScaleType.CENTER
            setOnClickListener {
                startGameActivity()
            }
        }

        // Create logo overlay component
        logoOverlay = LogoOverlayComponent(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setCallbacks(this@MapActivity)
            setMapTheme(true) // Use map theme with red text
        }

        containerLayout.addView(mapView)
        containerLayout.addView(recenterButton)
        containerLayout.addView(cameraButton)
        containerLayout.addView(logoOverlay)
        setContentView(containerLayout)
        
        // Update authentication state after views are set up
        logoOverlay.updateAuthenticationState()
    }

    /**
     * Start GameActivity with location data (like original MapActivity)
     */
    private fun startGameActivity() {
        if (::currentLocation.isInitialized) {
            val intent = Intent(this, GameActivity::class.java)
            intent.putExtra("lat", currentLocation.latitude)
            intent.putExtra("lon", currentLocation.longitude)
            intent.putExtra("alt", currentLocation.altitude)
            startActivity(intent)
        } else {
            Toast.makeText(this, "Getting location, please wait...", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Initialize Mapbox with gaming-style aesthetic and user location
     */
    private fun initializeMap() {
        mapboxMap = mapView.mapboxMap

        // Set style to Standard with 3D buildings and dynamic lighting
        mapboxMap.loadStyle(Style.STANDARD) { style ->
            configureMapStyle(style)
            
            // Enable location component if permissions are granted
            if (hasLocationPermission()) {
                setupLocationComponent()
                // Get user location and center map automatically
                centerMapOnUserLocation()
            } else {
                // Use default location if no permissions
                useDefaultLocation()
            }
            
            // Load artifacts from Firestore
            loadAnchorsFromFirestore()
        }
    }
    
    /**
     * Check if location permissions are granted
     */
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || 
        ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Center map on user's current location with fresh location request
     */
    private fun centerMapOnUserLocation() {
        if (!hasLocationPermission()) {
            Toast.makeText(this, "Location permission required to center map", Toast.LENGTH_SHORT).show()
            useDefaultLocation()
            return
        }
        
        try {
            // Show loading indicator
            Toast.makeText(this, "Getting your location...", Toast.LENGTH_SHORT).show()
            
            // Request fresh location
            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    locationResult.lastLocation?.let { location ->
                        // Store current location for GameActivity
                        currentLocation = location
                        
                        // Use user's current location
                        val targetLocation = Point.fromLngLat(location.longitude, location.latitude)
                        
                        // Animate to the target location with gaming perspective
                        val cameraOptions = CameraOptions.Builder()
                            .center(targetLocation)
                            .zoom(DEFAULT_ZOOM)
                            .pitch(DEFAULT_PITCH)
                            .bearing(DEFAULT_BEARING)
                            .build()
                        
                        // Set camera to location (using setCamera for reliability)
                        mapboxMap.setCamera(cameraOptions)
                        
                        Toast.makeText(this@MapActivity, "Centered on your location!", Toast.LENGTH_SHORT).show()
                    }
                    
                    // Stop location updates after getting one result
                    fusedLocationClient.removeLocationUpdates(this)
                }
            }
            
            // Request location update
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            
            // Also try to get last known location as backup
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    // Store current location for GameActivity
                    currentLocation = location
                    
                    val targetLocation = Point.fromLngLat(location.longitude, location.latitude)
                    
                    val cameraOptions = CameraOptions.Builder()
                        .center(targetLocation)
                        .zoom(DEFAULT_ZOOM)
                        .pitch(DEFAULT_PITCH)
                        .bearing(DEFAULT_BEARING)
                        .build()
                    
                    mapboxMap.setCamera(cameraOptions)
                }
            }.addOnFailureListener {
                // If all location attempts fail, use default location
                useDefaultLocation()
            }
            
        } catch (e: SecurityException) {
            // Permission not granted, use default location
            useDefaultLocation()
            Toast.makeText(this, "Location permission needed for best experience", Toast.LENGTH_LONG).show()
        }
    }
    
    /**
     * Fallback to default location
     */
    private fun useDefaultLocation() {
        val cameraOptions = CameraOptions.Builder()
            .center(DEFAULT_LOCATION)
            .zoom(DEFAULT_ZOOM)
            .pitch(DEFAULT_PITCH)
            .bearing(DEFAULT_BEARING)
            .build()
        
        mapboxMap.setCamera(cameraOptions)
        Toast.makeText(this, "Using default location (San Francisco)", Toast.LENGTH_SHORT).show()
    }

    /**
     * Configure map style for gaming aesthetics and load custom marker
     */
    private fun configureMapStyle(style: Style) {
        // Configure 3D buildings and gaming aesthetics
        style.setStyleImportConfigProperty(
            "standard",
            "showPlaceLabels",
            Value.valueOf(true)
        )
        style.setStyleImportConfigProperty(
            "standard",
            "showRoadLabels", 
            Value.valueOf(true)
        )
        style.setStyleImportConfigProperty(
            "standard",
            "show3dObjects",
            Value.valueOf(true)
        )
        style.setStyleImportConfigProperty(
            "standard",
            "lightPreset",
            Value.valueOf("dusk") // Gaming-style lighting
        )
        style.setStyleImportConfigProperty(
            "standard",
            "theme",
            Value.valueOf("default")
        )

        // Load custom Artifact marker icon
        firestore.collection("anchors").get().addOnSuccessListener { result ->
            var artifactCount = 0
            for (document in result.documents) {
                val icon = document.getString("profile_pic")
                val name = "Artifact ${++artifactCount}"
                style.addImage(name, BitmapFactory.decodeResource(resources, R.drawable.question_mark))
                loadCustomMarkerIcon(style, name.toString(), icon.toString())
            }
            Log.d("ANCHORS", "Loaded icons")
        }.addOnFailureListener {
            Log.e("ANCHORS", "Failed to load anchors", it)
        }
    }

    private val activeTargets = mutableSetOf<com.squareup.picasso.Target>()

    /**
     * Load custom marker icon from assets (same as original MapActivity)
     */
    private fun loadCustomMarkerIcon(style: Style, name: String, location: String) {
        try {
            // Load the same icon as original MapActivity
            val picassoTarget = object : com.squareup.picasso.Target {
                override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                    bitmap?.let {
                        // Use the bitmap here—Mapbox custom marker, save to file, etc.
                        style.addImage(name, it)
                        Log.d("MAPBOX", "Custom marker added to map!")
                    }
                    activeTargets.remove(this) // cleanup
                }

                override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                    Log.e("PICASSO", "Image load failed", e)
                    activeTargets.remove(this)
                }

                override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
                    // Optional: do something while it loads
                }
            }

            activeTargets.add(picassoTarget)

            Picasso.get()
                .load(location)
                .placeholder(R.drawable.default_pfp)
                .error(R.drawable.default_pfp)
                .memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)
                .networkPolicy(NetworkPolicy.NO_CACHE, NetworkPolicy.NO_STORE)
                .into(picassoTarget)

        } catch (e: IOException) {
            Log.e("MARKER", "Failed to load custom marker icon", e)
            // Fallback to default marker will be handled in marker creation
        }
    }

    private fun setupLocationComponent() {
        if (!hasLocationPermission()) {
            Log.d("LOCATION", "Location permission not granted, skipping location component setup")
            return
        }
        
        try {
            mapView.location.updateSettings {
                enabled = true
                pulsingEnabled = true
                pulsingColor = ContextCompat.getColor(this@MapActivity, android.R.color.holo_blue_bright)
                pulsingMaxRadius = 50.0f
            }
        } catch (e: SecurityException) {
            Log.e("LOCATION", "Failed to setup location component: permission denied", e)
        }
    }

    /**
     * Load AR anchor locations from Firestore and add as map markers
     */
    private fun loadAnchorsFromFirestore() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Use the same collection name as original MapActivity
                val anchorsSnapshot = firestore.collection("anchors").get().await()
                val annotationApi = mapView.annotations
                val pointAnnotationManager = annotationApi.createPointAnnotationManager()

                var artifactCount = 0
                for (document in anchorsSnapshot.documents) {
                    // Use the same field names as original MapActivity (capitalized)
                    val latitude = document.getDouble("Latitude") ?: continue
                    val longitude = document.getDouble("Longitude") ?: continue
                    
                    artifactCount++
                    val artifactName = "Artifact $artifactCount"

                    // Create gaming-style marker with Artifact naming
                    val pointAnnotationOptions = PointAnnotationOptions()
                        .withPoint(Point.fromLngLat(longitude, latitude))
                        .withIconSize(1.2) // Slightly larger for visibility
                        .withTextField(artifactName)
                        .withTextColor(Color.WHITE)
                        .withTextHaloColor(Color.BLACK)
                        .withTextHaloWidth(3.0)
                        .withTextSize(14.0)
                        .withTextOffset(listOf(0.0, -2.0)) // Offset text above marker
                    
                    // Try to use custom marker, fallback to default if not available
                    try {
                        pointAnnotationOptions.withIconImage("Artifact $artifactCount")
                    } catch (e: Exception) {
                        // Fallback to default marker
                        pointAnnotationOptions.withIconImage("marker-icon-default")
                    }

                    pointAnnotationManager.create(pointAnnotationOptions)
                }
                
                Log.d("ANCHORS", "Loaded $artifactCount artifact locations from 'anchors' collection")
                
                // Add geofencing for the loaded anchors
                setupGeofencing(anchorsSnapshot.documents)
                
            } catch (e: Exception) {
                // Handle error gracefully
                Log.e("ANCHORS", "Failed to load anchors from Firestore", e)
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Setup geofencing for anchor locations (similar to original MapActivity)
     */
    private fun setupGeofencing(anchorDocuments: List<com.google.firebase.firestore.DocumentSnapshot>) {
        try {
            val geofencingClient = LocationServices.getGeofencingClient(this)
            val geofenceList = mutableListOf<com.google.android.gms.location.Geofence>()
            
            var requestId = 0
            for (document in anchorDocuments) {
                val latitude = document.getDouble("Latitude") ?: continue
                val longitude = document.getDouble("Longitude") ?: continue
                
                val geofence = com.google.android.gms.location.Geofence.Builder()
                    .setRequestId("artifact_geofence_${++requestId}")
                    .setCircularRegion(latitude, longitude, 200f) // 200m radius like original
                    .setExpirationDuration(100000) // Same as original
                    .setTransitionTypes(
                        com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_ENTER or 
                        com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_EXIT
                    )
                    .build()
                
                geofenceList.add(geofence)
            }
            
            if (geofenceList.isNotEmpty()) {
                val geofencingRequest = com.google.android.gms.location.GeofencingRequest.Builder()
                    .setInitialTrigger(com.google.android.gms.location.GeofencingRequest.INITIAL_TRIGGER_ENTER)
                    .addGeofences(geofenceList)
                    .build()
                
                // Create pending intent for geofence events
                val geofenceIntent = android.content.Intent(this, com.aryoucovered.app.core.receiver.GeofenceBroadcastReceiver::class.java)
                val geofencePendingIntent = android.app.PendingIntent.getBroadcast(
                    this, 0, geofenceIntent, 
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_MUTABLE
                )
                
                // Add geofences (requires location permission)
                if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == 
                    android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    
                    geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)
                        .addOnSuccessListener {
                            Log.d("GEOFENCE", "Successfully added ${geofenceList.size} artifact geofences")
                        }
                        .addOnFailureListener { e ->
                            Log.e("GEOFENCE", "Failed to add geofences", e)
                        }
                }
            }
            
        } catch (e: Exception) {
            Log.e("GEOFENCE", "Error setting up geofencing", e)
        }
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
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
    
    override fun onResume() {
        super.onResume()
        // Update authentication state when returning to activity
        if (::logoOverlay.isInitialized) {
            logoOverlay.updateAuthenticationState()
        }
    }
    
    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        
        // Reset logo overlay on configuration change
        if (::logoOverlay.isInitialized) {
            logoOverlay.forceCollapse()
        }
    }
} 