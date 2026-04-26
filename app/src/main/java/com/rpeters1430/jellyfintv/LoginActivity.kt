package com.rpeters1430.jellyfintv

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.rpeters1430.jellyfintv.databinding.ActivityLoginBinding
import com.rpeters1430.jellyfintv.viewmodel.LoginViewModel

/**
 * First screen the user sees. Allows entering a Jellyfin server URL and credentials.
 * If a valid session already exists in preferences, the user is sent directly to [MainActivity].
 */
class LoginActivity : FragmentActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var viewModel: LoginViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as App
        viewModel = LoginViewModel(app.prefs, app.repository)

        // Try to resume an existing session before showing the UI
        viewModel.tryResumeSession()

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupObservers()
        setupClickListeners()
    }

    private fun setupObservers() {
        viewModel.state.observe(this) { state ->
            when (state) {
                is LoginViewModel.LoginState.Idle -> setUiEnabled(true)

                is LoginViewModel.LoginState.CheckingServer -> {
                    setUiEnabled(false)
                    showStatus("Connecting to server…")
                }

                is LoginViewModel.LoginState.ServerOk -> {
                    setUiEnabled(true)
                    showStatus("Server found! Enter your credentials.")
                    binding.loginGroup.visibility = android.view.View.VISIBLE
                    binding.btnConnect.visibility = android.view.View.GONE
                }

                is LoginViewModel.LoginState.LoggingIn -> {
                    setUiEnabled(false)
                    showStatus("Signing in…")
                }

                is LoginViewModel.LoginState.Success -> {
                    navigateToMain()
                }

                is LoginViewModel.LoginState.Error -> {
                    setUiEnabled(true)
                    showStatus("")
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnConnect.setOnClickListener {
            val url = binding.etServerUrl.text.toString().trim()
            if (url.isEmpty()) {
                Toast.makeText(this, "Please enter the server URL", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.connectToServer(url)
        }

        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString()
            if (username.isEmpty()) {
                Toast.makeText(this, "Please enter your username", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.login(username, password)
        }
    }

    private fun setUiEnabled(enabled: Boolean) {
        binding.etServerUrl.isEnabled = enabled
        binding.btnConnect.isEnabled = enabled
        binding.etUsername.isEnabled = enabled
        binding.etPassword.isEnabled = enabled
        binding.btnLogin.isEnabled = enabled
        binding.progressBar.visibility =
            if (enabled) android.view.View.GONE else android.view.View.VISIBLE
    }

    private fun showStatus(message: String) {
        binding.tvStatus.text = message
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
