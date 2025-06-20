package com.aryoucovered.app.feature.auth

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.aryoucovered.app.R
import com.aryoucovered.app.presentation.activity.MainActivity
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
import java.io.IOException

class SignUpActivity : AppCompatActivity() {

    private lateinit var nameInput: EditText
    private lateinit var bioInput: EditText
    private lateinit var jobInput: EditText
    private lateinit var factsInput: EditText
    private lateinit var submitButton: Button
    private lateinit var imageView: ImageView
    private lateinit var uploadButton: Button

    private var imageUri: Uri? = null
    private var photoFile: File? = null
    private var faceVerified = false
    private val CAMERA_PERMISSION_CODE = 2001

    private lateinit var phone: String
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && imageUri != null) {
            detectFace(photoFile!!,
                onSuccess = {
                    val rotatedBitmap = rotateBitmapIfRequired(BitmapFactory.decodeFile(photoFile!!.absolutePath), photoFile!!)
                    imageView.setImageBitmap(rotatedBitmap)
                    faceVerified = true
                    submitButton.isEnabled = true
                },
                onFailure = {
                    imageUri = null
                    faceVerified = false
                    imageView.setImageDrawable(null)
                    submitButton.isEnabled = false
                }
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        phone = intent.getStringExtra("phone") ?: ""
        nameInput = findViewById(R.id.name_input)
        bioInput = findViewById(R.id.bio_input)
        jobInput = findViewById(R.id.job_input)
        factsInput = findViewById(R.id.facts_input)
        submitButton = findViewById(R.id.submit_button)
        imageView = findViewById(R.id.profile_image)
        uploadButton = findViewById(R.id.upload_button)
        submitButton.isEnabled = false

        uploadButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
            } else {
                openCamera()
            }
        }

        submitButton.setOnClickListener {
            if (!faceVerified) {
                Toast.makeText(this, "Face not verified", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (photoFile == null || !photoFile!!.exists()) {
                Toast.makeText(this, "Please take a photo first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            sendToCutoutApi(photoFile!!,
                onSuccess = { cutoutBitmap ->
                    uploadToFirebase(cutoutBitmap)
                },
                onError = { errorMsg ->
                    runOnUiThread {
                        Toast.makeText(this, "Cutout API error: $errorMsg", Toast.LENGTH_LONG).show()
                    }
                }
            )
        }
    }

    private fun openCamera() {
        try {
            val imagesDir = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
            photoFile = File.createTempFile("profile_", ".jpg", imagesDir)
            imageUri = FileProvider.getUriForFile(this, "$packageName.provider", photoFile!!)

            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            cameraLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Camera error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun detectFace(file: File, onSuccess: () -> Unit, onFailure: () -> Unit) {
        try {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            val rotation = getImageRotation(file)
            val image = InputImage.fromBitmap(bitmap, rotation)

            val options = FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .build()

            FaceDetection.getClient(options).process(image)
                .addOnSuccessListener { faces ->
                    if (faces.isNotEmpty()) {
                        Log.d("MLKit", "Face detected")
                        onSuccess()
                    } else {
                        Log.d("MLKit", "No face detected")
                        Toast.makeText(this, "No face detected. Please retake photo.", Toast.LENGTH_SHORT).show()
                        onFailure()
                    }
                }
                .addOnFailureListener {
                    Log.e("MLKit", "Detection failed: ${it.message}")
                    Toast.makeText(this, "Face detection failed.", Toast.LENGTH_SHORT).show()
                    onFailure()
                }
        } catch (e: Exception) {
            Log.e("MLKit", "Error decoding image: ${e.message}")
            onFailure()
        }
    }

    private fun getImageRotation(file: File): Int {
        return try {
            val exif = ExifInterface(file.absolutePath)
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

    private fun rotateBitmapIfRequired(bitmap: Bitmap, file: File): Bitmap {
        val rotation = getImageRotation(file).toFloat()
        if (rotation == 0f) return bitmap

        val matrix = Matrix().apply { postRotate(rotation) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun sendToCutoutApi(file: File, onSuccess: (Bitmap) -> Unit, onError: (String) -> Unit) {
        val apiKey = "05205943f0b94f648ac250a91b1a146b"
        val url = "https://www.cutout.pro/api/v1/matting2?mattingType=3&crop=true&preview=true"

        val requestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, file.asRequestBody("image/jpeg".toMediaType()))
            .build()

        val request = Request.Builder()
            .url(url)
            .addHeader("APIKEY", apiKey)
            .post(requestBody)
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onError("Request failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val bodyString = response.body?.string()
                Log.e("CUTOUT_API_RAW", bodyString ?: "null")

                try {
                    val json = JSONObject(bodyString ?: "{}")
                    val code = json.optInt("code", -1)

                    if (code != 0 || !json.has("data")) {
                        val msg = json.optString("msg", "Unknown error")
                        onError("Cutout API failed: $msg")
                        return
                    }

                    val dataObject = json.getJSONObject("data")
                    val base64Image = when {
                        dataObject.has("imageBase64") -> dataObject.getString("imageBase64")
                        dataObject.has("imageBase") -> dataObject.getString("imageBase")
                        else -> {
                            onError("Image not returned in API response.")
                            return
                        }
                    }


                    val decoded = Base64.decode(base64Image, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
                    onSuccess(bitmap)
                } catch (e: Exception) {
                    onError("Failed to parse API response: ${e.message}")
                }
            }
        })
    }

    private fun uploadToFirebase(bitmap: Bitmap) {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
        val data = baos.toByteArray()

        val fileName = "cutout_${System.currentTimeMillis()}.png"
        val ref = FirebaseStorage.getInstance().reference.child("profile_pics/$fileName")

        ref.putBytes(data).addOnSuccessListener {
            ref.downloadUrl.addOnSuccessListener { uri ->
                saveUserToFirestore(uri.toString())
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Upload failed: ${it.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun saveUserToFirestore(imageUrl: String) {
        val userData = hashMapOf(
            "number" to phone,
            "name" to nameInput.text.toString(),
            "bio" to bioInput.text.toString(),
            "job_title" to jobInput.text.toString(),
            "fun_facts" to factsInput.text.toString(),
            "profile_pic" to imageUrl,
            "points" to 0
        )

        db.collection("users").document(phone).set(userData)
            .addOnSuccessListener {
                Toast.makeText(this, "Signed up!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Firestore error: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }
}
