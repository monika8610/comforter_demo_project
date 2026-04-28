package com.example.comforterproject.view.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.comforterproject.databinding.ActivityLoginBinding
import com.example.comforterproject.model.LoginRequest
import com.example.comforterproject.utils.PrefManager
import com.example.comforterproject.viewmodel.AuthViewModel
class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var viewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 🔹 Initialize binding
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        binding.btnLogin1.setOnClickListener {

            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Enter email & password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val request = LoginRequest(
                email = email,
                password = password
            )

            viewModel.login(request)
        }

        viewModel.loginResult.observe(this) { response ->

            Toast.makeText(this, response.message, Toast.LENGTH_SHORT).show()

            if (response.status) {
                response.token?.let { token ->
                    PrefManager(this).saveToken(token)
                }

                val intent = Intent(this, SplashActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
        }
    }
}
