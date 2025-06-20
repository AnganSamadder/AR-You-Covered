package com.aryoucovered.app.feature.ar

import android.location.Location
import android.os.*
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.aryoucovered.app.R
import com.google.ar.core.Config
import com.google.android.filament.EntityManager
import com.google.android.filament.LightManager
import com.google.ar.core.Anchor
import com.google.ar.core.GeospatialPose
import com.google.ar.core.HitResult
import com.google.ar.core.TrackingState
import com.google.firebase.firestore.FirebaseFirestore
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.math.*
import io.github.sceneview.node.*
import com.google.firebase.firestore.DocumentSnapshot
import io.github.sceneview.ar.arcore.isTracking
import io.github.sceneview.ar.node.AnchorNode

class ARActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore

    private lateinit var arSceneView: ARSceneView
    private lateinit var pointsText: TextView
    private lateinit var assetTypeText: TextView
    private lateinit var instructionText: TextView
    private lateinit var catchButton: Button
    private lateinit var placeButton: Button
    private lateinit var switchButton: Button
    private lateinit var backButton: Button


    private var placedModel: Boolean = false
    private var placedAnchor: Boolean = false
    private var trackingEarth: Boolean = false
    private var currentLoc: Location? = null
    private var type = 0

    private var types: ArrayList<String> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        firestore = FirebaseFirestore.getInstance()
        val test = hashMapOf("testField" to "It works!")
        FirebaseFirestore.getInstance().collection("test").add(test)
            .addOnSuccessListener {
                Log.d("FIRESTORE", "Manual write success")
            }.addOnFailureListener {
                Log.e("FIRESTORE", "Manual write fail", it)
            }

        setContentView(R.layout.activity_ar)

        initViews()

        configureSession()

        // Ed, Jake, ford, michael, sewing machine, roller skates
        types.add("Ed")
        types.add("Jake")
        types.add("Ford")
        types.add("Michael")
        types.add("Sewing Machine")
        types.add("Roller Skates")
        assetTypeText.text = types[type]

        //addSimpleLight()

        arSceneView.onSessionUpdated = { session, _ ->
            // Check if Earth is being tracked (API is linked correctly)
            if (!trackingEarth && session.earth?.isTracking == true) {
                Toast.makeText(this, "Tracking Earth!!!", Toast.LENGTH_SHORT).show()
                trackingEarth = true;
            }
        }

    }

    private fun addSimpleLight() {
        val lightEntity = EntityManager.get().create()
        LightManager.Builder(LightManager.Type.DIRECTIONAL)
            .color(1f, 1f, 1f)
            .intensity(100_000f)
            .direction(0f, -1f, -1f)
            .castShadows(false)
            .build(arSceneView.engine, lightEntity)
        arSceneView.scene.addEntity(lightEntity)
    }

    private fun configureSession() {
        try {
            arSceneView.sessionConfiguration = { session, config ->
                val possible = session.isGeospatialModeSupported(Config.GeospatialMode.ENABLED)
                if (possible) {
                    config.setGeospatialMode(Config.GeospatialMode.ENABLED)
                    config.setLightEstimationMode(Config.LightEstimationMode.ENVIRONMENTAL_HDR)
                    println("configured")
                } else {
                    Toast.makeText(this, "Geospatial mode not supported.", Toast.LENGTH_SHORT)
                        .show()
                    finish()
                }
            }
        } catch (e: Exception) {
            println(e)
            Toast.makeText(this, "Error configuring session: ${e.message}", Toast.LENGTH_SHORT)
                .show()
            finish()
        }
    }

    private fun initViews() {
        arSceneView = findViewById(R.id.ar_scene_view)
        arSceneView.onSessionFailed = { e ->
            Toast.makeText(this, "Error configuring session. ${e.message}", Toast.LENGTH_SHORT)
                .show()
            finish()
        }

        pointsText = findViewById(R.id.ar_points_text)
        assetTypeText = findViewById(R.id.asset_type)
        instructionText = findViewById(R.id.ar_instruction_text)
        catchButton = findViewById(R.id.catch_button)
        backButton = findViewById(R.id.back_button)
        placeButton = findViewById(R.id.place_button)
        switchButton = findViewById(R.id.switch_button)

        switchButton.setOnClickListener {
            if (type == types.size - 1) {
                type = 0
            } else {
                type++
            }
            assetTypeText.text = types[type]
        }

        // place anchors on click
        placeButton.setOnClickListener {
            // find the floor
            val hitResult: HitResult? = getHitResult()
            if (hitResult != null) {
                val anchor: Anchor = hitResult.createAnchor()
                placeAnchor(anchor)
                placedAnchor = true
            }
        }

        backButton.setOnClickListener { finish() }
    }

    private fun getHitResult(): HitResult? {
        // Ensure the ARSession is running and has a valid frame
        val frame = arSceneView.frame
        if (frame == null) {
            Toast.makeText(this, "AR session not ready.", Toast.LENGTH_SHORT).show()
            return null
        }

        // Coordinates for the center of the view
        val viewWidth = arSceneView.width
        val viewHeight = arSceneView.height
        val hitResults = frame.hitTest(viewWidth / 2f, viewHeight / 2f)

        if (hitResults.size == 0) {
            return null
        }
        val hitResult = hitResults.first()// {
        //    val trackable = it.trackable
        //    // Check if the hit is on a plane and the plane is being tracked
        //    trackable is Plane && trackable.trackingState == TrackingState.TRACKING
        //}
        if (hitResult.trackable::class.simpleName == "Plane" && hitResult.trackable.trackingState == TrackingState.TRACKING) {
            Toast.makeText(this, "Found anchor point.", Toast.LENGTH_SHORT).show()
            return hitResult
        }

        return null
    }

    private fun placeModel(modelPath: String, position: Position) {
        val modelLoader = ModelLoader(arSceneView.engine, this)
        val modelInstance = modelLoader.createModelInstance(modelPath)

        val node = ModelNode(modelInstance, scaleToUnits = .1f)
        node.position = Position(position.x, position.y, position.z)

        arSceneView.addChildNode(node)
        //catchButton.visibility = Button.VISIBLE
    }

    private fun placeModelWithAnchor(modelPath: String, position: Position, anchor: Anchor) {
        val modelLoader = ModelLoader(arSceneView.engine, this)
        val modelInstance = modelLoader.createModelInstance(modelPath)

        val node = ModelNode(modelInstance, scaleToUnits = .1f)
        node.position = Position(position.x, position.y, position.z)

        val anchorNode = AnchorNode(arSceneView.engine, anchor)
        anchorNode.addChildNode(node)
        arSceneView.addChildNode(anchorNode)
    }

    private fun getGeoPose(anchor: Anchor): GeospatialPose? {
        // convert local anchor to geospatial anchor
        try {
            return arSceneView.session?.earth?.getGeospatialPose(anchor.pose)
        } catch (e: Exception) {
            val state = arSceneView.session?.earth?.earthState
            println(state) // debug this line to see what's wrong
            return null
        }
    }

    private fun placeAnchor(anchor: Anchor) {
        val modelLoader = ModelLoader(arSceneView.engine, this)
        var modelInstance = modelLoader.createModelInstance("models/ColoredSphere.glb")
        val anchorNode = AnchorNode(arSceneView.engine, anchor)

        //anchorNode.addChildNode(ModelNode(modelInstance, scaleToUnits = .1f))
        arSceneView.addChildNode(anchorNode)

        val geoPose = getGeoPose(anchor)
        if (geoPose != null) {
            placeAnchor(geoPose)
            print(geoPose)
        }
    }

    private fun placeAnchor(pose: GeospatialPose) {
        val anchor = arSceneView.session?.earth?.createAnchor(
            pose.latitude, pose.longitude, pose.altitude, pose.eastUpSouthQuaternion)
        if (anchor != null) {
            val modelLoader = ModelLoader(arSceneView.engine, this)
            val modelInstance = modelLoader.createModelInstance("models/ColoredSphere.glb")
            val anchorNode = AnchorNode(arSceneView.engine, anchor)

            anchorNode.addChildNode(ModelNode(modelInstance, scaleToUnits = .1f))
            arSceneView.addChildNode(anchorNode)
            saveAnchorToFirestore(pose)
            print(pose) // this is what we'll store as it contains the lat/long/alt/orientation of the anchor
        }
    }
    //Adding comment
    private fun saveAnchorToFirestore(pose: GeospatialPose) {
        val counterRef = firestore.collection("metadata").document("counters")
        val anchorsRef = firestore.collection("anchors")

        firestore.runTransaction { transaction ->
            val snapshot: DocumentSnapshot = transaction.get(counterRef)
            val currentCount = snapshot.getLong("anchorCounter") ?: 0
            val nextCount = currentCount + 1
            val anchorId = "anchor$nextCount"

            val anchorData = hashMapOf(
                "Latitude" to pose.latitude,
                "Longitude" to pose.longitude,
                "Altitude" to pose.altitude,
                "Type" to assetTypeText.text,
            )

            transaction.set(anchorsRef.document(anchorId), anchorData)
            transaction.update(counterRef, "anchorCounter", nextCount)
            anchorId
        }.addOnSuccessListener { anchorName ->
            Toast.makeText(this, "$anchorName saved to Firestore!", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Failed to save anchor: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

}