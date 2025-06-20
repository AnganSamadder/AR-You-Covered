package com.aryoucovered.app.feature.game

import android.content.Intent
import android.os.*
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.aryoucovered.app.R
import com.google.ar.core.Config
import com.google.ar.core.Anchor
import com.google.ar.core.TrackingState
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.*
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.math.*
import io.github.sceneview.node.*
import kotlin.math.*
import kotlin.random.Random
import com.google.android.filament.EntityManager
import com.google.android.filament.LightManager
import com.google.ar.sceneform.rendering.ViewAttachmentManager
import io.github.sceneview.ar.node.AnchorNode
import com.google.firebase.auth.FirebaseAuth

class GameActivity : AppCompatActivity() {

    private var points = 0
    private val hitChance = 0.7f
    private lateinit var pointsText: TextView

    private lateinit var arSceneView: ARSceneView
    private lateinit var backButton: Button
    private lateinit var gestureDetector: GestureDetector
    private lateinit var policyNode: ModelNode
    private lateinit var objects: ArrayList<GameObject>
    private lateinit var firestore: FirebaseFirestore
    private lateinit var modelLoader: ModelLoader
    private lateinit var attachmentManager: ViewAttachmentManager
    private var loaded = false

    private var velocity = Position(0f, 0f, 0f)
    private var hasMoved = false
    private var shouldResetPosition = true
    private val swipeThreshold = 100f

    private var lastResetTime = 0L
    private var lastCameraPos: Position? = null
    private val resetDelayMillis = 1000L

    private val faceNodes = mutableListOf<ViewNode>()
    private val modelNodes = mutableListOf<ModelNode>()
    private var backgroundIntent: Intent? = null

    private lateinit var userPhone: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.game_ar)
        objects = ArrayList()

        pointsText = findViewById(R.id.points_text)
        pointsText.text = "Points: $points"

        userPhone = FirebaseAuth.getInstance().currentUser?.phoneNumber ?: run {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        arSceneView = findViewById(R.id.ar_scene_view)
        backButton = findViewById(R.id.back_button)
        backButton.setOnClickListener { finish() }

        modelLoader = ModelLoader(arSceneView.engine, this)
        attachmentManager = ViewAttachmentManager(this@GameActivity, arSceneView)
        attachmentManager.onResume()

        val modelInstance = modelLoader.createModelInstance("models/paper_tablet.glb")
        policyNode = ModelNode(modelInstance, scaleToUnits = 0.2f)
        arSceneView.addChildNode(policyNode)

        firestore = FirebaseFirestore.getInstance()
        addDirectionalLight()
        configureSession()
        backgroundIntent = Intent(this, BackgroundSoundService::class.java)
        startService(backgroundIntent)

        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true
            override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                if (e1 == null || hasMoved) return false
                val dx = e2.x - e1.x
                val dy = e2.y - e1.y
                if (abs(dx) > swipeThreshold || abs(dy) > swipeThreshold) {
                    val forward = arSceneView.cameraNode.forwardDirection
                    velocity = Position(forward.x * 0.07f, forward.y * 0.07f + (-dy / 10000f), forward.z * 0.07f)
                    hasMoved = true

                    if (simulateThrowHit()) {
                        val gained = Random.nextInt(5, 15)
                        points += gained
                        pointsText.text = "Points: $points"
                        updatePointsInFirestore(userPhone, points)
                        Log.i("THROW", "\uD83C\uDFAF Hit! +$gained points. Total: $points")
                    } else {
                        Log.i("THROW", "\uD83D\uDCA8 Miss! Total: $points")
                    }
                }
                return true
            }
        })

        arSceneView.setOnTouchListener { _, event -> gestureDetector.onTouchEvent(event); true }

        arSceneView.onFrame = {
            val camera = arSceneView.cameraNode
            val now = SystemClock.elapsedRealtime()

            if (!hasMoved) {
                val currentCameraPos = camera.worldPosition
                if (lastCameraPos != null && now - lastResetTime > resetDelayMillis && distanceBetween(currentCameraPos, lastCameraPos!!) > 0.02f) {
                    shouldResetPosition = true
                    lastResetTime = now
                }

                if (shouldResetPosition) {
                    val forward = camera.forwardDirection
                    val resetPos = Position(
                        camera.worldPosition.x + forward.x,
                        camera.worldPosition.y + forward.y,
                        camera.worldPosition.z + forward.z
                    )
                    policyNode.position = resetPos
                    val yaw = (atan2(forward.x, forward.z) * (180f / Math.PI)).toFloat()
                    policyNode.rotation = Rotation(0f, yaw + 180f, 0f)
                    shouldResetPosition = false
                    lastResetTime = now
                    lastCameraPos = camera.worldPosition
                }
            } else {
                velocity = Position(velocity.x, velocity.y - 0.0004f, velocity.z)
                val pos = policyNode.position
                val newY = (pos.y + velocity.y).coerceAtLeast(-0.5f)
                policyNode.position = Position(pos.x + velocity.x, newY, pos.z + velocity.z)
                policyNode.rotation = Rotation(policyNode.rotation.x - 2f, policyNode.rotation.y, policyNode.rotation.z)
                if (newY <= -0.5f) {
                    hasMoved = false
                    velocity = Position(0f, 0f, 0f)
                    shouldResetPosition = true
                    lastResetTime = now
                    lastCameraPos = camera.worldPosition
                }
            }

            fun faceCamera(node: Node, cameraPos: Position, facingOffset: Float = 0f) {
                val nodePos = node.worldPosition
                val dx = cameraPos.x - nodePos.x
                val dz = cameraPos.z - nodePos.z
                val angle = atan2(dx, dz) * (180f / PI).toFloat()
                node.rotation = Rotation(0f, angle + facingOffset, 0f)
            }

            val cameraPos = arSceneView.cameraNode.worldPosition
            for (faceNode in faceNodes) {
                faceCamera(faceNode, cameraPos)
            }
            for (modelNode in modelNodes) {
                faceCamera(modelNode, cameraPos, facingOffset = -90f)
            }

            if (!loaded && arSceneView.session?.earth?.trackingState == TrackingState.TRACKING) {
                loadGameObjects()
                loaded = true
            }
        }
    }

    private fun updatePointsInFirestore(phone: String, newPoints: Int) {
        firestore.collection("users").document(phone)
            .update("points", newPoints)
            .addOnSuccessListener {
                Log.d("POINTS", "Updated Firestore: $newPoints points")
            }
            .addOnFailureListener { e ->
                Log.e("POINTS", "Failed to update Firestore", e)
            }
    }

    private fun simulateThrowHit(): Boolean = Random.nextFloat() < hitChance

    private fun addDirectionalLight() {
        val lightEntity = EntityManager.get().create()
        LightManager.Builder(LightManager.Type.DIRECTIONAL)
            .color(1f, 1f, 1f)
            .intensity(50000f)
            .direction(0f, -1f, -0.3f)
            .castShadows(false)
            .build(arSceneView.engine, lightEntity)
        arSceneView.scene.addEntity(lightEntity)
    }

    private fun configureSession() {
        try {
            arSceneView.sessionConfiguration = { _, config ->
                config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
                config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
                config.instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP
                config.setGeospatialMode(Config.GeospatialMode.ENABLED)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error configuring AR session: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun distanceBetween(p1: Position, p2: Position): Float {
        val dx = p1.x - p2.x
        val dy = p1.y - p2.y
        val dz = p1.z - p2.z
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    private fun loadGameObject(attr: Attributes, anchor: Anchor): GameObject {
        val anchorNode = AnchorNode(arSceneView.engine, anchor)
        arSceneView.addChildNode(anchorNode)
        return if (attr.type.equals("Person", ignoreCase = true)) {
            val face = loadFace(anchorNode, attr.profilePicture)
            GameObject(face, attr)
        } else {
            val modelInstance = modelLoader.createModelInstance(attr.modelPath)
            if (modelInstance == null) {
                Log.e("MODEL_LOAD", "Failed to load model at ${attr.modelPath}")
                return GameObject(anchorNode, attr)
            }
            val node = ModelNode(modelInstance).apply {
                scale = Scale(attr.scale)
                rotation = Rotation(0f, 0f, 0f)
            }
            anchorNode.addChildNode(node)
            modelNodes.add(node)
            GameObject(node, attr)
        }
    }

    private fun loadGameObjects() {
        firestore.collection("anchors").get().addOnSuccessListener { result ->
            objects = ArrayList()
            for (document in result) {
                val lat = document.getDouble("Latitude")
                val lon = document.getDouble("Longitude")
                val alt = document.getDouble("Altitude")
                val type = document.getString("Type") ?: "Person"
                val icon = document.getString("profile_pic")

                if (lat == null || lon == null || alt == null) continue

                val anchor = arSceneView.session?.earth?.createAnchor(lat, lon, alt, 0f, 0f, 0f, 1f)
                if (anchor != null) {
                    val attr = getAttributes(type, icon)
                    objects.add(loadGameObject(attr, anchor))
                }
            }
            Log.d("ANCHORS", "Loaded ${objects.size} anchor locations")
        }.addOnFailureListener {
            Log.e("ANCHORS", "Failed to load anchors", it)
        }
    }

    private fun loadFace(root: AnchorNode, imgPath: String): ViewNode {
        val node = ViewNode(arSceneView.engine, arSceneView.modelLoader, attachmentManager)
        node.loadView(this, R.layout.face) { _, view ->
            root.addChildNode(node)
            Handler(Looper.getMainLooper()).post {
                val imageView = view.findViewById<ImageView>(R.id.imageView)
                Picasso.get()
                    .load(imgPath)
                    .placeholder(R.drawable.default_pfp)
                    .error(R.drawable.default_pfp)
                    .memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)
                    .networkPolicy(NetworkPolicy.NO_CACHE, NetworkPolicy.NO_STORE)
                    .into(imageView)
            }
        }
        node.scale = Scale(5f)
        node.position = Position(0f, -0.2f, 0f)
        faceNodes.add(node)
        return node
    }

    private fun assetExists(path: String): Boolean {
        return try {
            assets.open(path).close()
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun getAttributes(assetName: String, icon: String?): Attributes {
        val attributes = Attributes()
        attributes.type = assetName

        if (!icon.isNullOrEmpty()) {
            attributes.profilePicture = icon
            attributes.scale = 2f
        } else {
            attributes.scale = 1f
        }

        if (!assetName.equals("Person", ignoreCase = true)) {
            val normalized = assetName.replaceFirstChar { it.uppercaseChar() }
            val modelPath = "models/${normalized}.glb"
            val exists = assetExists(modelPath)
            Log.d("EXISTS", "Checking if $modelPath exists: $exists")
            attributes.modelPath = modelPath
        } else {
            Log.d("EXISTS", "Skipping model load for Person")
        }

        return attributes
    }

    override fun onResume() {
        super.onResume()
        attachmentManager.onResume()
    }

    override fun onPause() {
        attachmentManager.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService(backgroundIntent)
    }
}
