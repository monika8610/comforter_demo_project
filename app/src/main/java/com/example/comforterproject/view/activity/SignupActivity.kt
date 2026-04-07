package com.example.comforterproject.view.activity

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.comforterproject.databinding.ActivitySignupBinding
import com.example.comforterproject.model.SignupRequest
import com.example.comforterproject.viewmodel.AuthViewModel
class SignupActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignupBinding
    private lateinit var viewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        binding.btnSignup1.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val phone = binding.etPhone.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || phone.isEmpty()
                || password.isEmpty() || confirmPassword.isEmpty()
            ) {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val phoneLong = phone.toString()
            if (phoneLong == null) {
                Toast.makeText(this, "Invalid phone number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val request = SignupRequest(
                email = email,
                password = password,
                name = name,
                phone = phoneLong,
                password_confirmation = confirmPassword
            )

            // 🔥 API call
            viewModel.signup(request)
            println("##########-----------------signup request has been sent")
        }

        // 🔹 Observe response
        viewModel.signupResult.observe(this) { response ->
            if (response.status) {
                binding.tvStatus.text = "✅ ${response.message}"
                binding.tvStatus.setTextColor(getColor(android.R.color.holo_green_dark))

                response.token?.let { token ->
                    saveToken(token)
                    println("TOKEN SAVED: $token")
                }
            } else {
                binding.tvStatus.text = "❌ ${response.message}"
                binding.tvStatus.setTextColor(getColor(android.R.color.holo_red_dark))
            }
        }
    }

    private fun saveToken(token: String) {
        val sharedPref = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putString("USER_TOKEN", token)
        editor.apply()
    }
}