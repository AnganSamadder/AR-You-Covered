package com.aryoucovered.app.presentation.activity

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.aryoucovered.app.R
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

class SettingsActivity : AppCompatActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private lateinit var nameInput: EditText
    private lateinit var jobInput: EditText
    private lateinit var factInput: EditText
    private lateinit var bioInput: EditText
    private lateinit var profileImage: ImageView

    private lateinit var editNameBtn: ImageButton
    private lateinit var editJobBtn: ImageButton
    private lateinit var editFactBtn: ImageButton
    private lateinit var editBioBtn: ImageButton

    private var selectedImageUri: Uri? = null
    private var cameraImageUri: Uri? = null

    private lateinit var galleryLauncher: ActivityResultLauncher<Intent>
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        nameInput = findViewById(R.id.name_input)
        jobInput = findViewById(R.id.job_input)
        factInput = findViewById(R.id.fact_input)
        bioInput = findViewById(R.id.bio_input)
        profileImage = findViewById(R.id.profile_image)

        editNameBtn = findViewById(R.id.edit_name)
        editJobBtn = findViewById(R.id.edit_job)
        editFactBtn = findViewById(R.id.edit_fact)
        editBioBtn = findViewById(R.id.edit_bio)

        loadUserData()

        editNameBtn.setOnClickListener { enableEdit(nameInput) }
        editJobBtn.setOnClickListener { enableEdit(jobInput) }
        editFactBtn.setOnClickListener { enableEdit(factInput) }
        editBioBtn.setOnClickListener { enableEdit(bioInput) }

        findViewById<Button>(R.id.save_button).setOnClickListener { saveUserData() }
        findViewById<Button>(R.id.add_button).setOnClickListener { getLocationAndSave() }

        galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data?.data != null) {
                selectedImageUri = result.data!!.data
                profileImage.setImageURI(selectedImageUri)
                uploadSelectedProfilePic()
            }
        }

        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (cameraImageUri != null) {
                selectedImageUri = cameraImageUri
                profileImage.setImageURI(cameraImageUri)
                uploadSelectedProfilePic()
            }
        }

        findViewById<Button>(R.id.upload_button).setOnClickListener {
            val options = arrayOf("Choose from Gallery", "Take a Photo")
            AlertDialog.Builder(this)
                .setTitle("Update Profile Picture")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> openGallery()
                        1 -> openCamera()
                    }
                }.show()
        }
    }

    private fun enableEdit(field: EditText) {
        field.isEnabled = true
        field.requestFocus()
        field.backgroundTintList = getColorStateList(android.R.color.white)
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        galleryLauncher.launch(Intent.createChooser(intent, "Select Picture"))
    }

    private fun openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1001)
            return
        }

        try {
            val imagesDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val photoFile = File.createTempFile("profile_", ".jpg", imagesDir)
            cameraImageUri = FileProvider.getUriForFile(this, "$packageName.provider", photoFile)

            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            cameraLauncher.launch(cameraIntent)
        } catch (e: Exception) {
            Toast.makeText(this, "Camera launch failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getImageRotation(uri: Uri): Int {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val exif = ExifInterface(inputStream!!)
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } catch (e: Exception) {
            Log.e("Exif", "Failed to read EXIF: ${e.message}")
            0
        }
    }

    private fun rotateBitmapIfRequired(bitmap: Bitmap, uri: Uri): Bitmap {
        val rotation = getImageRotation(uri).toFloat()
        if (rotation == 0f) return bitmap
        val matrix = Matrix().apply { postRotate(rotation) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun uploadSelectedProfilePic() {
        if (selectedImageUri == null) return

        detectFace(selectedImageUri!!,
            onSuccess = {
                val inputStream = contentResolver.openInputStream(selectedImageUri!!)
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                val rotatedBitmap = rotateBitmapIfRequired(originalBitmap!!, selectedImageUri!!)
                sendToCutoutApi(rotatedBitmap,
                    onSuccess = { cutoutBitmap ->
                        val user = auth.currentUser ?: return@sendToCutoutApi
                        val phone = user.phoneNumber ?: return@sendToCutoutApi
                        uploadCutoutToFirebase(cutoutBitmap, phone)
                    },
                    onError = {
                        Toast.makeText(this, "Cutout API failed: $it", Toast.LENGTH_SHORT).show()
                    }
                )
            },
            onFailure = {
                Toast.makeText(this, "No face detected. Try again.", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun sendToCutoutApi(bitmap: Bitmap, onSuccess: (Bitmap) -> Unit, onError: (String) -> Unit) {
        val apiKey = "05205943f0b94f648ac250a91b1a146b"
        val url = "https://www.cutout.pro/api/v1/matting2?mattingType=3&crop=true&preview=true"

        val file = File.createTempFile("cutout_input", ".jpg", cacheDir)
        val out = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        out.flush()
        out.close()

        val requestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, file.asRequestBody("image/jpeg".toMediaType()))
            .build()

        val request = Request.Builder()
            .url(url)
            .addHeader("APIKEY", apiKey)
            .post(requestBody)
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = onError("${e.message}")

            override fun onResponse(call: Call, response: Response) {
                try {
                    val bodyString = response.body?.string()
                    val json = JSONObject(bodyString ?: "{}")
                    val base64Image = json.getJSONObject("data").getString("imageBase64")
                    val decoded = Base64.decode(base64Image, Base64.DEFAULT)
                    val cutoutBitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
                    runOnUiThread { onSuccess(cutoutBitmap) }
                } catch (e: Exception) {
                    runOnUiThread { onError("${e.message}") }
                }
            }
        })
    }

    private fun uploadCutoutToFirebase(bitmap: Bitmap, phone: String) {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
        val data = baos.toByteArray()

        val fileName = "cutout_${System.currentTimeMillis()}.png"
        val ref = storage.reference.child("profile_pics/$fileName")

        ref.putBytes(data).addOnSuccessListener {
            ref.downloadUrl.addOnSuccessListener { uri ->
                val newPicUrl = uri.toString()

                db.collection("users").document(phone).update("profile_pic", newPicUrl).addOnSuccessListener {
                    profileImage.setImageBitmap(bitmap)
                    Toast.makeText(this, "Photo updated!", Toast.LENGTH_SHORT).show()

                    db.collection("users").document(phone).get().addOnSuccessListener { userDoc ->
                        val phoneNum = userDoc.getString("number") ?: return@addOnSuccessListener
                        db.collection("anchors").whereEqualTo("Number", phoneNum).get().addOnSuccessListener { anchorDocs ->
                            if (!anchorDocs.isEmpty) {
                                val anchorId = anchorDocs.documents[0].id
                                db.collection("anchors").document(anchorId).update("profile_pic", newPicUrl)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun detectFace(uri: Uri, onSuccess: () -> Unit, onFailure: () -> Unit) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            val rotatedBitmap = rotateBitmapIfRequired(originalBitmap!!, uri)
            val image = InputImage.fromBitmap(rotatedBitmap, 0)

            val options = FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .build()

            FaceDetection.getClient(options).process(image)
                .addOnSuccessListener { faces ->
                    if (faces.isNotEmpty()) onSuccess() else onFailure()
                }
                .addOnFailureListener { onFailure() }
        } catch (e: Exception) {
            onFailure()
        }
    }

private fun loadUserData() {
        val user = auth.currentUser ?: return
        val phone = user.phoneNumber ?: return

        db.collection("users").document(phone).get().addOnSuccessListener { doc ->
            nameInput.setText(doc.getString("name") ?: "")
            jobInput.setText(doc.getString("job_title") ?: "")
            factInput.setText(doc.getString("fun_facts") ?: "")
            bioInput.setText(doc.getString("bio") ?: "")

            listOf(nameInput, jobInput, factInput, bioInput).forEach {
                it.isEnabled = false
                it.backgroundTintList = getColorStateList(android.R.color.darker_gray)
            }
        }
    }

    private fun saveUserData() {
        val user = auth.currentUser ?: return
        val phone = user.phoneNumber ?: return

        val updates = mapOf(
            "name" to nameInput.text.toString(),
            "job_title" to jobInput.text.toString(),
            "fun_facts" to factInput.text.toString(),
            "bio" to bioInput.text.toString()
        )

        db.collection("users").document(phone).update(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show()
                listOf(nameInput, jobInput, factInput, bioInput).forEach {
                    it.isEnabled = false
                    it.backgroundTintList = getColorStateList(android.R.color.darker_gray)
                }
            }
    }

    private fun getLocationAndSave() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show()
            return
        }

        val request = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .build()

        fusedLocationClient.getCurrentLocation(request, null).addOnSuccessListener { location ->
            if (location != null) {
                val user = auth.currentUser ?: return@addOnSuccessListener
                val phone = user.phoneNumber ?: return@addOnSuccessListener

                db.collection("users").document(phone).get().addOnSuccessListener { doc ->
                    val picUrl = doc.getString("profile_pic") ?: ""
                    val phoneNum = doc.getString("number") ?: ""

                    val anchorData = hashMapOf(
                        "Type" to "Person",
                        "Number" to phoneNum,
                        "Longitude" to location.longitude,
                        "Latitude" to location.latitude,
                        "Altitude" to location.altitude,
                        "profile_pic" to picUrl
                    )

                    db.collection("anchors").whereEqualTo("Number", phoneNum).get().addOnSuccessListener { querySnapshot ->
                        if (querySnapshot.isEmpty) {
                            db.collection("anchors").add(anchorData)
                        } else {
                            val docId = querySnapshot.documents[0].id
                            db.collection("anchors").document(docId).update(anchorData as Map<String, Any>)
                        }
                    }
                }
            }
        }
    }
}
