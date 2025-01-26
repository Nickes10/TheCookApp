import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.thecookapp.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import okhttp3.*
import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class FirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        remoteMessage.notification?.let {
            showNotification(it.title, it.body)
        }
    }

    private fun showNotification(title: String?, body: String?) {
        val channelId = "default_channel"
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Default Channel",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.mipmap.our_ic_launcher)
            .build()

        notificationManager.notify(0, notification)
    }

    companion object {
        private const val FCM_URL = "https://fcm.googleapis.com/v1/projects/YOUR_PROJECT_ID/messages:send"

        fun sendNotification(token: String, title: String, message: String) {
            val accessToken = "lollo"
            val jsonObject = JSONObject()
            val notification = JSONObject()

            try {
                notification.put("title", title)
                notification.put("body", message)

                // Construct the full message payload for the v1 API
                val messageObject = JSONObject()
                val notificationData = JSONObject()
                notificationData.put("token", token)
                notificationData.put("notification", notification)

                jsonObject.put("message", notificationData)

                val mediaType = "application/json; charset=utf-8".toMediaType()
                val client = OkHttpClient()
                val body = jsonObject.toString().toRequestBody(mediaType)

                // Make the HTTP request to the FCM v1 API
                val request = Request.Builder()
                    .addHeader("Authorization", "Bearer $accessToken")
                    .url(FCM_URL)
                    .post(body)
                    .build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        Log.e("FCM", "Notification failed: ${e.message}")
                    }

                    override fun onResponse(call: Call, response: Response) {
                        if (response.isSuccessful) {
                            Log.d("FCM", "Notification sent successfully: ${response.body?.string()}")
                        } else {
                            Log.e("FCM", "Error sending notification: ${response.message}")
                        }
                    }
                })
            } catch (e: Exception) {
                Log.e("FCM", "Error creating notification payload: ${e.message}")
            }
        }
    }
}
