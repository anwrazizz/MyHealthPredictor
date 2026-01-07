package com.example.myhealthpredictor.Auth

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myhealthpredictor.R
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var etFullname: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var cbTerms: CheckBox
    private lateinit var btnRegister: Button
    private lateinit var tvLogin: TextView
    private lateinit var progressBar: ProgressBar

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // ✅ Firebase (STABIL, TANPA KTX)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Init views
        btnBack = findViewById(R.id.btn_back)
        etFullname = findViewById(R.id.et_fullname)
        etEmail = findViewById(R.id.et_email)
        etPassword = findViewById(R.id.et_password)
        etConfirmPassword = findViewById(R.id.et_confirm_password)
        cbTerms = findViewById(R.id.cb_terms)
        btnRegister = findViewById(R.id.btn_register)
        tvLogin = findViewById(R.id.tv_login)
        progressBar = findViewById(R.id.progress_bar)

        btnBack.setOnClickListener { finish() }
        tvLogin.setOnClickListener { finish() }

        btnRegister.setOnClickListener {
            val fullname = etFullname.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            if (validateInput(fullname, email, password, confirmPassword)) {
                performRegister(fullname, email, password)
            }
        }
    }

    private fun validateInput(
        fullname: String,
        email: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        if (fullname.length < 3) {
            etFullname.error = "Nama lengkap minimal 3 karakter"
            return false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Format email tidak valid"
            return false
        }

        if (password.length < 6) {
            etPassword.error = "Password minimal 6 karakter"
            return false
        }

        if (password != confirmPassword) {
            etConfirmPassword.error = "Password tidak cocok"
            return false
        }

        if (!cbTerms.isChecked) {
            Toast.makeText(this, "Setujui syarat & ketentuan", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun performRegister(fullname: String, email: String, password: String) {
        setLoading(true)

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser

                    // Update display name
                    val profileUpdates = userProfileChangeRequest {
                        displayName = fullname
                    }

                    user?.updateProfile(profileUpdates)

                    // Simpan ke Firestore
                    saveUserToFirestore(user?.uid ?: "", fullname, email)
                } else {
                    setLoading(false)
                    Toast.makeText(
                        this,
                        task.exception?.message ?: "Registrasi gagal",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun saveUserToFirestore(userId: String, fullname: String, email: String) {
        val userData = hashMapOf(
            "uid" to userId,
            "fullname" to fullname,
            "email" to email,
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("users")
            .document(userId)
            .set(userData)
            .addOnSuccessListener {
                setLoading(false)
                Toast.makeText(
                    this,
                    "Registrasi berhasil, silakan login",
                    Toast.LENGTH_SHORT
                ).show()

                auth.signOut()
                finish()
            }
            .addOnFailureListener {
                setLoading(false)
                Toast.makeText(
                    this,
                    "Gagal menyimpan data user",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun setLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnRegister.isEnabled = !isLoading
        btnRegister.text = if (isLoading) "Memproses..." else "Daftar"
    }
}
