package com.aryoucovered.app.feature.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.aryoucovered.app.R
import com.aryoucovered.app.presentation.activity.MainActivity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

class SigninActivity : AppCompatActivity() {

    private lateinit var phoneInput: EditText
    private lateinit var verifyButton: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var verificationId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sign_in)

        auth = FirebaseAuth.getInstance()
        
        if (auth.currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        phoneInput = findViewById(R.id.phone_input)
        verifyButton = findViewById(R.id.verify_button)

        verifyButton.setOnClickListener {
            val phone = phoneInput.text.toString().trim()
            if (phone.isNotEmpty()) {
                sendVerificationCode("+1$phone")
            } else {
                Toast.makeText(this, "Enter a phone number", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendVerificationCode(phoneNumber: String) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    signInWithCredential(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Toast.makeText(this@SigninActivity, "Verification failed: ${e.message}", Toast.LENGTH_LONG).show()
                }

                override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                    this@SigninActivity.verificationId = verificationId
                    Toast.makeText(this@SigninActivity, "Code sent!", Toast.LENGTH_SHORT).show()

                    val intent = Intent(this@SigninActivity, VerifyCodeActivity::class.java)
                    intent.putExtra("verificationId", verificationId)
                    intent.putExtra("phone", phoneNumber)
                    startActivity(intent)
                }
            })
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun signInWithCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val phone = phoneInput.text.toString()
                    Toast.makeText(this, "Logged in with $phone", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Login failed", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
