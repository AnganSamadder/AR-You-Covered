package com.aryoucovered.app.feature.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.aryoucovered.app.R
import com.aryoucovered.app.presentation.activity.MainActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class VerifyCodeActivity : AppCompatActivity() {

    private lateinit var codeInput: EditText
    private lateinit var confirmButton: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var verificationId: String
    private lateinit var phone: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.verify_code)

        auth = FirebaseAuth.getInstance()

        verificationId = intent.getStringExtra("verificationId") ?: ""
        phone = intent.getStringExtra("phone") ?: ""

        codeInput = findViewById(R.id.code_input)
        confirmButton = findViewById(R.id.confirm_button)

        confirmButton.setOnClickListener {
            val code = codeInput.text.toString().trim()
            if (code.isNotEmpty()) {
                val credential = PhoneAuthProvider.getCredential(verificationId, code)
                signInWithCredential(credential)
            } else {
                Toast.makeText(this, "Please enter the verification code", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun signInWithCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val db = FirebaseFirestore.getInstance()
                    val usersRef = db.collection("users")

                    usersRef.whereEqualTo("number", phone).get()
                        .addOnSuccessListener { docs ->
                            if (docs.isEmpty) {
                                val intent = Intent(this, SignUpActivity::class.java)
                                intent.putExtra("phone", phone)
                                startActivity(intent)
                                finish()
                            } else {
                                Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, MainActivity::class.java))
                                finish()
                            }
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Error checking user: ${it.message}", Toast.LENGTH_LONG).show()
                        }

                } else {
                    Toast.makeText(this, "Invalid verification code", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
