package com.kucingsayabesar.interceptor

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val tokenEditText: EditText = findViewById(R.id.tokenEditText)
        val chatIdEditText: EditText = findViewById(R.id.chatIdEditText)
        val saveButton: Button = findViewById(R.id.saveButton)
        val helpButton: ImageButton = findViewById(R.id.helpButton) // добавим в layout

        val masterKey = MasterKey.Builder(this)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val prefs = EncryptedSharedPreferences.create(
            this,
            "secureBotSettings",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        tokenEditText.setText(prefs.getString("token", ""))
        chatIdEditText.setText(prefs.getString("chat_id", ""))

        saveButton.setOnClickListener {
            val token = tokenEditText.text.toString().trim()
            val chatId = chatIdEditText.text.toString().trim()

            if (token.isEmpty() || chatId.isEmpty()) {
                Toast.makeText(this, "Поля не могут быть пустыми", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            prefs.edit()
                .putString("token", token)
                .putString("chat_id", chatId)
                .apply()

            Toast.makeText(this, "Настройки сохранены безопасно", Toast.LENGTH_SHORT).show()
        }

        // Показать неоновую инструкцию при первом запуске
        val firstRun = prefs.getBoolean("firstRun", true)
        if (firstRun) {
            showNeonDialog()
            prefs.edit().putBoolean("firstRun", false).apply()
        }

        // Или вручную по кнопке “?”
        helpButton.setOnClickListener { showNeonDialog() }
    }

    private fun showNeonDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_neon_help, null)

        val builder = AlertDialog.Builder(this, R.style.NeonDialog)
            .setView(dialogView)

        val dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialogView.findViewById<Button>(R.id.closeButton).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}
